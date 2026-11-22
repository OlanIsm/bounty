package id.ac.binus.bounty.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.PathInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import id.ac.binus.bounty.R
import id.ac.binus.bounty.utils.SessionManager
import id.ac.binus.bounty.utils.ViewMotion

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var navigated = false
    private val navigateRunnable = Runnable { navigateNext() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        val content = findViewById<View>(R.id.splash_content)
        val burst = findViewById<View>(R.id.splash_burst)
        val mascot = findViewById<View>(R.id.splash_mascot)

        // Glide mascot + wordmark smoothly up from bottom to center
        val startOffset = (resources.displayMetrics.heightPixels * 0.35f)
        content.translationY = startOffset
        content.alpha = 0f
        content.scaleX = 0.90f
        content.scaleY = 0.90f

        content.animate()
            .translationY(0f)
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(680)
            .setInterpolator(PathInterpolator(0.22f, 1f, 0.36f, 1f))
            .withEndAction {
                ViewMotion.startSuccessBurstAnimation(burst, mascot)
            }
            .start()

        handler.postDelayed(navigateRunnable, 1250L)
    }

    private fun navigateNext() {
        if (navigated || isFinishing || isDestroyed) return
        navigated = true
        val destination = if (SessionManager(this).isFirebaseAuthenticated()) {
            MainActivity::class.java
        } else {
            LoginActivity::class.java
        }
        startActivity(
            Intent(this, destination)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        ViewMotion.applySlideUpFromBottom(this)
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacks(navigateRunnable)
        super.onDestroy()
    }
}
