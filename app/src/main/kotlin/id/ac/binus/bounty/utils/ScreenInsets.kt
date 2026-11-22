package id.ac.binus.bounty.utils

import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import id.ac.binus.bounty.R

object ScreenInsets {
    @JvmStatic
    fun apply(activity: AppCompatActivity) {
        activity.enableEdgeToEdge()
        val root = activity.findViewById<View>(R.id.main)
        val left = root.paddingLeft
        val top = root.paddingTop
        val right = root.paddingRight
        val bottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val safe = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime()
            )
            view.setPadding(left + safe.left, top + safe.top, right + safe.right, bottom + safe.bottom)
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(root)
    }

    /**
     * Edge-to-edge layout for MainActivity: content fills 100% to the bottom of the screen
     * with no colored gap beneath the floating navbar.
     */
    @JvmStatic
    fun applyMain(activity: AppCompatActivity, navCard: View, screenContent: View) {
        activity.enableEdgeToEdge()
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).isAppearanceLightNavigationBars = true
        val root = activity.findViewById<View>(R.id.main)
        val left = root.paddingLeft
        val top = root.paddingTop
        val right = root.paddingRight
        val navLp = navCard.layoutParams as? ViewGroup.MarginLayoutParams
        val baseNavBottom = navLp?.bottomMargin ?: 0
        val contentBaseBottom = screenContent.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(left + bars.left, top + bars.top, right + bars.right, 0)
            navLp?.let {
                it.bottomMargin = baseNavBottom + bars.bottom
                navCard.layoutParams = it
            }
            screenContent.setPadding(
                screenContent.paddingLeft,
                screenContent.paddingTop,
                screenContent.paddingRight,
                contentBaseBottom + ime.bottom
            )
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(root)
    }

    /**
     * Edge-to-edge for screens with a colored top hero and a light bottom sheet:
     * applies top/side insets to #main and bottom/IME insets to [bottomTarget] so no
     * colored strip appears below the sheet.
     */
    @JvmStatic
    fun applyHeroEdgeToEdge(activity: AppCompatActivity, bottomTarget: View) {
        activity.enableEdgeToEdge()
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).isAppearanceLightNavigationBars = true
        val root = activity.findViewById<View>(R.id.main)
        val left = root.paddingLeft
        val top = root.paddingTop
        val right = root.paddingRight
        val targetBottom = bottomTarget.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val safe = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime()
            )
            view.setPadding(left + safe.left, top + safe.top, right + safe.right, 0)
            bottomTarget.setPadding(
                bottomTarget.paddingLeft,
                bottomTarget.paddingTop,
                bottomTarget.paddingRight,
                targetBottom + safe.bottom
            )
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(root)
    }
}
