package com.essential.shortcuts

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.PathInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Premium fluid QR scanner animation.
 */
class ScannerOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // ── Paints ─────────────────────────────────────────────────────────
    private val dimPaint = Paint().apply {
        color = Color.parseColor("#B3000000") // 70% opacity black
        style = Paint.Style.FILL
    }

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val modulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    // ── Internals ──────────────────────────────────────────────────────
    private val cutoutPath = Path()
    private val linePathObj = Path()

    // ── Layout ─────────────────────────────────────────────────────────
    private var baseSide = 0f
    private var cx = 0f
    private var cy = 0f

    // ── State ──────────────────────────────────────────────────────────
    private var isScanning = true
    private var time = 0f
    private var transitionFract = 0f
    private var targetBoundingBox: RectF? = null
    
    // Success flash state
    private var successRippleAlpha = 0
    private var successRippleRadius = 0f

    // ── Data ───────────────────────────────────────────────────────────
    private class ModuleConfig(
        val gridX: Int, val gridY: Int,
        val pathGroup: Int, val indexInGroup: Int
    )

    private class PathData(val index: Int) {
        val f1x = Random.nextFloat() * 1.5f + 0.5f
        val f1y = Random.nextFloat() * 1.5f + 0.5f
        val f2x = Random.nextFloat() * 2.0f + 1.0f
        val f2y = Random.nextFloat() * 2.0f + 1.0f

        val px1 = Random.nextFloat() * Math.PI.toFloat() * 2f
        val py1 = Random.nextFloat() * Math.PI.toFloat() * 2f
        val px2 = Random.nextFloat() * Math.PI.toFloat() * 2f
        val py2 = Random.nextFloat() * Math.PI.toFloat() * 2f

        // ensure sum <= 1.0 so they never escape the 1/2 frame side bounds
        val a1x = Random.nextFloat() * 0.4f + 0.2f
        val a2x = 0.95f - a1x 
        val a1y = Random.nextFloat() * 0.4f + 0.2f
        val a2y = 0.95f - a1y
    }

    private val modules = mutableListOf<ModuleConfig>()
    private val paths = mutableListOf<PathData>()

    // ── Animators ──────────────────────────────────────────────────────
    private val timeAnim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 100000L
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { 
            time = it.currentPlayTime / 1000f 
            invalidate()
        }
    }

    private var transitionAnim: ValueAnimator? = null

    // ═════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ═════════════════════════════════════════════════════════════════

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        cx = w / 2f
        cy = h / 2f - h * 0.05f 
        baseSide = min(w, h) * 0.65f
        initGridIfNeeded()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        timeAnim.start()
    }

    override fun onDetachedFromWindow() {
        timeAnim.cancel()
        transitionAnim?.cancel()
        super.onDetachedFromWindow()
    }

    private fun initGridIfNeeded() {
        if (modules.isNotEmpty()) return

        val grid = Array(13) { BooleanArray(13) }
        
        fun addFinder(fcx: Int, fcy: Int) {
            for (x in 0..4) {
                for (y in 0..4) {
                    if (x == 0 || x == 4 || y == 0 || y == 4) grid[x + fcx][y + fcy] = true
                    if (x == 2 && y == 2) grid[x + fcx][y + fcy] = true
                }
            }
        }
        
        addFinder(0, 0)
        addFinder(8, 0)
        addFinder(0, 8)
        
        // 51 finder modules. Fill rest to 96
        var count = 51
        while (count < 96) {
            val rx = Random.nextInt(13)
            val ry = Random.nextInt(13)
            if (!grid[rx][ry]) {
                grid[rx][ry] = true
                count++
            }
        }
        
        val allCoords = mutableListOf<Pair<Int, Int>>()
        for (x in 0..12) for (y in 0..12) if (grid[x][y]) allCoords.add(x to y)
        allCoords.shuffle()
        
        // 96 modules -> 12 paths of 8 nodes
        for (p in 0..11) {
            paths.add(PathData(p))
            for (i in 0..7) {
                val idx = p * 8 + i
                modules.add(ModuleConfig(allCoords[idx].first, allCoords[idx].second, p, i))
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  Public API
    // ═════════════════════════════════════════════════════════════════

    fun startDecodingAnimation(targetRect: RectF? = null) {
        if (!isScanning) return
        isScanning = false
        targetBoundingBox = targetRect
        
        // Beautiful elegant lock-in curve
        transitionAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200
            interpolator = PathInterpolator(0.3f, 0.0f, 0.1f, 1.0f)
            addUpdateListener {
                transitionFract = it.animatedValue as Float
                invalidate()
            }
        }
        transitionAnim?.start()
    }

    fun updateTargetBoundingBox(targetRect: RectF?) {
        if (targetRect != null) {
            targetBoundingBox = targetRect
            invalidate()
        }
    }

    fun showSuccess() {
        // Dramatic confirming ripple
        val rippleAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val v = it.animatedValue as Float
                // Ripple expands outward from the frame slightly
                successRippleRadius = baseSide * (1f + 0.15f * v)
                successRippleAlpha = (255 * (1f - v)).toInt().coerceIn(0, 255)
                invalidate()
            }
        }
        rippleAnim.start()
    }

    fun reset() {
        isScanning = true
        transitionFract = 0f
        successRippleAlpha = 0
        targetBoundingBox = null
        transitionAnim?.cancel()
        
        modules.clear()
        paths.clear()
        initGridIfNeeded()
        invalidate()
    }
    
    // For backwards compatibility with ScannerActivity (which previously called onDetected)
    fun onDetected() {
        startDecodingAnimation(null)
    }

    // ═════════════════════════════════════════════════════════════════
    //  Math
    // ═════════════════════════════════════════════════════════════════

    private fun getMovingPosition(m: ModuleConfig, tTime: Float, limitSide: Float, centerX: Float, centerY: Float): PointF {
        val p = paths[m.pathGroup]
        val radius = limitSide / 2f
        
        val t = tTime - m.indexInGroup * 0.04f // Tight spacing for smooth lines
        
        val dx = (sin(t * p.f1x + p.px1) * p.a1x + sin(t * p.f2x + p.px2) * p.a2x) * radius
        val dy = (cos(t * p.f1y + p.py1) * cos(t * p.f2y + p.py2) * p.a1y + sin(t * p.f2y * 1.3f + p.py2) * p.a2y) * radius
        
        return PointF(centerX + dx * 0.9f, centerY + dy * 0.9f)
    }

    private fun getTargetPosition(m: ModuleConfig, gridSide: Float, moduleSize: Float, centerX: Float, centerY: Float): PointF {
        val startX = centerX - gridSide / 2f
        val startY = centerY - gridSide / 2f
        return PointF(
            startX + m.gridX * moduleSize + moduleSize / 2f,
            startY + m.gridY * moduleSize + moduleSize / 2f
        )
    }

    // ═════════════════════════════════════════════════════════════════
    //  Drawing
    // ═════════════════════════════════════════════════════════════════

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (baseSide == 0f) return

        val w = width.toFloat()
        val h = height.toFloat()
        
        // The fixed resting geometry
        val defaultSide = baseSide
        var fLeft = cx - defaultSide / 2f
        var fTop = cy - defaultSide / 2f
        var fBot = cy + defaultSide / 2f
        var fRight = cx + defaultSide / 2f

        // Transition bounds if we have a target (where the QR code physically is)
        if (targetBoundingBox != null) {
            val tRect = targetBoundingBox!!
            fLeft += (tRect.left - fLeft) * transitionFract
            fTop += (tRect.top - fTop) * transitionFract
            fRight += (tRect.right - fRight) * transitionFract
            fBot += (tRect.bottom - fBot) * transitionFract
        }

        val currentW = fRight - fLeft
        val currentH = fBot - fTop
        val minDim = min(currentW, currentH)
        val cornerRad = min(64f, minDim * 0.1f) // Ensure stable corners when small

        // 1. Dim background with transparent cutout window
        cutoutPath.reset()
        cutoutPath.addRoundRect(fLeft, fTop, fRight, fBot, cornerRad, cornerRad, Path.Direction.CW)
        
        canvas.save()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canvas.clipOutPath(cutoutPath)
        } else {
            @Suppress("DEPRECATION")
            canvas.clipPath(cutoutPath, Region.Op.DIFFERENCE)
        }
        canvas.drawRect(0f, 0f, w, h, dimPaint)
        canvas.restore()

        // 2. Draw the 4 edges/brackets
        val bracketLen = minDim * 0.15f
        linePathObj.reset()
        // Top Left
        linePathObj.moveTo(fLeft, fTop + bracketLen)
        linePathObj.lineTo(fLeft, fTop + cornerRad)
        linePathObj.arcTo(fLeft, fTop, fLeft + cornerRad * 2, fTop + cornerRad * 2, 180f, 90f, false)
        linePathObj.lineTo(fLeft + bracketLen, fTop)
        
        // Top Right
        linePathObj.moveTo(fRight - bracketLen, fTop)
        linePathObj.lineTo(fRight - cornerRad, fTop)
        linePathObj.arcTo(fRight - cornerRad * 2, fTop, fRight, fTop + cornerRad * 2, 270f, 90f, false)
        linePathObj.lineTo(fRight, fTop + bracketLen)
        
        // Bottom Right
        linePathObj.moveTo(fRight, fBot - bracketLen)
        linePathObj.lineTo(fRight, fBot - cornerRad)
        linePathObj.arcTo(fRight - cornerRad * 2, fBot - cornerRad * 2, fRight, fBot, 0f, 90f, false)
        linePathObj.lineTo(fRight - bracketLen, fBot)
        
        // Bottom Left
        linePathObj.moveTo(fLeft + bracketLen, fBot)
        linePathObj.lineTo(fLeft + cornerRad, fBot)
        linePathObj.arcTo(fLeft, fBot - cornerRad * 2, fLeft + cornerRad * 2, fBot, 90f, 90f, false)
        linePathObj.lineTo(fLeft, fBot - bracketLen)
        
        canvas.drawPath(linePathObj, framePaint)

        // 3. Clip all organic drawing to the inside of the frame
        canvas.save()
        canvas.clipPath(cutoutPath)

        val fract = transitionFract
        if (fract > 0.01f) {
            val gridSide = minDim * 0.85f
            val moduleSize = gridSide / 13f
            val fCx = fLeft + currentW / 2f
            val fCy = fTop + currentH / 2f

            for (pIdx in 0..11) {
                val pathModules = modules.filter { it.pathGroup == pIdx }.sortedBy { it.indexInGroup }
                val points = pathModules.map { m ->
                    val organicPos = getMovingPosition(m, time, baseSide, cx, cy) // Origin matches static frame during roam
                    val targetPos = getTargetPosition(m, gridSide, moduleSize, fCx, fCy) // Target matches dynamic frame
                    PointF(
                        organicPos.x + (targetPos.x - organicPos.x) * fract,
                        organicPos.y + (targetPos.y - organicPos.y) * fract
                    )
                }

                // Draw modules (small glowing dots building into sharp square modules)
                modulePaint.alpha = 255 // always fully readable
                val size = moduleSize * (0.15f + 0.75f * fract) // small -> 90% full size
                val radius = size * (0.5f - 0.35f * fract) // capsule when small, sharp rounded rect when big
                
                for (pt in points) {
                    canvas.drawRoundRect(
                        pt.x - size / 2f, pt.y - size / 2f,
                        pt.x + size / 2f, pt.y + size / 2f,
                        radius, radius, modulePaint
                    )
                }
            }
        }
        
        canvas.restore()

        // 4. Success Ripple (drawn above clip to expand outward)
        if (successRippleAlpha > 0) {
            ripplePaint.alpha = successRippleAlpha
            val rad = successRippleRadius
            val srleft = cx - rad / 2f
            val srtop = cy - rad / 2f
            val srrgt = cx + rad / 2f
            val srbot = cy + rad / 2f
            canvas.drawRoundRect(srleft, srtop, srrgt, srbot, cornerRad * 1.25f, cornerRad * 1.25f, ripplePaint)
        }
    }
}
