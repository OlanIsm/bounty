package id.ac.binus.bounty.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.time.LocalDate;

import id.ac.binus.bounty.models.Challenge;
import id.ac.binus.bounty.models.Proof;

@Database(entities = {Challenge.class, Proof.class}, version = 1, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {
    public static final String DATABASE_NAME = "bounty_database";
    private static AppDatabase instance;

    // onCreate runs inside Room's creation transaction, before the first DAO query.
    static final Callback SEED_CALLBACK = new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            String deadline = LocalDate.now().plusDays(7).toString();
            long createdAt = System.currentTimeMillis();
            String[] titles = {"30 Push-ups at the Park", "Sing a Song in Public", "Finish 5km Walk"};
            String[] descriptions = {
                    "Lakukan 30 push-up di taman dan kirim foto sebagai bukti.",
                    "Nyanyikan satu lagu di tempat umum dan kirim foto sebagai bukti.",
                    "Selesaikan jalan kaki 5 km dan kirim foto jarak sebagai bukti."
            };
            double[] rewards = {20000, 35000, 50000};
            for (int i = 0; i < titles.length; i++) {
                db.execSQL("INSERT INTO challenges (creatorId, creatorName, creatorAvatar, title, "
                                + "description, reward, deadline, status, participantId, participantName, createdAt) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, ?)",
                        new Object[]{"mock_creator_001", "Bounty Demo", "", titles[i], descriptions[i],
                                rewards[i], deadline, Challenge.OPEN, createdAt});
            }
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DATABASE_NAME)
                    .addCallback(SEED_CALLBACK)
                    .build();
        }
        return instance;
    }

    /** Changes status and inserts proof together; failure rolls back both operations. */
    public long submitProof(Proof proof) {
        if (proof.challengeId <= 0 || proof.hunterId.trim().isEmpty()
                || proof.description.trim().isEmpty() || proof.description.length() > 2000
                || proof.imageUri.isEmpty() || !Proof.PENDING.equals(proof.status) || proof.submittedAt <= 0) {
            throw new IllegalArgumentException("Invalid proof");
        }
        return runInTransaction(() -> {
            if (challengeDao().markSubmitted(proof.challengeId, proof.hunterId) != 1) return 0L;
            return proofDao().insertProof(proof);
        });
    }

    /** Reviews only the current pending submission, keeping both status changes atomic. */
    public boolean reviewProof(int challengeId, int proofId, String creatorId, boolean approve) {
        if (challengeId <= 0 || proofId <= 0 || creatorId == null || creatorId.trim().isEmpty()) return false;
        return runInTransaction(() -> {
            Challenge challenge = challengeDao().getChallengeById(challengeId);
            Proof proof = proofDao().getProofByChallengeId(challengeId);
            if (challenge == null || proof == null || proof.id != proofId
                    || !creatorId.equals(challenge.creatorId) || !Challenge.SUBMITTED.equals(challenge.status)
                    || !Proof.PENDING.equals(proof.status) || !proof.hunterId.equals(challenge.participantId)) return false;
            proof.status = approve ? Proof.APPROVED : Proof.REJECTED;
            challenge.status = approve ? Challenge.COMPLETED : Challenge.ACCEPTED;
            if (proofDao().updateProof(proof) != 1 || challengeDao().updateChallenge(challenge) != 1) {
                throw new IllegalStateException("Review records changed during transaction");
            }
            return true;
        });
    }

    public abstract ChallengeDao challengeDao();
    public abstract ProofDao proofDao();
}
