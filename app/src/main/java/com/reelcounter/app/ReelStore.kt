package com.reelcounter.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DayCount(val date: String, val label: String, val count: Int)

data class ReelStats(
    val today: Int,
    val yesterday: Int,
    val last7: Int,
    val last30: Int,
    val total: Int,
    val bestDayLabel: String?,
    val bestDayCount: Int,
    val bestHour: Int?,
    val bestHourCount: Int,
    val todayByApp: Map<ReelApp, Int>,
    val weekByApp: Map<ReelApp, Int>,
    /** Purana din pehle, aaj sab se aakhir mein. */
    val last7Days: List<DayCount>
)

/**
 * Sab ginti SharedPreferences mein rehti hai:
 *   "d|2026-10-26|tiktok" -> us din us app ki reels
 *   "h|22"                -> raat 10 se 11 baje tak ki kul reels (ghante ke hisaab se)
 */
object ReelStore {
    private const val PREFS = "reel_counts"

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun dayKey(cal: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

    fun add(ctx: Context, app: ReelApp, n: Int = 1) {
        val p = prefs(ctx)
        val cal = Calendar.getInstance()
        val dk = "d|${dayKey(cal)}|${app.key}"
        val hk = "h|${cal.get(Calendar.HOUR_OF_DAY)}"
        p.edit()
            .putInt(dk, p.getInt(dk, 0) + n)
            .putInt(hk, p.getInt(hk, 0) + n)
            .apply()
    }

    fun todayTotal(ctx: Context): Int {
        val prefix = "d|${dayKey(Calendar.getInstance())}|"
        var sum = 0
        for ((k, v) in prefs(ctx).all) {
            if (k.startsWith(prefix)) sum += (v as? Int) ?: 0
        }
        return sum
    }

    fun reset(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }

    private data class DayInfo(val key: String, val label: String, val weekday: String)

    fun stats(ctx: Context): ReelStats {
        val dateLabel = SimpleDateFormat("d MMM", Locale.ENGLISH)
        val weekdayLabel = SimpleDateFormat("EEE", Locale.ENGLISH)

        // Aaj se peeche 30 din (index 0 = aaj)
        val days = ArrayList<DayInfo>()
        for (i in 0 until 30) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            days.add(DayInfo(dayKey(c), dateLabel.format(c.time), weekdayLabel.format(c.time)))
        }

        val perDay = HashMap<String, Int>()
        val perDayApp = HashMap<String, HashMap<ReelApp, Int>>()
        val perHour = HashMap<Int, Int>()
        var total = 0

        for ((k, v) in prefs(ctx).all) {
            val value = (v as? Int) ?: continue
            val parts = k.split("|")
            when (parts.firstOrNull()) {
                "d" -> {
                    if (parts.size < 3) continue
                    val date = parts[1]
                    val app = ReelApp.fromKey(parts[2]) ?: continue
                    perDay[date] = (perDay[date] ?: 0) + value
                    val m = perDayApp.getOrPut(date) { HashMap() }
                    m[app] = (m[app] ?: 0) + value
                    total += value
                }
                "h" -> {
                    val h = parts.getOrNull(1)?.toIntOrNull() ?: continue
                    perHour[h] = value
                }
            }
        }

        fun dayTotal(i: Int) = perDay[days[i].key] ?: 0

        var last7 = 0
        for (i in 0 until 7) last7 += dayTotal(i)
        var last30 = 0
        for (i in 0 until 30) last30 += dayTotal(i)

        val todayByApp = HashMap<ReelApp, Int>(perDayApp[days[0].key] ?: HashMap())
        val weekByApp = HashMap<ReelApp, Int>()
        for (i in 0 until 7) {
            perDayApp[days[i].key]?.forEach { (app, n) ->
                weekByApp[app] = (weekByApp[app] ?: 0) + n
            }
        }

        val bestDay = perDay.entries.maxByOrNull { it.value }
        val bestDayLabel = bestDay?.let {
            try {
                val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it.key)
                if (d != null) SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(d) else it.key
            } catch (e: Exception) {
                it.key
            }
        }

        val bestHour = perHour.entries.maxByOrNull { it.value }

        val last7Days = ArrayList<DayCount>()
        for (i in 6 downTo 0) {
            last7Days.add(DayCount(days[i].key, days[i].weekday, dayTotal(i)))
        }

        return ReelStats(
            today = dayTotal(0),
            yesterday = dayTotal(1),
            last7 = last7,
            last30 = last30,
            total = total,
            bestDayLabel = bestDayLabel,
            bestDayCount = bestDay?.value ?: 0,
            bestHour = bestHour?.key,
            bestHourCount = bestHour?.value ?: 0,
            todayByApp = todayByApp,
            weekByApp = weekByApp,
            last7Days = last7Days
        )
    }
}
