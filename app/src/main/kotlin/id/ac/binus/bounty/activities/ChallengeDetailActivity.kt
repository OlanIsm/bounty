package id.ac.binus.bounty.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import id.ac.binus.bounty.R
import id.ac.binus.bounty.data.FirebaseBountyRepository
import id.ac.binus.bounty.models.Challenge
import id.ac.binus.bounty.models.User
import id.ac.binus.bounty.utils.ActionConfirmation
import id.ac.binus.bounty.utils.ChallengeDisplay
import id.ac.binus.bounty.utils.ScreenInsets
import id.ac.binus.bounty.utils.SessionManager
import id.ac.binus.bounty.utils.ViewMotion

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
                acceptModel.accept(data.getString("challenge").orEmpty(), user)
            } else {
                Snackbar.make(findViewById(R.id.main), R.string.accept_unavailable, Snackbar.LENGTH_LONG).show()
            }
        }
        supportFragmentManager.setFragmentResultListener("confirm_cancel", this) { _, data ->
            val user = SessionManager(this).getCurrentUser()
            if (user != null && user.id == data.getString("actor")) {
                acceptModel.cancel(data.getString("challenge").orEmpty(), user)
            } else {
                Snackbar.make(findViewById(R.id.main), R.string.accept_unavailable, Snackbar.LENGTH_LONG).show()
            }
        }
        val acceptBtn = findViewById<MaterialButton>(R.id.accept_button)
        ViewMotion.applySpringPress(acceptBtn) {
            val user = SessionManager(this).getCurrentUser()
            val challenge = currentChallenge ?: return@applySpringPress
            if (!acceptBtn.isEnabled || user == null) return@applySpringPress
            val isHunter = challenge.status == Challenge.ACCEPTED && challenge.participantId == user.id
            val data = Bundle().apply {
                putString("challenge", challenge.cloudId)
                putString("actor", user.id)
            }
            if (isHunter) {
                ActionConfirmation.show(
                    supportFragmentManager, "confirm_cancel", getString(R.string.confirm_cancel_title),
                    getString(R.string.confirm_cancel_message),
                    getString(R.string.cancel_challenge_action), data
                )
            } else {
                ActionConfirmation.show(
                    supportFragmentManager, "confirm_accept", getString(R.string.confirm_accept_title),
                    getString(R.string.confirm_accept_message, challenge.title, ChallengeDisplay.rupiah(this, challenge.reward)),
                    getString(R.string.accept_action), data
                )
            }
        }
        val submitProofBtn = findViewById<View>(R.id.submit_proof_button)
        ViewMotion.applySpringPress(submitProofBtn) {
            val id = currentChallenge?.cloudId ?: return@applySpringPress
            startActivity(Intent(this, SubmitProofActivity::class.java).putExtra(EXTRA_CLOUD_CHALLENGE_ID, id))
        }
        val reviewProofBtn = findViewById<View>(R.id.review_proof_button)
        ViewMotion.applySpringPress(reviewProofBtn) {
            val id = currentChallenge?.cloudId ?: return@applySpringPress
            startActivity(Intent(this, ReviewProofActivity::class.java).putExtra(EXTRA_CLOUD_CHALLENGE_ID, id))
        }
        acceptModel.state.observe(this) { state ->
            bindAccept()
            if (state != AcceptModel.IDLE && state != AcceptModel.SAVING && state != AcceptModel.CANCELING) {
                val message = when (state) {
                    AcceptModel.SAVED -> R.string.challenge_accepted
                    AcceptModel.CANCELED -> R.string.challenge_canceled
                    AcceptModel.UNAVAILABLE -> R.string.accept_unavailable
                    else -> R.string.accept_error
                }
                if (state == AcceptModel.SAVED) {
                    showAcceptSuccessPopup()
                } else {
                    Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG).show()
                }
                acceptModel.state.value = AcceptModel.IDLE
                loadChallenge()
            }
        }
        findViewById<MaterialToolbar>(R.id.detail_toolbar).setNavigationOnClickListener { finish() }
        val retryBtn = findViewById<View>(R.id.detail_retry)
        ViewMotion.applySpringPress(retryBtn) { loadChallenge() }

        val letsGoBtn = findViewById<View>(R.id.success_lets_go_button)
        if (letsGoBtn != null) {
            ViewMotion.applySpringPress(letsGoBtn) { hideAcceptSuccessPopup() }
        }
        val overlay = findViewById<View>(R.id.accept_success_overlay)
        val contentContainer = findViewById<View>(R.id.success_content_container)
        overlay?.setOnClickListener { hideAcceptSuccessPopup() }
        contentContainer?.setOnClickListener { /* prevent closing on modal content tap */ }

        onBackPressedDispatcher.addCallback(this) {
            if (overlay?.visibility == View.VISIBLE) {
                hideAcceptSuccessPopup()
            } else {
                finish()
            }
        }
    }

    private fun showAcceptSuccessPopup() {
        val overlay = findViewById<View>(R.id.accept_success_overlay) ?: return
        val card = findViewById<View>(R.id.success_content_container) ?: overlay
        val burst = findViewById<View>(R.id.success_burst_glow)
        val mascot = findViewById<View>(R.id.success_mascot_view)

        overlay.alpha = 0f
        overlay.visibility = View.VISIBLE
        overlay.animate().alpha(1f).setDuration(260).start()

        card.scaleX = 0.8f
        card.scaleY = 0.8f
        card.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(360)
            .setInterpolator(OvershootInterpolator(1.3f))
            .start()

        if (burst != null && mascot != null) {
            ViewMotion.startSuccessBurstAnimation(burst, mascot)
        }
    }

    private fun hideAcceptSuccessPopup() {
        val overlay = findViewById<View>(R.id.accept_success_overlay) ?: return
        val card = findViewById<View>(R.id.success_content_container) ?: overlay
        card.animate()
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(180)
            .setInterpolator(DecelerateInterpolator())
            .start()

        overlay.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                overlay.visibility = View.GONE
            }
            .start()
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
        val id = intent.getStringExtra(EXTRA_CLOUD_CHALLENGE_ID)
        if (id.isNullOrBlank()) {
            showMessage(R.string.challenge_not_found, false)
            return
        }
        FirebaseBountyRepository.getChallenge(id) { result ->
            if (isDestroyed || isFinishing || currentRequest != request) return@getChallenge
            result.onSuccess { challenge ->
                if (challenge == null) {
                    showMessage(R.string.challenge_not_found, false)
                    return@onSuccess
                }
                currentChallenge = challenge
                bindAccept()
                val content = findViewById<View>(R.id.detail_content)
                ChallengeDisplay.bind(content, challenge)
                content.findViewById<TextView>(R.id.challenge_description).text = challenge.description
                findViewById<View>(R.id.detail_loading).visibility = View.GONE
                content.visibility = View.VISIBLE
                ViewMotion.animateEntrance(content)
            }.onFailure { error ->
                Log.e("BountyDetail", "Unable to load challenge $id", error)
                showMessage(R.string.challenge_load_error, true)
                Snackbar.make(findViewById(R.id.main), R.string.challenge_load_error, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun bindAccept() {
        val user = SessionManager(this).getCurrentUser()
        val challenge = currentChallenge
        val button = findViewById<MaterialButton>(R.id.accept_button)
        val saving = acceptModel.state.value == AcceptModel.SAVING
        val canceling = acceptModel.state.value == AcceptModel.CANCELING
        val own = challenge != null && user != null && challenge.creatorId == user.id
        val available = challenge != null && challenge.status == Challenge.OPEN && challenge.participantId == null
        val isHunter = challenge != null && user != null && challenge.status == Challenge.ACCEPTED && user.id == challenge.participantId

        if (isHunter) {
            button.isEnabled = !canceling
            button.setBackgroundResource(R.drawable.bg_btn_duolingo_3d_red)
            button.setText(if (canceling) R.string.canceling_challenge else R.string.cancel_challenge_action)
        } else {
            button.setBackgroundResource(R.drawable.bg_btn_duolingo_3d_primary)
            button.isEnabled = user != null && available && !own && !saving
            button.setText(if (saving) R.string.accepting_challenge else R.string.accept_action)
        }

        findViewById<View>(R.id.submit_proof_button).visibility =
            if (isHunter) View.VISIBLE else View.GONE
        findViewById<View>(R.id.review_proof_button).visibility =
            if (own && challenge?.status == Challenge.SUBMITTED) View.VISIBLE else View.GONE
        val result = findViewById<TextView>(R.id.reward_result)
        val completed = challenge?.status == Challenge.COMPLETED
        result.visibility = if (completed) View.VISIBLE else View.GONE
        if (completed) {
            val completedChallenge = requireNotNull(challenge)
            result.text = getString(
                R.string.reward_result_paid,
                ChallengeDisplay.rupiah(this, completedChallenge.reward), completedChallenge.participantName
            )
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

        fun accept(id: String, user: User) {
            if (state.value != IDLE) return
            state.value = SAVING
            FirebaseBountyRepository.acceptChallenge(id, user.id, user.name) { result ->
                result.onSuccess { state.value = if (it) SAVED else UNAVAILABLE }
                    .onFailure { error ->
                        Log.e("BountyAccept", "Unable to accept challenge", error)
                        state.value = ERROR
                    }
            }
        }

        fun cancel(id: String, user: User) {
            if (state.value != IDLE) return
            state.value = CANCELING
            FirebaseBountyRepository.cancelChallenge(id, user.id) { result ->
                result.onSuccess { state.value = if (it) CANCELED else UNAVAILABLE }
                    .onFailure { error ->
                        Log.e("BountyAccept", "Unable to cancel challenge", error)
                        state.value = ERROR
                    }
            }
        }

        companion object {
            const val IDLE = 0
            const val SAVING = 1
            const val SAVED = 2
            const val UNAVAILABLE = 3
            const val ERROR = 4
            const val CANCELING = 5
            const val CANCELED = 6
        }
    }

    companion object {
        const val EXTRA_CHALLENGE_ID = "challenge_id"
        const val EXTRA_CLOUD_CHALLENGE_ID = "cloud_challenge_id"
    }
}
