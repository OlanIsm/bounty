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

    @Update
    int updateChallenge(Challenge challenge);

    @Delete
    int deleteChallenge(Challenge challenge);
}
