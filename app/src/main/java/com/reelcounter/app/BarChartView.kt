package com.reelcounter.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.TypedValue
import android.view.View

/** Pichhle 7 din ka seedha saada bar chart. Aakhri bar = aaj. */
class BarChartView(context: Context, private val data: List<DayCount>) : View(context) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB9A8FF.toInt() }
    private val todayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6C47FF.toInt() }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF555555.toInt()
        textAlign = Paint.Align.CENTER
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 11f, resources.displayMetrics
        )
    }
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val ts = textPaint.textSize
        val labelH = ts * 1.8f
        val topPad = ts * 1.6f
        val chartH = h - labelH - topPad
        val baseline = h - labelH
        val max = (data.maxOf { it.count }).coerceAtLeast(1)
        val slot = w / data.size
        val barW = slot * 0.55f

        data.forEachIndexed { i, d ->
            val cx = slot * i + slot / 2f
            val bh = chartH * d.count.toFloat() / max.toFloat()
            val top = baseline - bh
            rect.set(cx - barW / 2f, top, cx + barW / 2f, baseline)
            canvas.drawRoundRect(rect, 8f, 8f, if (i == data.lastIndex) todayPaint else barPaint)
            canvas.drawText(d.count.toString(), cx, top - ts * 0.4f, textPaint)
            canvas.drawText(d.label, cx, baseline + ts * 1.3f, textPaint)
        }
    }
}
