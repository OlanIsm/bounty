package id.ac.binus.bounty;

import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.widget.DatePicker;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.textfield.TextInputLayout;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import id.ac.binus.bounty.activities.MainActivity;
import id.ac.binus.bounty.database.AppDatabase;
import id.ac.binus.bounty.models.Challenge;
import id.ac.binus.bounty.models.User;
import id.ac.binus.bounty.utils.SessionManager;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
public class CreateChallengeTest {
    @Test
    public void validationDraftRestorationAndSinglePublishRefreshHome() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SessionManager session = new SessionManager(context);
        assertTrue(session.login(SessionManager.DEMO_EMAIL));
        User creator = session.getCurrentUser();
        AppDatabase db = AppDatabase.getInstance(context);
        int count = db.challengeDao().getAllChallenges().size();
        CountDownLatch release = new CountDownLatch(1);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.nav_create)).perform(click());
            publish();
            scenario.onActivity(activity -> {
                for (int id : new int[]{R.id.create_title_layout, R.id.create_description_layout,
                        R.id.create_reward_layout, R.id.create_deadline_layout}) {
                    assertNotNull(((TextInputLayout) activity.findViewById(id)).getError());
                }
            });
            assertEquals(count, db.challengeDao().getAllChallenges().size());
            input(R.id.create_title_input, " M5 published challenge ");
            input(R.id.create_description_input, " Complete this local challenge and send proof. ");
            input(R.id.create_reward_input, "0");
            input(R.id.create_deadline_input, "2030-02-30");
            publish();
            scenario.onActivity(activity -> {
                assertNotNull(((TextInputLayout) activity.findViewById(R.id.create_reward_layout)).getError());
                assertNotNull(((TextInputLayout) activity.findViewById(R.id.create_deadline_layout)).getError());
            });
            input(R.id.create_reward_input, "45000");
            input(R.id.create_deadline_input, LocalDate.now().minusDays(1).toString());
            publish();
            scenario.onActivity(activity -> assertNotNull(
                    ((TextInputLayout) activity.findViewById(R.id.create_deadline_layout)).getError()));
            assertEquals(count, db.challengeDao().getAllChallenges().size());

            onView(withContentDescription(R.string.choose_deadline)).perform(scrollTo(), click());
            onView(withClassName(equalTo(DatePicker.class.getName()))).check(matches(isDisplayed()));
            onView(withId(android.R.id.button1)).perform(click());
            String deadline = LocalDate.now().plusDays(7).toString();
            input(R.id.create_deadline_input, deadline);
            onView(withId(R.id.nav_home)).perform(click());
            onView(withId(R.id.nav_create)).perform(click());
            onView(withId(R.id.create_title_input)).check(matches(withText(" M5 published challenge ")));
            scenario.recreate();
            onView(withId(R.id.create_reward_input)).check(matches(withText("45000")));
            onView(withId(R.id.create_deadline_input)).check(matches(withText(deadline)));
            // Also preserve a detached Create draft when another tab is recreated.
            onView(withId(R.id.nav_profile)).perform(click());
            scenario.recreate();
            onView(withId(R.id.nav_create)).perform(click());
            onView(withId(R.id.create_title_input)).check(matches(withText(" M5 published challenge ")));

            CountDownLatch blocked = new CountDownLatch(1);
            db.getTransactionExecutor().execute(() -> {
                blocked.countDown();
                try { release.await(30, TimeUnit.SECONDS); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            });
            assertTrue(blocked.await(5, TimeUnit.SECONDS));
            publish();
            onView(withId(R.id.publish_button)).check(matches(not(isEnabled())));
            scenario.onActivity(activity -> activity.findViewById(R.id.publish_button).performClick());
            scenario.recreate();
            onView(withId(R.id.publish_button)).check(matches(not(isEnabled())));
            release.countDown();
            waitForPublishedFeed();
            List<Challenge> rows = db.challengeDao().getAllChallenges();
            assertEquals(count + 1, rows.size());
            Challenge saved = rows.get(0);
            assertEquals("M5 published challenge", saved.title);
            assertEquals("Complete this local challenge and send proof.", saved.description);
            assertEquals(45000, saved.reward, 0);
            assertEquals(deadline, saved.deadline);
            assertEquals(creator.id, saved.creatorId);
            assertEquals(creator.name, saved.creatorName);
            assertEquals(creator.avatarUrl, saved.creatorAvatar);
            assertEquals(Challenge.OPEN, saved.status);
            assertNull(saved.participantId);
            assertTrue(saved.createdAt > 0);
            assertEquals(creator.demoBalance, session.getCurrentUser().demoBalance, 0);
            onView(withId(R.id.nav_create)).perform(click());
            onView(withId(R.id.create_title_input)).check(matches(withText("")));
            onView(withId(R.id.nav_home)).perform(click());
            waitForPublishedFeed();
            onView(withText(saved.title)).check(matches(isDisplayed())).perform(click());
            onView(withId(R.id.detail_toolbar)).check(matches(isDisplayed()));
            pressBack();
        } finally {
            release.countDown();
            for (Challenge row : db.challengeDao().getChallengesByCreator(creator.id)) {
                if (row.title.equals("M5 published challenge")) db.challengeDao().deleteChallenge(row);
            }
            session.logout();
        }
    }

    private void input(int id, String value) {
        onView(withId(id)).perform(scrollTo(), replaceText(value), closeSoftKeyboard());
    }
    private void publish() {
        onView(withId(R.id.publish_button)).perform(scrollTo(), click());
    }
    private void waitForPublishedFeed() {
        onView(isRoot()).perform(new ViewAction() {
            @Override public Matcher<View> getConstraints() { return isRoot(); }
            @Override public String getDescription() { return "wait for published challenge in Home"; }
            @Override public void perform(UiController ui, View root) {
                long end = SystemClock.uptimeMillis() + 5000;
                do {
                    View loading = root.findViewById(R.id.feed_loading);
                    if (loading != null && loading.getVisibility() == View.GONE) return;
                    ui.loopMainThreadForAtLeast(16);
                } while (SystemClock.uptimeMillis() < end);
                throw new AssertionError("Publish did not refresh Home within 5 seconds");
            }
        });
    }
}
