package com.essential.shortcuts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.min

class CompassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var targetAzimuth = 0f
    private var currentAzimuth = 0f

    private var pitch = 0f // -90 to +90 degrees
    private var roll = 0f  // -180 to +180 degrees

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#48484A")
    }

    private val innerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#3A3A3C")
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#8E8E93")
    }

    private val tickMajorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.parseColor("#E5E5EA")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E5E5EA")
        textSize = 36f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val northTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF3B30") // Apple-style vibrant red for North
        textSize = 42f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val degreeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8E8E93")
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    private val northNeedlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FF3B30")
    }

    private val southNeedlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#E5E5EA")
    }

    private val levelDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#34C759") // Green bubble level dot
    }

    private val levelRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#636366")
    }

    private val needlePathNorth = Path()
    private val needlePathSouth = Path()

    fun setAzimuth(azimuth: Float) {
        // Normalize angle difference to avoid 360-degree spin when crossing North (0 <-> 359)
        var diff = (azimuth - currentAzimuth) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f

        targetAzimuth = currentAzimuth + diff

        // Exponential smoothing filter for silky smooth rotation
        currentAzimuth += (targetAzimuth - currentAzimuth) * 0.25f
        currentAzimuth = (currentAzimuth + 360f) % 360f

        invalidate()
    }

    fun setPitchAndRoll(pitchDeg: Float, rollDeg: Float) {
        this.pitch = pitchDeg.coerceIn(-45f, 45f)
        this.roll = rollDeg.coerceIn(-45f, 45f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        if (width <= 0 || height <= 0) return

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(width, height) / 2f - 24f

        // Draw Outer Rings
        canvas.drawCircle(centerX, centerY, radius, circlePaint)
        canvas.drawCircle(centerX, centerY, radius * 0.82f, innerCirclePaint)

        // Save canvas state before rotating dial
        canvas.save()
        // Rotate dial opposite to phone heading so North stays pointing towards true North
        canvas.rotate(-currentAzimuth, centerX, centerY)

        // Draw Dial Tick Marks & Labels
        val outerTickRadius = radius - 4f
        val majorTickRadius = radius - 24f
        val minorTickRadius = radius - 14f
        val labelRadius = radius - 48f
        val degreeRadius = radius - 78f

        for (i in 0 until 360 step 5) {
            val angleRad = Math.toRadians(i.toDouble())
            val cosA = Math.cos(angleRad).toFloat()
            val sinA = Math.sin(angleRad).toFloat()

            val isMajor = i % 30 == 0
            val isCardinal = i % 90 == 0
            val innerR = if (isCardinal) majorTickRadius - 6f else if (isMajor) majorTickRadius else minorTickRadius

            val startX = centerX + outerTickRadius * sinA
            val startY = centerY - outerTickRadius * cosA
            val endX = centerX + innerR * sinA
            val endY = centerY - innerR * cosA

            val p = if (isCardinal || isMajor) tickMajorPaint else tickPaint
            canvas.drawLine(startX, startY, endX, endY, p)

            if (isCardinal) {
                val labelX = centerX + labelRadius * sinA
                val labelY = centerY - labelRadius * cosA + (textPaint.textSize / 3f)

                val label = when (i) {
                    0 -> "N"
                    90 -> "E"
                    180 -> "S"
                    270 -> "W"
                    else -> ""
                }

                if (i == 0) {
                    canvas.drawText(label, labelX, labelY, northTextPaint)
                } else {
                    canvas.drawText(label, labelX, labelY, textPaint)
                }
            } else if (isMajor) {
                val degreeX = centerX + degreeRadius * sinA
                val degreeY = centerY - degreeRadius * cosA + (degreeTextPaint.textSize / 3f)
                val dirLabel = when (i) {
                    45 -> "NE"
                    135 -> "SE"
                    225 -> "SW"
                    315 -> "NW"
                    else -> "$i°"
                }
                canvas.drawText(dirLabel, degreeX, degreeY, degreeTextPaint)
            }
        }

        // Restore canvas from dial rotation
        canvas.restore()

        // Draw Fixed Top Direction Pointer Arrow
        val needleLength = radius * 0.42f
        val needleWidth = 22f

        needlePathNorth.reset()
        needlePathNorth.moveTo(centerX, centerY - needleLength)
        needlePathNorth.lineTo(centerX - needleWidth / 2f, centerY)
        needlePathNorth.lineTo(centerX + needleWidth / 2f, centerY)
        needlePathNorth.close()

        needlePathSouth.reset()
        needlePathSouth.moveTo(centerX, centerY + needleLength)
        needlePathSouth.lineTo(centerX - needleWidth / 2f, centerY)
        needlePathSouth.lineTo(centerX + needleWidth / 2f, centerY)
        needlePathSouth.close()

        canvas.drawPath(needlePathNorth, northNeedlePaint)
        canvas.drawPath(needlePathSouth, southNeedlePaint)

        // Draw Center Pivot Ring
        canvas.drawCircle(centerX, centerY, 14f, circlePaint)
        canvas.drawCircle(centerX, centerY, 8f, southNeedlePaint)

        // Draw Integrated Bubble Level / Crosshair Indicator at Center
        val levelRingRadius = 40f
        canvas.drawCircle(centerX, centerY, levelRingRadius, levelRingPaint)

        val maxOffset = levelRingRadius - 8f
        val dotOffsetX = (roll / 45f).coerceIn(-1f, 1f) * maxOffset
        val dotOffsetY = (pitch / 45f).coerceIn(-1f, 1f) * maxOffset

        val isLevel = abs(pitch) < 3f && abs(roll) < 3f
        levelDotPaint.color = if (isLevel) Color.parseColor("#34C759") else Color.parseColor("#FFCC00")
        canvas.drawCircle(centerX + dotOffsetX, centerY + dotOffsetY, 8f, levelDotPaint)
    }
}
