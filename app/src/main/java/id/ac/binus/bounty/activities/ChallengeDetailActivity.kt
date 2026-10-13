package id.ac.binus.bounty.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import id.ac.binus.bounty.R
import id.ac.binus.bounty.database.AppDatabase
import id.ac.binus.bounty.models.Challenge
import id.ac.binus.bounty.models.User
import id.ac.binus.bounty.utils.ActionConfirmation
import id.ac.binus.bounty.utils.ChallengeDisplay
import id.ac.binus.bounty.utils.ScreenInsets
import id.ac.binus.bounty.utils.SessionManager

class ChallengeDetailActivity : AppCompatActivity() {
    private var request = 0
    private var currentChallenge: Challenge? = null
    private lateinit var acceptModel: AcceptModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge_detail)
        ScreenInsets.apply(this)
        acceptModel = ViewModelProvider(this)[AcceptModel::class.java]
        supportFragmentManager.setFragmentResultListener("confirm_accept", this) { _, data ->
            val user = SessionManager(this).getCurrentUser()
            if (user != null && user.id == data.getString("actor")) {
                acceptModel.accept(AppDatabase.getInstance(this), data.getInt("challenge"), user)
            } else {
                Snackbar.make(findViewById(R.id.main), R.string.accept_unavailable, Snackbar.LENGTH_LONG).show()
            }
        }
        findViewById<View>(R.id.accept_button).setOnClickListener { view ->
            val user = SessionManager(this).getCurrentUser()
            val challenge = currentChallenge ?: return@setOnClickListener
            if (!view.isEnabled || user == null) return@setOnClickListener
            val data = Bundle().apply {
                putInt("challenge", challenge.id)
                putString("actor", user.id)
            }
            ActionConfirmation.show(
                supportFragmentManager, "confirm_accept", getString(R.string.confirm_accept_title),
                getString(R.string.confirm_accept_message, challenge.title, ChallengeDisplay.rupiah(this, challenge.reward)),
                getString(R.string.accept_action), data
            )
        }
        findViewById<View>(R.id.submit_proof_button).setOnClickListener {
            val id = currentChallenge?.id ?: return@setOnClickListener
            startActivity(Intent(this, SubmitProofActivity::class.java).putExtra(EXTRA_CHALLENGE_ID, id))
        }
        findViewById<View>(R.id.review_proof_button).setOnClickListener {
            val id = currentChallenge?.id ?: return@setOnClickListener
            startActivity(Intent(this, ReviewProofActivity::class.java).putExtra(EXTRA_CHALLENGE_ID, id))
        }
        acceptModel.state.observe(this) { state ->
            bindAccept()
            if (state != AcceptModel.IDLE && state != AcceptModel.SAVING) {
                val message = when (state) {
                    AcceptModel.SAVED -> R.string.challenge_accepted
                    AcceptModel.UNAVAILABLE -> R.string.accept_unavailable
                    else -> R.string.accept_error
                }
                acceptModel.state.value = AcceptModel.IDLE
                loadChallenge()
                Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG).show()
            }
        }
        findViewById<MaterialToolbar>(R.id.detail_toolbar).setNavigationOnClickListener { finish() }
        findViewById<View>(R.id.detail_retry).setOnClickListener { loadChallenge() }
    }

    override fun onStart() {
        super.onStart()
        if (!SessionManager(this).isFirebaseAuthenticated()) {
            startActivity(Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
            return
        }
        loadChallenge()
    }

    private fun loadChallenge() {
        val currentRequest = ++request
        findViewById<View>(R.id.detail_loading).visibility = View.VISIBLE
        findViewById<View>(R.id.detail_content).visibility = View.GONE
        findViewById<View>(R.id.detail_message).visibility = View.GONE
        findViewById<View>(R.id.detail_retry).visibility = View.GONE
        val id = intent.getIntExtra(EXTRA_CHALLENGE_ID, -1)
        if (id <= 0) {
            showMessage(R.string.challenge_not_found, false)
            return
        }
        val db = AppDatabase.getInstance(this)
        db.queryExecutor.execute {
            try {
                val challenge = db.challengeDao().getChallengeById(id)
                runOnUiThread {
                    if (isDestroyed || isFinishing || currentRequest != request) return@runOnUiThread
                    if (challenge == null) {
                        showMessage(R.string.challenge_not_found, false)
                        return@runOnUiThread
                    }
                    currentChallenge = challenge
                    bindAccept()
                    val content = findViewById<View>(R.id.detail_content)
                    ChallengeDisplay.bind(content, challenge)
                    content.findViewById<TextView>(R.id.challenge_description).text = challenge.description
                    findViewById<View>(R.id.detail_loading).visibility = View.GONE
                    content.visibility = View.VISIBLE
                }
            } catch (error: RuntimeException) {
                Log.e("BountyDetail", "Unable to load challenge $id", error)
                runOnUiThread {
                    if (isDestroyed || isFinishing || currentRequest != request) return@runOnUiThread
                    showMessage(R.string.challenge_load_error, true)
                    Snackbar.make(findViewById(R.id.main), R.string.challenge_load_error, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun bindAccept() {
        val user = SessionManager(this).getCurrentUser()
        val challenge = currentChallenge
        val button = findViewById<MaterialButton>(R.id.accept_button)
        val saving = acceptModel.state.value == AcceptModel.SAVING
        val own = challenge != null && user != null && challenge.creatorId == user.id
        val available = challenge != null && challenge.status == Challenge.OPEN && challenge.participantId == null
        button.isEnabled = user != null && available && !own && !saving
        button.setText(if (saving) R.string.accepting_challenge else R.string.accept_action)
        findViewById<View>(R.id.submit_proof_button).visibility =
            if (challenge != null && user != null && challenge.status == Challenge.ACCEPTED && user.id == challenge.participantId) View.VISIBLE else View.GONE
        findViewById<View>(R.id.review_proof_button).visibility =
            if (own && challenge?.status == Challenge.SUBMITTED) View.VISIBLE else View.GONE
        val result = findViewById<TextView>(R.id.reward_result)
        val completed = challenge?.status == Challenge.COMPLETED
        result.visibility = if (completed) View.VISIBLE else View.GONE
        if (completed) {
            val completedChallenge = requireNotNull(challenge)
            val paid = SessionManager(this).isRewardCredited(completedChallenge.id, completedChallenge.participantId)
            result.text = if (paid) getString(
                R.string.reward_result_paid,
                ChallengeDisplay.rupiah(this, completedChallenge.reward), completedChallenge.participantName
            ) else getString(R.string.reward_pending)
        }
        val message = findViewById<TextView>(R.id.accept_message)
        message.text = if (challenge?.participantId != null) {
            getString(R.string.accept_hunter, challenge.participantName)
        } else {
            getString(if (own) R.string.accept_own else if (available) R.string.accept_ready else R.string.accept_unavailable)
        }
    }

    private fun showMessage(text: Int, retry: Boolean) {
        findViewById<View>(R.id.detail_loading).visibility = View.GONE
        findViewById<TextView>(R.id.detail_message).apply {
            setText(text)
            visibility = View.VISIBLE
        }
        findViewById<View>(R.id.detail_retry).visibility = if (retry) View.VISIBLE else View.GONE
    }

    class AcceptModel : ViewModel() {
        val state = MutableLiveData(IDLE)

        fun accept(db: AppDatabase, id: Int, user: User) {
            if (state.value != IDLE) return
            state.value = SAVING
            db.transactionExecutor.execute {
                try {
                    state.postValue(if (db.challengeDao().acceptChallenge(id, user.id, user.name) == 1) SAVED else UNAVAILABLE)
                } catch (error: RuntimeException) {
                    Log.e("BountyAccept", "Unable to accept challenge", error)
                    state.postValue(ERROR)
                }
            }
        }

        companion object {
            const val IDLE = 0
            const val SAVING = 1
            const val SAVED = 2
            const val UNAVAILABLE = 3
            const val ERROR = 4
        }
    }

    companion object {
        const val EXTRA_CHALLENGE_ID = "challenge_id"
    }
}
