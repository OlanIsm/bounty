package id.ac.binus.bounty.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import id.ac.binus.bounty.models.Challenge
import id.ac.binus.bounty.models.Proof
import java.time.LocalDate

@Database(entities = [Challenge::class, Proof::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun challengeDao(): ChallengeDao
    abstract fun proofDao(): ProofDao

    fun submitProof(proof: Proof): Long {
        if (proof.challengeId <= 0 || proof.hunterId.trim().isEmpty() || proof.description.trim().isEmpty() ||
            proof.description.length > 2000 || proof.imageUri.isEmpty() || proof.status != Proof.PENDING ||
            proof.submittedAt <= 0
        ) throw IllegalArgumentException("Invalid proof")
        return runInTransaction(java.util.concurrent.Callable {
            if (challengeDao().markSubmitted(proof.challengeId, proof.hunterId) != 1) 0L
            else proofDao().insertProof(proof)
        })
    }

    fun reviewProof(challengeId: Int, proofId: Int, creatorId: String?, approve: Boolean): Boolean {
        if (challengeId <= 0 || proofId <= 0 || creatorId.isNullOrBlank()) return false
        return runInTransaction(java.util.concurrent.Callable {
            val challenge = challengeDao().getChallengeById(challengeId) ?: return@Callable false
            val proof = proofDao().getProofByChallengeId(challengeId) ?: return@Callable false
            if (proof.id != proofId || creatorId != challenge.creatorId || challenge.status != Challenge.SUBMITTED ||
                proof.status != Proof.PENDING || proof.hunterId != challenge.participantId
            ) return@Callable false
            proof.status = if (approve) Proof.APPROVED else Proof.REJECTED
            challenge.status = if (approve) Challenge.COMPLETED else Challenge.ACCEPTED
            if (proofDao().updateProof(proof) != 1 || challengeDao().updateChallenge(challenge) != 1) {
                throw IllegalStateException("Review records changed during transaction")
            }
            true
        })
    }

    companion object {
        const val DATABASE_NAME = "bounty_database"

        @JvmField
        val SEED_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                val deadline = LocalDate.now().plusDays(7).toString()
                val createdAt = System.currentTimeMillis()
                val titles = arrayOf("30 Push-ups at the Park", "Sing a Song in Public", "Finish 5km Walk")
                val descriptions = arrayOf(
                    "Lakukan 30 push-up di taman dan kirim foto sebagai bukti.",
                    "Nyanyikan satu lagu di tempat umum dan kirim foto sebagai bukti.",
                    "Selesaikan jalan kaki 5 km dan kirim foto jarak sebagai bukti."
                )
                val rewards = doubleArrayOf(20000.0, 35000.0, 50000.0)
                for (index in titles.indices) {
                    db.execSQL(
                        "INSERT INTO challenges (creatorId, creatorName, creatorAvatar, title, description, reward, deadline, status, participantId, participantName, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, ?)",
                        arrayOf<Any>("mock_creator_001", "Bounty Demo", "", titles[index], descriptions[index],
                            rewards[index], deadline, Challenge.OPEN, createdAt)
                    )
                }
            }
        }

        @Volatile private var instance: AppDatabase? = null

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): AppDatabase = instance ?: Room.databaseBuilder(
            context.applicationContext, AppDatabase::class.java, DATABASE_NAME
        ).addCallback(SEED_CALLBACK).build().also { instance = it }
    }
}
