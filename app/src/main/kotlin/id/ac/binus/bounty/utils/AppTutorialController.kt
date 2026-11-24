package id.ac.binus.bounty.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import id.ac.binus.bounty.BuildConfig
import id.ac.binus.bounty.R

/**
 * Orchestrates the 12-step interactive mascot walkthrough across MainActivity's tabs.
 * Persists completion per user in SharedPreferences and supports replay from Profile settings.
 */
class AppTutorialController(
    private val activity: AppCompatActivity,
    private val overlay: AppTutorialOverlay,
    private val getSelectedDestination: () -> Int,
    private val navigateToDestination: (Int) -> Unit,
    private val findNavItemView: (Int) -> View?,
    private val onActiveStateChanged: (Boolean) -> Unit = {}
) {

    data class Step(
        @StringRes val titleRes: Int,
        @StringRes val bodyRes: Int,
        @DrawableRes val mascotRes: Int = R.drawable.mascot_leaderboard,
        @IdRes val screenDestination: Int,
        @IdRes val targetViewId: Int? = null,
        val targetIsNavItem: Boolean = false,
        val circularSpotlight: Boolean = false,
        val requireTargetTap: Boolean = false,
        @IdRes val expectedNextDestination: Int? = null,
        @StringRes val tapHintRes: Int? = null,
        @StringRes val nextLabelRes: Int = R.string.tutorial_next
    )

    private val handler = Handler(Looper.getMainLooper())
    private var currentIndex = -1
    private var pendingRender: Runnable? = null

    val isActive: Boolean
        get() = currentIndex >= 0

    private val steps: List<Step> = listOf(
        // 1. Welcome
        Step(
            titleRes = R.string.tutorial_step_welcome_title,
            bodyRes = R.string.tutorial_step_welcome_body,
            mascotRes = R.drawable.mascot_leaderboard,
            screenDestination = R.id.nav_home,
            nextLabelRes = R.string.tutorial_start_cta
        ),
        // 2. Demo Balance on Home
        Step(
            titleRes = R.string.tutorial_step_balance_title,
            bodyRes = R.string.tutorial_step_balance_body,
            mascotRes = R.drawable.mascot_confirmed,
            screenDestination = R.id.nav_home,
            targetViewId = R.id.demo_balance_container
        ),
        // 3. Find Friends icon on Home
        Step(
            titleRes = R.string.tutorial_step_friends_title,
            bodyRes = R.string.tutorial_step_friends_body,
            mascotRes = R.drawable.mascot_leaderboard,
            screenDestination = R.id.nav_home,
            targetViewId = R.id.btn_home_search_friends,
            circularSpotlight = true
        ),
        // 4. Chat icon on Home
        Step(
            titleRes = R.string.tutorial_step_chat_title,
            bodyRes = R.string.tutorial_step_chat_body,
            mascotRes = R.drawable.mascot_confirmed,
            screenDestination = R.id.nav_home,
            targetViewId = R.id.btn_home_chat,
            circularSpotlight = true
        ),
        // 5. Interactive: Tap Challenge tab
        Step(
            titleRes = R.string.tutorial_step_tap_challenges_title,
            bodyRes = R.string.tutorial_step_tap_challenges_body,
            mascotRes = R.drawable.mascot_leaderboard,
            screenDestination = R.id.nav_home,
            targetViewId = R.id.nav_my_challenges,
            targetIsNavItem = true,
            requireTargetTap = true,
            expectedNextDestination = R.id.nav_my_challenges,
            tapHintRes = R.string.tutorial_tap_challenge_tab
        ),
        // 6. Explain My Challenges & Accept/Proof flow
        Step(
            titleRes = R.string.tutorial_step_challenges_info_title,
            bodyRes = R.string.tutorial_step_challenges_info_body,
            mascotRes = R.drawable.mascot_confirmed,
            screenDestination = R.id.nav_my_challenges,
            targetViewId = R.id.challenge_toggle_group
        ),
        // 7. Interactive: Tap Create (+) FAB in bottom nav
        Step(
            titleRes = R.string.tutorial_step_tap_create_title,
            bodyRes = R.string.tutorial_step_tap_create_body,
            mascotRes = R.drawable.mascot_leaderboard,
            screenDestination = R.id.nav_my_challenges,
            targetViewId = R.id.nav_create,
            targetIsNavItem = true,
            circularSpotlight = true,
            requireTargetTap = true,
            expectedNextDestination = R.id.nav_create,
            tapHintRes = R.string.tutorial_tap_create_tab
        ),
        // 8. Explain Create Challenge screen (no need to submit)
        Step(
            titleRes = R.string.tutorial_step_create_info_title,
            bodyRes = R.string.tutorial_step_create_info_body,
            mascotRes = R.drawable.mascot_confirmed,
            screenDestination = R.id.nav_create,
            targetViewId = R.id.create_form_card
        ),
        // 9. Interactive: Tap Leaderboard tab
        Step(
            titleRes = R.string.tutorial_step_tap_leaderboard_title,
            bodyRes = R.string.tutorial_step_tap_leaderboard_body,
            mascotRes = R.drawable.mascot_leaderboard,
            screenDestination = R.id.nav_create,
            targetViewId = R.id.nav_leaderboard,
            targetIsNavItem = true,
            requireTargetTap = true,
            expectedNextDestination = R.id.nav_leaderboard,
            tapHintRes = R.string.tutorial_tap_leaderboard_tab
        ),
        // 10. Explain Leaderboard
        Step(
            titleRes = R.string.tutorial_step_leaderboard_info_title,
            bodyRes = R.string.tutorial_step_leaderboard_info_body,
            mascotRes = R.drawable.mascot_leaderboard,
            screenDestination = R.id.nav_leaderboard,
            targetViewId = R.id.leaderboard_toggle_group
        ),
        // 11. Interactive: Tap Profile tab
        Step(
            titleRes = R.string.tutorial_step_tap_profile_title,
            bodyRes = R.string.tutorial_step_tap_profile_body,
            mascotRes = R.drawable.mascot_confirmed,
            screenDestination = R.id.nav_leaderboard,
            targetViewId = R.id.nav_profile,
            targetIsNavItem = true,
            requireTargetTap = true,
            expectedNextDestination = R.id.nav_profile,
            tapHintRes = R.string.tutorial_tap_profile_tab
        ),
        // 12. Profile, Settings & Finish
        Step(
            titleRes = R.string.tutorial_step_profile_finish_title,
            bodyRes = R.string.tutorial_step_profile_finish_body,
            mascotRes = R.drawable.mascot_confirmed,
            screenDestination = R.id.nav_profile,
            targetViewId = R.id.btn_profile_settings,
            circularSpotlight = true,
            nextLabelRes = R.string.tutorial_finish_cta
        )
    )

    init {
        overlay.onNextClick = { advance() }
        overlay.onBackClick = { goBack() }
        overlay.onSkipClick = { finish(returnToHome = false) }
    }

    fun startIfFirstTime(userId: String) {
        if (BuildConfig.APPLICATION_ID.endsWith(".testing")) return
        if (isCompleted(activity, userId)) return
        if (isActive) return
        handler.postDelayed({
            if (!activity.isFinishing && !activity.isDestroyed && !isActive) {
                start(fromBeginning = true)
            }
        }, 420)
    }

    fun start(fromBeginning: Boolean = true) {
        if (fromBeginning) {
            currentIndex = 0
        } else if (currentIndex < 0) {
            currentIndex = 0
        }
        onActiveStateChanged(true)
        renderCurrentStep(delayMs = 0)
    }

    fun onDestinationChanged(destinationId: Int) {
        if (!isActive) return
        val step = steps.getOrNull(currentIndex) ?: return
        if (step.requireTargetTap && step.expectedNextDestination == destinationId) {
            currentIndex++
            if (currentIndex >= steps.size) {
                finish(returnToHome = true)
            } else {
                // Wait for the 210ms horizontal tab swap animation to settle before measuring
                renderCurrentStep(delayMs = 260)
            }
        }
    }

    fun handleBackPressed(): Boolean {
        if (!isActive) return false
        finish(returnToHome = false)
        return true
    }

    fun destroy() {
        pendingRender?.let(handler::removeCallbacks)
        pendingRender = null
    }

    private fun advance() {
        if (!isActive) return
        if (currentIndex + 1 >= steps.size) {
            finish(returnToHome = true)
            return
        }
        currentIndex++
        renderCurrentStep(delayMs = 0)
    }

    private fun goBack() {
        if (!isActive || currentIndex <= 0) return
        currentIndex--
        // If stepping back onto an interactive tap step, ensure we're on its source screen
        renderCurrentStep(delayMs = 0)
    }

    private fun finish(returnToHome: Boolean) {
        pendingRender?.let(handler::removeCallbacks)
        pendingRender = null
        currentIndex = -1
        onActiveStateChanged(false)
        SessionManager(activity).getCurrentUser()?.id?.let { uid ->
            markCompleted(activity, uid)
        }
        overlay.hideOverlay {
            if (returnToHome && getSelectedDestination() != R.id.nav_home) {
                navigateToDestination(R.id.nav_home)
            }
        }
    }

    private fun renderCurrentStep(delayMs: Long) {
        pendingRender?.let(handler::removeCallbacks)
        val step = steps.getOrNull(currentIndex) ?: return

        if (getSelectedDestination() != step.screenDestination) {
            navigateToDestination(step.screenDestination)
            scheduleRender(step, delayMs = 260)
            return
        }

        // Scroll Home feed to top so header icons & balance card are visible in viewport
        if (step.screenDestination == R.id.nav_home && !step.targetIsNavItem && step.targetViewId != null) {
            activity.findViewById<RecyclerView?>(R.id.challenge_list)?.scrollToPosition(0)
        }

        scheduleRender(step, delayMs = delayMs)
    }

    private fun scheduleRender(step: Step, delayMs: Long) {
        val task = Runnable {
            if (activity.isFinishing || activity.isDestroyed || !isActive) return@Runnable
            val targetView = resolveTargetView(step)
            if (step.targetViewId != null && (targetView == null || targetView.width == 0)) {
                // Retry once after layout pass in case RecyclerView header just bound
                handler.postDelayed({
                    if (!activity.isFinishing && !activity.isDestroyed && isActive) {
                        presentStep(step, resolveTargetView(step))
                    }
                }, 140)
            } else {
                presentStep(step, targetView)
            }
        }
        pendingRender = task
        if (delayMs > 0) {
            handler.postDelayed(task, delayMs)
        } else {
            overlay.post(task)
        }
    }

    private fun resolveTargetView(step: Step): View? {
        val id = step.targetViewId ?: return null
        return if (step.targetIsNavItem) {
            findNavItemView(id)
        } else {
            activity.findViewById(id)
        }
    }

    private fun presentStep(step: Step, targetView: View?) {
        val canGoBack = currentIndex > 0 && !steps[currentIndex - 1].requireTargetTap
        overlay.showStep(
            stepIndex = currentIndex,
            totalSteps = steps.size,
            title = activity.getString(step.titleRes),
            body = activity.getString(step.bodyRes),
            mascotRes = step.mascotRes,
            nextLabel = activity.getString(step.nextLabelRes),
            showBack = canGoBack,
            requireTap = step.requireTargetTap,
            tapHint = step.tapHintRes?.let(activity::getString),
            targetView = targetView,
            circularSpotlight = step.circularSpotlight
        )
    }

    companion object {
        private const val PREFS_NAME = "bounty_tutorial"
        private const val KEY_PREFIX = "completed_"

        fun isCompleted(context: Context, userId: String): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean("$KEY_PREFIX$userId", false)
        }

        fun markCompleted(context: Context, userId: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("$KEY_PREFIX$userId", true)
                .apply()
        }
    }
}
