package id.ac.binus.bounty;

import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import id.ac.binus.bounty.activities.LoginActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class FirebaseAuthenticationFormTest {
    @Test
    public void validatesLoginAndRegistrationWithoutCreatingAnAccount() {
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                InstrumentationRegistry.getInstrumentation().getTargetContext().startActivity(intent));

        onView(withId(R.id.login_button)).perform(click());
        onView(withText(R.string.invalid_email)).check(matches(isDisplayed()));
        onView(withId(R.id.email_input)).perform(replaceText("hunter@example.com"));
        onView(withId(R.id.login_button)).perform(click());
        onView(withText(R.string.required_password)).check(matches(isDisplayed()));

        onView(withId(R.id.register_link)).perform(click());
        onView(withId(R.id.register_button)).perform(click());
        onView(withText(R.string.required_name)).check(matches(isDisplayed()));
        onView(withText(R.string.invalid_email)).check(matches(isDisplayed()));
        onView(withText(R.string.weak_password)).check(matches(isDisplayed()));
        onView(withId(R.id.name_input)).perform(replaceText("Hunter"));
        onView(withId(R.id.email_input)).perform(replaceText("not-an-email"));
        onView(withId(R.id.password_input)).perform(replaceText("short"), closeSoftKeyboard());
        onView(withId(R.id.register_button)).perform(click());
        onView(withText(R.string.invalid_email)).check(matches(isDisplayed()));
        onView(withText(R.string.weak_password)).check(matches(isDisplayed()));
    }
}
