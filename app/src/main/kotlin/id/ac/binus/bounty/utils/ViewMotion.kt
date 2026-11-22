package id.ac.binus.bounty.utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import id.ac.binus.bounty.R

/**
 * Soothing ease-in-out motion utilities inspired by Apple Fluid Interfaces
 * and Emil Kowalski design engineering principles:
 * - Soft tactile press (scale to 0.97f, 110ms ease-out) & calm settle (220ms ease-in-out)
 * - Smooth enter transitions with gentle vertical glide + alpha fade
 * - In-place auth morph transitions (hero cross-fade + rising form sheet)
 */
object ViewMotion {
    private val soothingEase = PathInterpolator(0.2f, 0f, 0f, 1f)
    private val gentleOut = PathInterpolator(0.22f, 1f, 0.36f, 1f)

    @SuppressLint("ClickableViewAccessibility")
    @JvmStatic
    fun applySpringPress(view: View, onClick: (() -> Unit)? = null) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.97f)
                        .scaleY(0.97f)
                        .setDuration(80)
                        .setInterpolator(soothingEase)
                        .start()
                }
                MotionEvent.ACTION_UP -> {
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(160)
                        .setInterpolator(gentleOut)
                        .start()
                    if (onClick != null) {
                        onClick.invoke()
                    } else {
                        v.performClick()
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(150)
                        .setInterpolator(gentleOut)
                        .start()
                }
            }
            true
        }
    }

    @JvmStatic
    fun animateEntrance(view: View, delayMs: Long = 0) {
        view.alpha = 0f
        view.translationY = 12f
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(200)
            .setStartDelay(delayMs)
            .setInterpolator(soothingEase)
            .withLayer()
            .start()
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun applySlideUpFromBottom(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN,
                R.anim.bounty_slide_in_bottom,
                R.anim.bounty_slide_out_top
            )
        } else {
            activity.overridePendingTransition(R.anim.bounty_slide_in_bottom, R.anim.bounty_slide_out_top)
        }
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun applySlideFromRight(activity: Activity, opening: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (opening) {
                activity.overrideActivityTransition(
                    Activity.OVERRIDE_TRANSITION_OPEN,
                    R.anim.bounty_slide_in_right,
                    R.anim.bounty_slide_out_left
                )
            } else {
                activity.overrideActivityTransition(
                    Activity.OVERRIDE_TRANSITION_CLOSE,
                    R.anim.bounty_slide_in_left,
                    R.anim.bounty_slide_out_right
                )
            }
        } else {
            if (opening) {
                activity.overridePendingTransition(R.anim.bounty_slide_in_right, R.anim.bounty_slide_out_left)
            } else {
                activity.overridePendingTransition(R.anim.bounty_slide_in_left, R.anim.bounty_slide_out_right)
            }
        }
    }

    /**
     * Directional horizontal slide between bottom-nav destinations:
     * slideLeft = true when navigating to a tab on the right (old slides left, new enters from right),
     * slideLeft = false when navigating to a tab on the left (old slides right, new enters from left).
     */
    @JvmStatic
    fun animateHorizontalTabSwap(
        container: ViewGroup,
        outgoingView: View?,
        incomingView: View,
        slideLeft: Boolean
    ) {
        // Clean up any stale exiting views from rapid tab taps
        for (i in container.childCount - 1 downTo 0) {
            val child = container.getChildAt(i)
            if (child !== outgoingView && child !== incomingView) {
                child.animate().cancel()
                container.removeView(child)
            }
        }
        val width = (container.width.takeIf { it > 0 } ?: container.resources.displayMetrics.widthPixels).toFloat()
        val direction = if (slideLeft) 1f else -1f

        if (incomingView.parent == null) {
            container.addView(incomingView)
        }
        incomingView.animate().cancel()
        incomingView.translationX = width * direction
        incomingView.alpha = 0.96f
        incomingView.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(210)
            .setInterpolator(soothingEase)
            .withLayer()
            .start()

        if (outgoingView != null && outgoingView !== incomingView) {
            outgoingView.animate().cancel()
            outgoingView.animate()
                .translationX(-width * direction)
                .alpha(0.90f)
                .setDuration(210)
                .setInterpolator(soothingEase)
                .withLayer()
                .withEndAction {
                    outgoingView.translationX = 0f
                    outgoingView.alpha = 1f
                    container.removeView(outgoingView)
                }
                .start()
        }
    }

    /**
     * Suppresses default horizontal slide when morphing between Login and Register.
     */
    @Suppress("DEPRECATION")
    @JvmStatic
    fun disableWindowTransition(activity: Activity, opening: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val type = if (opening) Activity.OVERRIDE_TRANSITION_OPEN else Activity.OVERRIDE_TRANSITION_CLOSE
            activity.overrideActivityTransition(type, 0, 0)
        } else {
            activity.overridePendingTransition(0, 0)
        }
    }

    /**
     * Smooth in-place morph for Login <-> Sign Up:
     * hero mascot + title cross-fade gently while the form sheet rises up smoothly
     * and its children stagger in with a calming ease-in-out curve.
     */
    @JvmStatic
    fun animateAuthMorph(heroContainer: View?, sheet: View) {
        val density = sheet.resources.displayMetrics.density
        heroContainer?.let { hero ->
            hero.alpha = 0f
            hero.scaleX = 0.94f
            hero.scaleY = 0.94f
            hero.translationY = -10f * density
            hero.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(380)
                .setInterpolator(soothingEase)
                .start()
        }

        sheet.translationY = 34f * density
        sheet.alpha = 0.92f
        sheet.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(360)
            .setInterpolator(soothingEase)
            .start()

        if (sheet is ViewGroup) {
            for (i in 0 until sheet.childCount) {
                val child = sheet.getChildAt(i)
                if (child.visibility == View.GONE) continue
                child.alpha = 0f
                child.translationY = 14f * density
                child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(55L + (i * 35L))
                    .setDuration(300)
                    .setInterpolator(soothingEase)
                    .start()
            }
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

    /**
     * Animated radiant burst and floating thumbs-up mascot for challenge accepted modal.
     */
    @JvmStatic
    fun startSuccessBurstAnimation(burstView: View, mascotView: View) {
        val rotateAnim = ObjectAnimator.ofFloat(burstView, View.ROTATION, 0f, 360f).apply {
            duration = 14000
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
        }

        val scaleXBurst = ObjectAnimator.ofFloat(burstView, View.SCALE_X, 0.92f, 1.12f).apply {
            duration = 2200
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleYBurst = ObjectAnimator.ofFloat(burstView, View.SCALE_Y, 0.92f, 1.12f).apply {
            duration = 2200
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }

        val mascotFloat = ObjectAnimator.ofFloat(mascotView, View.TRANSLATION_Y, -8f, 8f).apply {
            duration = 1800
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }

        val mascotScaleX = ObjectAnimator.ofFloat(mascotView, View.SCALE_X, 0.97f, 1.03f).apply {
            duration = 1800
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val mascotScaleY = ObjectAnimator.ofFloat(mascotView, View.SCALE_Y, 0.97f, 1.03f).apply {
            duration = 1800
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(rotateAnim, scaleXBurst, scaleYBurst, mascotFloat, mascotScaleX, mascotScaleY)
            start()
        }
    }
}
