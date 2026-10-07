package id.ac.binus.bounty.database;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import id.ac.binus.bounty.models.Challenge;
import id.ac.binus.bounty.models.Proof;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AppDatabaseTest {
    @Test
    public void seedCrudRelationshipsAndPersistence() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name = "bounty_database_test";
        context.deleteDatabase(name);
        AppDatabase db = open(context, name);
        try {
            List<Challenge> seeds = db.challengeDao().getAllChallenges();
            assertEquals(3, seeds.size());
            String[] titles = {"30 Push-ups at the Park", "Sing a Song in Public", "Finish 5km Walk"};
            double[] rewards = {20000, 35000, 50000};
            for (int i = 0; i < seeds.size(); i++) {
                Challenge seed = db.challengeDao().getChallengeById(i + 1);
                assertEquals(titles[i], seed.title);
                assertEquals(rewards[i], seed.reward, 0);
                assertEquals(Challenge.OPEN, seed.status);
                assertNull(seed.participantId);
                assertNull(seed.participantName);
                assertFalse(seed.deadline.isEmpty());
            }
            assertSame(AppDatabase.getInstance(context), AppDatabase.getInstance(context));

            Challenge challenge = new Challenge();
            challenge.creatorId = "test_creator";
            challenge.title = "Persistence check";
            challenge.reward = 10000;
            challenge.createdAt = System.currentTimeMillis() + 1;
            challenge.id = (int) db.challengeDao().insertChallenge(challenge);
            assertTrue(challenge.id > 3);
            assertEquals(challenge.id, db.challengeDao().getAllChallenges().get(0).id);
            assertEquals(1, db.challengeDao().getChallengesByCreator("test_creator").size());
            assertTrue(db.challengeDao().getChallengesByParticipant("test_hunter").isEmpty());
            challenge.participantId = "test_hunter";
            challenge.participantName = "Hunter";
            challenge.status = Challenge.ACCEPTED;
            assertEquals(1, db.challengeDao().updateChallenge(challenge));
            assertEquals(challenge.id, db.challengeDao().getChallengesByParticipant("test_hunter").get(0).id);
            assertNull(db.proofDao().getProofByChallengeId(challenge.id));

            Proof proof = new Proof();
            proof.challengeId = challenge.id;
            proof.hunterId = "test_hunter";
            proof.description = "Done";
            proof.imageUri = "content://test/photo";
            proof.submittedAt = 1;
            proof.id = (int) db.proofDao().insertProof(proof);
            proof.status = Proof.REJECTED;
            assertEquals(1, db.proofDao().updateProof(proof));
            assertEquals(Proof.REJECTED, db.proofDao().getProofByChallengeId(challenge.id).status);
            proof.id = 0;
            proof.status = Proof.PENDING;
            proof.submittedAt = 2;
            proof.id = (int) db.proofDao().insertProof(proof);
            assertEquals(proof.id, db.proofDao().getProofByChallengeId(challenge.id).id);

            db.challengeDao().deleteChallenge(seeds.get(0));
            db.close();
            db = open(context, name);
            assertEquals(3, db.challengeDao().getAllChallenges().size());
            assertNull(db.challengeDao().getChallengeById(seeds.get(0).id));
            assertEquals(Challenge.ACCEPTED, db.challengeDao().getChallengeById(challenge.id).status);
            assertEquals("content://test/photo", db.proofDao().getProofByChallengeId(challenge.id).imageUri);
            assertEquals(1, db.challengeDao().deleteChallenge(challenge));
            assertNull(db.proofDao().getProofByChallengeId(challenge.id));
            proof.id = 0;
            try {
                db.proofDao().insertProof(proof);
                fail("Proof must reference an existing challenge");
            } catch (SQLiteConstraintException expected) {
                // Room/SQLite enforces the relationship even for direct DAO callers.
            }
        } finally {
            db.close();
            context.deleteDatabase(name);
        }
    }

    private AppDatabase open(Context context, String name) {
        return Room.databaseBuilder(context, AppDatabase.class, name)
                .addCallback(AppDatabase.SEED_CALLBACK).build();
    }
}
