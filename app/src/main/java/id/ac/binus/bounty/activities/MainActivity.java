package id.ac.binus.bounty.activities;

import android.os.Bundle;
import android.content.Intent;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import id.ac.binus.bounty.R;
import id.ac.binus.bounty.models.User;
import id.ac.binus.bounty.utils.ScreenInsets;
import id.ac.binus.bounty.utils.SessionManager;

/** M1 signed-in placeholder; the full Home and navigation belong to M2. */
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ScreenInsets.apply(this);
        findViewById(R.id.logout_button).setOnClickListener(view -> {
            new SessionManager(this).logout();
            openLogin();
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
        ((TextView) findViewById(R.id.welcome_text)).setText(getString(R.string.welcome_user, user.name));
        ((TextView) findViewById(R.id.user_email)).setText(user.email);
    }

    private void openLogin() {
        startActivity(new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
