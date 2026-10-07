package id.ac.binus.bounty;

import android.app.Instrumentation;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.OutputStream;
import java.time.LocalDate;
import id.ac.binus.bounty.activities.SplashActivity;
import id.ac.binus.bounty.database.AppDatabase;
import id.ac.binus.bounty.models.Challenge;
import id.ac.binus.bounty.models.Proof;
import id.ac.binus.bounty.models.User;
import id.ac.binus.bounty.utils.SessionManager;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

/** Continuous public UI flow; Test-Android.ps1 checks the final state after a process restart. */
@RunWith(AndroidJUnit4.class)
public class MainFlowTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final Context context = instrumentation.getTargetContext();
    private final String title = "M12 complete challenge";

    @Test public void creatorAndHunterCompleteRejectResubmitApproveFlow() throws Exception {
        // The restart fixture is intentionally retained only in the disposable installation.
        assertEquals("id.ac.binus.bounty.testing", context.getPackageName());
        android.accessibilityservice.AccessibilityServiceInfo info = instrumentation.getUiAutomation().getServiceInfo();
        int originalFlags = info.flags;
        info.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        instrumentation.getUiAutomation().setServiceInfo(info);
        SessionManager session = new SessionManager(context);
        session.logout();
        AppDatabase db = AppDatabase.getInstance(context);
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "M12-proof.png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        Uri image = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        assertNotNull(image);
        Bitmap bitmap = Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(android.graphics.Color.GREEN);
        try (OutputStream output = context.getContentResolver().openOutputStream(image)) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        }
        bitmap.recycle();
        try (ActivityScenario<SplashActivity> launch = ActivityScenario.launch(SplashActivity.class)) {
            register("M12 Creator", "m12-creator@bounty.local");
            login("m12-creator@bounty.local");
            User creator = session.getCurrentUser();
            assertNotNull(creator);
            int before = db.challengeDao().getAllChallenges().size();
            onView(withId(R.id.nav_create)).perform(click());
            onView(withId(R.id.publish_button)).perform(scrollTo(), click());
            assertEquals(before, db.challengeDao().getAllChallenges().size());
            input(R.id.create_title_input, title);
            input(R.id.create_description_input, "Send a photo of the completed challenge.");
            input(R.id.create_reward_input, "20000");
            input(R.id.create_deadline_input, LocalDate.now().plusDays(7).toString());
            onView(withId(R.id.publish_button)).perform(scrollTo(), click());
            awaitView(R.id.challenge_title, title);
            Challenge challenge = db.challengeDao().getChallengesByCreator(creator.id).stream()
                    .filter(row -> title.equals(row.title)).findFirst().orElseThrow(AssertionError::new);
            assertEquals(Challenge.OPEN, challenge.status);
            assertEquals(20000, challenge.reward, 0);
            logout();

            register("M12 Hunter", "m12-hunter@bounty.local");
            login("m12-hunter@bounty.local");
            User hunter = session.getCurrentUser();
            assertEquals(50000, hunter.demoBalance, 0);
            openChallenge();
            onView(withId(R.id.accept_button)).perform(scrollTo(), click());
            onView(withId(android.R.id.button1)).perform(click());
            awaitView(R.id.challenge_status, Challenge.ACCEPTED);
            assertEquals(hunter.id, db.challengeDao().getChallengeById(challenge.id).participantId);
            submit("First submission");
            assertEquals(Proof.PENDING, db.proofDao().getProofByChallengeId(challenge.id).status);
            pressBack(); awaitView(R.id.nav_profile, null); logout();

            login("m12-creator@bounty.local");
            openChallenge();
            review();
            onView(withId(R.id.review_reject)).perform(scrollTo(), click());
            awaitView(R.id.challenge_status, Challenge.ACCEPTED);
            Proof rejected = db.proofDao().getProofByChallengeId(challenge.id);
            assertEquals(Proof.REJECTED, rejected.status);
            pressBack(); awaitView(R.id.nav_profile, null); logout();

            login("m12-hunter@bounty.local");
            assertEquals(50000, session.getCurrentUser().demoBalance, 0);
            openChallenge();
            submit("Resubmitted after rejection");
            Proof resubmitted = db.proofDao().getProofByChallengeId(challenge.id);
            assertTrue(resubmitted.id > rejected.id);
            assertEquals(Proof.PENDING, resubmitted.status);
            assertEquals("Resubmitted after rejection", resubmitted.description);
            pressBack(); awaitView(R.id.nav_profile, null); logout();

            login("m12-creator@bounty.local");
            openChallenge();
            review();
            onView(withId(R.id.review_description)).check(matches(withText(resubmitted.description)));
            onView(withId(R.id.review_approve)).perform(scrollTo(), click());
            onView(withId(android.R.id.button1)).perform(click());
            awaitView(R.id.challenge_status, Challenge.COMPLETED);
            awaitView(R.id.reward_result, context.getString(R.string.reward_result_paid, "Rp20.000", hunter.name));
            assertEquals(Proof.APPROVED, db.proofDao().getProofByChallengeId(challenge.id).status);
            assertEquals(50000, session.getCurrentUser().demoBalance, 0);
            pressBack(); awaitView(R.id.nav_profile, null); logout();

            login("m12-hunter@bounty.local");
            awaitView(R.id.demo_balance_amount, "Rp70.000");
            assertEquals(70000, session.getCurrentUser().demoBalance, 0);
            assertTrue(session.isRewardCredited(challenge.id, hunter.id));
            onView(withId(R.id.nav_my_challenges)).perform(click());
            awaitView(R.id.challenge_title, title);
            onView(withText(title)).perform(click());
            awaitView(R.id.challenge_status, Challenge.COMPLETED);
            pressBack(); awaitView(R.id.nav_home, null);
            onView(withId(R.id.nav_home)).perform(click());
            awaitView(R.id.demo_balance_amount, "Rp70.000");
        } finally {
            context.getContentResolver().delete(image, null, null);
            info.flags = originalFlags;
            instrumentation.getUiAutomation().setServiceInfo(info);
        }
    }

    private void register(String name, String email) {
        awaitView(R.id.register_link, null);
        onView(withId(R.id.register_link)).perform(scrollTo(), click());
        input(R.id.name_input, name); input(R.id.email_input, email);
        onView(withId(R.id.register_button)).perform(scrollTo(), click());
        awaitView(R.id.login_button, null);
    }
    private void login(String email) {
        awaitView(R.id.login_button, null);
        input(R.id.email_input, email);
        onView(withId(R.id.login_button)).perform(scrollTo(), click());
        awaitView(R.id.nav_home, null);
    }
    private void logout() {
        onView(withId(R.id.nav_profile)).perform(click());
        awaitView(R.id.logout_button, null);
        onView(withId(R.id.logout_button)).perform(scrollTo(), click());
        awaitView(R.id.login_button, null);
        assertFalse(new SessionManager(context).isLoggedIn());
    }
    private void openChallenge() {
        onView(withId(R.id.nav_home)).perform(click());
        awaitView(R.id.challenge_title, title);
        onView(withText(title)).perform(click());
        awaitView(R.id.challenge_status, null);
    }
    private void submit(String description) {
        onView(withId(R.id.submit_proof_button)).perform(scrollTo(), click());
        awaitView(R.id.proof_submit, null);
        input(R.id.proof_description_input, description);
        onView(withId(R.id.choose_proof_photo)).perform(scrollTo(), click());
        SubmitProofTest.chooseNativeImage(instrumentation, "M12-proof");
        awaitView(R.id.proof_submit, null);
        onView(withId(R.id.proof_submit)).perform(scrollTo(), click());
        awaitView(R.id.challenge_status, Challenge.SUBMITTED);
    }
    private void review() {
        onView(withId(R.id.review_proof_button)).perform(scrollTo(), click());
        awaitView(R.id.review_approve, null);
    }
    private void input(int id, String text) {
        onView(withId(id)).perform(scrollTo(), replaceText(text), closeSoftKeyboard());
    }
    private void awaitView(int id, String text) {
        String resource = context.getResources().getResourceEntryName(id);
        long end = SystemClock.uptimeMillis() + 20000;
        do {
            AccessibilityNodeInfo node = find(instrumentation.getUiAutomation().getRootInActiveWindow(),
                    context.getPackageName() + ":id/" + resource, text);
            if (node != null && node.isEnabled()) {
                instrumentation.waitForIdleSync();
                return;
            }
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() < end);
        AccessibilityNodeInfo active = instrumentation.getUiAutomation().getRootInActiveWindow();
        throw new AssertionError("UI did not show " + resource + " / " + text + "; active package: "
                + (active == null ? "none" : active.getPackageName()));
    }
    private AccessibilityNodeInfo find(AccessibilityNodeInfo node, String resource, String text) {
        if (node == null) return null;
        if (resource.equals(node.getViewIdResourceName()) && (node.isVisibleToUser() || resource.endsWith(":id/proof_submit"))
                && (text == null || (node.getText() != null && text.contentEquals(node.getText())))) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = find(node.getChild(i), resource, text);
            if (found != null) return found;
        }
        return null;
    }
}
