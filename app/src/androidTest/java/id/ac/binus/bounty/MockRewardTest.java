package id.ac.binus.bounty;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import id.ac.binus.bounty.activities.MainActivity;
import id.ac.binus.bounty.activities.ReviewProofActivity;
import id.ac.binus.bounty.activities.ChallengeDetailActivity;
import id.ac.binus.bounty.database.AppDatabase;
import id.ac.binus.bounty.models.Challenge;
import id.ac.binus.bounty.models.Proof;
import id.ac.binus.bounty.models.User;
import id.ac.binus.bounty.utils.SessionManager;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class MockRewardTest {
    @Test public void approvalCreditsOnlyHunterOnceAndRecoversAfterInterruption() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SessionManager session = new SessionManager(context);
        String email = "m9-" + System.currentTimeMillis() + "@bounty.local";
        assertTrue(session.register("Reward Hunter", email));
        assertTrue(session.login(email));
        User hunter = session.getCurrentUser();
        assertTrue(session.login(SessionManager.DEMO_EMAIL));
        User creator = session.getCurrentUser();
        AppDatabase db = AppDatabase.getInstance(context);
        File photo = new File(context.getFilesDir(), "m9-proof.png");
        Bitmap bitmap = Bitmap.createBitmap(60, 40, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(android.graphics.Color.GREEN);
        try (FileOutputStream output = new FileOutputStream(photo)) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        }
        bitmap.recycle();
        Challenge first = challenge(db, creator, hunter, 20000);
        Challenge recovery = challenge(db, creator, hunter, 10000);
        Challenge concurrent = challenge(db, creator, hunter, 5000);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            assertEquals(0, session.creditCompletedRewards(db, hunter.id), 0);
            Proof rejected = proof(db, first, hunter, photo);
            assertFalse(db.reviewProof(first.id, rejected.id, hunter.id, true));
            assertTrue(db.reviewProof(first.id, rejected.id, creator.id, false));
            assertEquals(0, session.creditCompletedRewards(db, hunter.id), 0);
            Proof approved = proof(db, first, hunter, photo);
            assertFalse(db.reviewProof(first.id, rejected.id, creator.id, true));
            Intent intent = new Intent(context, ReviewProofActivity.class)
                    .putExtra(ChallengeDetailActivity.EXTRA_CHALLENGE_ID, first.id);
            try (ActivityScenario<ReviewProofActivity> review = ActivityScenario.launch(intent)) {
                long end = SystemClock.uptimeMillis() + 5000;
                AtomicReference<Boolean> ready = new AtomicReference<>(false);
                do {
                    review.onActivity(activity -> {
                        if (activity.findViewById(R.id.review_approve).isEnabled()) {
                            ready.set(true);
                            activity.findViewById(R.id.review_approve).performClick();
                            activity.findViewById(R.id.review_approve).performClick();
                        }
                    });
                    if (ready.get()) break;
                    SystemClock.sleep(16);
                } while (SystemClock.uptimeMillis() < end);
                assertTrue("Approve must become available", ready.get());
                waitForBalance(context, email, 70000);
            }
            assertEquals(Challenge.COMPLETED, db.challengeDao().getChallengeById(first.id).status);
            assertEquals(Proof.APPROVED, db.proofDao().getProofByChallengeId(first.id).status);
            assertTrue(session.isRewardCredited(first.id, hunter.id));
            assertFalse(db.reviewProof(first.id, approved.id, creator.id, true));
            assertEquals(creator.demoBalance, session.getCurrentUser().demoBalance, 0);
            assertEquals(0, new SessionManager(context).creditCompletedRewards(db, hunter.id), 0);

            // Simulate process termination between the committed Room approval and preferences write.
            Proof pending = proof(db, recovery, hunter, photo);
            assertTrue(db.reviewProof(recovery.id, pending.id, creator.id, true));
            assertFalse(session.isRewardCredited(recovery.id, hunter.id));
            assertTrue(session.login(email));
            try (ActivityScenario<MainActivity> home = ActivityScenario.launch(MainActivity.class)) {
                waitForBalance(context, email, 80000);
                waitForDisplayedBalance(home, "Rp80.000");
                home.onActivity(activity -> activity.findViewById(R.id.nav_profile).performClick());
                home.recreate();
                waitForDisplayedBalance(home, "Rp80.000");
            }
            Proof concurrentProof = proof(db, concurrent, hunter, photo);
            assertTrue(db.reviewProof(concurrent.id, concurrentProof.id, creator.id, true));
            Future<Double> a = workers.submit(() -> new SessionManager(context).creditCompletedRewards(db, hunter.id));
            Future<Double> b = workers.submit(() -> new SessionManager(context).creditCompletedRewards(db, hunter.id));
            assertEquals(5000, a.get(5, TimeUnit.SECONDS) + b.get(5, TimeUnit.SECONDS), 0);
            assertTrue(session.isRewardCredited(concurrent.id, hunter.id));
            session.logout();
            assertTrue(new SessionManager(context).login(email));
            assertEquals(85000, new SessionManager(context).getCurrentUser().demoBalance, 0);
            db.challengeDao().deleteChallenge(first);
            db.challengeDao().deleteChallenge(recovery);
            db.challengeDao().deleteChallenge(concurrent);
            assertEquals(0, session.creditCompletedRewards(db, hunter.id), 0);
            assertEquals(85000, session.getCurrentUser().demoBalance, 0);
        } finally {
            workers.shutdownNow();
            db.challengeDao().deleteChallenge(first);
            db.challengeDao().deleteChallenge(recovery);
            db.challengeDao().deleteChallenge(concurrent);
            context.getSharedPreferences("bounty_accounts", Context.MODE_PRIVATE).edit()
                    .remove(email).remove("reward_" + first.id).remove("reward_" + recovery.id).remove("reward_" + concurrent.id).commit();
            photo.delete();
            session.logout();
        }
    }

    private Challenge challenge(AppDatabase db, User creator, User hunter, double reward) {
        Challenge c = new Challenge();
        c.title = "M9 reward check";
        c.creatorId = creator.id;
        c.creatorName = creator.name;
        c.reward = reward;
        c.createdAt = System.currentTimeMillis();
        c.id = (int) db.challengeDao().insertChallenge(c);
        assertEquals(1, db.challengeDao().acceptChallenge(c.id, hunter.id, hunter.name));
        return c;
    }
    private Proof proof(AppDatabase db, Challenge c, User hunter, File photo) {
        Proof p = new Proof();
        p.challengeId = c.id;
        p.hunterId = hunter.id;
        p.description = "Completed reward check";
        p.imageUri = Uri.fromFile(photo).toString();
        p.submittedAt = System.currentTimeMillis();
        p.id = (int) db.submitProof(p);
        assertTrue(p.id > 0);
        return p;
    }
    private void waitForDisplayedBalance(ActivityScenario<MainActivity> home, String expected) {
        long end = SystemClock.uptimeMillis() + 5000;
        AtomicReference<String> displayed = new AtomicReference<>("");
        do {
            home.onActivity(activity -> displayed.set(
                    ((TextView) activity.findViewById(R.id.demo_balance_amount)).getText().toString()));
            if (expected.equals(displayed.get())) return;
            SystemClock.sleep(16);
        } while (SystemClock.uptimeMillis() < end);
        assertEquals(expected, displayed.get());
    }
    private void waitForBalance(Context context, String email, double expected) {
        long end = SystemClock.uptimeMillis() + 5000;
        while (SystemClock.uptimeMillis() < end) {
            String stored = context.getSharedPreferences("bounty_accounts", Context.MODE_PRIVATE).getString(email, "");
            User user = new com.google.gson.Gson().fromJson(stored, User.class);
            if (user != null && user.demoBalance == expected) return;
            SystemClock.sleep(16);
        }
        fail("Expected persisted hunter balance " + expected);
    }
}
