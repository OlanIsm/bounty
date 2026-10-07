package id.ac.binus.bounty.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import id.ac.binus.bounty.R;
import id.ac.binus.bounty.utils.ScreenInsets;
import id.ac.binus.bounty.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn()) {
            openHome();
            return;
        }
        setContentView(R.layout.activity_login);
        ScreenInsets.apply(this);
        TextInputEditText email = findViewById(R.id.email_input);
        TextInputLayout emailLayout = findViewById(R.id.email_layout);
        String registeredEmail = getIntent().getStringExtra("registered_email");
        if (savedInstanceState == null && registeredEmail != null) {
            email.setText(registeredEmail);
        }
        findViewById(R.id.login_button).setOnClickListener(view -> {
            emailLayout.setError(null);
            String value = email.getText().toString();
            if (!SessionManager.isValidEmail(value)) {
                emailLayout.setError(getString(R.string.invalid_email));
            } else if (!session.login(value)) {
                emailLayout.setError(getString(R.string.unknown_account));
            } else {
                openHome();
            }
        });
        findViewById(R.id.register_link).setOnClickListener(view ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void openHome() {
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
