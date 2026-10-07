package id.ac.binus.bounty.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import id.ac.binus.bounty.R;
import id.ac.binus.bounty.utils.ScreenInsets;
import id.ac.binus.bounty.utils.SessionManager;

public class RegisterActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }
        setContentView(R.layout.activity_register);
        ScreenInsets.apply(this);
        TextInputEditText name = findViewById(R.id.name_input);
        TextInputEditText email = findViewById(R.id.email_input);
        TextInputLayout nameLayout = findViewById(R.id.name_layout);
        TextInputLayout emailLayout = findViewById(R.id.email_layout);
        findViewById(R.id.register_button).setOnClickListener(view -> {
            nameLayout.setError(null);
            emailLayout.setError(null);
            String nameValue = name.getText().toString().trim();
            String emailValue = SessionManager.normalizeEmail(email.getText().toString());
            boolean valid = true;
            if (nameValue.isEmpty()) {
                nameLayout.setError(getString(R.string.required_name));
                valid = false;
            }
            if (!SessionManager.isValidEmail(emailValue)) {
                emailLayout.setError(getString(R.string.invalid_email));
                valid = false;
            }
            if (!valid) {
                return;
            }
            if (!session.register(nameValue, emailValue)) {
                emailLayout.setError(getString(R.string.duplicate_email));
                return;
            }
            startActivity(new Intent(this, LoginActivity.class)
                    .putExtra("registered_email", emailValue)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });
        findViewById(R.id.login_link).setOnClickListener(view -> finish());
    }
}
