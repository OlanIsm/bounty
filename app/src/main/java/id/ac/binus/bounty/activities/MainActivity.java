package id.ac.binus.bounty.activities;

import android.os.Bundle;
import android.content.Intent;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.view.View;
import android.util.Log;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

import id.ac.binus.bounty.R;
import id.ac.binus.bounty.adapters.ChallengeAdapter;
import id.ac.binus.bounty.database.AppDatabase;
import id.ac.binus.bounty.models.Challenge;
import java.util.List;
import id.ac.binus.bounty.models.User;
import id.ac.binus.bounty.utils.ScreenInsets;
import id.ac.binus.bounty.utils.SessionManager;

/** Four top-level destinations with a local Room challenge feed. */
public class MainActivity extends AppCompatActivity {
    private BottomNavigationView navigation;
    private int selectedDestination = R.id.nav_home;
    private int feedRequest;
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

    @Override
    protected void onResume() {
        super.onResume();
        if (navigation != null && selectedDestination == R.id.nav_home) loadFeed();
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
        // ponytail: reinflate destination screens; retain editable form state when forms arrive in M5.
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
        } else if (destination == R.id.nav_home) {
            RecyclerView list = findViewById(R.id.challenge_list);
            list.setLayoutManager(new LinearLayoutManager(this));
            list.setAdapter(new ChallengeAdapter(challenge -> startActivity(
                    new Intent(this, ChallengeDetailActivity.class)
                            .putExtra(ChallengeDetailActivity.EXTRA_CHALLENGE_ID, challenge.id))));
            findViewById(R.id.feed_retry).setOnClickListener(view -> loadFeed());
            if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) loadFeed();
        }
        return true;
    }

    private void loadFeed() {
        View home = findViewById(R.id.home_screen);
        if (home == null) return;
        int request = ++feedRequest;
        home.findViewById(R.id.feed_loading).setVisibility(View.VISIBLE);
        home.findViewById(R.id.feed_message).setVisibility(View.GONE);
        home.findViewById(R.id.feed_retry).setVisibility(View.GONE);
        AppDatabase db = AppDatabase.getInstance(this);
        db.getQueryExecutor().execute(() -> {
            try {
                List<Challenge> challenges = db.challengeDao().getAllChallenges();
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing() || request != feedRequest || findViewById(R.id.home_screen) != home) return;
                    RecyclerView list = home.findViewById(R.id.challenge_list);
                    ((ChallengeAdapter) list.getAdapter()).submitList(challenges);
                    home.findViewById(R.id.feed_loading).setVisibility(View.GONE);
                    list.setVisibility(challenges.isEmpty() ? View.GONE : View.VISIBLE);
                    TextView message = home.findViewById(R.id.feed_message);
                    message.setText(R.string.no_available_challenges);
                    message.setVisibility(challenges.isEmpty() ? View.VISIBLE : View.GONE);
                });
            } catch (RuntimeException error) {
                Log.e("BountyFeed", "Unable to load challenges", error);
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing() || request != feedRequest || findViewById(R.id.home_screen) != home) return;
                    home.findViewById(R.id.feed_loading).setVisibility(View.GONE);
                    home.findViewById(R.id.challenge_list).setVisibility(View.GONE);
                    TextView message = home.findViewById(R.id.feed_message);
                    message.setText(R.string.challenge_load_error);
                    message.setVisibility(View.VISIBLE);
                    home.findViewById(R.id.feed_retry).setVisibility(View.VISIBLE);
                    Snackbar.make(home, R.string.challenge_load_error, Snackbar.LENGTH_LONG).show();
                });
            }
        });
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
