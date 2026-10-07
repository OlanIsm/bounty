package id.ac.binus.bounty;

import android.accessibilityservice.AccessibilityService;
import android.app.Instrumentation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.textfield.TextInputLayout;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import id.ac.binus.bounty.activities.ChallengeDetailActivity;
import id.ac.binus.bounty.activities.SubmitProofActivity;
import id.ac.binus.bounty.models.Challenge;
import id.ac.binus.bounty.models.Proof;
import id.ac.binus.bounty.models.User;
import id.ac.binus.bounty.database.AppDatabase;
import id.ac.binus.bounty.utils.SessionManager;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SubmitProofTest {
    @Test public void nativePickerDraftAndSingleTransactionalSubmission() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        SessionManager session = new SessionManager(context);
        assertTrue(session.login(SessionManager.DEMO_EMAIL));
        User hunter = session.getCurrentUser();
        AppDatabase db = AppDatabase.getInstance(context);
        Challenge challenge = new Challenge();
        challenge.title = "M7 proof challenge";
        challenge.creatorId = "m7_creator";
        challenge.creatorName = "Creator";
        challenge.status = Challenge.ACCEPTED;
        challenge.participantId = hunter.id;
        challenge.participantName = hunter.name;
        challenge.id = (int) db.challengeDao().insertChallenge(challenge);
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "M7-proof.png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        Uri image = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        assertNotNull(image);
        Bitmap bitmap = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(android.graphics.Color.GREEN);
        try (OutputStream stream = context.getContentResolver().openOutputStream(image)) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        }
        bitmap.recycle();
        CountDownLatch release = new CountDownLatch(1);
        try {
            Proof invalidHunter = proof(challenge.id, "other", image.toString());
            assertEquals(0, db.submitProof(invalidHunter));
            assertNull(db.proofDao().getProofByChallengeId(challenge.id));
            Intent intent = new Intent(context, SubmitProofActivity.class)
                    .putExtra(ChallengeDetailActivity.EXTRA_CHALLENGE_ID, challenge.id);
            try (ActivityScenario<SubmitProofActivity> scenario = ActivityScenario.launch(intent)) {
                waitForLoad();
                onView(withId(R.id.proof_submit)).perform(scrollTo(), click());
                scenario.onActivity(activity -> assertNotNull(
                        ((TextInputLayout) activity.findViewById(R.id.proof_description_layout)).getError()));
                onView(withId(R.id.proof_description_input)).perform(scrollTo(), replaceText(" Completed challenge with photo. "), closeSoftKeyboard());
                onView(withId(R.id.proof_submit)).perform(scrollTo(), click());
                assertNull(db.proofDao().getProofByChallengeId(challenge.id));
                onView(withId(R.id.choose_proof_photo)).perform(scrollTo(), click());
                // Cancel the system picker without losing the description.
                SystemClock.sleep(500);
                instrumentation.getUiAutomation().performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                instrumentation.waitForIdleSync();
                waitForLoad();
                onView(withId(R.id.proof_description_input)).check(matches(withText(" Completed challenge with photo. ")));
                onView(withId(R.id.choose_proof_photo)).perform(scrollTo(), click());
                chooseNativeImage(instrumentation);
                waitForLoad();
                onView(withId(R.id.proof_photo)).perform(scrollTo()).check(matches(isDisplayed()));
                scenario.recreate();
                waitForLoad();
                onView(withId(R.id.proof_photo)).perform(scrollTo()).check(matches(isDisplayed()));
                onView(withId(R.id.proof_description_input)).check(matches(withText(" Completed challenge with photo. ")));
                CountDownLatch blocked = new CountDownLatch(1);
                db.getTransactionExecutor().execute(() -> {
                    blocked.countDown();
                    try { release.await(30, TimeUnit.SECONDS); }
                    catch (InterruptedException error) { Thread.currentThread().interrupt(); }
                });
                assertTrue(blocked.await(5, TimeUnit.SECONDS));
                onView(withId(R.id.proof_submit)).perform(scrollTo(), click()).check(matches(not(isEnabled())));
                scenario.onActivity(activity -> activity.findViewById(R.id.proof_submit).performClick());
                scenario.recreate();
                waitForLoad();
                onView(withId(R.id.proof_submit)).perform(scrollTo()).check(matches(not(isEnabled())));
                release.countDown();
                long end = SystemClock.uptimeMillis() + 5000;
                while (db.proofDao().getProofByChallengeId(challenge.id) == null && SystemClock.uptimeMillis() < end) SystemClock.sleep(16);
            }
            Proof saved = db.proofDao().getProofByChallengeId(challenge.id);
            assertNotNull(saved);
            assertEquals("Completed challenge with photo.", saved.description);
            assertEquals(hunter.id, saved.hunterId);
            assertEquals(Proof.PENDING, saved.status);
            assertTrue(saved.submittedAt > 0);
            assertEquals(Challenge.SUBMITTED, db.challengeDao().getChallengeById(challenge.id).status);
            assertTrue(context.getContentResolver().getPersistedUriPermissions().stream()
                    .anyMatch(permission -> permission.isReadPermission() && permission.getUri().toString().equals(saved.imageUri)));
            assertEquals(0, db.submitProof(proof(challenge.id, hunter.id, image.toString())));
            assertEquals(saved.id, db.proofDao().getProofByChallengeId(challenge.id).id);
            assertEquals(hunter.demoBalance, session.getCurrentUser().demoBalance, 0);
            // An insert failure must also roll back the status transition.
            challenge.status = Challenge.ACCEPTED;
            db.challengeDao().updateChallenge(challenge);
            Proof duplicate = proof(challenge.id, hunter.id, image.toString());
            duplicate.id = saved.id;
            try { db.submitProof(duplicate); fail("Duplicate primary key must fail"); }
            catch (android.database.sqlite.SQLiteConstraintException expected) { }
            assertEquals(Challenge.ACCEPTED, db.challengeDao().getChallengeById(challenge.id).status);
        } finally {
            release.countDown();
            db.challengeDao().deleteChallenge(challenge);
            context.getContentResolver().delete(image, null, null);
            session.logout();
        }
    }
    private Proof proof(int id, String hunter, String uri) {
        Proof proof = new Proof();
        proof.challengeId = id;
        proof.hunterId = hunter;
        proof.description = "Proof description";
        proof.imageUri = uri;
        proof.submittedAt = System.currentTimeMillis();
        return proof;
    }
    private void chooseNativeImage(Instrumentation instrumentation) {
        long end = SystemClock.uptimeMillis() + 10000;
        do {
            AccessibilityNodeInfo root = instrumentation.getUiAutomation().getRootInActiveWindow();
            AccessibilityNodeInfo node = findImage(root);
            if (node != null) {
                android.graphics.Rect bounds = new android.graphics.Rect();
                node.getBoundsInScreen(bounds);
                try (android.os.ParcelFileDescriptor command = instrumentation.getUiAutomation()
                        .executeShellCommand("input tap " + bounds.centerX() + " " + bounds.centerY());
                     java.io.InputStream output = new java.io.FileInputStream(command.getFileDescriptor())) {
                    while (output.read() != -1) { }
                } catch (java.io.IOException error) { throw new AssertionError(error); }
                long returned = SystemClock.uptimeMillis() + 5000;
                do {
                    AccessibilityNodeInfo active = instrumentation.getUiAutomation().getRootInActiveWindow();
                    if (active != null && instrumentation.getTargetContext().getPackageName().contentEquals(active.getPackageName())) {
                        instrumentation.waitForIdleSync();
                        return;
                    }
                    SystemClock.sleep(100);
                } while (SystemClock.uptimeMillis() < returned);
                throw new AssertionError("Picker did not return after tapping " + bounds);

            }
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() < end);
        throw new AssertionError("Native picker did not show M7-proof.png");
    }
    private AccessibilityNodeInfo findImage(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if ((node.getText() != null && node.getText().toString().contains("M7-proof"))
                || (node.getContentDescription() != null && node.getContentDescription().toString().startsWith("M7-proof"))) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findImage(node.getChild(i));
            if (found != null) return found;
        }
        return null;
    }
    private void waitForLoad() {
        onView(isRoot()).perform(new ViewAction() {
            public Matcher<View> getConstraints() { return isRoot(); }
            public String getDescription() { return "wait for proof challenge"; }
            public void perform(UiController ui, View root) {
                long end = SystemClock.uptimeMillis() + 5000;
                do {
                    View loading = root.findViewById(R.id.proof_loading);
                    if (loading != null && loading.getVisibility() == View.GONE) return;
                    ui.loopMainThreadForAtLeast(16);
                } while (SystemClock.uptimeMillis() < end);
                throw new AssertionError("Proof screen did not finish loading");
            }
        });
    }
}
