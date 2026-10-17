package com.essential.shortcuts

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class WheelCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var items = listOf("A", "B", "C")
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val colors = intArrayOf(
        Color.parseColor("#5C5C5C"),
        Color.parseColor("#787878"),
        Color.parseColor("#4A4A4A"),
        Color.parseColor("#6E6E6E"),
        Color.parseColor("#555555"),
        Color.parseColor("#8A8A8A"),
        Color.parseColor("#636363"),
        Color.parseColor("#717171")
    )

    fun setItems(newItems: List<String>) {
        items = newItems
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy) - 8f
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val segmentAngle = 360f / items.size

        for (i in items.indices) {
            paint.color = colors[i % colors.size]
            canvas.drawArc(rect, i * segmentAngle - 90, segmentAngle, true, paint)

            // Draw text
            canvas.save()
            canvas.rotate(i * segmentAngle + segmentAngle / 2, cx, cy)
            canvas.drawText(
                if (items[i].length > 8) items[i].substring(0, 8) + ".." else items[i],
                cx, cy - radius * 0.35f, textPaint
            )
            canvas.restore()
        }

        // Draw center circle
        paint.color = Color.parseColor("#3D3D3D")
        canvas.drawCircle(cx, cy, radius * 0.12f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(cx, cy, radius * 0.08f, paint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(size, size)
    }
}
