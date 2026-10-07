package id.ac.binus.bounty.activities;

import android.os.Bundle;
import android.content.Intent;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.view.View;
import android.util.Log;
import android.app.DatePickerDialog;
import android.os.Parcelable;
import android.util.SparseArray;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import id.ac.binus.bounty.R;
import id.ac.binus.bounty.adapters.ChallengeAdapter;
import id.ac.binus.bounty.database.AppDatabase;
import id.ac.binus.bounty.models.Challenge;
import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import id.ac.binus.bounty.models.User;
import id.ac.binus.bounty.utils.ScreenInsets;
import id.ac.binus.bounty.utils.SessionManager;

/** Four top-level destinations with a local Room challenge feed. */
public class MainActivity extends AppCompatActivity {
    private BottomNavigationView navigation;
    private int selectedDestination = R.id.nav_home;
    private int feedRequest;
    private View createScreen;
    private SparseArray<Parcelable> createState;
    private PublishModel publishModel;
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
        publishModel = new ViewModelProvider(this).get(PublishModel.class);
        createState = savedInstanceState == null ? null : savedInstanceState.getSparseParcelableArray("create_state");
        getOnBackPressedDispatcher().addCallback(this, backToHome);
        navigation.setOnItemSelectedListener(item -> showScreen(item.getItemId()));
        int destination = savedInstanceState == null ? R.id.nav_home
                : savedInstanceState.getInt("selected_destination", R.id.nav_home);
        navigation.setSelectedItemId(navigation.getMenu().findItem(destination) == null
                ? R.id.nav_home : destination);
        navigation.setOnItemReselectedListener(item -> { });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (navigation == null) return;
        // Consume publish results after Android has restored the navigation/view hierarchy.
        publishModel.state.observe(this, state -> {
            updatePublishState();
            if (state == PublishModel.SAVED) {
                createScreen = null;
                createState = null;
                publishModel.state.setValue(PublishModel.IDLE);
                new WindowInsetsControllerCompat(getWindow(), findViewById(R.id.main))
                        .hide(WindowInsetsCompat.Type.ime());
                boolean alreadyHome = selectedDestination == R.id.nav_home;
                navigation.setSelectedItemId(R.id.nav_home);
                // If already on Home, refresh the existing adapter after a background publish.
                if (alreadyHome) loadFeed();
                showSnackbar(R.string.challenge_published);
            } else if (state == PublishModel.ERROR) {
                showSnackbar(R.string.challenge_publish_error);
                publishModel.state.setValue(PublishModel.IDLE);
            }
        });
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
        if (createScreen != null) {
            createState = new SparseArray<>();
            createScreen.saveHierarchyState(createState);
        }
        outState.putSparseParcelableArray("create_state", createState);
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
        if (selectedDestination == R.id.nav_create && destination != R.id.nav_create && createScreen != null) {
            createScreen.clearFocus();
            new WindowInsetsControllerCompat(getWindow(), content).hide(WindowInsetsCompat.Type.ime());
        }
        content.removeAllViews();
        if (destination == R.id.nav_create) {
            if (createScreen == null) {
                createScreen = getLayoutInflater().inflate(layout, content, false);
                if (createState != null) createScreen.restoreHierarchyState(createState);
                createScreen.findViewById(R.id.publish_button).setOnClickListener(view -> publishChallenge());
                ((TextInputLayout) createScreen.findViewById(R.id.create_deadline_layout))
                        .setEndIconOnClickListener(view -> chooseDeadline());
            }
            content.addView(createScreen);
            updatePublishState();
        } else {
            getLayoutInflater().inflate(layout, content, true);
        }
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

