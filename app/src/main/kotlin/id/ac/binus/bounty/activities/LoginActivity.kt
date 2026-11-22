package id.ac.binus.bounty.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import id.ac.binus.bounty.R
import id.ac.binus.bounty.data.FirebaseAccountRepository
import id.ac.binus.bounty.utils.ScreenInsets
import id.ac.binus.bounty.utils.SessionManager
import id.ac.binus.bounty.utils.ViewMotion

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SessionManager(this).isFirebaseAuthenticated()) {
            openHome()
            return
        }
        setContentView(R.layout.activity_login)
        ScreenInsets.applyHeroEdgeToEdge(this, findViewById(R.id.login_sheet))
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        ViewMotion.startSuccessBurstAnimation(
            findViewById(R.id.auth_hero_burst),
            findViewById(R.id.auth_hero_mascot)
        )

        val email = findViewById<TextInputEditText>(R.id.email_input)
        val password = findViewById<TextInputEditText>(R.id.password_input)
        val emailLayout = findViewById<TextInputLayout>(R.id.email_layout)
        val passwordLayout = findViewById<TextInputLayout>(R.id.password_layout)
        if (savedInstanceState == null) intent.getStringExtra("registered_email")?.let { email.setText(it) }

        ViewMotion.applySpringPress(findViewById(R.id.login_button)) {
            emailLayout.error = null
            passwordLayout.error = null
            val emailValue = SessionManager.normalizeEmail(email.text?.toString())
            val passwordValue = password.text?.toString().orEmpty()
            if (!SessionManager.isValidEmail(emailValue)) {
                emailLayout.error = getString(R.string.invalid_email)
            } else if (passwordValue.isEmpty()) {
                passwordLayout.error = getString(R.string.required_password)
            } else {
                setBusy(true)
                FirebaseAccountRepository.signIn(emailValue, passwordValue) { result ->
                    runOnUiThread {
                        setBusy(false)
                        result.onSuccess { account ->
                            SessionManager(this).cacheFirebaseUser(account.uid, account.name, account.email)
                            openHome()
                        }.onFailure { error ->
                            passwordLayout.error = getString(FirebaseAccountRepository.errorMessage(error, true))
                        }
                    }
                }
            }
        }

        ViewMotion.applySpringPress(findViewById(R.id.google_login_button)) {
            emailLayout.error = null
            passwordLayout.error = null
            setBusy(true)
            FirebaseAccountRepository.signInWithGoogle(this) { result ->
                runOnUiThread {
                    setBusy(false)
                    result.onSuccess { account ->
                        SessionManager(this).cacheFirebaseUser(account.uid, account.name, account.email)
                        openHome()
                    }.onFailure { error ->
                        Toast.makeText(
                            this,
                            getString(FirebaseAccountRepository.errorMessage(error, true)),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        ViewMotion.applySpringPress(findViewById(R.id.register_link)) {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setBusy(busy: Boolean) {
        findViewById<View>(R.id.auth_loading).visibility = if (busy) View.VISIBLE else View.GONE
        findViewById<View>(R.id.login_button).isEnabled = !busy
        findViewById<View>(R.id.google_login_button).isEnabled = !busy
        findViewById<View>(R.id.register_link).isEnabled = !busy
    }

    private fun openHome() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
}
