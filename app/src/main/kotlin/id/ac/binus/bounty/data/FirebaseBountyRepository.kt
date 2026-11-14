package id.ac.binus.bounty.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import id.ac.binus.bounty.models.Challenge
import id.ac.binus.bounty.models.Proof

object FirebaseBountyRepository {
    const val MAX_PROOF_IMAGE_BYTES = 512 * 1024
    private val firestore get() = FirebaseFirestore.getInstance()
    private val challenges get() = firestore.collection("challenges")

    fun loadChallenges(uid: String, mine: Boolean, callback: (Result<List<Challenge>>) -> Unit) {
        if (!mine) {
            challenges.orderBy("createdAt", Query.Direction.DESCENDING).get()
                .addOnSuccessListener { snapshot -> callback(Result.success(snapshot.documents.mapNotNull(::toChallenge))) }
                .addOnFailureListener { callback(Result.failure(it)) }
            return
        }
        val created = challenges.whereEqualTo("creatorId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING).get()
        val joined = challenges.whereEqualTo("participantId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING).get()
        com.google.android.gms.tasks.Tasks.whenAllSuccess<com.google.firebase.firestore.QuerySnapshot>(created, joined)
            .addOnSuccessListener { results ->
                val rows = results.flatMap { it.documents }.distinctBy { it.id }
                    .mapNotNull(::toChallenge).sortedByDescending { it.createdAt }
                callback(Result.success(rows))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun getChallenge(id: String, callback: (Result<Challenge?>) -> Unit) {
        challenges.document(id).get()
            .addOnSuccessListener { snapshot -> callback(Result.success(snapshot.takeIf { it.exists() }?.let(::toChallenge))) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun createChallenge(challenge: Challenge, callback: (Result<String>) -> Unit) {
        val reference = challenges.document()
        val data = mapOf(
            "creatorId" to challenge.creatorId,
            "creatorName" to challenge.creatorName,
            "creatorAvatar" to challenge.creatorAvatar,
            "title" to challenge.title,
            "description" to challenge.description,
            "rewardAmount" to challenge.reward.toLong(),
            "deadline" to challenge.deadline,
            "status" to Challenge.OPEN,
            "participantId" to null,
            "participantName" to null,
            "latestProofId" to null,
            "createdAt" to FieldValue.serverTimestamp()
        )
        reference.set(data)
            .addOnSuccessListener { callback(Result.success(reference.id)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun acceptChallenge(id: String, uid: String, name: String, callback: (Result<Boolean>) -> Unit) {
        val reference = challenges.document(id)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(reference)
            if (!snapshot.exists() || snapshot.getString("status") != Challenge.OPEN ||
                snapshot.getString("creatorId") == uid || snapshot.getString("participantId") != null
            ) return@runTransaction false
            transaction.update(reference, mapOf(
                "status" to Challenge.ACCEPTED,
                "participantId" to uid,
                "participantName" to name
            ))
            true
        }.addOnSuccessListener { callback(Result.success(it)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun getLatestProof(challenge: Challenge, callback: (Result<Proof?>) -> Unit) {
        val challengeId = challenge.cloudId ?: return callback(Result.failure(IllegalArgumentException("Missing challenge ID")))
        val proofId = challenge.latestProofId ?: return callback(Result.success(null))
        challenges.document(challengeId).collection("proofs").document(proofId).get()
            .addOnSuccessListener { snapshot -> callback(Result.success(snapshot.takeIf { it.exists() }?.let { toProof(challenge, it.id, it.data.orEmpty()) })) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun submitProof(challengeId: String, proof: Proof, imageData: ByteArray, callback: (Result<Boolean>) -> Unit) {
        if (imageData.isEmpty() || imageData.size > MAX_PROOF_IMAGE_BYTES) {
            callback(Result.failure(IllegalArgumentException("Proof image exceeds the Firestore limit")))
            return
        }
        val challengeReference = challenges.document(challengeId)
        val proofReference = challengeReference.collection("proofs").document()
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(challengeReference)
            if (!snapshot.exists() || snapshot.getString("status") != Challenge.ACCEPTED ||
                snapshot.getString("participantId") != proof.hunterId
            ) return@runTransaction false
            transaction.set(proofReference, mapOf(
                "challengeId" to challengeId,
                "hunterId" to proof.hunterId,
                "description" to proof.description,
                "imageData" to Blob.fromBytes(imageData),
                "status" to Proof.PENDING,
                "submittedAt" to FieldValue.serverTimestamp()
            ))
            transaction.update(challengeReference, mapOf(
                "status" to Challenge.SUBMITTED,
                "latestProofId" to proofReference.id
            ))
            true
        }.addOnSuccessListener { callback(Result.success(it)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun reviewProof(challenge: Challenge, proof: Proof, creatorId: String, approve: Boolean, callback: (Result<Boolean>) -> Unit) {
        val challengeId = challenge.cloudId ?: return callback(Result.failure(IllegalArgumentException("Missing challenge ID")))
        val proofId = proof.cloudId ?: return callback(Result.failure(IllegalArgumentException("Missing proof ID")))
        val challengeReference = challenges.document(challengeId)
        val proofReference = challengeReference.collection("proofs").document(proofId)
        val receiptReference = firestore.collection("users").document(proof.hunterId)
            .collection("rewardReceipts").document(challengeId)
        firestore.runTransaction { transaction ->
            val challengeSnapshot = transaction.get(challengeReference)
            val proofSnapshot = transaction.get(proofReference)
            if (!challengeSnapshot.exists() || !proofSnapshot.exists() ||
                challengeSnapshot.getString("creatorId") != creatorId ||
                challengeSnapshot.getString("status") != Challenge.SUBMITTED ||
                challengeSnapshot.getString("latestProofId") != proofId ||
                proofSnapshot.getString("status") != Proof.PENDING ||
                proofSnapshot.getString("hunterId") != challengeSnapshot.getString("participantId")
            ) return@runTransaction false
            val nextChallengeStatus = if (approve) Challenge.COMPLETED else Challenge.ACCEPTED
            transaction.update(proofReference, "status", if (approve) Proof.APPROVED else Proof.REJECTED)
            transaction.update(challengeReference, "status", nextChallengeStatus)
            if (approve) transaction.set(receiptReference, mapOf(
                "challengeId" to challengeId,
                "proofId" to proofId,
                "amount" to (challengeSnapshot.getLong("rewardAmount") ?: 0L),
                "creditedAt" to FieldValue.serverTimestamp()
            ))
            true
        }.addOnSuccessListener { callback(Result.success(it)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun loadBalance(uid: String, callback: (Result<Double>) -> Unit) {
        val profile = firestore.collection("users").document(uid)
        val receipts = profile.collection("rewardReceipts").get()
        val account = profile.get()
        com.google.android.gms.tasks.Tasks.whenAllSuccess<Any>(receipts, account)
            .addOnSuccessListener { results ->
                val receiptRows = results[0] as com.google.firebase.firestore.QuerySnapshot
                val accountRow = results[1] as com.google.firebase.firestore.DocumentSnapshot
                if (!accountRow.exists()) {
                    callback(Result.failure(IllegalStateException("Firebase profile is missing")))
                    return@addOnSuccessListener
                }
                val initial = accountRow.getLong("initialDemoBalance") ?: 50000L
                val total = initial + receiptRows.documents.sumOf { it.getLong("amount") ?: 0L }
                callback(Result.success(total.toDouble()))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    private fun toChallenge(snapshot: com.google.firebase.firestore.DocumentSnapshot): Challenge? {
        if (!snapshot.exists()) return null
        return Challenge().apply {
            id = snapshot.id.hashCode() and Int.MAX_VALUE
            cloudId = snapshot.id
            creatorId = snapshot.getString("creatorId").orEmpty()
            creatorName = snapshot.getString("creatorName").orEmpty()
            creatorAvatar = snapshot.getString("creatorAvatar").orEmpty()
            title = snapshot.getString("title").orEmpty()
            description = snapshot.getString("description").orEmpty()
            reward = (snapshot.getLong("rewardAmount") ?: 0L).toDouble()
            deadline = snapshot.getString("deadline").orEmpty()
            status = snapshot.getString("status") ?: Challenge.OPEN
            participantId = snapshot.getString("participantId")
            participantName = snapshot.getString("participantName")
            latestProofId = snapshot.getString("latestProofId")
            createdAt = snapshot.getTimestamp("createdAt")?.toDate()?.time ?: 0L
        }
    }

    private fun toProof(challenge: Challenge, id: String, data: Map<String, Any>): Proof = Proof().apply {
        this.id = id.hashCode() and Int.MAX_VALUE
        challengeId = challenge.id
        challengeCloudId = challenge.cloudId
        cloudId = id
        hunterId = data["hunterId"] as? String ?: ""
        description = data["description"] as? String ?: ""
        imageData = (data["imageData"] as? Blob)?.toBytes()
        imageUri = ""
        status = data["status"] as? String ?: Proof.PENDING
        submittedAt = (data["submittedAt"] as? Timestamp)?.toDate()?.time ?: 0L
    }
}
