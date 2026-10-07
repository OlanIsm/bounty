package id.ac.binus.bounty.activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import id.ac.binus.bounty.R;
import id.ac.binus.bounty.database.AppDatabase;
import id.ac.binus.bounty.models.Challenge;
import id.ac.binus.bounty.models.Proof;
import id.ac.binus.bounty.models.User;
import id.ac.binus.bounty.utils.ScreenInsets;
import id.ac.binus.bounty.utils.ActionConfirmation;
import id.ac.binus.bounty.utils.ChallengeDisplay;
import id.ac.binus.bounty.utils.SessionManager;

public class ReviewProofActivity extends AppCompatActivity {
    private ReviewModel model;
    private Proof currentProof;
    private Challenge currentChallenge;
    private boolean imageReady;
    private int request;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_review_proof);
        ScreenInsets.apply(this);
        model = new ViewModelProvider(this).get(ReviewModel.class);
        ((MaterialToolbar) findViewById(R.id.review_toolbar)).setNavigationOnClickListener(view -> finish());
        findViewById(R.id.review_retry).setOnClickListener(view -> loadReview());
        findViewById(R.id.review_photo_retry).setOnClickListener(view -> loadReview());
        getSupportFragmentManager().setFragmentResultListener("confirm_approve", this, (key, data) -> {
            SessionManager session = new SessionManager(getApplicationContext());
            User user = session.getCurrentUser();
            if (user == null || !user.id.equals(data.getString("actor"))) return;
            Proof proof = new Proof();
            proof.id = data.getInt("proof"); proof.challengeId = data.getInt("challenge");
            proof.hunterId = data.getString("hunter");
            // Room rechecks creator, latest proof and state, even after dialog recreation.
            model.review(AppDatabase.getInstance(this), proof, user.id, true, session);
        });
        findViewById(R.id.review_approve).setOnClickListener(view -> confirmApproval());
        findViewById(R.id.review_reject).setOnClickListener(view -> review(false));
    }

    @Override protected void onPostCreate(Bundle state) {
        super.onPostCreate(state);
        model.state.observe(this, result -> {
            updateControls();
            if (result == ReviewModel.APPROVED || result == ReviewModel.REJECTED || result == ReviewModel.REWARD_PENDING) {
                Toast.makeText(this, result == ReviewModel.APPROVED ? R.string.reward_approved
                        : result == ReviewModel.REWARD_PENDING ? R.string.reward_pending
                        : R.string.review_rejected, Toast.LENGTH_LONG).show();
                finish();
            } else if (result == ReviewModel.ERROR || result == ReviewModel.UNAVAILABLE) {
                Snackbar.make(findViewById(R.id.main), result == ReviewModel.ERROR ? R.string.review_save_error
                        : R.string.review_unavailable, Snackbar.LENGTH_LONG).show();
                model.state.setValue(ReviewModel.IDLE);
                loadReview();
            }
        });
    }

    @Override protected void onStart() {
        super.onStart();
        if (!new SessionManager(this).isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }
        loadReview();
    }

    private void loadReview() {
        int current = ++request;
        currentProof = null;
        currentChallenge = null;
        imageReady = false;
        updateControls();
        findViewById(R.id.review_loading).setVisibility(View.VISIBLE);
        findViewById(R.id.review_content).setVisibility(View.GONE);
        findViewById(R.id.review_message).setVisibility(View.GONE);
        findViewById(R.id.review_retry).setVisibility(View.GONE);
        User user = new SessionManager(this).getCurrentUser();
        if (user == null) return;
        int id = getIntent().getIntExtra(ChallengeDetailActivity.EXTRA_CHALLENGE_ID, -1);
        AppDatabase db = AppDatabase.getInstance(this);
        db.getQueryExecutor().execute(() -> {
            try {
                Challenge challenge = db.challengeDao().getChallengeById(id);
                Proof proof = db.proofDao().getProofByChallengeId(id);
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing() || current != request) return;
                    if (challenge == null) { showMessage(R.string.challenge_not_found, false); return; }
                    if (!user.id.equals(challenge.creatorId) || !Challenge.SUBMITTED.equals(challenge.status)) {
                        showMessage(R.string.review_unavailable, false); return;
                    }
                    if (proof == null) { showMessage(R.string.review_missing_proof, true); return; }
                    if (!Proof.PENDING.equals(proof.status) || !proof.hunterId.equals(challenge.participantId)) {
                        showMessage(R.string.review_unavailable, false); return;
                    }
                    currentProof = proof;
                    currentChallenge = challenge;
                    ((TextView) findViewById(R.id.review_title)).setText(challenge.title);
                    ((TextView) findViewById(R.id.review_hunter)).setText(getString(R.string.accept_hunter, challenge.participantName));
                    ((TextView) findViewById(R.id.review_description)).setText(proof.description);
                    ChallengeDisplay.bindStatus(findViewById(R.id.review_status), proof.status);
                    findViewById(R.id.review_loading).setVisibility(View.GONE);
                    findViewById(R.id.review_content).setVisibility(View.VISIBLE);
                    showImage(proof, current);
                    updateControls();
                });
            } catch (RuntimeException error) {
                Log.e("BountyReview", "Unable to load proof", error);
                runOnUiThread(() -> {
                    if (!isDestroyed() && !isFinishing() && current == request) showMessage(R.string.challenge_load_error, true);
                });
            }
        });
    }

    private void showImage(Proof proof, int current) {
        ImageView photo = findViewById(R.id.review_photo);
        photo.setVisibility(View.VISIBLE);
        findViewById(R.id.review_photo_error).setVisibility(View.GONE);
        findViewById(R.id.review_photo_retry).setVisibility(View.GONE);
        Glide.with(this).load(Uri.parse(proof.imageUri)).listener(new RequestListener<Drawable>() {
            @Override public boolean onLoadFailed(@Nullable GlideException error, Object source,
                    Target<Drawable> target, boolean first) {
                if (!isDestroyed() && current == request) {
                    imageReady = false;
                    photo.setVisibility(View.GONE);
                    findViewById(R.id.review_photo_error).setVisibility(View.VISIBLE);
                    findViewById(R.id.review_photo_retry).setVisibility(View.VISIBLE);
                    updateControls();
                }
                return false;
            }
            @Override public boolean onResourceReady(Drawable resource, Object source, Target<Drawable> target,
                    DataSource dataSource, boolean first) {
                if (!isDestroyed() && current == request) { imageReady = true; updateControls(); }
                return false;
            }
        }).into(photo);
    }

    private void showMessage(int text, boolean retry) {
        findViewById(R.id.review_loading).setVisibility(View.GONE);
        findViewById(R.id.review_content).setVisibility(View.GONE);
        TextView message = findViewById(R.id.review_message);
        message.setText(text);
        message.setVisibility(View.VISIBLE);
        findViewById(R.id.review_retry).setVisibility(retry ? View.VISIBLE : View.GONE);
    }

    private void updateControls() {
        boolean saving = model.state.getValue() == ReviewModel.SAVING;
        boolean ready = currentProof != null && model.state.getValue() == ReviewModel.IDLE;
        findViewById(R.id.review_approve).setEnabled(ready && imageReady);
        findViewById(R.id.review_reject).setEnabled(ready);
        findViewById(R.id.review_photo_retry).setEnabled(!saving);
        findViewById(R.id.review_saving).setVisibility(saving ? View.VISIBLE : View.GONE);
    }

    private void confirmApproval() {
        User user = new SessionManager(this).getCurrentUser();
        if (user == null || currentProof == null || currentChallenge == null || !imageReady
                || model.state.getValue() != ReviewModel.IDLE) return;
        Bundle data = new Bundle();
        data.putInt("proof", currentProof.id); data.putInt("challenge", currentProof.challengeId);
        data.putString("hunter", currentProof.hunterId); data.putString("actor", user.id);
        ActionConfirmation.show(getSupportFragmentManager(), "confirm_approve", getString(R.string.confirm_approve_title),
                getString(R.string.confirm_approve_message, currentChallenge.title,
                        ChallengeDisplay.rupiah(this, currentChallenge.reward), currentChallenge.participantName),
                getString(R.string.approve_proof_action), data);
    }

    private void review(boolean approve) {
        if (currentProof == null || (approve && !imageReady)) return;
        User user = new SessionManager(this).getCurrentUser();
        if (user != null) model.review(AppDatabase.getInstance(this), currentProof, user.id, approve, new SessionManager(getApplicationContext()));
    }

    public static class ReviewModel extends ViewModel {
        static final int IDLE = 0, SAVING = 1, APPROVED = 2, REJECTED = 3, UNAVAILABLE = 4, ERROR = 5, REWARD_PENDING = 6;
        final MutableLiveData<Integer> state = new MutableLiveData<>(IDLE);
        void review(AppDatabase db, Proof proof, String creatorId, boolean approve, SessionManager session) {
            if (state.getValue() != IDLE) return;
            state.setValue(SAVING);
            db.getTransactionExecutor().execute(() -> {
                try {
                    if (!db.reviewProof(proof.challengeId, proof.id, creatorId, approve)) {
                        state.postValue(UNAVAILABLE);
                        return;
                    }
                    if (!approve) { state.postValue(REJECTED); return; }
                    try {
                        session.creditCompletedRewards(db, proof.hunterId);
                        state.postValue(APPROVED);
                    } catch (RuntimeException error) {
                        Log.e("BountyReward", "Approved proof awaits demo reward recovery", error);
                        state.postValue(REWARD_PENDING);
                    }
                } catch (RuntimeException error) {
                    Log.e("BountyReview", "Unable to review proof", error);
                    state.postValue(ERROR);
                }
            });
        }
    }
}
