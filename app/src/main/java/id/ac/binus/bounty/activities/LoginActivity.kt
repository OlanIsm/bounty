package id.ac.binus.bounty.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import id.ac.binus.bounty.R
import id.ac.binus.bounty.data.FirebaseAccountRepository
import id.ac.binus.bounty.utils.ScreenInsets
import id.ac.binus.bounty.utils.SessionManager

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SessionManager(this).isFirebaseAuthenticated()) {
            openHome()
            return
        }
        setContentView(R.layout.activity_login)
        ScreenInsets.apply(this)
        val email = findViewById<TextInputEditText>(R.id.email_input)
        val password = findViewById<TextInputEditText>(R.id.password_input)
        val emailLayout = findViewById<TextInputLayout>(R.id.email_layout)
        val passwordLayout = findViewById<TextInputLayout>(R.id.password_layout)
        if (savedInstanceState == null) intent.getStringExtra("registered_email")?.let { email.setText(it) }
        findViewById<android.view.View>(R.id.login_button).setOnClickListener {
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
        findViewById<android.view.View>(R.id.register_link).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setBusy(busy: Boolean) {
        findViewById<android.view.View>(R.id.login_button).isEnabled = !busy
        findViewById<android.view.View>(R.id.register_link).isEnabled = !busy
    }

    private fun openHome() {
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
}
