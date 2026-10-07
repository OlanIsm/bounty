package id.ac.binus.bounty;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Test;
import org.junit.runner.RunWith;

import id.ac.binus.bounty.activities.MainActivity;
import id.ac.binus.bounty.utils.SessionManager;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class MainNavigationTest {
    @Test
    public void allDestinationsRestoreAndLogoutWorksFromProfile() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SessionManager session = new SessionManager(context);
        assertTrue(session.login(SessionManager.DEMO_EMAIL));
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            checkDestination(scenario, R.id.nav_home, R.id.home_screen, R.string.home_title);
            onView(withId(R.id.welcome_text)).check(matches(withText("Halo, Insan!")));
            onView(withId(R.id.nav_my_challenges)).perform(click());
            checkDestination(scenario, R.id.nav_my_challenges, R.id.my_challenges_screen, R.string.my_challenges_title);
            scenario.recreate();
            checkDestination(scenario, R.id.nav_my_challenges, R.id.my_challenges_screen, R.string.my_challenges_title);
            onView(withId(R.id.nav_create)).perform(click());
            checkDestination(scenario, R.id.nav_create, R.id.create_screen, R.string.create_title);
            scenario.recreate();
            checkDestination(scenario, R.id.nav_create, R.id.create_screen, R.string.create_title);
            pressBack();
            checkDestination(scenario, R.id.nav_home, R.id.home_screen, R.string.home_title);
            onView(withId(R.id.nav_profile)).perform(click());
            checkDestination(scenario, R.id.nav_profile, R.id.profile_screen, R.string.profile_title);
            onView(withId(R.id.profile_name)).check(matches(withText("Insan")));
            onView(withId(R.id.user_email)).check(matches(withText(SessionManager.DEMO_EMAIL)));
            scenario.recreate();
            onView(withId(R.id.profile_name)).check(matches(withText("Insan")));
            checkDestination(scenario, R.id.nav_profile, R.id.profile_screen, R.string.profile_title);
            onView(withId(R.id.nav_profile)).perform(click());
            onView(withId(R.id.logout_button)).perform(click());
            onView(withId(R.id.login_button)).check(matches(isDisplayed()));
        } finally {
            session.logout();
        }
    }

    private void checkDestination(ActivityScenario<MainActivity> scenario, int destination, int screen, int title) {
        onView(withId(screen)).check(matches(isDisplayed()));
        scenario.onActivity(activity -> {
            assertEquals(1, ((android.widget.FrameLayout) activity.findViewById(R.id.screen_content)).getChildCount());
            BottomNavigationView navigation = activity.findViewById(R.id.bottom_navigation);
            MaterialToolbar toolbar = activity.findViewById(R.id.main_toolbar);
            assertEquals(destination, navigation.getSelectedItemId());
            assertEquals(activity.getString(title), toolbar.getTitle().toString());
        });
    }
}
