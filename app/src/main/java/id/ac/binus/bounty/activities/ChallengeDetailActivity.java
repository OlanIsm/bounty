package id.ac.binus.bounty.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.MutableLiveData;
import com.google.android.material.button.MaterialButton;
import id.ac.binus.bounty.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import id.ac.binus.bounty.R;
import id.ac.binus.bounty.database.AppDatabase;
import id.ac.binus.bounty.models.Challenge;
import id.ac.binus.bounty.utils.ChallengeDisplay;
import id.ac.binus.bounty.utils.ActionConfirmation;
import id.ac.binus.bounty.utils.ScreenInsets;
import id.ac.binus.bounty.utils.SessionManager;

public class ChallengeDetailActivity extends AppCompatActivity {
    public static final String EXTRA_CHALLENGE_ID = "challenge_id";
    private int request;
    private Challenge currentChallenge;
    private AcceptModel acceptModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge_detail);
        ScreenInsets.apply(this);
        acceptModel = new ViewModelProvider(this).get(AcceptModel.class);
        getSupportFragmentManager().setFragmentResultListener("confirm_accept", this, (key, data) -> {
            User user = new SessionManager(this).getCurrentUser();
            if (user != null && user.id.equals(data.getString("actor")))
                acceptModel.accept(AppDatabase.getInstance(this), data.getInt("challenge"), user);
            else Snackbar.make(findViewById(R.id.main), R.string.accept_unavailable, Snackbar.LENGTH_LONG).show();
        });
        findViewById(R.id.accept_button).setOnClickListener(view -> {
            User user = new SessionManager(this).getCurrentUser();
            if (!view.isEnabled() || user == null || currentChallenge == null) return;
            Bundle data = new Bundle();
            data.putInt("challenge", currentChallenge.id); data.putString("actor", user.id);
            ActionConfirmation.show(getSupportFragmentManager(), "confirm_accept", getString(R.string.confirm_accept_title),
                    getString(R.string.confirm_accept_message, currentChallenge.title,
                            ChallengeDisplay.rupiah(this, currentChallenge.reward)), getString(R.string.accept_action), data);
        });
        findViewById(R.id.submit_proof_button).setOnClickListener(view -> startActivity(
                new Intent(this, SubmitProofActivity.class).putExtra(EXTRA_CHALLENGE_ID, currentChallenge.id)));
        findViewById(R.id.review_proof_button).setOnClickListener(view -> startActivity(
                new Intent(this, ReviewProofActivity.class).putExtra(EXTRA_CHALLENGE_ID, currentChallenge.id)));
        acceptModel.state.observe(this, state -> {
            bindAccept();
            if (state != AcceptModel.IDLE && state != AcceptModel.SAVING) {
                int message = state == AcceptModel.SAVED ? R.string.challenge_accepted
                        : state == AcceptModel.UNAVAILABLE ? R.string.accept_unavailable : R.string.accept_error;
                acceptModel.state.setValue(AcceptModel.IDLE);
                loadChallenge();
                Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG).show();
            }
        });
        ((MaterialToolbar) findViewById(R.id.detail_toolbar)).setNavigationOnClickListener(view -> finish());
        findViewById(R.id.detail_retry).setOnClickListener(view -> loadChallenge());
    }
    @Override
    protected void onStart() {
        super.onStart();
        if (!new SessionManager(this).isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }
        loadChallenge();
    }
    private void loadChallenge() {
        int currentRequest = ++request;
        findViewById(R.id.detail_loading).setVisibility(View.VISIBLE);
        findViewById(R.id.detail_content).setVisibility(View.GONE);
        findViewById(R.id.detail_message).setVisibility(View.GONE);
        findViewById(R.id.detail_retry).setVisibility(View.GONE);
        int id = getIntent().getIntExtra(EXTRA_CHALLENGE_ID, -1);
        if (id <= 0) {
            showMessage(R.string.challenge_not_found, false);
            return;
        }
        AppDatabase db = AppDatabase.getInstance(this);
        db.getQueryExecutor().execute(() -> {
            try {
                Challenge challenge = db.challengeDao().getChallengeById(id);
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing() || currentRequest != request) return;
                    if (challenge == null) {
                        showMessage(R.string.challenge_not_found, false);
                        return;
                    }
                    currentChallenge = challenge;
                    bindAccept();
                    View content = findViewById(R.id.detail_content);
                    ChallengeDisplay.bind(content, challenge);
                    ((TextView) content.findViewById(R.id.challenge_description)).setText(challenge.description);
                    findViewById(R.id.detail_loading).setVisibility(View.GONE);
                    content.setVisibility(View.VISIBLE);
                });
            } catch (RuntimeException error) {
                Log.e("BountyDetail", "Unable to load challenge " + id, error);
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing() || currentRequest != request) return;
                    showMessage(R.string.challenge_load_error, true);
                    Snackbar.make(findViewById(R.id.main), R.string.challenge_load_error, Snackbar.LENGTH_LONG).show();
                });
            }
        });
    }
    private void bindAccept() {
        User user = new SessionManager(this).getCurrentUser();
        Challenge challenge = currentChallenge;
        MaterialButton button = findViewById(R.id.accept_button);
        boolean saving = acceptModel.state.getValue() == AcceptModel.SAVING;
        boolean own = challenge != null && user != null && challenge.creatorId.equals(user.id);
        boolean available = challenge != null && Challenge.OPEN.equals(challenge.status)
                && challenge.participantId == null;
        button.setEnabled(user != null && available && !own && !saving);
        button.setText(saving ? R.string.accepting_challenge : R.string.accept_action);
        findViewById(R.id.submit_proof_button).setVisibility(challenge != null && user != null
                && Challenge.ACCEPTED.equals(challenge.status) && user.id.equals(challenge.participantId)
                ? View.VISIBLE : View.GONE);
        findViewById(R.id.review_proof_button).setVisibility(own && Challenge.SUBMITTED.equals(challenge.status)
                ? View.VISIBLE : View.GONE);
        TextView result = findViewById(R.id.reward_result);
        boolean completed = challenge != null && Challenge.COMPLETED.equals(challenge.status);
        result.setVisibility(completed ? View.VISIBLE : View.GONE);
        if (completed) {
            boolean paid = new SessionManager(this).isRewardCredited(challenge.id, challenge.participantId);
            result.setText(paid ? getString(R.string.reward_result_paid,
                    ChallengeDisplay.rupiah(this, challenge.reward), challenge.participantName)
                    : getString(R.string.reward_pending));
        }
        TextView message = findViewById(R.id.accept_message);
        if (challenge != null && challenge.participantId != null) {
            message.setText(getString(R.string.accept_hunter, challenge.participantName));
        } else {
            message.setText(own ? R.string.accept_own : available ? R.string.accept_ready : R.string.accept_unavailable);
        }
    }

    /** Keeps acceptance across rotation; the conditional SQL update chooses one hunter. */
    public static class AcceptModel extends ViewModel {
        static final int IDLE = 0, SAVING = 1, SAVED = 2, UNAVAILABLE = 3, ERROR = 4;
        final MutableLiveData<Integer> state = new MutableLiveData<>(IDLE);

        void accept(AppDatabase db, int id, User user) {
            if (state.getValue() != IDLE) return;
            state.setValue(SAVING);
            db.getTransactionExecutor().execute(() -> {
                try {
                    state.postValue(db.challengeDao().acceptChallenge(id, user.id, user.name) == 1
                            ? SAVED : UNAVAILABLE);
                } catch (RuntimeException error) {
                    Log.e("BountyAccept", "Unable to accept challenge", error);
                    state.postValue(ERROR);
                }
            });
        }
    }

    private void showMessage(int text, boolean retry) {
        findViewById(R.id.detail_loading).setVisibility(View.GONE);
        TextView message = findViewById(R.id.detail_message);
        message.setText(text);
        message.setVisibility(View.VISIBLE);
        findViewById(R.id.detail_retry).setVisibility(retry ? View.VISIBLE : View.GONE);
    }
}
