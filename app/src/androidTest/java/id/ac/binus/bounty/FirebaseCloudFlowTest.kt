package id.ac.binus.bounty

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import id.ac.binus.bounty.data.FirebaseBountyRepository
import id.ac.binus.bounty.activities.SubmitProofActivity
import id.ac.binus.bounty.models.Challenge
import id.ac.binus.bounty.models.Proof
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FirebaseCloudFlowTest {
    @Test
    fun proofImageCompressionRespectsFirestoreByteCap() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val image = Bitmap.createBitmap(1600, 1600, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(1600 * 1600)
        var seed = 1
        for (index in pixels.indices) {
            seed = seed * 1_664_525 + 1_013_904_223
            pixels[index] = (0xff shl 24) or (seed and 0x00ffffff)
        }
        image.setPixels(pixels, 0, 1600, 0, 0, 1600, 1600)
        val file = File(context.cacheDir, "proof-compression-test.jpg")
        try {
            file.outputStream().use { check(image.compress(Bitmap.CompressFormat.JPEG, 100, it)) }
            val compressed = SubmitProofActivity.ProofModel().compressProofImage(context, Uri.fromFile(file))
            assertTrue(compressed.isNotEmpty())
            assertTrue(compressed.size <= FirebaseBountyRepository.MAX_PROOF_IMAGE_BYTES)
        } finally {
            image.recycle()
            file.delete()
        }
    }

    @Test
    fun challengeProofReviewAndRewardUseFirestoreTransactions() {
        check(BuildConfig.USE_FIREBASE_EMULATORS) { "Run only with Firebase emulators enabled" }
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val creator = createAccount(auth, firestore, "creator")
        val hunter = createAccount(auth, firestore, "hunter")
        try {
            Tasks.await(auth.signInWithEmailAndPassword(creator.email, PASSWORD))
            val challenge = Challenge().apply {
                creatorId = creator.uid
                creatorName = creator.name
                title = "Cloud test challenge"
                description = "Verify cloud state transitions"
                reward = 20000.0
                deadline = "2030-12-31"
            }
            val challengeId = awaitResult { FirebaseBountyRepository.createChallenge(challenge, it) }
            assertFalse(awaitResult { FirebaseBountyRepository.acceptChallenge(challengeId, creator.uid, creator.name, it) })

            Tasks.await(auth.signInWithEmailAndPassword(hunter.email, PASSWORD))
            assertTrue(awaitResult { FirebaseBountyRepository.acceptChallenge(challengeId, hunter.uid, hunter.name, it) })
            val firstProof = proof(hunter.uid, "First proof")
            assertTrue(awaitResult { FirebaseBountyRepository.submitProof(challengeId, firstProof, IMAGE_BYTES, it) })

            Tasks.await(auth.signInWithEmailAndPassword(creator.email, PASSWORD))
            val submitted = awaitResult { FirebaseBountyRepository.getChallenge(challengeId, it) }
            assertEquals(Challenge.SUBMITTED, submitted?.status)
            val rejected = awaitResult { FirebaseBountyRepository.getLatestProof(requireNotNull(submitted), it) }
            assertNotNull(rejected)
            assertTrue(awaitResult { FirebaseBountyRepository.reviewProof(requireNotNull(submitted), requireNotNull(rejected), creator.uid, false, it) })

            Tasks.await(auth.signInWithEmailAndPassword(hunter.email, PASSWORD))
            val acceptedAgain = awaitResult { FirebaseBountyRepository.getChallenge(challengeId, it) }
            assertEquals(Challenge.ACCEPTED, acceptedAgain?.status)
            val secondProof = proof(hunter.uid, "Replacement proof")
            assertTrue(awaitResult { FirebaseBountyRepository.submitProof(challengeId, secondProof, IMAGE_BYTES, it) })

            Tasks.await(auth.signInWithEmailAndPassword(creator.email, PASSWORD))
            val latestChallenge = awaitResult { FirebaseBountyRepository.getChallenge(challengeId, it) }
            val latestProof = awaitResult { FirebaseBountyRepository.getLatestProof(requireNotNull(latestChallenge), it) }
            assertTrue(awaitResult { FirebaseBountyRepository.reviewProof(requireNotNull(latestChallenge), requireNotNull(latestProof), creator.uid, true, it) })
            assertEquals(Challenge.COMPLETED, awaitResult { FirebaseBountyRepository.getChallenge(challengeId, it) }?.status)

            Tasks.await(auth.signInWithEmailAndPassword(hunter.email, PASSWORD))
            assertEquals(70000.0, awaitResult { FirebaseBountyRepository.loadBalance(hunter.uid, it) }, 0.0)
            val receipts = Tasks.await(firestore.collection("users").document(hunter.uid)
                .collection("rewardReceipts").get())
            assertEquals(1, receipts.size())
        } finally {
            auth.signOut()
        }
    }

    private fun createAccount(auth: FirebaseAuth, firestore: FirebaseFirestore, role: String): Account {
        val email = "${role}-${UUID.randomUUID()}@bounty.test"
        val credential = Tasks.await(auth.createUserWithEmailAndPassword(email, PASSWORD))
        val user = requireNotNull(credential.user)
        val name = "Cloud $role"
        Tasks.await(firestore.collection("users").document(user.uid).set(mapOf(
            "displayName" to name,
            "email" to email,
            "initialDemoBalance" to 50000,
            "createdAt" to FieldValue.serverTimestamp()
        )))
        return Account(user.uid, email, name)
    }

    private fun proof(uid: String, description: String) = Proof().apply {
        hunterId = uid
        this.description = description
    }

    private fun <T> awaitResult(operation: ((Result<T>) -> Unit) -> Unit): T {
        val latch = CountDownLatch(1)
        var result: Result<T>? = null
        operation { value -> result = value; latch.countDown() }
        check(latch.await(15, TimeUnit.SECONDS)) { "Firebase operation timed out" }
        return requireNotNull(result).getOrThrow()
    }

    private data class Account(val uid: String, val email: String, val name: String)

    companion object {
        private const val PASSWORD = "Bounty123!"
        private val IMAGE_BYTES = ByteArray(256) { (it % 255).toByte() }
    }
}
