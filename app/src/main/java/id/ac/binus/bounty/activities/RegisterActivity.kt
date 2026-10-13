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

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SessionManager(this).isFirebaseAuthenticated) {
            openHome()
            return
        }
        setContentView(R.layout.activity_register)
        ScreenInsets.apply(this)
        val name = findViewById<TextInputEditText>(R.id.name_input)
        val email = findViewById<TextInputEditText>(R.id.email_input)
        val password = findViewById<TextInputEditText>(R.id.password_input)
        val nameLayout = findViewById<TextInputLayout>(R.id.name_layout)
        val emailLayout = findViewById<TextInputLayout>(R.id.email_layout)
        val passwordLayout = findViewById<TextInputLayout>(R.id.password_layout)
        findViewById<android.view.View>(R.id.register_button).setOnClickListener {
            nameLayout.error = null
            emailLayout.error = null
            passwordLayout.error = null
            val nameValue = name.text?.toString()?.trim().orEmpty()
            val emailValue = SessionManager.normalizeEmail(email.text?.toString())
            val passwordValue = password.text?.toString().orEmpty()
            var valid = true
            if (nameValue.isEmpty()) {
                nameLayout.error = getString(R.string.required_name)
                valid = false
            }
            if (!SessionManager.isValidEmail(emailValue)) {
                emailLayout.error = getString(R.string.invalid_email)
                valid = false
            }
            if (passwordValue.length < 6) {
                passwordLayout.error = getString(R.string.weak_password)
                valid = false
            }
            if (!valid) return@setOnClickListener
            setBusy(true)
            FirebaseAccountRepository.register(nameValue, emailValue, passwordValue) { result ->
                runOnUiThread {
                    setBusy(false)
                    result.onSuccess { account ->
                        SessionManager(this).cacheFirebaseUser(account.uid, account.name, account.email)
                        openHome()
                    }.onFailure { error ->
                        emailLayout.error = getString(FirebaseAccountRepository.errorMessage(error, false))
                    }
                }
            }
        }
        findViewById<android.view.View>(R.id.login_link).setOnClickListener { finish() }
    }

    private fun setBusy(busy: Boolean) {
        findViewById<android.view.View>(R.id.register_button).isEnabled = !busy
        findViewById<android.view.View>(R.id.login_link).isEnabled = !busy
    }

    private fun openHome() {
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
}
