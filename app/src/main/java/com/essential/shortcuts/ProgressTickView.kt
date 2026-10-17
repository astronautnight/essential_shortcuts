package com.essential.shortcuts

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateInterpolator

class ProgressTickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val INITIAL_SWEEP = 120f
    private val START_ANGLE = -90f

    private var arcSweep = 0f
        set(v) { field = v; invalidate() }

    private var overlayAlpha = 0f
        set(v) { field = v; invalidate() }

    private var tickProgress = 0f
        set(v) { field = v; invalidate() }

    private val isNightMode: Boolean
        get() = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private val step2Receiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (visibility == VISIBLE && overlayAlpha > 0f) {
                animateSecondStep()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val filter2 = IntentFilter("com.essential.shortcuts.ACTION_STEP_2")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(step2Receiver, filter2, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(step2Receiver, filter2)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try {
            context.unregisterReceiver(step2Receiver)
        } catch (e: Exception) {}
    }

    private var isCustomColor = false

    fun setProgress(progress: Float, colorInt: Int? = null) {
        if (colorInt != null) {
            isCustomColor = true
            arcPaint.color = colorInt
            tickPaint.color = colorInt
        }
        arcSweep = 360f * progress.coerceIn(0f, 1f)
        overlayAlpha = 1f 
        visibility = VISIBLE
    }

    private fun applyThemeColors() {
        if (isNightMode) {
            // Dark mode (from FIRST code)
            circleBgPaint.color = Color.parseColor("#55000000")
            arcBgPaint.color    = Color.parseColor("#44FFFFFF")
            if (!isCustomColor) {
                arcPaint.color      = Color.WHITE
                tickPaint.color     = Color.WHITE
            }
        } else {
            // Light mode (from SECOND code)
            circleBgPaint.color = Color.TRANSPARENT
            arcBgPaint.color    = Color.parseColor("#BBFFFFFF")
            if (!isCustomColor) {
                arcPaint.color      = Color.WHITE
                tickPaint.color     = Color.WHITE
            }
        }
    }

    private val circleBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0f
        strokeCap = Paint.Cap.ROUND
    }

    private val arcBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0f
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val arcRect = RectF()
    private val fullTickPath = Path()
    private val drawnTickPath = Path()
    private val pm = android.graphics.PathMeasure()

    private var currentAnimator: ValueAnimator? = null

    private var cx = 0f
    private var cy = 0f
    private var radius = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cx = w / 2f
        cy = h / 2f
        radius = minOf(w, h) / 2f * 0.82f

        val strokeW = radius * 0.22f
        arcPaint.strokeWidth = strokeW
        arcBgPaint.strokeWidth = strokeW
        tickPaint.strokeWidth = radius * 0.18f

        val inset = strokeW / 2f
        arcRect.set(cx - radius + inset, cy - radius + inset,
            cx + radius - inset, cy + radius - inset)

        buildTickPath()
    }

    private fun buildTickPath() {
        val r = radius * 0.45f
        val x0 = cx - r * 0.65f; val y0 = cy
        val x1 = cx - r * 0.08f; val y1 = cy + r * 0.52f
        val x2 = cx + r * 0.65f; val y2 = cy - r * 0.52f

        fullTickPath.reset()
        fullTickPath.moveTo(x0, y0)
        fullTickPath.lineTo(x1, y1)
        fullTickPath.lineTo(x2, y2)
        pm.setPath(fullTickPath, false)
    }

    override fun onDraw(canvas: Canvas) {
        if (overlayAlpha <= 0f) return

        applyThemeColors()
        val alpha = (overlayAlpha.coerceIn(0f, 1f) * 255).toInt()

        // 🔥 COMBINED LOGIC

        // Draw background ONLY in dark mode (from SECOND code)
        if (isNightMode) {
            circleBgPaint.alpha = alpha
            canvas.drawCircle(cx, cy, radius + arcPaint.strokeWidth / 2f, circleBgPaint)
        }

        // Arc track (adjusted per mode)
        arcBgPaint.alpha = if (isNightMode)
            (alpha * 0.5f).toInt()
        else
            (alpha * 0.8f).toInt()

        canvas.drawArc(arcRect, START_ANGLE, 360f, false, arcBgPaint)

        // Active arc
        if (arcSweep > 0f) {
            arcPaint.alpha = alpha
            canvas.drawArc(arcRect, START_ANGLE, arcSweep, false, arcPaint)
        }

        // Tick
        if (tickProgress > 0f) {
            val length = pm.length * tickProgress
            drawnTickPath.reset()
            pm.getSegment(0f, length, drawnTickPath, true)
            tickPaint.alpha = alpha
            canvas.drawPath(drawnTickPath, tickPaint)
        }
    }

    fun animateStart(onReady: () -> Unit = {}) {
        cancelCurrent()
        visibility = VISIBLE

        val fadeIn = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                overlayAlpha = it.animatedValue as Float
                tickProgress = it.animatedValue as Float
            }
        }

        val arcUp = ValueAnimator.ofFloat(0f, INITIAL_SWEEP).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
            addUpdateListener { arcSweep = it.animatedValue as Float }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { onReady() }
            })
        }

        android.animation.AnimatorSet().also {
            it.play(arcUp).after(fadeIn)
            it.start()
        }
    }

    fun animateSecondStep(onReady: () -> Unit = {}) {
        cancelCurrent()
        currentAnimator = ValueAnimator.ofFloat(arcSweep, 240f).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
            addUpdateListener { arcSweep = it.animatedValue as Float }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { onReady() }
            })
            start()
        }
    }

    fun animateComplete(onDone: () -> Unit = {}) {
        cancelCurrent()
        currentAnimator = ValueAnimator.ofFloat(arcSweep, 360f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
            addUpdateListener { arcSweep = it.animatedValue as Float }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    fadeOut(onDone)
                }
            })
            start()
        }
    }

    fun animateRevert(onDone: () -> Unit = {}) {
        cancelCurrent()
        currentAnimator = ValueAnimator.ofFloat(arcSweep, 0f).apply {
            duration = 500
            interpolator = AccelerateInterpolator()
            addUpdateListener { arcSweep = it.animatedValue as Float }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    fadeOut(onDone)
                }
            })
            start()
        }
    }

    private fun fadeOut(onDone: () -> Unit) {
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 300
            interpolator = AccelerateInterpolator()
            addUpdateListener {
                overlayAlpha = it.animatedValue as Float
                tickProgress = it.animatedValue as Float
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    arcSweep = 0f
                    visibility = GONE
                    onDone()
                }
            })
            start()
        }
    }

    private fun cancelCurrent() {
        currentAnimator?.cancel()
        currentAnimator = null
    }

    fun reset() {
        cancelCurrent()
        arcSweep = 0f
        overlayAlpha = 0f
        tickProgress = 0f
        visibility = GONE
    }
}
