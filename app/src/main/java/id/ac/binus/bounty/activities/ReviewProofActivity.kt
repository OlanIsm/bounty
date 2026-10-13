package id.ac.binus.bounty.activities

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import id.ac.binus.bounty.R
import id.ac.binus.bounty.database.AppDatabase
import id.ac.binus.bounty.models.Challenge
import id.ac.binus.bounty.models.Proof
import id.ac.binus.bounty.models.User
import id.ac.binus.bounty.utils.ActionConfirmation
import id.ac.binus.bounty.utils.ChallengeDisplay
import id.ac.binus.bounty.utils.ScreenInsets
import id.ac.binus.bounty.utils.SessionManager

class ReviewProofActivity : AppCompatActivity() {
    private lateinit var model: ReviewModel
    private var currentProof: Proof? = null
    private var currentChallenge: Challenge? = null
    private var imageReady = false
    private var request = 0

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_review_proof)
        ScreenInsets.apply(this)
        model = ViewModelProvider(this)[ReviewModel::class.java]
        findViewById<MaterialToolbar>(R.id.review_toolbar).setNavigationOnClickListener { finish() }
        findViewById<View>(R.id.review_retry).setOnClickListener { loadReview() }
        findViewById<View>(R.id.review_photo_retry).setOnClickListener { loadReview() }
        supportFragmentManager.setFragmentResultListener("confirm_approve", this) { _, data ->
            val session = SessionManager(applicationContext)
            val user = session.getCurrentUser()
            if (user == null || user.id != data.getString("actor")) return@setFragmentResultListener
            val proof = Proof().apply {
                id = data.getInt("proof")
                challengeId = data.getInt("challenge")
                hunterId = data.getString("hunter").orEmpty()
            }
            model.review(AppDatabase.getInstance(this), proof, user.id, true, session)
        }
        findViewById<View>(R.id.review_approve).setOnClickListener { confirmApproval() }
        findViewById<View>(R.id.review_reject).setOnClickListener { review(false) }
    }

    override fun onPostCreate(state: Bundle?) {
        super.onPostCreate(state)
        model.state.observe(this) { result ->
            updateControls()
            if (result == ReviewModel.APPROVED || result == ReviewModel.REJECTED || result == ReviewModel.REWARD_PENDING) {
                val message = when (result) {
                    ReviewModel.APPROVED -> R.string.reward_approved
                    ReviewModel.REWARD_PENDING -> R.string.reward_pending
                    else -> R.string.review_rejected
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                finish()
            } else if (result == ReviewModel.ERROR || result == ReviewModel.UNAVAILABLE) {
                Snackbar.make(
                    findViewById(R.id.main),
                    if (result == ReviewModel.ERROR) R.string.review_save_error else R.string.review_unavailable,
                    Snackbar.LENGTH_LONG
                ).show()
                model.state.value = ReviewModel.IDLE
                loadReview()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!SessionManager(this).isFirebaseAuthenticated()) {
            startActivity(Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
            return
        }
        loadReview()
    }

    private fun loadReview() {
        val current = ++request
        currentProof = null
        currentChallenge = null
        imageReady = false
        updateControls()
        findViewById<View>(R.id.review_loading).visibility = View.VISIBLE
        findViewById<View>(R.id.review_content).visibility = View.GONE
        findViewById<View>(R.id.review_message).visibility = View.GONE
        findViewById<View>(R.id.review_retry).visibility = View.GONE
        val user = SessionManager(this).getCurrentUser() ?: return
        val id = intent.getIntExtra(ChallengeDetailActivity.EXTRA_CHALLENGE_ID, -1)
        val db = AppDatabase.getInstance(this)
        db.queryExecutor.execute {
            try {
                val challenge = db.challengeDao().getChallengeById(id)
                val proof = db.proofDao().getProofByChallengeId(id)
                runOnUiThread {
                    if (isDestroyed || isFinishing || current != request) return@runOnUiThread
                    when {
                        challenge == null -> showMessage(R.string.challenge_not_found, false)
                        user.id != challenge.creatorId || challenge.status != Challenge.SUBMITTED ->
                            showMessage(R.string.review_unavailable, false)
                        proof == null -> showMessage(R.string.review_missing_proof, true)
                        proof.status != Proof.PENDING || proof.hunterId != challenge.participantId ->
                            showMessage(R.string.review_unavailable, false)
                        else -> {
                            currentProof = proof
                            currentChallenge = challenge
                            findViewById<TextView>(R.id.review_title).text = challenge.title
                            findViewById<TextView>(R.id.review_hunter).text =
                                getString(R.string.accept_hunter, challenge.participantName)
                            findViewById<TextView>(R.id.review_description).text = proof.description
                            ChallengeDisplay.bindStatus(findViewById(R.id.review_status), proof.status)
                            findViewById<View>(R.id.review_loading).visibility = View.GONE
                            findViewById<View>(R.id.review_content).visibility = View.VISIBLE
                            showImage(proof, current)
                            updateControls()
                        }
                    }
                }
            } catch (error: RuntimeException) {
                Log.e("BountyReview", "Unable to load proof", error)
                runOnUiThread {
                    if (!isDestroyed && !isFinishing && current == request) showMessage(R.string.challenge_load_error, true)
                }
            }
        }
    }

    private fun showImage(proof: Proof, current: Int) {
        val photo = findViewById<ImageView>(R.id.review_photo)
        photo.visibility = View.VISIBLE
        findViewById<View>(R.id.review_photo_error).visibility = View.GONE
        findViewById<View>(R.id.review_photo_retry).visibility = View.GONE
        Glide.with(this).load(Uri.parse(proof.imageUri)).listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(error: GlideException?, model: Any?, target: Target<Drawable>?, first: Boolean): Boolean {
                if (!isDestroyed && current == request) {
                    imageReady = false
                    photo.visibility = View.GONE
                    findViewById<View>(R.id.review_photo_error).visibility = View.VISIBLE
                    findViewById<View>(R.id.review_photo_retry).visibility = View.VISIBLE
                    updateControls()
                }
                return false
            }

            override fun onResourceReady(
                resource: Drawable, model: Any?, target: Target<Drawable>?, dataSource: DataSource, first: Boolean
            ): Boolean {
                if (!isDestroyed && current == request) {
                    imageReady = true
                    updateControls()
                }
                return false
            }
        }).into(photo)
    }

    private fun showMessage(text: Int, retry: Boolean) {
        findViewById<View>(R.id.review_loading).visibility = View.GONE
        findViewById<View>(R.id.review_content).visibility = View.GONE
        findViewById<TextView>(R.id.review_message).apply {
            setText(text)
            visibility = View.VISIBLE
        }
        findViewById<View>(R.id.review_retry).visibility = if (retry) View.VISIBLE else View.GONE
    }

    private fun updateControls() {
        val saving = model.state.value == ReviewModel.SAVING
        val ready = currentProof != null && model.state.value == ReviewModel.IDLE
        findViewById<View>(R.id.review_approve).isEnabled = ready && imageReady
        findViewById<View>(R.id.review_reject).isEnabled = ready
        findViewById<View>(R.id.review_photo_retry).isEnabled = !saving
        findViewById<View>(R.id.review_saving).visibility = if (saving) View.VISIBLE else View.GONE
    }

    private fun confirmApproval() {
        val user = SessionManager(this).getCurrentUser() ?: return
        val proof = currentProof ?: return
        val challenge = currentChallenge ?: return
        if (!imageReady || model.state.value != ReviewModel.IDLE) return
        val data = Bundle().apply {
            putInt("proof", proof.id)
            putInt("challenge", proof.challengeId)
            putString("hunter", proof.hunterId)
            putString("actor", user.id)
        }
        ActionConfirmation.show(
            supportFragmentManager, "confirm_approve", getString(R.string.confirm_approve_title),
            getString(R.string.confirm_approve_message, challenge.title,
                ChallengeDisplay.rupiah(this, challenge.reward), challenge.participantName),
            getString(R.string.approve_proof_action), data
        )
    }

    private fun review(approve: Boolean) {
        val proof = currentProof ?: return
        if (approve && !imageReady) return
        val user = SessionManager(this).getCurrentUser() ?: return
        model.review(AppDatabase.getInstance(this), proof, user.id, approve, SessionManager(applicationContext))
    }

    class ReviewModel : ViewModel() {
        val state = MutableLiveData(IDLE)

        fun review(db: AppDatabase, proof: Proof, creatorId: String, approve: Boolean, session: SessionManager) {
            if (state.value != IDLE) return
            state.value = SAVING
            db.transactionExecutor.execute {
                try {
                    if (!db.reviewProof(proof.challengeId, proof.id, creatorId, approve)) {
                        state.postValue(UNAVAILABLE)
                        return@execute
                    }
                    if (!approve) {
                        state.postValue(REJECTED)
                        return@execute
                    }
                    try {
                        session.creditCompletedRewards(db, proof.hunterId)
                        state.postValue(APPROVED)
                    } catch (error: RuntimeException) {
                        Log.e("BountyReward", "Approved proof awaits demo reward recovery", error)
                        state.postValue(REWARD_PENDING)
                    }
                } catch (error: RuntimeException) {
                    Log.e("BountyReview", "Unable to review proof", error)
                    state.postValue(ERROR)
                }
            }
        }

        companion object {
            const val IDLE = 0
            const val SAVING = 1
            const val APPROVED = 2
            const val REJECTED = 3
            const val UNAVAILABLE = 4
            const val ERROR = 5
            const val REWARD_PENDING = 6
        }
    }
}
