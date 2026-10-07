package id.ac.binus.bounty.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import id.ac.binus.bounty.R;
import id.ac.binus.bounty.database.AppDatabase;
import id.ac.binus.bounty.models.Challenge;
import id.ac.binus.bounty.utils.ChallengeDisplay;
import id.ac.binus.bounty.utils.ScreenInsets;
import id.ac.binus.bounty.utils.SessionManager;

public class ChallengeDetailActivity extends AppCompatActivity {
    public static final String EXTRA_CHALLENGE_ID = "challenge_id";
    private int request;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge_detail);
        ScreenInsets.apply(this);
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
    private void showMessage(int text, boolean retry) {
        findViewById(R.id.detail_loading).setVisibility(View.GONE);
        TextView message = findViewById(R.id.detail_message);
        message.setText(text);
        message.setVisibility(View.VISIBLE);
        findViewById(R.id.detail_retry).setVisibility(retry ? View.VISIBLE : View.GONE);
    }
}
