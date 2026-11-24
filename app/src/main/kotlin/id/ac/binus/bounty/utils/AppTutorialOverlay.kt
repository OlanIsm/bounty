package id.ac.binus.bounty.utils

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.google.android.material.progressindicator.LinearProgressIndicator
import id.ac.binus.bounty.R

/**
 * Full-screen interactive walkthrough overlay:
 * - Dims everything except a rounded spotlight cutout around the target view
 * - Strictly blocks touches outside the speech card (and outside the spotlight on tap-required steps)
 * - Displays the floating Bounty pirate-robot mascot + speech card automatically positioned
 *   above or below the spotlight so it never obscures the highlighted UI element.
 */
class AppTutorialOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val easeOut = PathInterpolator(0.22f, 1f, 0.36f, 1f)

    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BA0F122B")
        style = Paint.Style.FILL
    }
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val outerHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66FFC83D")
        style = Paint.Style.STROKE
        strokeWidth = 6f * density
    }
    private val innerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
    }

    private val currentSpot = RectF()
    private val startSpot = RectF()
    private val endSpot = RectF()
    private var hasSpotlight = false
    private var spotCornerRadius = 18f * density
    private var requireTargetTap = false
    private var trackingCardTouch = false
    private var pulsePhase = 0f

    private var spotAnimator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null
    private var mascotBobAnimator: ObjectAnimator? = null

    private val cardWrapper: View
    private val cardView: View
    private val mascotView: ImageView
    private val stepBadge: TextView
    private val progressBar: LinearProgressIndicator
    private val titleView: TextView
    private val bodyView: TextView
    private val skipBtn: View
    private val backBtn: View
    private val nextBtn: TextView
    private val actionsRow: View
    private val tapHintBox: View
    private val tapHintText: TextView

    var onNextClick: (() -> Unit)? = null
    var onBackClick: (() -> Unit)? = null
    var onSkipClick: (() -> Unit)? = null

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
        visibility = GONE

        cardWrapper = LayoutInflater.from(context).inflate(R.layout.view_tutorial_card, this, false)
        addView(
            cardWrapper,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.TOP)
        )

        cardView = cardWrapper.findViewById(R.id.tutorial_card)
        mascotView = cardWrapper.findViewById(R.id.tutorial_mascot)
        stepBadge = cardWrapper.findViewById(R.id.tutorial_step_badge)
        progressBar = cardWrapper.findViewById(R.id.tutorial_progress)
        titleView = cardWrapper.findViewById(R.id.tutorial_title)
        bodyView = cardWrapper.findViewById(R.id.tutorial_body)
        skipBtn = cardWrapper.findViewById(R.id.tutorial_skip_btn)
        backBtn = cardWrapper.findViewById(R.id.tutorial_back_btn)
        nextBtn = cardWrapper.findViewById(R.id.tutorial_next_btn)
        actionsRow = cardWrapper.findViewById(R.id.tutorial_actions_row)
        tapHintBox = cardWrapper.findViewById(R.id.tutorial_tap_hint_box)
        tapHintText = cardWrapper.findViewById(R.id.tutorial_tap_hint_text)

        ViewMotion.applySpringPress(nextBtn) { onNextClick?.invoke() }
        ViewMotion.applySpringPress(backBtn) { onBackClick?.invoke() }
        ViewMotion.applySpringPress(skipBtn) { onSkipClick?.invoke() }
    }

    fun showStep(
        stepIndex: Int,
        totalSteps: Int,
        title: String,
        body: String,
        @DrawableRes mascotRes: Int,
        nextLabel: String,
        showBack: Boolean,
        requireTap: Boolean,
        tapHint: String?,
        targetView: View?,
        circularSpotlight: Boolean = false
    ) {
        val wasHidden = visibility != VISIBLE
        visibility = VISIBLE
        requireTargetTap = requireTap

        stepBadge.text = context.getString(R.string.tutorial_step_counter, stepIndex + 1, totalSteps)
        progressBar.max = totalSteps
        progressBar.setProgressCompat(stepIndex + 1, !wasHidden)
        titleView.text = title
        bodyView.text = body
        mascotView.setImageResource(mascotRes)

        if (requireTap) {
            actionsRow.visibility = GONE
            tapHintBox.visibility = VISIBLE
            tapHintText.text = tapHint ?: context.getString(R.string.tutorial_tap_highlighted)
            startPulseAnimation()
        } else {
            actionsRow.visibility = VISIBLE
            tapHintBox.visibility = GONE
            nextBtn.text = nextLabel
            backBtn.visibility = if (showBack) VISIBLE else GONE
            stopPulseAnimation()
        }

        if (wasHidden) {
            alpha = 0f
            animate().alpha(1f).setDuration(200).setInterpolator(easeOut).start()
            startMascotBob()
        }

        // Compute spotlight bounds relative to this overlay
        if (targetView != null && targetView.isShown && targetView.width > 0 && targetView.height > 0) {
            val targetLoc = IntArray(2)
            val overlayLoc = IntArray(2)
            targetView.getLocationInWindow(targetLoc)
            getLocationInWindow(overlayLoc)

            val pad = (if (circularSpotlight) 6f else 8f) * density
            val left = (targetLoc[0] - overlayLoc[0]) - pad
            val top = (targetLoc[1] - overlayLoc[1]) - pad
            val right = left + targetView.width + pad * 2f
            val bottom = top + targetView.height + pad * 2f
            val newRect = RectF(left, top, right, bottom)
            spotCornerRadius = if (circularSpotlight) {
                (newRect.width().coerceAtLeast(newRect.height()) / 2f)
            } else {
                20f * density
            }
            animateSpotlightTo(newRect)
            positionCardForTarget(newRect)
        } else {
            hasSpotlight = false
            spotAnimator?.cancel()
            currentSpot.setEmpty()
            invalidate()
            positionCardCentered()
        }

        animateCardTransition()
    }

    fun hideOverlay(onEnd: (() -> Unit)? = null) {
        if (visibility != VISIBLE) {
            onEnd?.invoke()
            return
        }
        stopPulseAnimation()
        stopMascotBob()
        spotAnimator?.cancel()
        animate()
            .alpha(0f)
            .setDuration(180)
            .setInterpolator(easeOut)
            .withEndAction {
                visibility = GONE
                alpha = 1f
                hasSpotlight = false
                currentSpot.setEmpty()
                onEnd?.invoke()
            }
            .start()
    }

    private fun animateSpotlightTo(targetRect: RectF) {
        spotAnimator?.cancel()
        if (!hasSpotlight || currentSpot.isEmpty) {
            hasSpotlight = true
            currentSpot.set(targetRect)
            invalidate()
            return
        }
        startSpot.set(currentSpot)
        endSpot.set(targetRect)
        spotAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 260
            interpolator = easeOut
            addUpdateListener { anim ->
                val f = anim.animatedValue as Float
                currentSpot.set(
                    startSpot.left + (endSpot.left - startSpot.left) * f,
                    startSpot.top + (endSpot.top - startSpot.top) * f,
                    startSpot.right + (endSpot.right - startSpot.right) * f,
                    startSpot.bottom + (endSpot.bottom - startSpot.bottom) * f
                )
                invalidate()
            }
            start()
        }
    }

    private fun positionCardCentered() {
        val lp = cardWrapper.layoutParams as LayoutParams
        lp.gravity = Gravity.CENTER_VERTICAL
        lp.topMargin = 0
        lp.bottomMargin = 0
        cardWrapper.layoutParams = lp
    }

    private fun positionCardForTarget(spot: RectF) {
        val overlayHeight = if (height > 0) height else resources.displayMetrics.heightPixels
        val lp = cardWrapper.layoutParams as LayoutParams
        val gap = (16 * density).toInt()
        val minTopMargin = (44 * density).toInt()
        val minBottomMargin = (24 * density).toInt()

        val targetCenterY = spot.centerY()
        if (targetCenterY > overlayHeight * 0.52f) {
            // Target is in lower half (e.g. bottom nav) -> place card ABOVE spotlight
            lp.gravity = Gravity.BOTTOM
            lp.topMargin = 0
            val bottomDist = (overlayHeight - spot.top + gap).toInt().coerceAtLeast(minBottomMargin)
            lp.bottomMargin = bottomDist.coerceAtMost((overlayHeight * 0.58f).toInt())
        } else {
            // Target is in upper half -> place card BELOW spotlight
            lp.gravity = Gravity.TOP
            lp.bottomMargin = 0
            val topDist = (spot.bottom + gap).toInt().coerceAtLeast(minTopMargin)
            lp.topMargin = topDist.coerceAtMost((overlayHeight * 0.55f).toInt())
        }
        cardWrapper.layoutParams = lp
    }

    private fun animateCardTransition() {
        cardWrapper.animate().cancel()
        cardWrapper.alpha = 0f
        cardWrapper.scaleX = 0.95f
        cardWrapper.scaleY = 0.95f
        cardWrapper.translationY = 10f * density
        cardWrapper.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(220)
            .setInterpolator(easeOut)
            .start()
    }

    private fun startPulseAnimation() {
        if (pulseAnimator?.isRunning == true) return
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1100
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                pulsePhase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        pulsePhase = 0f
    }

    private fun startMascotBob() {
        if (mascotBobAnimator?.isRunning == true) return
        mascotBobAnimator = ObjectAnimator.ofFloat(mascotView, View.TRANSLATION_Y, -4f * density, 4f * density).apply {
            duration = 1800
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun stopMascotBob() {
        mascotBobAnimator?.cancel()
        mascotBobAnimator = null
        mascotView.translationY = 0f
    }

    private fun triggerBlockedTapNudge() {
        if (requireTargetTap) {
            tapHintBox.animate().cancel()
            tapHintBox.scaleX = 0.96f
            tapHintBox.scaleY = 0.96f
            tapHintBox.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180)
                .setInterpolator(easeOut)
                .start()
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (visibility == VISIBLE) {
            val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
            if (hasSpotlight && !currentSpot.isEmpty) {
                canvas.drawRoundRect(currentSpot, spotCornerRadius, spotCornerRadius, clearPaint)
            }
            canvas.restoreToCount(saveCount)

            if (hasSpotlight && !currentSpot.isEmpty) {
                val expand = if (requireTargetTap) (3f + pulsePhase * 5f) * density else 2f * density
                val haloRect = RectF(
                    currentSpot.left - expand,
                    currentSpot.top - expand,
                    currentSpot.right + expand,
                    currentSpot.bottom + expand
                )
                outerHaloPaint.alpha = if (requireTargetTap) (150 - (pulsePhase * 70).toInt()) else 110
                canvas.drawRoundRect(haloRect, spotCornerRadius + expand, spotCornerRadius + expand, outerHaloPaint)
                canvas.drawRoundRect(currentSpot, spotCornerRadius, spotCornerRadius, innerRingPaint)
            }
        }
        super.dispatchDraw(canvas)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (visibility != VISIBLE) return false

        // Always allow interaction with the tutorial speech card (Next / Back / Skip)
        val x = ev.x
        val y = ev.y
        val cardLeft = cardWrapper.left + cardView.left
        val cardTop = cardWrapper.top + cardView.top
        val cardRight = cardLeft + cardView.width
        val cardBottom = cardTop + cardView.height
        val insideCard = x >= cardLeft && x <= cardRight && y >= cardTop && y <= cardBottom

        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            trackingCardTouch = insideCard
        }
        if (trackingCardTouch) {
            val handled = super.dispatchTouchEvent(ev)
            if (ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL) {
                trackingCardTouch = false
            }
            return handled
        }

        // On interactive tap-required steps, pass through ONLY taps inside the spotlight cutout
        if (requireTargetTap && hasSpotlight && !currentSpot.isEmpty) {
            val slop = 6f * density
            if (x >= currentSpot.left - slop &&
                x <= currentSpot.right + slop &&
                y >= currentSpot.top - slop &&
                y <= currentSpot.bottom + slop
            ) {
                return false
            }
        }

        // Block all other touches outside the spotlight & card
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            triggerBlockedTapNudge()
        }
        return true
    }

    override fun onDetachedFromWindow() {
        stopPulseAnimation()
        stopMascotBob()
        spotAnimator?.cancel()
        super.onDetachedFromWindow()
    }
}
