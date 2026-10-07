package id.ac.binus.bounty.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import id.ac.binus.bounty.utils.SessionManager;

/** Immediate routing after the platform launch splash; no artificial delay. */
public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Class<?> destination = new SessionManager(this).isLoggedIn()
                ? MainActivity.class : LoginActivity.class;
        startActivity(new Intent(this, destination)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
