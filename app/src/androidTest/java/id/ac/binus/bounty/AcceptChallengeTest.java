package id.ac.binus.bounty;

import android.content.Context;
import android.content.Intent;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import id.ac.binus.bounty.activities.ChallengeDetailActivity;
import id.ac.binus.bounty.activities.MainActivity;
import id.ac.binus.bounty.database.AppDatabase;
import id.ac.binus.bounty.models.Challenge;
import id.ac.binus.bounty.models.User;
import id.ac.binus.bounty.utils.SessionManager;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AcceptChallengeTest {
    @Test public void acceptancePersistsOnceAcrossRotationAndAppearsOnlyForItsUser() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SessionManager session = new SessionManager(context);
        assertTrue(session.login(SessionManager.DEMO_EMAIL));
        User hunter = session.getCurrentUser();
        AppDatabase db = AppDatabase.getInstance(context);
        Challenge challenge = new Challenge();
        challenge.title = "M6 accepted challenge";
        challenge.creatorId = "m6_creator";
        challenge.creatorName = "Creator";
        challenge.description = "Accept this challenge.";
        challenge.deadline = "2030-12-31";
        challenge.reward = 20000;
        challenge.createdAt = System.currentTimeMillis();
        challenge.id = (int) db.challengeDao().insertChallenge(challenge);
        CountDownLatch release = new CountDownLatch(1);
        try {
            assertEquals(0, db.challengeDao().acceptChallenge(challenge.id, challenge.creatorId, "Creator"));
            Intent detail = new Intent(context, ChallengeDetailActivity.class)
                    .putExtra(ChallengeDetailActivity.EXTRA_CHALLENGE_ID, challenge.id);
            try (ActivityScenario<ChallengeDetailActivity> scenario = ActivityScenario.launch(detail)) {
                waitForLoad(R.id.detail_loading);
                onView(withId(R.id.accept_button)).perform(scrollTo()).check(matches(isEnabled()));
                onView(withId(R.id.accept_button)).perform(click());
                assertEquals(Challenge.OPEN, db.challengeDao().getChallengeById(challenge.id).status);
                onView(withId(android.R.id.button2)).perform(click());
                assertEquals(Challenge.OPEN, db.challengeDao().getChallengeById(challenge.id).status);
                onView(withId(R.id.accept_button)).perform(click());
                scenario.recreate();
                onView(withText(R.string.confirm_accept_title)).check(matches(isDisplayed()));
                CountDownLatch blocked = new CountDownLatch(1);
                db.getTransactionExecutor().execute(() -> {
                    blocked.countDown();
                    try { release.await(30, TimeUnit.SECONDS); }
                    catch (InterruptedException error) { Thread.currentThread().interrupt(); }
                });
                assertTrue(blocked.await(5, TimeUnit.SECONDS));
                onView(withId(android.R.id.button1)).perform(click());
                onView(withId(R.id.accept_button)).check(matches(not(isEnabled())));
                scenario.onActivity(activity -> activity.findViewById(R.id.accept_button).performClick());
                scenario.recreate();
                waitForLoad(R.id.detail_loading);
                onView(withId(R.id.accept_button)).perform(scrollTo()).check(matches(not(isEnabled())));
                release.countDown();
                onView(isRoot()).perform(new ViewAction() {
                    public Matcher<View> getConstraints() { return isRoot(); }
                    public String getDescription() { return "wait for ACCEPTED detail"; }
                    public void perform(UiController ui, View root) {
                        long end = SystemClock.uptimeMillis() + 5000;
                        do {
                            android.widget.TextView status = root.findViewById(R.id.challenge_status);
                            if (status != null && Challenge.ACCEPTED.contentEquals(status.getText())) return;
                            ui.loopMainThreadForAtLeast(16);
                        } while (SystemClock.uptimeMillis() < end);
                        throw new AssertionError("Acceptance did not refresh detail");
                    }
                });
                onView(withId(R.id.accept_button)).perform(scrollTo()).check(matches(not(isEnabled())));
                scenario.recreate();
                waitForLoad(R.id.detail_loading);
                onView(withId(R.id.challenge_status)).check(matches(withText(Challenge.ACCEPTED)));
            }
            Challenge saved = db.challengeDao().getChallengeById(challenge.id);
            assertEquals(Challenge.ACCEPTED, saved.status);
            assertEquals(hunter.id, saved.participantId);
            assertEquals(hunter.name, saved.participantName);
            assertEquals(0, db.challengeDao().acceptChallenge(challenge.id, "second_hunter", "Other"));
            assertEquals(0, db.challengeDao().acceptChallenge(-1, hunter.id, hunter.name));
            assertEquals(hunter.id, db.challengeDao().getChallengeById(challenge.id).participantId);
            assertTrue(db.challengeDao().getMyChallenges(hunter.id).stream().anyMatch(row -> row.id == challenge.id));
            assertTrue(db.challengeDao().getMyChallenges(challenge.creatorId).stream().anyMatch(row -> row.id == challenge.id));
            assertTrue(db.challengeDao().getMyChallenges("second_hunter").isEmpty());
            assertEquals(hunter.demoBalance, session.getCurrentUser().demoBalance, 0);
            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                onView(withId(R.id.nav_my_challenges)).perform(click());
                waitForLoad(R.id.feed_loading);
                onView(withText(challenge.title)).check(matches(isDisplayed()));
                scenario.recreate();
                waitForLoad(R.id.feed_loading);
                onView(withText(challenge.title)).check(matches(isDisplayed()));
            }
        } finally {
            release.countDown();
            db.challengeDao().deleteChallenge(challenge);
            session.logout();
        }
    }
    private void waitForLoad(int id) {
        onView(isRoot()).perform(new ViewAction() {
            public Matcher<View> getConstraints() { return isRoot(); }
            public String getDescription() { return "wait for Room list/detail"; }
            public void perform(UiController ui, View root) {
                long end = SystemClock.uptimeMillis() + 5000;
                do {
                    View loading = root.findViewById(id);
                    if (loading != null && loading.getVisibility() == View.GONE) return;
                    ui.loopMainThreadForAtLeast(16);
                } while (SystemClock.uptimeMillis() < end);
                throw new AssertionError("Room load timed out");
            }
        });
    }
}
