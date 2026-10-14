package org.matchat.core.ui.theme

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.EditText

/**
 * Hides the phone's white soft-key label bar at the bottom of the screen — on
 * keypad phones that bar is the system *navigation bar*. MatChat draws its own
 * labelled soft-key bar ([org.matchat.core.ui.softkey.SoftkeyBarView]), so the
 * system one only wastes ~26dp of a 320px screen with labels that may not
 * match the keys. Hidden on every screen, with no setting to show it again.
 * Adapted from the Flip-DumbPhoneGuide's `templates/SystemBars.kt`
 * (docs/layout-and-type.md, "System soft-key bar"), confirmed on FlipWeather.
 *
 * **Except while a text field has focus** (user's call): the system T9
 * keyboard uses that bar for its word and mode labels, so typing needs it.
 * [hideNavigation] does nothing then, and its re-hide listener leaves the bar
 * alone; MainActivity calls [showNavigation] when focus moves into a text
 * field and [hideNavigation] when it moves out.
 *
 * Call [hideNavigation] from `onResume()` and `onWindowFocusChanged(true)`.
 *
 * Two layers, always both:
 *  - Android 11+: the insets controller.
 *  - Every API level: the old sticky-immersive flags, on the decor view AND in
 *    the window attributes — vendor keypad ROMs often honor only these, and the
 *    window attributes survive system-initiated clears better. A listener puts
 *    them back if the system clears them (after a dialog, say).
 * The window is not extended under the bar, so content just grows into the
 * freed space; if a ROM refuses to hide it, our bar sits above it, not behind.
 */
object SystemBars {

    @Suppress("DEPRECATION")
    private const val LEGACY_FLAGS =
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

    /** True while a text field has focus — the keyboard needs the bar then. */
    private fun typing(activity: Activity) = activity.currentFocus is EditText

    fun hideNavigation(activity: Activity) {
        val window = activity.window ?: return
        val decor = window.decorView
        val hide = hide@{
            if (typing(activity)) return@hide
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.let {
                    it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    it.hide(WindowInsets.Type.navigationBars())
                }
            }
            @Suppress("DEPRECATION")
            run {
                decor.systemUiVisibility = decor.systemUiVisibility or LEGACY_FLAGS
                val lp = window.attributes
                if (lp.systemUiVisibility and LEGACY_FLAGS != LEGACY_FLAGS) {
                    lp.systemUiVisibility = lp.systemUiVisibility or LEGACY_FLAGS
                    window.attributes = lp
                }
                decor.setOnSystemUiVisibilityChangeListener { visibility ->
                    val shown = visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0
                    if (shown && !typing(activity)) {
                        decor.systemUiVisibility = decor.systemUiVisibility or LEGACY_FLAGS
                    }
                }
            }
        }
        hide()
        // Early in a launch the decor isn't attached to the window yet; apply again once it is.
        decor.post { hide() }
    }

    /** Brings the bar back, for a text field's keyboard labels. */
    fun showNavigation(activity: Activity) {
        val window = activity.window ?: return
        val decor = window.decorView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.navigationBars())
        }
        @Suppress("DEPRECATION")
        run {
            decor.systemUiVisibility = decor.systemUiVisibility and LEGACY_FLAGS.inv()
            val lp = window.attributes
            if (lp.systemUiVisibility and LEGACY_FLAGS != 0) {
                lp.systemUiVisibility = lp.systemUiVisibility and LEGACY_FLAGS.inv()
                window.attributes = lp
            }
        }
    }
}
