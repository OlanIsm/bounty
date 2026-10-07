package id.ac.binus.bounty.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.io.InputStream;
import id.ac.binus.bounty.R;
import id.ac.binus.bounty.database.AppDatabase;
import id.ac.binus.bounty.models.Challenge;
import id.ac.binus.bounty.models.Proof;
import id.ac.binus.bounty.models.User;
import id.ac.binus.bounty.utils.ScreenInsets;
import id.ac.binus.bounty.utils.SessionManager;

public class SubmitProofActivity extends AppCompatActivity {
    private Uri imageUri;
    private ProofModel model;
    private boolean eligible;
    private int request;
    private final ActivityResultLauncher<String[]> picker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    imageUri = uri;
                    showPhoto();
                } catch (SecurityException error) {
                    ((TextView) findViewById(R.id.proof_photo_message)).setText(R.string.proof_photo_error);
                }
            });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_proof);
        ScreenInsets.apply(this);
        model = new ViewModelProvider(this).get(ProofModel.class);
        if (savedInstanceState != null) {
            String savedUri = savedInstanceState.getString("proof_image");
            if (savedUri != null) imageUri = Uri.parse(savedUri);
        }
        ((MaterialToolbar) findViewById(R.id.proof_toolbar)).setNavigationOnClickListener(view -> finish());
        findViewById(R.id.choose_proof_photo).setOnClickListener(view -> picker.launch(new String[]{"image/*"}));
        findViewById(R.id.proof_submit).setOnClickListener(view -> submit());
        findViewById(R.id.proof_retry).setOnClickListener(view -> loadChallenge());
        showPhoto();
    }

    @Override protected void onPostCreate(Bundle state) {
        super.onPostCreate(state);
        model.state.observe(this, result -> {
            updateSaving();
            if (result == ProofModel.SAVED) {
                Toast.makeText(this, R.string.proof_saved, Toast.LENGTH_LONG).show();
                finish();
            } else if (result == ProofModel.ERROR || result == ProofModel.IMAGE_ERROR) {
                int message = result == ProofModel.IMAGE_ERROR ? R.string.proof_photo_error : R.string.proof_save_error;
                Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG).show();
                if (result == ProofModel.IMAGE_ERROR) {
                    ((TextView) findViewById(R.id.proof_photo_message)).setText(message);
                }
                model.state.setValue(ProofModel.IDLE);
            } else if (result == ProofModel.UNAVAILABLE) {
                model.state.setValue(ProofModel.IDLE);
                loadChallenge();
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
        loadChallenge();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        if (imageUri != null) state.putString("proof_image", imageUri.toString());
        super.onSaveInstanceState(state);
    }

    private void showPhoto() {
        ImageView photo = findViewById(R.id.proof_photo);
        photo.setVisibility(imageUri == null ? View.GONE : View.VISIBLE);
        if (imageUri != null) Glide.with(this).load(imageUri).error(R.drawable.ic_avatar).into(photo);
        ((TextView) findViewById(R.id.proof_photo_message)).setText(
                imageUri == null ? R.string.proof_photo_required : R.string.proof_photo_preview);
    }

    private void loadChallenge() {
        int current = ++request;
        eligible = false;
        updateSaving();
        findViewById(R.id.proof_loading).setVisibility(View.VISIBLE);
        findViewById(R.id.proof_form).setVisibility(View.GONE);
        findViewById(R.id.proof_message).setVisibility(View.GONE);
        findViewById(R.id.proof_retry).setVisibility(View.GONE);
        User user = new SessionManager(this).getCurrentUser();
        if (user == null) return;
        int id = getIntent().getIntExtra(ChallengeDetailActivity.EXTRA_CHALLENGE_ID, -1);
        AppDatabase db = AppDatabase.getInstance(this);
        db.getQueryExecutor().execute(() -> {
            try {
                Challenge challenge = db.challengeDao().getChallengeById(id);
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing() || current != request) return;
                    if (challenge == null) { showMessage(R.string.challenge_not_found, false); return; }
                    eligible = Challenge.ACCEPTED.equals(challenge.status) && user.id.equals(challenge.participantId);
                    if (!eligible && model.state.getValue() != ProofModel.SAVING) {
                        showMessage(R.string.proof_unavailable, false);
                        return;
                    }
                    ((TextView) findViewById(R.id.proof_challenge_title)).setText(challenge.title);
                    findViewById(R.id.proof_loading).setVisibility(View.GONE);
                    findViewById(R.id.proof_form).setVisibility(View.VISIBLE);
                    updateSaving();
                });
            } catch (RuntimeException error) {
                Log.e("BountyProof", "Unable to load challenge", error);
                runOnUiThread(() -> {
                    if (!isDestroyed() && !isFinishing() && current == request) showMessage(R.string.challenge_load_error, true);
                });
            }
        });
    }

    private void showMessage(int text, boolean retry) {
        eligible = false;
        updateSaving();
        findViewById(R.id.proof_loading).setVisibility(View.GONE);
        findViewById(R.id.proof_form).setVisibility(View.GONE);
        TextView message = findViewById(R.id.proof_message);
        message.setText(text);
        message.setVisibility(View.VISIBLE);
        findViewById(R.id.proof_retry).setVisibility(retry ? View.VISIBLE : View.GONE);
    }

    private void updateSaving() {
        boolean saving = model.state.getValue() == ProofModel.SAVING;
        for (int id : new int[]{R.id.choose_proof_photo, R.id.proof_description_layout, R.id.proof_submit}) {
            findViewById(id).setEnabled(eligible && !saving);
        }
        findViewById(R.id.proof_saving).setVisibility(saving ? View.VISIBLE : View.GONE);
        ((MaterialButton) findViewById(R.id.proof_submit)).setText(saving ? R.string.proof_submitting : R.string.submit_proof_action);
    }

    private void submit() {
        if (!eligible || model.state.getValue() != ProofModel.IDLE) return;
        User user = new SessionManager(this).getCurrentUser();
        if (user == null) return;
        String description = ((TextInputEditText) findViewById(R.id.proof_description_input)).getText().toString().trim();
        boolean valid = !description.isEmpty() && description.length() <= 2000;
        ((TextInputLayout) findViewById(R.id.proof_description_layout))
                .setError(valid ? null : getString(R.string.proof_description_required));
        if (imageUri == null) ((TextView) findViewById(R.id.proof_photo_message)).setText(R.string.proof_photo_required);
        if (!valid || imageUri == null) return;
        Proof proof = new Proof();
        proof.challengeId = getIntent().getIntExtra(ChallengeDetailActivity.EXTRA_CHALLENGE_ID, -1);
        proof.hunterId = user.id;
        proof.description = description;
        proof.imageUri = imageUri.toString();
        proof.submittedAt = System.currentTimeMillis();
        model.submit(getApplicationContext(), proof);
    }

    public static class ProofModel extends ViewModel {
        static final int IDLE = 0, SAVING = 1, SAVED = 2, UNAVAILABLE = 3, ERROR = 4, IMAGE_ERROR = 5;
        final MutableLiveData<Integer> state = new MutableLiveData<>(IDLE);

        void submit(Context context, Proof proof) {
            if (state.getValue() != IDLE) return;
            state.setValue(SAVING);
            AppDatabase db = AppDatabase.getInstance(context);
            db.getTransactionExecutor().execute(() -> {
                // Decode bounds only: validate image access without allocating a full-sized bitmap.
                try (InputStream stream = context.getContentResolver().openInputStream(Uri.parse(proof.imageUri))) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(stream, null, options);
                    if (options.outWidth <= 0 || options.outHeight <= 0) throw new IllegalArgumentException("Unreadable image");
                } catch (Exception error) {
                    Log.e("BountyProof", "Unable to read proof image", error);
                    state.postValue(IMAGE_ERROR);
                    return;
                }
                try {
                    state.postValue(db.submitProof(proof) > 0 ? SAVED : UNAVAILABLE);
                } catch (RuntimeException error) {
                    Log.e("BountyProof", "Unable to submit proof", error);
                    state.postValue(ERROR);
                }
            });
        }
    }
}
