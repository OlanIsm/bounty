package id.ac.binus.bounty;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import id.ac.binus.bounty.activities.ChallengeDetailActivity;
import id.ac.binus.bounty.activities.ReviewProofActivity;
import id.ac.binus.bounty.database.AppDatabase;
import id.ac.binus.bounty.models.Challenge;
import id.ac.binus.bounty.models.Proof;
import id.ac.binus.bounty.models.User;
import id.ac.binus.bounty.utils.SessionManager;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ReviewProofTest {
    @Test public void creatorRejectsThenApprovesLatestProofWithAtomicGuards() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SessionManager session = new SessionManager(context);
        assertTrue(session.login(SessionManager.DEMO_EMAIL));
        User creator = session.getCurrentUser();
        AppDatabase db = AppDatabase.getInstance(context);
        Challenge challenge = new Challenge();
        challenge.title = "M8 review challenge";
        challenge.creatorId = creator.id;
        challenge.creatorName = creator.name;
        challenge.createdAt = System.currentTimeMillis();
        challenge.id = (int) db.challengeDao().insertChallenge(challenge);
        assertEquals(1, db.challengeDao().acceptChallenge(challenge.id, "m8_hunter", "Hunter"));
        File image = new File(context.getFilesDir(), "m8-proof.png");
        Bitmap bitmap = Bitmap.createBitmap(60, 40, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(android.graphics.Color.GREEN);
        try (FileOutputStream stream = new FileOutputStream(image)) { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)); }
        bitmap.recycle();
        String uri = Uri.fromFile(image).toString();
        Proof first = proof(challenge.id, uri, "First proof");
        first.id = (int) db.submitProof(first);
        CountDownLatch release = new CountDownLatch(1);
        try {
            assertFalse(db.reviewProof(challenge.id, first.id, "other_creator", true));
            assertFalse(db.reviewProof(-1, first.id, creator.id, true));
            Intent intent = new Intent(context, ReviewProofActivity.class)
                    .putExtra(ChallengeDetailActivity.EXTRA_CHALLENGE_ID, challenge.id);
            try (ActivityScenario<ReviewProofActivity> scenario = ActivityScenario.launch(intent)) {
                waitForView(R.id.review_loading, false);
                waitForView(R.id.review_approve, true);
                onView(withId(R.id.review_description)).check(matches(withText("First proof")));
                onView(withId(R.id.review_hunter)).check(matches(withText("Hunter: Hunter")));
                CountDownLatch blocked = new CountDownLatch(1);
                db.getTransactionExecutor().execute(() -> {
                    blocked.countDown();
                    try { release.await(30, TimeUnit.SECONDS); }
                    catch (InterruptedException error) { Thread.currentThread().interrupt(); }
                });
                assertTrue(blocked.await(5, TimeUnit.SECONDS));
                onView(withId(R.id.review_reject)).perform(scrollTo(), click()).check(matches(not(isEnabled())));
                scenario.onActivity(activity -> activity.findViewById(R.id.review_approve).performClick());
                scenario.recreate();
                waitForView(R.id.review_loading, false);
                onView(withId(R.id.review_reject)).perform(scrollTo()).check(matches(not(isEnabled())));
                release.countDown();
                waitForStatus(db, challenge.id, Challenge.ACCEPTED);
            }
            assertEquals(Proof.REJECTED, db.proofDao().getProofByChallengeId(challenge.id).status);
            assertFalse(db.reviewProof(challenge.id, first.id, creator.id, true));
            Proof next = proof(challenge.id, "file:///missing-m8-image.png", "Resubmitted proof");
            next.id = (int) db.submitProof(next);
            assertTrue(next.id > first.id);
            assertFalse(db.reviewProof(challenge.id, first.id, creator.id, true));
            assertEquals(Proof.PENDING, db.proofDao().getProofByChallengeId(challenge.id).status);
            try (ActivityScenario<ReviewProofActivity> scenario = ActivityScenario.launch(intent)) {
                waitForView(R.id.review_loading, false);
                waitForView(R.id.review_photo_error, false);
                onView(withId(R.id.review_approve)).perform(scrollTo()).check(matches(not(isEnabled())));
                onView(withId(R.id.review_reject)).check(matches(isEnabled()));
                next.imageUri = uri;
                db.proofDao().updateProof(next);
                onView(withId(R.id.review_photo_retry)).perform(scrollTo(), click());
                waitForView(R.id.review_loading, false);
                waitForView(R.id.review_approve, true);
                onView(withId(R.id.review_description)).check(matches(withText("Resubmitted proof")));
                scenario.recreate();
                waitForView(R.id.review_loading, false);
                waitForView(R.id.review_approve, true);
                // Force failure after updating the proof, verifying both changes roll back.
                db.getOpenHelper().getWritableDatabase().execSQL("CREATE TRIGGER m8_abort_review BEFORE UPDATE OF status ON challenges "
                        + "WHEN NEW.id = " + challenge.id + " AND NEW.status = 'COMPLETED' BEGIN SELECT RAISE(ABORT, 'M8 rollback'); END");
                try { db.reviewProof(challenge.id, next.id, creator.id, true); fail("Trigger must abort review"); }
                catch (android.database.sqlite.SQLiteException expected) { }
                finally { db.getOpenHelper().getWritableDatabase().execSQL("DROP TRIGGER IF EXISTS m8_abort_review"); }
                assertEquals(Proof.PENDING, db.proofDao().getProofByChallengeId(challenge.id).status);
                assertEquals(Challenge.SUBMITTED, db.challengeDao().getChallengeById(challenge.id).status);
                onView(withId(R.id.review_approve)).perform(scrollTo(), click());
                assertEquals(Challenge.SUBMITTED, db.challengeDao().getChallengeById(challenge.id).status);
                onView(withId(android.R.id.button2)).perform(click());
                assertEquals(Proof.PENDING, db.proofDao().getProofByChallengeId(challenge.id).status);
                onView(withId(R.id.review_approve)).perform(scrollTo(), click());
                scenario.recreate();
                onView(withText(R.string.confirm_approve_title)).check(matches(isDisplayed()));
                onView(withId(android.R.id.button1)).perform(click());
                waitForStatus(db, challenge.id, Challenge.COMPLETED);
            }
            assertEquals(Proof.APPROVED, db.proofDao().getProofByChallengeId(challenge.id).status);
            assertFalse(db.reviewProof(challenge.id, next.id, creator.id, false));
            assertEquals(creator.demoBalance, session.getCurrentUser().demoBalance, 0);
            Intent review = new Intent(context, ReviewProofActivity.class)
                    .putExtra(ChallengeDetailActivity.EXTRA_CHALLENGE_ID, challenge.id);
            try (ActivityScenario<ReviewProofActivity> denied = ActivityScenario.launch(review)) {
                waitForView(R.id.review_loading, false);
                onView(withId(R.id.review_message)).check(matches(withText(R.string.review_unavailable)));
            }
            review.putExtra(ChallengeDetailActivity.EXTRA_CHALLENGE_ID, -1);
            try (ActivityScenario<ReviewProofActivity> missing = ActivityScenario.launch(review)) {
                waitForView(R.id.review_loading, false);
                onView(withId(R.id.review_message)).check(matches(withText(R.string.challenge_not_found)));
            }
        } finally {
            release.countDown();
            db.getOpenHelper().getWritableDatabase().execSQL("DROP TRIGGER IF EXISTS m8_abort_review");
            db.challengeDao().deleteChallenge(challenge);
            assertTrue(image.delete());
            session.logout();
        }
    }
    private Proof proof(int id, String uri, String description) {
        Proof proof = new Proof();
        proof.challengeId = id;
        proof.hunterId = "m8_hunter";
        proof.description = description;
        proof.imageUri = uri;
        proof.submittedAt = System.currentTimeMillis();
        return proof;
    }
    private void waitForStatus(AppDatabase db, int id, String status) {
        long end = SystemClock.uptimeMillis() + 5000;
        while (!status.equals(db.challengeDao().getChallengeById(id).status)
                && SystemClock.uptimeMillis() < end) SystemClock.sleep(16);
        assertEquals(status, db.challengeDao().getChallengeById(id).status);
    }
    private void waitForView(int id, boolean enabled) {
        onView(isRoot()).perform(new ViewAction() {
            public Matcher<View> getConstraints() { return isRoot(); }
            public String getDescription() { return "wait for review view " + id; }
            public void perform(UiController ui, View root) {
                long end = SystemClock.uptimeMillis() + 5000;
                do {
                    View view = root.findViewById(id);
                    if (view != null && (enabled ? view.isEnabled() : id == R.id.review_photo_error
                            ? view.getVisibility() == View.VISIBLE : view.getVisibility() == View.GONE)) return;
                    ui.loopMainThreadForAtLeast(16);
                } while (SystemClock.uptimeMillis() < end);
                throw new AssertionError("Review view timed out: " + id);
            }
        });
    }
}
