package id.ac.binus.bounty.utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

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
}