    private void chooseDeadline() {
        TextInputEditText input = createScreen.findViewById(R.id.create_deadline_input);
        LocalDate selected = LocalDate.now().plusDays(7);
        try { selected = LocalDate.parse(input.getText().toString()); }
        catch (DateTimeParseException ignored) { }
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) -> {
            input.setText(LocalDate.of(year, month + 1, day).toString());
            ((TextInputLayout) createScreen.findViewById(R.id.create_deadline_layout)).setError(null);
        }, selected.getYear(), selected.getMonthValue() - 1, selected.getDayOfMonth());
        picker.getDatePicker().setMinDate(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        picker.show();
    }

    private void publishChallenge() {
        if (publishModel.state.getValue() == PublishModel.SAVING) return;
        User user = new SessionManager(this).getCurrentUser();
        if (user == null) { openLogin(); return; }
        String title = inputText(R.id.create_title_input);
        String description = inputText(R.id.create_description_input);
        String rewardText = inputText(R.id.create_reward_input);
        String deadline = inputText(R.id.create_deadline_input);
        boolean titleValid = !title.isEmpty() && title.length() <= 100;
        boolean descriptionValid = !description.isEmpty() && description.length() <= 2000;
        double reward = 0;
        try { if (rewardText.matches("[0-9]+")) reward = Double.parseDouble(rewardText); }
        catch (NumberFormatException ignored) { }
        boolean rewardValid = Double.isFinite(reward) && reward > 0;
        boolean deadlineValid = false;
        try {
            LocalDate date = LocalDate.parse(deadline);
            deadlineValid = deadline.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}") && !date.isBefore(LocalDate.now());
        } catch (DateTimeParseException ignored) { }
        setFormError(R.id.create_title_layout, titleValid, R.string.required_challenge_title);
        setFormError(R.id.create_description_layout, descriptionValid, R.string.required_challenge_description);
        setFormError(R.id.create_reward_layout, rewardValid, R.string.invalid_reward);
        setFormError(R.id.create_deadline_layout, deadlineValid, R.string.invalid_deadline);
        if (!titleValid || !descriptionValid || !rewardValid || !deadlineValid) return;
        Challenge challenge = new Challenge();
        challenge.title = title;
        challenge.description = description;
        challenge.reward = reward;
        challenge.deadline = deadline;
        challenge.creatorId = user.id;
        challenge.creatorName = user.name;
        challenge.creatorAvatar = user.avatarUrl;
        challenge.createdAt = System.currentTimeMillis();
        publishModel.publish(AppDatabase.getInstance(this), challenge);
    }

    private String inputText(int id) {
        return ((TextInputEditText) createScreen.findViewById(id)).getText().toString().trim();
    }

    private void setFormError(int id, boolean valid, int message) {
        ((TextInputLayout) createScreen.findViewById(id)).setError(valid ? null : getString(message));
    }

    private void updatePublishState() {
        if (createScreen == null) return;
        boolean saving = publishModel.state.getValue() == PublishModel.SAVING;
        for (int id : new int[]{R.id.create_title_layout, R.id.create_description_layout,
                R.id.create_reward_layout, R.id.create_deadline_layout, R.id.publish_button}) {
            createScreen.findViewById(id).setEnabled(!saving);
        }
        createScreen.findViewById(R.id.publish_loading).setVisibility(saving ? View.VISIBLE : View.GONE);
        ((MaterialButton) createScreen.findViewById(R.id.publish_button))
                .setText(saving ? R.string.publishing_challenge : R.string.publish_action);
    }

    /** Keeps an in-flight Room insert across rotation without retaining an Activity or View. */
    public static class PublishModel extends ViewModel {
        static final int IDLE = 0, SAVING = 1, SAVED = 2, ERROR = 3;
        final MutableLiveData<Integer> state = new MutableLiveData<>(IDLE);

        void publish(AppDatabase db, Challenge challenge) {
            if (state.getValue() == SAVING || state.getValue() == SAVED) return;
            state.setValue(SAVING);
            db.getTransactionExecutor().execute(() -> {
                try {
                    db.challengeDao().insertChallenge(challenge);
                    state.postValue(SAVED);
                } catch (RuntimeException error) {
                    Log.e("BountyCreate", "Unable to publish challenge", error);
                    state.postValue(ERROR);
                }
            });
        }
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
                    showSnackbar(R.string.challenge_load_error);
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

    private void showSnackbar(int message) {
        Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG)
                .setAnchorView(navigation).show();
    }
}
