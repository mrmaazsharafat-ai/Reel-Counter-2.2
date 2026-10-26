package com.reelcounter.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var content: LinearLayout
    private var lastSignature = ""

    private val ticker = object : Runnable {
        override fun run() {
            render()
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(BG)
        content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(16), dp(28), dp(16), dp(32))
        scroll.addView(content, ViewGroup.LayoutParams(MATCH, WRAP))
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        lastSignature = ""
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    // ---------- Screen banana ----------

    private fun render() {
        val enabled = isServiceEnabled()
        val s = ReelStore.stats(this)
        val signature = "$enabled|${s.total}|${s.today}"
        if (signature == lastSignature) return
        lastSignature = signature

        content.removeAllViews()

        content.addView(text("🎬 Reel Counter", 26f, TEXT, true))
        content.addView(
            text("Facebook, TikTok, Instagram aur YouTube ki reels yahan gin li jati hain.", 13f, MUTED),
            lp(4)
        )

        // --- Status ---
        val status = card()
        if (enabled) {
            status.addView(text("✅ Counter chal raha hai", 16f, GREEN, true))
            status.addView(
                text(
                    "Koi bhi reel app kholein, screen ke top par chota counter nazar aaye ga.",
                    13f, MUTED
                ),
                lp(6)
            )
        } else {
            status.addView(text("⚠️ Counter abhi band hai", 16f, ORANGE, true))
            status.addView(
                text(
                    "1) Neeche \"Accessibility on karein\" dabayen, \"Reel Counter\" chunein aur ON karein.\n\n" +
                        "2) Agar \"Restricted setting\" ka message aaye (Android 13 ya naya): " +
                        "\"App info kholein\" dabayen, upar right ke teen nuqton (⋮) mein " +
                        "\"Allow restricted settings\" chunein, phir step 1 dobara karein.",
                    13f, TEXT
                ),
                lp(8)
            )
            status.addView(button("Accessibility on karein") {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }, lp(10))
            status.addView(button("App info kholein") {
                val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                i.data = Uri.parse("package:$packageName")
                startActivity(i)
            }, lp(4))
        }
        content.addView(status, lp(16))

        // --- Aaj ---
        val hero = card(ACCENT)
        hero.addView(text("Aaj ki reels", 14f, 0xCCFFFFFF.toInt()))
        hero.addView(text("${s.today}", 56f, Color.WHITE, true))
        hero.addView(text("Kal: ${s.yesterday}", 13f, 0xCCFFFFFF.toInt()))
        content.addView(hero, lp(12))

        // --- Hisaab ---
        val totals = card()
        totals.addView(text("Hisaab", 16f, TEXT, true))
        totals.addView(row("Pichhle 7 din", "${s.last7}"), lp(6))
        totals.addView(row("Pichhle 30 din", "${s.last30}"))
        totals.addView(row("Kul (shuru se ab tak)", "${s.total}"))
        content.addView(totals, lp(12))

        // --- Chart ---
        val chart = card()
        chart.addView(text("Pichhle 7 din", 16f, TEXT, true))
        chart.addView(BarChartView(this, s.last7Days), LinearLayout.LayoutParams(MATCH, dp(170)).apply {
            topMargin = dp(8)
        })
        content.addView(chart, lp(12))

        // --- Records ---
        val records = card()
        records.addView(text("Sab se zyada", 16f, TEXT, true))
        val bestDay = if (s.bestDayLabel != null) "${s.bestDayLabel} — ${s.bestDayCount}" else "abhi data nahi"
        val bestHour = if (s.bestHour != null) hourRange(s.bestHour) else "abhi data nahi"
        records.addView(row("Sab se zyada din", bestDay), lp(6))
        records.addView(row("Sab se zyada waqt", bestHour))
        records.addView(row("Roz ka average (7 din)", String.format(Locale.US, "%.1f", s.last7 / 7.0)))
        content.addView(records, lp(12))

        // --- App ke hisaab se ---
        val perApp = card()
        perApp.addView(text("App ke hisaab se", 16f, TEXT, true))
        perApp.addView(text("aaj  ·  pichhle 7 din", 12f, MUTED), lp(2))
        for (app in ReelApp.values()) {
            val t = s.todayByApp[app] ?: 0
            val w = s.weekByApp[app] ?: 0
            perApp.addView(row(app.label, "$t  ·  $w"))
        }
        content.addView(perApp, lp(12))

        // --- Reset ---
        content.addView(button("Saara data saaf karein") {
            AlertDialog.Builder(this)
                .setTitle("Data saaf karein?")
                .setMessage("Ab tak ki saari ginti hamesha ke liye mit jaye gi.")
                .setPositiveButton("Haan, saaf karein") { _, _ ->
                    ReelStore.reset(this)
                    lastSignature = ""
                    render()
                }
                .setNegativeButton("Nahi", null)
                .show()
        }, lp(16))

        content.addView(
            text(
                "Ginti andaazan hoti hai: har swipe ko ek reel maana jata hai. Sara data sirf aap ke phone mein rehta hai, internet par kuch nahi jata.",
                11f, MUTED
            ),
            lp(12)
        )
    }

    // ---------- Chhote helpers ----------

    private fun isServiceEnabled(): Boolean {
        val setting = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return setting.split(':').any { it.contains(packageName) && it.contains("ReelService") }
    }

    private fun hourLabel(h: Int): String {
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "$h12 ${if (h < 12) "AM" else "PM"}"
    }

    private fun hourRange(h: Int): String = "${hourLabel(h)} – ${hourLabel((h + 1) % 24)}"

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun lp(top: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(top) }

    private fun card(color: Int = Color.WHITE): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(16), dp(14), dp(16), dp(14))
        val bg = GradientDrawable()
        bg.setColor(color)
        bg.cornerRadius = dp(16).toFloat()
        c.background = bg
        return c
    }

    private fun text(s: String, sizeSp: Float, color: Int = TEXT, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = sizeSp
        t.setTextColor(color)
        if (bold) t.setTypeface(t.typeface, android.graphics.Typeface.BOLD)
        return t
    }

    private fun row(label: String, value: String): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.setPadding(0, dp(6), 0, dp(6))
        r.addView(text(label, 15f, TEXT), LinearLayout.LayoutParams(0, WRAP, 1f))
        r.addView(text(value, 15f, TEXT, true), LinearLayout.LayoutParams(WRAP, WRAP))
        return r
    }

    private fun button(label: String, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = label
        b.isAllCaps = false
        b.setOnClickListener { onClick() }
        return b
    }

    companion object {
        private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
        private val BG = 0xFFF6F4FB.toInt()
        private val TEXT = 0xFF1B1B25.toInt()
        private val MUTED = 0xFF6B6B7B.toInt()
        private val ACCENT = 0xFF6C47FF.toInt()
        private val GREEN = 0xFF1E8E3E.toInt()
        private val ORANGE = 0xFFD9730D.toInt()
    }
}
