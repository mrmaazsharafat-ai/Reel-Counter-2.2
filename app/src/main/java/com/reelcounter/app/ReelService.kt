package com.reelcounter.app

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import java.util.Locale

/**
 * Accessibility Service: jab Facebook / TikTok / Instagram / YouTube mein reel swipe hoti hai
 * to ginti +1 karti hai, aur screen ke top par chota counter dikhati hai.
 */
class ReelService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var overlay: TextView? = null

    private val lastCounted = HashMap<ReelApp, Long>()
    private val lastActive = HashMap<ReelApp, Long>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        val app = ReelApp.fromPackage(pkg)

        // Kaun si app samne hai: us hisaab se counter dikhao ya chhupao.
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            when {
                app != null -> showOverlay()
                isTransientPackage(pkg) -> Unit
                else -> hideOverlay()
            }
        }

        if (app == null || event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return
        if (!inReelScreen(app)) return

        val now = SystemClock.elapsedRealtime()
        val lastActiveAt = lastActive[app] ?: 0L
        val lastCountAt = lastCounted[app] ?: 0L
        lastActive[app] = now

        // Ek swipe par kai scroll events aate hain, isliye thora rukna zaroori hai.
        if (now - lastCountAt < DEBOUNCE_MS) return

        // Naya session: pehli reel jo pehle se chal rahi thi wo bhi gin lo.
        val newSession = now - lastActiveAt > SESSION_GAP_MS
        ReelStore.add(this, app, if (newSession) 2 else 1)
        lastCounted[app] = now

        showOverlay()
        updateOverlay()
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        hideOverlay()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        hideOverlay()
        super.onDestroy()
    }

    // ---------- Reel screen pehchanna ----------

    private fun inReelScreen(app: ReelApp): Boolean {
        val ids = app.reelIds ?: return true
        val root = rootInActiveWindow ?: return false
        return try {
            hasIdContaining(root, ids, intArrayOf(NODE_BUDGET))
        } finally {
            recycleSafe(root)
        }
    }

    private fun hasIdContaining(node: AccessibilityNodeInfo, words: List<String>, budget: IntArray): Boolean {
        if (budget[0] <= 0) return false
        budget[0]--
        val id = node.viewIdResourceName?.lowercase(Locale.ROOT)
        if (id != null && words.any { id.contains(it) }) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = hasIdContaining(child, words, budget)
            recycleSafe(child)
            if (found) return true
        }
        return false
    }

    @Suppress("DEPRECATION")
    private fun recycleSafe(node: AccessibilityNodeInfo) {
        try {
            node.recycle()
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun isTransientPackage(pkg: String): Boolean {
        val p = pkg.lowercase(Locale.ROOT)
        return p == "android" ||
            p == "com.android.systemui" ||
            p.contains("inputmethod") ||
            p.contains("keyboard")
    }

    // ---------- Screen ke top ka chota counter ----------

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun showOverlay() {
        if (overlay != null) {
            updateOverlay()
            return
        }
        val wm = windowManager ?: return
        val tv = TextView(this)
        tv.textSize = 13f
        tv.setTextColor(Color.WHITE)
        tv.typeface = Typeface.DEFAULT_BOLD
        tv.setPadding(dp(12), dp(5), dp(12), dp(5))
        val bg = GradientDrawable()
        bg.setColor(0xCC000000.toInt())
        bg.cornerRadius = dp(16).toFloat()
        tv.background = bg

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = dp(30)

        try {
            wm.addView(tv, params)
            overlay = tv
            updateOverlay()
        } catch (e: Exception) {
            overlay = null
        }
    }

    private fun updateOverlay() {
        overlay?.text = "🎬 Aaj: ${ReelStore.todayTotal(this)}"
    }

    private fun hideOverlay() {
        val v = overlay ?: return
        try {
            windowManager?.removeView(v)
        } catch (e: Exception) {
            // ignore
        }
        overlay = null
    }

    companion object {
        private const val DEBOUNCE_MS = 1000L
        private const val SESSION_GAP_MS = 60_000L
        private const val NODE_BUDGET = 600
    }
}
