package id.ac.binus.bounty.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import id.ac.binus.bounty.models.Proof

@Dao
interface ProofDao {
    @Insert fun insertProof(proof: Proof): Long

    @Query("SELECT * FROM proofs WHERE challengeId = :challengeId ORDER BY submittedAt DESC, id DESC LIMIT 1")
    fun getProofByChallengeId(challengeId: Int): Proof?

    @Update fun updateProof(proof: Proof): Int
}
