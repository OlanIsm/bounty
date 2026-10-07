package id.ac.binus.bounty.activities;

import android.os.Bundle;
import android.content.Intent;
import android.widget.TextView;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.appbar.MaterialToolbar;

import id.ac.binus.bounty.R;
import id.ac.binus.bounty.models.User;
import id.ac.binus.bounty.utils.ScreenInsets;
import id.ac.binus.bounty.utils.SessionManager;

/** Four top-level M2 destinations; challenge features are added in later milestones. */
public class MainActivity extends AppCompatActivity {
    private BottomNavigationView navigation;
    private int selectedDestination = R.id.nav_home;
    private final OnBackPressedCallback backToHome = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            navigation.setSelectedItemId(R.id.nav_home);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!new SessionManager(this).isLoggedIn()) {
            openLogin();
            return;
        }
        setContentView(R.layout.activity_main);
        ScreenInsets.apply(this);
        navigation = findViewById(R.id.bottom_navigation);
        getOnBackPressedDispatcher().addCallback(this, backToHome);
        navigation.setOnItemSelectedListener(item -> showScreen(item.getItemId()));
        int destination = savedInstanceState == null ? R.id.nav_home
                : savedInstanceState.getInt("selected_destination", R.id.nav_home);
        navigation.setSelectedItemId(navigation.getMenu().findItem(destination) == null
                ? R.id.nav_home : destination);
        navigation.setOnItemReselectedListener(item -> { });
    }

    @Override
    protected void onStart() {
        super.onStart();
        User user = new SessionManager(this).getCurrentUser();
        if (user == null) {
            openLogin();
            return;
        }
        bindUser(user);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("selected_destination", selectedDestination);
        super.onSaveInstanceState(outState);
    }

    private boolean showScreen(int destination) {
        int layout;
        int title;
        if (destination == R.id.nav_home) {
            layout = R.layout.screen_home;
            title = R.string.home_title;
        } else if (destination == R.id.nav_my_challenges) {
            layout = R.layout.screen_my_challenges;
            title = R.string.my_challenges_title;
        } else if (destination == R.id.nav_create) {
            layout = R.layout.screen_create;
            title = R.string.create_title;
        } else if (destination == R.id.nav_profile) {
            layout = R.layout.screen_profile;
            title = R.string.profile_title;
        } else {
            return false;
        }
        FrameLayout content = findViewById(R.id.screen_content);
        // ponytail: reinflate static M2 screens; retain form state when editable forms arrive in M5.
        content.removeAllViews();
        getLayoutInflater().inflate(layout, content, true);
        ((MaterialToolbar) findViewById(R.id.main_toolbar)).setTitle(title);
        selectedDestination = destination;
        backToHome.setEnabled(destination != R.id.nav_home);
        User user = new SessionManager(this).getCurrentUser();
        if (user != null) {
            bindUser(user);
        }
        if (destination == R.id.nav_profile) {
            findViewById(R.id.logout_button).setOnClickListener(view -> {
                new SessionManager(this).logout();
                openLogin();
            });
        }
        return true;
    }

    private void bindUser(User user) {
        if (selectedDestination == R.id.nav_home) {
            ((TextView) findViewById(R.id.welcome_text))
                    .setText(getString(R.string.welcome_user, user.name));
        } else if (selectedDestination == R.id.nav_profile) {
            ((TextView) findViewById(R.id.profile_name)).setText(user.name);
            ((TextView) findViewById(R.id.user_email)).setText(user.email);
        }
    }

    private void openLogin() {
        startActivity(new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
