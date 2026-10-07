package id.ac.binus.bounty.activities;

import android.os.Bundle;
import android.content.Intent;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
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
import androidx.recyclerview.widget.ConcatAdapter;
import android.view.ViewGroup;
import android.view.LayoutInflater;

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
import java.util.Collections;
import id.ac.binus.bounty.network.ApiClient;
import id.ac.binus.bounty.network.RandomUserApi;
import id.ac.binus.bounty.network.RandomUserResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import id.ac.binus.bounty.models.User;
import id.ac.binus.bounty.utils.ScreenInsets;
import id.ac.binus.bounty.utils.ChallengeDisplay;
import id.ac.binus.bounty.utils.SessionManager;

/** Four top-level destinations with a local Room challenge feed. */
public class MainActivity extends AppCompatActivity {
    private BottomNavigationView navigation;
    private int selectedDestination = R.id.nav_home;
    private int feedRequest;
    private ChallengeAdapter feedAdapter;
    private HomeHeaderAdapter homeHeaderAdapter;
    private int balanceRequest;
    private View createScreen;
    private SparseArray<Parcelable> createState;
    private PublishModel publishModel;
    private DemoUsersModel demoUsersModel;
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
        demoUsersModel = new ViewModelProvider(this).get(DemoUsersModel.class);
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
        demoUsersModel.state.observe(this, state -> bindDemoUsers());
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
        refreshBalance();
        if (navigation != null && (selectedDestination == R.id.nav_home || selectedDestination == R.id.nav_my_challenges)) loadFeed();
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
        if (destination == R.id.nav_profile) {
            findViewById(R.id.demo_users_retry).setOnClickListener(view -> demoUsersModel.load(ApiClient.getApi()));
            if (demoUsersModel.state.getValue() == null) demoUsersModel.load(ApiClient.getApi());
            bindDemoUsers();
            findViewById(R.id.logout_button).setOnClickListener(view -> {
                new SessionManager(this).logout();
                openLogin();
            });
        } else if (destination == R.id.nav_home || destination == R.id.nav_my_challenges) {
            RecyclerView list = findViewById(R.id.challenge_list);
            list.setLayoutManager(new LinearLayoutManager(this));
            feedAdapter = new ChallengeAdapter(challenge -> startActivity(
                    new Intent(this, ChallengeDetailActivity.class)
                            .putExtra(ChallengeDetailActivity.EXTRA_CHALLENGE_ID, challenge.id)));
            list.setItemAnimator(null);
            if (destination == R.id.nav_home) {
                homeHeaderAdapter = new HomeHeaderAdapter(user);
                list.setAdapter(new ConcatAdapter(homeHeaderAdapter, feedAdapter));
            } else list.setAdapter(feedAdapter);
            findViewById(R.id.feed_retry).setOnClickListener(view -> loadFeed());
            if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) loadFeed();
        }
        if (user != null) bindUser(user);
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) refreshBalance();
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
        int screenId = selectedDestination == R.id.nav_my_challenges ? R.id.my_challenges_screen : R.id.home_screen;
        boolean mine = selectedDestination == R.id.nav_my_challenges;
        User user = new SessionManager(this).getCurrentUser();
        if (user == null) return;
        View home = findViewById(screenId);
        if (home == null) return;
        int request = ++feedRequest;
        home.findViewById(R.id.feed_loading).setVisibility(View.VISIBLE);
        home.findViewById(R.id.feed_message).setVisibility(View.GONE);
        home.findViewById(R.id.feed_retry).setVisibility(View.GONE);
        AppDatabase db = AppDatabase.getInstance(this);
        db.getQueryExecutor().execute(() -> {
            try {
                List<Challenge> challenges = mine ? db.challengeDao().getMyChallenges(user.id)
                        : db.challengeDao().getAllChallenges();
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing() || request != feedRequest || findViewById(screenId) != home) return;
                    RecyclerView list = home.findViewById(R.id.challenge_list);
                    feedAdapter.submitList(challenges);
                    home.findViewById(R.id.feed_loading).setVisibility(View.GONE);
                    list.setVisibility(mine && challenges.isEmpty() ? View.GONE : View.VISIBLE);
                    TextView message = home.findViewById(R.id.feed_message);
                    message.setText(mine ? R.string.no_my_challenges : R.string.no_available_challenges);
                    message.setVisibility(challenges.isEmpty() ? View.VISIBLE : View.GONE);
                });
            } catch (RuntimeException error) {
                Log.e("BountyFeed", "Unable to load challenges", error);
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing() || request != feedRequest || findViewById(screenId) != home) return;
                    home.findViewById(R.id.feed_loading).setVisibility(View.GONE);
                    feedAdapter.submitList(Collections.emptyList());
                    home.findViewById(R.id.challenge_list).setVisibility(mine ? View.GONE : View.VISIBLE);
                    TextView message = home.findViewById(R.id.feed_message);
                    message.setText(R.string.challenge_load_error);
                    message.setVisibility(View.VISIBLE);
                    home.findViewById(R.id.feed_retry).setVisibility(View.VISIBLE);
                    showSnackbar(R.string.challenge_load_error);
                });
            }
        });
    }

    private static class HomeHeaderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        User user;
        HomeHeaderAdapter(User user) { this.user = user; }
        @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
            return new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_home_header, parent, false)) { };
        }
        @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (user == null) return;
            View root = holder.itemView;
            ((TextView) root.findViewById(R.id.welcome_text)).setText(root.getContext().getString(R.string.welcome_user, user.name));
            ((TextView) root.findViewById(R.id.demo_balance_amount)).setText(ChallengeDisplay.rupiah(root.getContext(), user.demoBalance));
        }
        @Override public int getItemCount() { return 1; }
    }

    private void refreshBalance() {
        if (navigation == null || (selectedDestination != R.id.nav_home && selectedDestination != R.id.nav_profile)) return;
        SessionManager session = new SessionManager(getApplicationContext());
        User user = session.getCurrentUser();
        if (user == null) return;
        int request = ++balanceRequest;
        AppDatabase db = AppDatabase.getInstance(this);
        db.getTransactionExecutor().execute(() -> {
            try {
                session.creditCompletedRewards(db, user.id);
                runOnUiThread(() -> {
                    User current = session.getCurrentUser();
                    if (!isDestroyed() && !isFinishing() && request == balanceRequest
                            && current != null && current.id.equals(user.id)) bindUser(current);
                });
            } catch (RuntimeException error) {
                Log.e("BountyReward", "Unable to recover demo rewards", error);
                runOnUiThread(() -> {
                    if (!isDestroyed() && !isFinishing() && request == balanceRequest)
                        showSnackbar(R.string.reward_refresh_error);
                });
            }
        });
    }

    private void bindUser(User user) {
        TextView balance = findViewById(R.id.demo_balance_amount);
        if (balance != null) balance.setText(ChallengeDisplay.rupiah(this, user.demoBalance));
        if (selectedDestination == R.id.nav_home) {
            if (homeHeaderAdapter != null) {
                homeHeaderAdapter.user = user;
                homeHeaderAdapter.notifyItemChanged(0);
            }
        } else if (selectedDestination == R.id.nav_profile) {
            ((TextView) findViewById(R.id.profile_name)).setText(user.name);
            ((TextView) findViewById(R.id.user_email)).setText(user.email);
        }
    }

    // ponytail: at most ten demo rows; use RecyclerView if pagination is introduced.
    private void bindDemoUsers() {
        LinearLayout list = findViewById(R.id.demo_users_list);
        DemoUsersModel.State state = demoUsersModel.state.getValue();
        if (list == null || state == null) return;
        findViewById(R.id.demo_users_loading).setVisibility(state.loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.demo_users_error).setVisibility(state.failed ? View.VISIBLE : View.GONE);
        findViewById(R.id.demo_users_retry).setVisibility(state.failed ? View.VISIBLE : View.GONE);
        list.removeAllViews();
        for (User user : state.users) {
            View row = getLayoutInflater().inflate(R.layout.item_demo_user, list, false);
            ((TextView) row.findViewById(R.id.demo_user_name)).setText(user.name);
            TextView email = row.findViewById(R.id.demo_user_email);
            email.setText(user.email);
            email.setVisibility(user.email.isEmpty() ? View.GONE : View.VISIBLE);
            ImageView avatar = row.findViewById(R.id.demo_user_avatar);
            Glide.with(avatar).load(user.avatarUrl).placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar).circleCrop().into(avatar);
            list.addView(row);
        }
    }

    /** Retains one request and its result across rotation and tab changes. */
    public static class DemoUsersModel extends ViewModel {
        public static class State {
            public final List<User> users;
            public final boolean loading, failed;
            State(List<User> users, boolean loading, boolean failed) {
                this.users = users; this.loading = loading; this.failed = failed;
            }
        }
        public final MutableLiveData<State> state = new MutableLiveData<>();
        private Call<RandomUserResponse> request;
        public void load(RandomUserApi api) {
            if (state.getValue() != null && state.getValue().loading) return;
            state.setValue(new State(Collections.emptyList(), true, false));
            request = api.getUsers(10);
            request.enqueue(new Callback<RandomUserResponse>() {
                @Override public void onResponse(Call<RandomUserResponse> call, Response<RandomUserResponse> response) {
                    if (call != request || call.isCanceled()) return;
                    List<User> users = response.isSuccessful() && response.body() != null
                            ? response.body().toUsers() : Collections.emptyList();
                    if (users.isEmpty()) fallback();
                    else state.setValue(new State(users, false, false));
                }
                @Override public void onFailure(Call<RandomUserResponse> call, Throwable error) {
                    if (call != request || call.isCanceled()) return;
                    Log.w("BountyApi", "Unable to load demo users", error);
                    fallback();
                }
            });
        }
        private void fallback() {
            state.setValue(new State(Collections.singletonList(
                    new User("api_fallback", "Bounty User", "", "", 0)), false, true));
        }
        @Override protected void onCleared() { if (request != null) request.cancel(); }
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
