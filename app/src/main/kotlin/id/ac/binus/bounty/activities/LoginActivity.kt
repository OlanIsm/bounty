package id.ac.binus.bounty.activities

import android.content.Intent
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import id.ac.binus.bounty.R
import id.ac.binus.bounty.data.FirebaseAccountRepository
import id.ac.binus.bounty.utils.ScreenInsets
import id.ac.binus.bounty.utils.SessionManager
import id.ac.binus.bounty.utils.ViewMotion

class LoginActivity : AppCompatActivity() {

    private var isSignUpMode = false
    private val soothingEase = PathInterpolator(0.42f, 0f, 0.2f, 1f)

    private val backToLoginCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            setAuthMode(signUp = false, animate = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SessionManager(this).isFirebaseAuthenticated()) {
            openHome()
            return
        }
        setContentView(R.layout.activity_login)
        ScreenInsets.applyHeroEdgeToEdge(this, findViewById(R.id.login_sheet))
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        onBackPressedDispatcher.addCallback(this, backToLoginCallback)

        ViewMotion.startSuccessBurstAnimation(
            findViewById(R.id.auth_hero_burst),
            findViewById(R.id.auth_hero_mascot)
        )

        val name = findViewById<TextInputEditText>(R.id.name_input)
        val email = findViewById<TextInputEditText>(R.id.email_input)
        val password = findViewById<TextInputEditText>(R.id.password_input)
        val nameLayout = findViewById<TextInputLayout>(R.id.name_layout)
        val emailLayout = findViewById<TextInputLayout>(R.id.email_layout)
        val passwordLayout = findViewById<TextInputLayout>(R.id.password_layout)

        if (savedInstanceState == null) {
            intent.getStringExtra("registered_email")?.let { email.setText(it) }
        } else {
            isSignUpMode = savedInstanceState.getBoolean("is_signup_mode", false)
            setAuthMode(isSignUpMode, animate = false)
        }

        ViewMotion.applySpringPress(findViewById(R.id.btn_auth_back)) {
            setAuthMode(signUp = false, animate = true)
        }

        ViewMotion.applySpringPress(findViewById(R.id.login_button)) {
            nameLayout.error = null
            emailLayout.error = null
            passwordLayout.error = null
            val emailValue = SessionManager.normalizeEmail(email.text?.toString())
            val passwordValue = password.text?.toString().orEmpty()

            if (isSignUpMode) {
                val nameValue = name.text?.toString()?.trim().orEmpty()
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
                if (!valid) return@applySpringPress
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
            } else {
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
        }

        ViewMotion.applySpringPress(findViewById(R.id.google_login_button)) {
            nameLayout.error = null
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
                            getString(FirebaseAccountRepository.errorMessage(error, !isSignUpMode)),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        ViewMotion.applySpringPress(findViewById(R.id.register_link)) {
            setAuthMode(signUp = !isSignUpMode, animate = true)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("is_signup_mode", isSignUpMode)
        super.onSaveInstanceState(outState)
    }

    /**
     * Smoothly lifts the curved white form sheet upward to reveal the Name field (or collapses
     * it back down for Login) with zero activity swap / blink, while cross-fading mascot & title.
     */
    private fun setAuthMode(signUp: Boolean, animate: Boolean) {
        isSignUpMode = signUp
        backToLoginCallback.isEnabled = signUp

        val root = findViewById<ViewGroup>(R.id.auth_root_container)
        val nameLayout = findViewById<TextInputLayout>(R.id.name_layout)
        val emailLayout = findViewById<TextInputLayout>(R.id.email_layout)
        val passwordLayout = findViewById<TextInputLayout>(R.id.password_layout)
        val title = findViewById<TextView>(R.id.app_title)
        val subtitle = findViewById<TextView>(R.id.auth_hero_subtitle)
        val pills = findViewById<View>(R.id.auth_hero_pills)
        val mascot = findViewById<ImageView>(R.id.auth_hero_mascot)
        val backBtn = findViewById<View>(R.id.btn_auth_back)
        val submitBtn = findViewById<MaterialButton>(R.id.login_button)
        val switchLink = findViewById<MaterialButton>(R.id.register_link)

        nameLayout.error = null
        emailLayout.error = null
        passwordLayout.error = null

        if (animate) {
            val transition = TransitionSet().apply {
                ordering = TransitionSet.ORDERING_TOGETHER
                addTransition(ChangeBounds().setDuration(380).setInterpolator(soothingEase))
                addTransition(Fade(Fade.IN).setDuration(320).setStartDelay(80).setInterpolator(soothingEase))
                addTransition(Fade(Fade.OUT).setDuration(180).setInterpolator(soothingEase))
            }
            TransitionManager.beginDelayedTransition(root, transition)

            // Cross-fade mascot & hero text smoothly
            mascot.animate()
                .alpha(0.35f)
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(150)
                .setInterpolator(soothingEase)
                .withEndAction {
                    mascot.setImageResource(
                        if (signUp) R.drawable.mascot_leaderboard else R.drawable.mascot_confirmed
                    )
                    mascot.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(260)
                        .setInterpolator(soothingEase)
                        .start()
                }
                .start()

            listOf(title, subtitle).forEach { tv ->
                tv.animate().alpha(0f).setDuration(130).setInterpolator(soothingEase).withEndAction {
                    title.setText(if (signUp) R.string.register_title else R.string.login_title)
                    subtitle.setText(if (signUp) R.string.register_hero_subtitle else R.string.login_hero_subtitle)
                    tv.animate().alpha(1f).setDuration(240).setInterpolator(soothingEase).start()
                }.start()
            }
        } else {
            mascot.setImageResource(if (signUp) R.drawable.mascot_leaderboard else R.drawable.mascot_confirmed)
            title.setText(if (signUp) R.string.register_title else R.string.login_title)
            subtitle.setText(if (signUp) R.string.register_hero_subtitle else R.string.login_hero_subtitle)
        }

        nameLayout.visibility = if (signUp) View.VISIBLE else View.GONE
        pills.visibility = if (signUp) View.GONE else View.VISIBLE
        backBtn.visibility = if (signUp) View.VISIBLE else View.GONE
        submitBtn.setText(if (signUp) R.string.register_action else R.string.login_action)
        switchLink.setText(if (signUp) R.string.login_link else R.string.register_link)
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

    companion object {
        internal var returningFromRegister = false
    }
}
