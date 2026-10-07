package id.ac.binus.bounty.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import id.ac.binus.bounty.models.Challenge;

@Dao
public interface ChallengeDao {
    @Insert
    long insertChallenge(Challenge challenge);

    @Query("SELECT * FROM challenges ORDER BY createdAt DESC, id DESC")
    List<Challenge> getAllChallenges();

    @Query("SELECT * FROM challenges WHERE id = :id")
    Challenge getChallengeById(int id);

    @Query("SELECT * FROM challenges WHERE creatorId = :creatorId ORDER BY createdAt DESC, id DESC")
    List<Challenge> getChallengesByCreator(String creatorId);

    @Query("SELECT * FROM challenges WHERE participantId = :participantId ORDER BY createdAt DESC, id DESC")
    List<Challenge> getChallengesByParticipant(String participantId);

    @Query("SELECT * FROM challenges WHERE creatorId = :userId OR participantId = :userId ORDER BY createdAt DESC, id DESC")
    List<Challenge> getMyChallenges(String userId);

    @Query("UPDATE challenges SET status = 'ACCEPTED', participantId = :hunterId, participantName = :hunterName "
            + "WHERE id = :id AND status = 'OPEN' AND participantId IS NULL AND creatorId != :hunterId")
    int acceptChallenge(int id, String hunterId, String hunterName);

    @Query("UPDATE challenges SET status = 'SUBMITTED' WHERE id = :id AND status = 'ACCEPTED' AND participantId = :hunterId")
    int markSubmitted(int id, String hunterId);

    @Update
    int updateChallenge(Challenge challenge);

    @Delete
    int deleteChallenge(Challenge challenge);
}
