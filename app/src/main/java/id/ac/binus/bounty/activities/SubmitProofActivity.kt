package id.ac.binus.bounty.activities

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import id.ac.binus.bounty.R
import id.ac.binus.bounty.data.FirebaseBountyRepository
import id.ac.binus.bounty.models.Challenge
import id.ac.binus.bounty.models.Proof
import id.ac.binus.bounty.models.User
import id.ac.binus.bounty.utils.ScreenInsets
import id.ac.binus.bounty.utils.SessionManager
import java.io.IOException
import java.io.ByteArrayOutputStream

class SubmitProofActivity : AppCompatActivity() {
    private var imageUri: Uri? = null
    private lateinit var model: ProofModel
    private var eligible = false
    private var request = 0
    private val picker: ActivityResultLauncher<Array<String>> = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            imageUri = uri
            showPhoto()
        } catch (_: SecurityException) {
            findViewById<TextView>(R.id.proof_photo_message).setText(R.string.proof_photo_error)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submit_proof)
        ScreenInsets.apply(this)
        model = ViewModelProvider(this)[ProofModel::class.java]
        savedInstanceState?.getString("proof_image")?.let { imageUri = Uri.parse(it) }
        findViewById<MaterialToolbar>(R.id.proof_toolbar).setNavigationOnClickListener { finish() }
        findViewById<View>(R.id.choose_proof_photo).setOnClickListener { picker.launch(arrayOf("image/*")) }
        findViewById<View>(R.id.proof_submit).setOnClickListener { submit() }
        findViewById<View>(R.id.proof_retry).setOnClickListener { loadChallenge() }
        showPhoto()
    }

    override fun onPostCreate(state: Bundle?) {
        super.onPostCreate(state)
        model.state.observe(this) { result ->
            updateSaving()
            when (result) {
                ProofModel.SAVED -> {
                    Toast.makeText(this, R.string.proof_saved, Toast.LENGTH_LONG).show()
                    finish()
                }
                ProofModel.ERROR, ProofModel.IMAGE_ERROR -> {
                    val message = if (result == ProofModel.IMAGE_ERROR) R.string.proof_photo_error else R.string.proof_save_error
                    Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG).show()
                    if (result == ProofModel.IMAGE_ERROR) findViewById<TextView>(R.id.proof_photo_message).setText(message)
                    model.state.value = ProofModel.IDLE
                }
                ProofModel.UNAVAILABLE -> {
                    model.state.value = ProofModel.IDLE
                    loadChallenge()
                }
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
        loadChallenge()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        imageUri?.let { outState.putString("proof_image", it.toString()) }
        super.onSaveInstanceState(outState)
    }

    private fun showPhoto() {
        val photo = findViewById<ImageView>(R.id.proof_photo)
        photo.visibility = if (imageUri == null) View.GONE else View.VISIBLE
        imageUri?.let { Glide.with(this).load(it).error(R.drawable.ic_avatar).into(photo) }
        findViewById<TextView>(R.id.proof_photo_message).setText(
            if (imageUri == null) R.string.proof_photo_required else R.string.proof_photo_preview
        )
    }

    private fun loadChallenge() {
        val current = ++request
        eligible = false
        updateSaving()
        findViewById<View>(R.id.proof_loading).visibility = View.VISIBLE
        findViewById<View>(R.id.proof_form).visibility = View.GONE
        findViewById<View>(R.id.proof_message).visibility = View.GONE
        findViewById<View>(R.id.proof_retry).visibility = View.GONE
        val user = SessionManager(this).getCurrentUser() ?: return
        val id = intent.getStringExtra(ChallengeDetailActivity.EXTRA_CLOUD_CHALLENGE_ID)
        if (id.isNullOrBlank()) {
            showMessage(R.string.challenge_not_found, false)
            return
        }
        FirebaseBountyRepository.getChallenge(id) { result ->
            if (isDestroyed || isFinishing || current != request) return@getChallenge
            result.onSuccess { challenge ->
                if (challenge == null) {
                    showMessage(R.string.challenge_not_found, false)
                    return@onSuccess
                }
                eligible = challenge.status == Challenge.ACCEPTED && user.id == challenge.participantId
                if (!eligible && model.state.value != ProofModel.SAVING) {
                    showMessage(R.string.proof_unavailable, false)
                    return@onSuccess
                }
                findViewById<TextView>(R.id.proof_challenge_title).text = challenge.title
                findViewById<View>(R.id.proof_loading).visibility = View.GONE
                findViewById<View>(R.id.proof_form).visibility = View.VISIBLE
                updateSaving()
            }.onFailure { error ->
                Log.e("BountyProof", "Unable to load challenge", error)
                showMessage(R.string.challenge_load_error, true)
            }
        }
    }

    private fun showMessage(text: Int, retry: Boolean) {
        eligible = false
        updateSaving()
        findViewById<View>(R.id.proof_loading).visibility = View.GONE
        findViewById<View>(R.id.proof_form).visibility = View.GONE
        findViewById<TextView>(R.id.proof_message).apply {
            setText(text)
            visibility = View.VISIBLE
        }
        findViewById<View>(R.id.proof_retry).visibility = if (retry) View.VISIBLE else View.GONE
    }

    private fun updateSaving() {
        val saving = model.state.value == ProofModel.SAVING
        listOf(R.id.choose_proof_photo, R.id.proof_description_layout, R.id.proof_submit).forEach {
            findViewById<View>(it).isEnabled = eligible && !saving
        }
        findViewById<View>(R.id.proof_saving).visibility = if (saving) View.VISIBLE else View.GONE
        findViewById<MaterialButton>(R.id.proof_submit).setText(
            if (saving) R.string.proof_submitting else R.string.submit_proof_action
        )
    }

    private fun submit() {
        if (!eligible || model.state.value != ProofModel.IDLE) return
        val user = SessionManager(this).getCurrentUser() ?: return
        val image = imageUri ?: return
        val description = findViewById<TextInputEditText>(R.id.proof_description_input).text?.toString()?.trim().orEmpty()
        val valid = description.isNotEmpty() && description.length <= 2000
        findViewById<TextInputLayout>(R.id.proof_description_layout).error =
            if (valid) null else getString(R.string.proof_description_required)
        if (imageUri == null) findViewById<TextView>(R.id.proof_photo_message).setText(R.string.proof_photo_required)
        if (!valid || imageUri == null) return
        val proof = Proof().apply {
            challengeCloudId = intent.getStringExtra(ChallengeDetailActivity.EXTRA_CLOUD_CHALLENGE_ID)
            hunterId = user.id
            this.description = description
            imageUri = image.toString()
            submittedAt = System.currentTimeMillis()
        }
        model.submit(applicationContext, proof)
    }

    class ProofModel : ViewModel() {
        val state = MutableLiveData(IDLE)

        fun submit(context: Context, proof: Proof) {
            if (state.value != IDLE) return
            state.value = SAVING
            Thread {
                try {
                    val imageData = compressProofImage(context, Uri.parse(proof.imageUri))
                    val challengeId = proof.challengeCloudId ?: throw IllegalArgumentException("Missing challenge ID")
                    FirebaseBountyRepository.submitProof(challengeId, proof, imageData) { result ->
                        result.onSuccess { state.value = if (it) SAVED else UNAVAILABLE }
                            .onFailure { error ->
                                Log.e("BountyProof", "Unable to submit proof", error)
                                state.value = ERROR
                            }
                    }
                } catch (error: Exception) {
                    Log.e("BountyProof", "Unable to read proof image", error)
                    state.postValue(if (error is IOException || error is IllegalArgumentException) IMAGE_ERROR else ERROR)
                }
            }.start()
        }

        internal fun compressProofImage(context: Context, uri: Uri): ByteArray {
            val resolver = context.contentResolver
            val bounds = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { BitmapFactory.decodeStream(stream, null, it) }
            } ?: throw IOException("Unable to open proof image")
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Unreadable proof image")
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1280) sample *= 2
            var bitmap = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: throw IOException("Unable to decode proof image")
            try {
                while (true) {
                    for (quality in listOf(82, 72, 62, 52, 42)) {
                        val output = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
                        if (output.size() <= FirebaseBountyRepository.MAX_PROOF_IMAGE_BYTES) return output.toByteArray()
                    }
                    if (bitmap.width <= 320 || bitmap.height <= 320) throw IllegalArgumentException("Proof image is too large")
                    val smaller = Bitmap.createScaledBitmap(bitmap, (bitmap.width * 0.75).toInt(), (bitmap.height * 0.75).toInt(), true)
                    if (smaller !== bitmap) bitmap.recycle()
                    bitmap = smaller
                }
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }

        companion object {
            const val IDLE = 0
            const val SAVING = 1
            const val SAVED = 2
            const val UNAVAILABLE = 3
            const val ERROR = 4
            const val IMAGE_ERROR = 5
        }
    }
}
