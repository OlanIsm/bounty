package id.ac.binus.bounty.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import id.ac.binus.bounty.models.Proof;

@Dao
public interface ProofDao {
    @Insert
    long insertProof(Proof proof);

    @Query("SELECT * FROM proofs WHERE challengeId = :challengeId ORDER BY submittedAt DESC, id DESC LIMIT 1")
    Proof getProofByChallengeId(int challengeId);

    @Update
    int updateProof(Proof proof);
}
