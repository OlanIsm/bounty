package id.ac.binus.bounty.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import id.ac.binus.bounty.models.Challenge

@Dao
interface ChallengeDao {
    @Insert fun insertChallenge(challenge: Challenge): Long

    @Query("SELECT * FROM challenges ORDER BY createdAt DESC, id DESC")
    fun getAllChallenges(): List<Challenge>

    @Query("SELECT * FROM challenges WHERE id = :id")
    fun getChallengeById(id: Int): Challenge?

    @Query("SELECT * FROM challenges WHERE creatorId = :creatorId ORDER BY createdAt DESC, id DESC")
    fun getChallengesByCreator(creatorId: String): List<Challenge>

    @Query("SELECT * FROM challenges WHERE participantId = :participantId ORDER BY createdAt DESC, id DESC")
    fun getChallengesByParticipant(participantId: String): List<Challenge>

    @Query("SELECT * FROM challenges WHERE creatorId = :userId OR participantId = :userId ORDER BY createdAt DESC, id DESC")
    fun getMyChallenges(userId: String): List<Challenge>

    @Query("UPDATE challenges SET status = 'ACCEPTED', participantId = :hunterId, participantName = :hunterName WHERE id = :id AND status = 'OPEN' AND participantId IS NULL AND creatorId != :hunterId")
    fun acceptChallenge(id: Int, hunterId: String, hunterName: String): Int

    @Query("UPDATE challenges SET status = 'SUBMITTED' WHERE id = :id AND status = 'ACCEPTED' AND participantId = :hunterId")
    fun markSubmitted(id: Int, hunterId: String): Int

    @Query("SELECT * FROM challenges WHERE status = 'COMPLETED' AND participantId = :hunterId AND EXISTS (SELECT 1 FROM proofs WHERE challengeId = challenges.id AND hunterId = :hunterId AND status = 'APPROVED') ORDER BY id")
    fun getRewardableChallenges(hunterId: String): List<Challenge>

    @Update fun updateChallenge(challenge: Challenge): Int
    @Delete fun deleteChallenge(challenge: Challenge): Int
}
