package id.ac.binus.bounty.utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import id.ac.binus.bounty.R

/**
 * Fluid, tactile motion utilities inspired by Apple Designing Fluid Interfaces (WWDC)
 * and Emil Kowalski design engineering principles:
 * - Instant response on pointer-down (scale to 0.96f, 90ms ease-out)
 * - Natural spring bounce on release (scale to 1.0f, OvershootInterpolator)
 * - Enter transitions: scale from 0.95f + alpha fade (nothing appears from nothing)
 */
object ViewMotion {
    private val springInterpolator = OvershootInterpolator(1.4f)
    private val pressInterpolator = DecelerateInterpolator(1.8f)

    @SuppressLint("ClickableViewAccessibility")
    @JvmStatic
    fun applySpringPress(view: View, onClick: (() -> Unit)? = null) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.96f)
                        .scaleY(0.96f)
                        .setDuration(90)
                        .setInterpolator(pressInterpolator)
                        .start()
                }
                MotionEvent.ACTION_UP -> {
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(180)
                        .setInterpolator(springInterpolator)
                        .start()
                    if (onClick != null) {
                        v.postDelayed({ onClick.invoke() }, 50)
                    } else {
                        v.performClick()
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(160)
                        .setInterpolator(springInterpolator)
                        .start()
                }
            }
            true
        }
    }

    @JvmStatic
    fun animateEntrance(view: View, delayMs: Long = 0) {
        view.alpha = 0f
        view.scaleX = 0.95f
        view.scaleY = 0.95f
        view.translationY = 24f

        val alphaAnim = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)
        val scaleXAnim = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.95f, 1f)
        val scaleYAnim = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.95f, 1f)
        val transYAnim = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 24f, 0f)

        AnimatorSet().apply {
            playTogether(alphaAnim, scaleXAnim, scaleYAnim, transYAnim)
            duration = 260
            startDelay = delayMs
            interpolator = DecelerateInterpolator(1.6f)
            start()
        }
    }

    /**
     * Gentle, flowing coin animation:
     * - Slow yaw rotation (-22 to +22 deg) facing left/right smoothly
     * - Subtle floating translation (-4dp to +4dp)
     * Calm pace so it does not stress the eye.
     */
    @JvmStatic
    fun startGentleCoinAnimation(coinView: View) {
        // Avoid stacking duplicate animators
        if (coinView.getTag(R.id.tag_animating) == true) return
        coinView.setTag(R.id.tag_animating, true)

        val yawAnim = ObjectAnimator.ofFloat(coinView, View.ROTATION_Y, -22f, 22f).apply {
            duration = 3200
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }

        val floatAnim = ObjectAnimator.ofFloat(coinView, View.TRANSLATION_Y, -4f, 4f).apply {
            duration = 2600
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(yawAnim, floatAnim)
            start()
        }
    }

    /**
     * Subtle shiny sweep across a surface or card:
     * Sweeps periodically with an elegant pause.
     */
    @JvmStatic
    fun startShimmerSweep(shineView: View, container: View, intervalMs: Long = 4200) {
        if (shineView.getTag(R.id.tag_animating) == true) return
        shineView.setTag(R.id.tag_animating, true)

        container.post {
            val width = if (container.width > 0) container.width.toFloat() else 600f
            shineView.translationX = -shineView.width.toFloat().coerceAtLeast(100f)
            shineView.alpha = 0f

            val sweepAnim = ValueAnimator.ofFloat(-120f, width + 120f).apply {
                duration = 1600
                startDelay = 600
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { anim ->
                    val pos = anim.animatedValue as Float
                    shineView.translationX = pos
                    // Smooth bell-curve alpha for shine
                    val progress = anim.animatedFraction
                    shineView.alpha = if (progress < 0.2f) {
                        progress / 0.2f * 0.7f
                    } else if (progress > 0.8f) {
                        (1f - progress) / 0.2f * 0.7f
                    } else {
                        0.7f
                    }
                }
            }

            fun loop() {
                sweepAnim.start()
                container.postDelayed({ loop() }, intervalMs)
            }

            loop()
        }
    }
}
