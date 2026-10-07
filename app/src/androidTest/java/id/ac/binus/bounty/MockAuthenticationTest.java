package id.ac.binus.bounty;

import android.content.Context;
import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import id.ac.binus.bounty.activities.MainActivity;
import id.ac.binus.bounty.activities.SplashActivity;
import id.ac.binus.bounty.models.User;
import id.ac.binus.bounty.utils.SessionManager;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBackUnconditionally;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class MockAuthenticationTest {
    @Test
    public void registerLoginPersistLogoutAndGuardHome() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("bounty_accounts", Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences("bounty_session", Context.MODE_PRIVATE).edit().clear().commit();
        SessionManager session = new SessionManager(context);
        try {
            assertFalse(session.isLoggedIn());
            assertFalse(session.login(null));
            assertFalse(session.login("unknown@example.com"));
            try {
                session.register(" ", "invalid");
                fail("Invalid registration must be rejected");
            } catch (IllegalArgumentException expected) {
                assertFalse(session.isLoggedIn());
            }

            launch(context, SplashActivity.class);
            onView(withId(R.id.login_button)).perform(click());
            onView(withText(R.string.invalid_email)).check(matches(isDisplayed()));
            onView(withId(R.id.email_input)).perform(replaceText("unknown@example.com"), closeSoftKeyboard());
            onView(withId(R.id.login_button)).perform(click());
            onView(withText(R.string.unknown_account)).check(matches(isDisplayed()));
            onView(withId(R.id.register_link)).perform(click());
            onView(withId(R.id.register_button)).perform(click());
            onView(withText(R.string.required_name)).check(matches(isDisplayed()));
            onView(withText(R.string.invalid_email)).check(matches(isDisplayed()));
            onView(withId(R.id.name_input)).perform(replaceText(" Hunter "));
            onView(withId(R.id.email_input)).perform(replaceText(" HUNTER@example.com "), closeSoftKeyboard());
            onView(withId(R.id.register_button)).perform(click());
            onView(withId(R.id.email_input)).check(matches(withText("hunter@example.com")));
            assertFalse(session.isLoggedIn());

            onView(withId(R.id.register_link)).perform(click());
            onView(withId(R.id.name_input)).perform(replaceText("Other Hunter"));
            onView(withId(R.id.email_input)).perform(replaceText("hunter@example.com"), closeSoftKeyboard());
            onView(withId(R.id.register_button)).perform(click());
            onView(withText(R.string.duplicate_email)).check(matches(isDisplayed()));
            onView(withId(R.id.login_link)).perform(click());
            onView(withId(R.id.login_button)).perform(click());
            onView(withId(R.id.welcome_text)).check(matches(withText("Halo, Hunter!")));
            User user = new SessionManager(context).getCurrentUser();
            assertNotNull(user);
            assertEquals("Hunter", user.name);
            assertEquals("hunter@example.com", user.email);
            assertEquals("", user.avatarUrl);
            assertEquals(50000, user.demoBalance, 0);
            String userId = user.id;

            launch(context, SplashActivity.class);
            onView(withId(R.id.logout_button)).check(matches(isDisplayed()));
            assertEquals(userId, new SessionManager(context).getCurrentUser().id);
            onView(withId(R.id.logout_button)).perform(click());
            onView(withId(R.id.login_button)).check(matches(isDisplayed()));
            assertFalse(new SessionManager(context).isLoggedIn());
            pressBackUnconditionally();
            launch(context, MainActivity.class);
            onView(withId(R.id.login_button)).check(matches(isDisplayed()));

            assertTrue(session.login(" HUNTER@EXAMPLE.COM "));
            assertEquals(userId, session.getCurrentUser().id);
            session.logout();
            onView(withId(R.id.email_input)).perform(replaceText(SessionManager.DEMO_EMAIL), closeSoftKeyboard());
            onView(withId(R.id.login_button)).perform(click());
            onView(withId(R.id.welcome_text)).check(matches(withText("Halo, Insan!")));
            assertEquals("local_user_001", session.getCurrentUser().id);
            onView(withId(R.id.logout_button)).perform(click());
            assertNull(session.getCurrentUser());
        } finally {
            session.logout();
            context.getSharedPreferences("bounty_accounts", Context.MODE_PRIVATE)
                    .edit().remove("hunter@example.com").commit();
        }
    }

    private void launch(Context context, Class<?> activity) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                context.startActivity(new Intent(context, activity)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
