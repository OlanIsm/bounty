package id.ac.binus.bounty;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import id.ac.binus.bounty.activities.ChallengeDetailActivity;
import id.ac.binus.bounty.activities.MainActivity;
import id.ac.binus.bounty.database.AppDatabase;
import id.ac.binus.bounty.database.ChallengeDao;
import id.ac.binus.bounty.models.Challenge;
import id.ac.binus.bounty.utils.SessionManager;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ChallengeFeedTest {
    @Test
    public void roomFeedOpensDetailRestoresRefreshesAndHandlesMissingData() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SessionManager session = new SessionManager(context);
        assertTrue(session.login(SessionManager.DEMO_EMAIL));
        ChallengeDao dao = AppDatabase.getInstance(context).challengeDao();
        List<Challenge> original = dao.getAllChallenges();
        Challenge challenge = new Challenge();
        challenge.title = "M4 feed check";
        challenge.creatorId = "feed_test_creator";
        challenge.creatorName = "Test Creator";
        challenge.description = "Description from Room, including the full proof instructions.";
        challenge.reward = 25000;
        challenge.deadline = "2030-12-31";
        challenge.createdAt = System.currentTimeMillis() + 1000;
        challenge.id = (int) dao.insertChallenge(challenge);
        try (ActivityScenario<MainActivity> main = ActivityScenario.launch(MainActivity.class)) {
            waitForLoad(R.id.feed_loading, original.size() + 1);
            onView(withText(challenge.title)).check(matches(isDisplayed())).perform(click());
            waitForLoad(R.id.detail_loading, -1);
            checkDetail(challenge);
            onView(withId(R.id.challenge_deadline)).check(matches(withText("Batas waktu: 31 Des 2030")));
            pressBack();
            waitForLoad(R.id.feed_loading, original.size() + 1);
            main.recreate();
            waitForLoad(R.id.feed_loading, original.size() + 1);
            onView(withText(challenge.title)).check(matches(isDisplayed()));

            Intent detail = new Intent(context, ChallengeDetailActivity.class)
                    .putExtra(ChallengeDetailActivity.EXTRA_CHALLENGE_ID, challenge.id);
            challenge.status = Challenge.ACCEPTED;
            challenge.deadline = "31 Dec";
            dao.updateChallenge(challenge);
            onView(withId(R.id.nav_profile)).perform(click());
            onView(withId(R.id.nav_home)).perform(click());
            waitForLoad(R.id.feed_loading, original.size() + 1);
            onView(withText(challenge.title)).perform(click());
            waitForLoad(R.id.detail_loading, -1);
            onView(withId(R.id.challenge_status)).check(matches(withText(Challenge.ACCEPTED)));
            onView(withId(R.id.challenge_deadline)).check(matches(withText("Batas waktu: 31 Dec")));
            pressBack();

            for (Challenge item : dao.getAllChallenges()) dao.deleteChallenge(item);
            onView(withId(R.id.nav_profile)).perform(click());
            onView(withId(R.id.nav_home)).perform(click());
            waitForLoad(R.id.feed_loading, 0);
            onView(withId(R.id.feed_message)).check(matches(withText(R.string.no_available_challenges)));
            // ActivityScenario's launcher clears the task; finish Home checks before standalone detail scenarios.
            dao.insertChallenge(challenge);
            try (ActivityScenario<ChallengeDetailActivity> scenario = ActivityScenario.launch(detail)) {
                waitForLoad(R.id.detail_loading, -1);
                scenario.recreate();
                waitForLoad(R.id.detail_loading, -1);
                checkDetail(challenge);
            }
            dao.deleteChallenge(challenge);
            try (ActivityScenario<ChallengeDetailActivity> missing = ActivityScenario.launch(detail)) {
                waitForLoad(R.id.detail_loading, -1);
                onView(withId(R.id.detail_message)).check(matches(withText(R.string.challenge_not_found)));
                missing.recreate();
                waitForLoad(R.id.detail_loading, -1);
                onView(withId(R.id.detail_message)).check(matches(isDisplayed()));
            }
            try (ActivityScenario<ChallengeDetailActivity> invalid = ActivityScenario.launch(
                    new Intent(context, ChallengeDetailActivity.class))) {
                waitForLoad(R.id.detail_loading, -1);
                onView(withId(R.id.detail_message)).check(matches(withText(R.string.challenge_not_found)));
            }
        } finally {
            dao.deleteChallenge(challenge);
            for (Challenge item : original) {
                if (dao.getChallengeById(item.id) == null) dao.insertChallenge(item);
            }
            session.logout();
        }
    }

    private void checkDetail(Challenge challenge) {
        onView(withId(R.id.detail_content)).check(matches(isDisplayed()));
        onView(withId(R.id.challenge_title)).check(matches(withText(challenge.title)));
        onView(withId(R.id.challenge_creator)).check(matches(withText(challenge.creatorName)));
        onView(withId(R.id.challenge_description)).check(matches(withText(challenge.description)));
        onView(withId(R.id.challenge_reward)).check(matches(withText("Rp25.000")));
        onView(withId(R.id.challenge_status)).check(matches(withText(challenge.status)));
    }

    // Wait for the real worker/UI result; Espresso does not track Room's query executor.
    private void waitForLoad(int loadingId, int feedSize) {
        onView(isRoot()).perform(new ViewAction() {
            @Override public Matcher<View> getConstraints() { return isRoot(); }
            @Override public String getDescription() { return "wait for Room content to bind"; }
            @Override public void perform(UiController ui, View root) {
                long end = SystemClock.uptimeMillis() + 5000;
                do {
                    View loading = root.findViewById(loadingId);
                    RecyclerView list = root.findViewById(R.id.challenge_list);
                    if (loading != null && loading.getVisibility() == View.GONE
                            && (feedSize < 0 || list.getAdapter().getItemCount() == feedSize)) return;
                    ui.loopMainThreadForAtLeast(16);
                } while (SystemClock.uptimeMillis() < end);
                throw new AssertionError("Room content did not load within 5 seconds");
            }
        });
    }
}
