package com.essential.shortcuts

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur
import kotlin.math.roundToInt

/**
 * LiquidGlassBottomBar — a two-tab navigation bar styled as liquid glass:
 *  • Real-time background blur (matching the top bar) with blur_overlay tint
 *  • Translucent pill with top-edge specular highlight and gradient border
 *  • Spring-animated capsule indicator that slides between tabs
 *  • Interactive "balloon" press feedback (subtle scale + lens highlight)
 *  • Dark-mode aware colours
 */
class LiquidGlassBottomBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BlurView(context, attrs, defStyleAttr) {

    // ── Blur & Clipping ───────────────────────────────────────────────────────
    private var hasCustomBlurSetup = false
    private val clipPath = Path()

    // ── DPI helper ───────────────────────────────────────────────────────────
    private val dp = resources.displayMetrics.density

    // ── Geometry constants ───────────────────────────────────────────────────
    private val barRadius       = 36f * dp
    private val tabPad          = 5f * dp   // gap between bar edge and capsule
    private val capsuleRadius   = 30f * dp
    private val borderWidth     = 1.2f * dp

    // ── Paints ───────────────────────────────────────────────────────────────
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = borderWidth
    }

    private val specularPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val capsulePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val capsuleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.8f * dp
    }

    private val capsuleLensPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ── State ────────────────────────────────────────────────────────────────
    private var selectedIndex  = 0
    private val tabCount       = 2
    /** Animated X of the capsule's left edge */
    private var capsuleLeft    = 0f
    /** Pulsed gently on press [0..1] */
    private var pressProgress  = 0f
    private var capsuleScale   = 1f
    private var isDark         = false

    // ── Drag state ──────────────────────────────────────────────────────────
    private var isDragging    = false
    private var touchStartX   = 0f
    private var capsuleStartX = 0f
    private val touchSlop     = android.view.ViewConfiguration.get(context).scaledTouchSlop

    // ── Children ─────────────────────────────────────────────────────────────
    private lateinit var recentTab: LinearLayout
    private lateinit var allToolsTab: LinearLayout

    // ── Callbacks ────────────────────────────────────────────────────────────
    var onTabSelected: ((Int) -> Unit)? = null

    // ── Animation handles ────────────────────────────────────────────────────
    private var capsuleSpring: SpringAnimation? = null
    private var pressAnim:     ValueAnimator? = null
    private var scaleAnim:     ValueAnimator? = null

    // ── Reusable rects ───────────────────────────────────────────────────────
    private val barRectF     = RectF()
    private val capsuleRectF = RectF()
    private val tmpRectF     = RectF()

    // ── FloatPropertyCompat for spring ───────────────────────────────────────
    private val capsuleLeftProp = object : FloatPropertyCompat<LiquidGlassBottomBar>("capsuleLeft") {
        override fun getValue(obj: LiquidGlassBottomBar) = obj.capsuleLeft
        override fun setValue(obj: LiquidGlassBottomBar, value: Float) {
            obj.capsuleLeft = value
            obj.invalidate()
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    init {
        setWillNotDraw(false)
        clipChildren  = false
        clipToPadding = false
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, barRadius)
            }
        }
        clipToOutline = true
        buildTabs()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!hasCustomBlurSetup) {
            val root = (parent as? ViewGroup)
                ?: ((context as? Activity)?.findViewById(android.R.id.content) as? ViewGroup)
                ?: ((context as? Activity)?.window?.decorView as? ViewGroup)
            if (root != null) {
                setupBlur(root)
            }
        }
    }

    /**
     * Sets up the real-time frosted glass blur matching the top bar.
     */
    fun setupBlur(rootView: ViewGroup) {
        val blurAlgorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RenderEffectBlur()
        } else {
            RenderScriptBlur(context)
        }
        val decorView = (context as? Activity)?.window?.decorView
        val clearDrawable = decorView?.background ?: ContextCompat.getDrawable(context, R.color.background_light)
        val overlayColor = ContextCompat.getColor(context, R.color.blur_overlay)

        setupWith(rootView, blurAlgorithm)
            .setFrameClearDrawable(clearDrawable)
            .setBlurRadius(15f)
            .setOverlayColor(overlayColor)

        hasCustomBlurSetup = true
        invalidate()
    }

    // ── Build child tabs ─────────────────────────────────────────────────────

    private fun buildTabs() {
        recentTab   = makeTab(R.drawable.ic_recent,    "Recent")
        allToolsTab = makeTab(R.drawable.ic_all_tools, "Shortcuts")
        addView(recentTab)
        addView(allToolsTab)
        recentTab.setOnClickListener   { selectTab(0) }
        allToolsTab.setOnClickListener { selectTab(1) }
    }

    private fun makeTab(iconRes: Int, label: String): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity     = android.view.Gravity.CENTER
            isClickable = true
            isFocusable = true
            background  = null

            addView(ImageView(context).apply {
                id         = generateViewId()
                layoutParams = LinearLayout.LayoutParams((24 * dp).toInt(), (24 * dp).toInt())
                setImageResource(iconRes)
                tag        = "icon"
            })
            addView(TextView(context).apply {
                id   = generateViewId()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = (2 * dp).toInt() }
                text     = label
                textSize = 11f
                typeface = Typeface.DEFAULT
                tag      = "label"
            })
        }

    // ── Public API ───────────────────────────────────────────────────────────

    /** Select tab and fire the callback */
    fun selectTab(index: Int) {
        if (index == selectedIndex) {
            // Even if already selected, snap back to position (important after drag)
            springCapsuleTo(tabLeft(index))
            return
        }
        selectedIndex = index
        applyTabColors()
        springCapsuleTo(tabLeft(index))
        onTabSelected?.invoke(index)
    }

    /** Select tab without firing the callback (used for back-button sync) */
    fun setSelectedTabWithoutCallback(index: Int) {
        selectedIndex = index
        applyTabColors()
        // Snap immediately; layout might not be ready yet, handled in onSizeChanged
        if (width > 0) capsuleLeft = tabLeft(index)
        invalidate()
    }

    // ── Measurement & layout ─────────────────────────────────────────────────

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val totalW  = MeasureSpec.getSize(widthMeasureSpec)
        val totalH  = (64 * dp).toInt()
        
        // Use a compact uniform width for tabs
        val maxTabW = 110f * dp 
        val availableW = totalW - (tabPad * 2).toInt()
        val tabW = if (maxTabW * tabCount <= availableW) maxTabW.toInt() else availableW / tabCount
        
        val tabH = totalH - (tabPad * 2).toInt()
        repeat(childCount) { i ->
            getChildAt(i).measure(
                MeasureSpec.makeMeasureSpec(tabW, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(tabH, MeasureSpec.EXACTLY)
            )
        }
        
        // Shrink the overall bar width to fit the buttons precisely
        val measuredW = tabW * tabCount + (tabPad * 2).toInt()
        setMeasuredDimension(measuredW, totalH)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (childCount == 0) return
        val tabW = getChildAt(0).measuredWidth
        val tabH = (b - t) - (tabPad * 2).toInt()
        val top = tabPad.toInt()
        
        repeat(childCount) { i ->
            val left = tabPad.toInt() + i * tabW
            getChildAt(i).layout(left, top, left + tabW, top + tabH)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        barRectF.set(0f, 0f, w.toFloat(), h.toFloat())
        // Always snap on size change (rotation / first layout)
        capsuleLeft = tabLeft(selectedIndex)
        invalidateOutline()
    }

    // ── Drawing ──────────────────────────────────────────────────────────────

    override fun draw(canvas: Canvas) {
        if (width > 0 && height > 0) {
            if (barRectF.isEmpty) {
                barRectF.set(0f, 0f, width.toFloat(), height.toFloat())
            }
            val saveCount = canvas.save()
            clipPath.reset()
            clipPath.addRoundRect(barRectF, barRadius, barRadius, Path.Direction.CW)
            canvas.clipPath(clipPath)
            super.draw(canvas)
            canvas.restoreToCount(saveCount)
        } else {
            super.draw(canvas)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val dark = nightMode()
        if (dark != isDark) {
            isDark = dark
            if (hasCustomBlurSetup) {
                setOverlayColor(ContextCompat.getColor(context, R.color.blur_overlay))
            }
        }
        drawBar(canvas)
        // Capsule moved to dispatchDraw to be on top of text
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        
        // Draw the selection capsule ON TOP of the tabs (text/icons)
        drawCapsule(canvas)
        
        // Thin specular sheen drawn on top of everything
        if (!isDark) {
            specularPaint.shader = LinearGradient(
                0f, barRectF.top, 0f, barRectF.top + 4f * dp,
                Color.argb(50, 255, 255, 255),
                Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(barRectF, barRadius, barRadius, specularPaint)
        }
    }

    private fun drawBar(canvas: Canvas) {
        val h = barRectF.height()

        // ── Glass fill ──────────────────────────────────────────────────────
        // If blur is active, BlurView draws the real-time blurred background + blur_overlay.
        // If blur is not active, fallback to drawing the translucent gradient.
        if (!hasCustomBlurSetup) {
            if (isDark) {
                bgPaint.shader = null
                bgPaint.color  = Color.argb(248, 26, 26, 30)
            } else {
                bgPaint.shader = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(
                        Color.argb(230, 255, 255, 255),
                        Color.argb(228, 250, 250, 254),
                        Color.argb(235, 242, 242, 247)
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(barRectF, barRadius, barRadius, bgPaint)
        }

        // ── Top-edge inner highlight ───────────────────────────────────────
        val hRect = RectF(barRectF).apply { inset(1f * dp, 1f * dp) }
        val hlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            if (isDark) {
                color = Color.argb(12, 255, 255, 255)
            } else {
                shader = LinearGradient(
                    0f, hRect.top, 0f, hRect.top + 18 * dp,
                    Color.argb(90, 255, 255, 255),
                    Color.TRANSPARENT, Shader.TileMode.CLAMP
                )
            }
        }
        canvas.drawRoundRect(hRect, barRadius - dp, barRadius - dp, hlPaint)

        // ── Border ────────────────────────────────────────────────────────
        val bi = borderWidth / 2f
        tmpRectF.set(barRectF); tmpRectF.inset(bi, bi)
        if (isDark) {
            borderPaint.shader = null
            borderPaint.color  = Color.argb(40, 255, 255, 255)
        } else {
            borderPaint.shader = LinearGradient(
                0f, 0f, 0f, h,
                intArrayOf(
                    Color.argb(65, 255, 255, 255),
                    Color.argb(28, 0,   0,   0  ),
                    Color.argb(18, 0,   0,   0  )
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(tmpRectF, barRadius - bi, barRadius - bi, borderPaint)
    }

    private fun drawCapsule(canvas: Canvas) {
        if (width == 0) return
        val tabW = tabWidth()
        val tabH = tabHeight()

        capsuleRectF.set(capsuleLeft, tabPad, capsuleLeft + tabW, tabPad + tabH)

        // Scale around centre for press feedback
        if (capsuleScale != 1f) {
            val cx = capsuleRectF.centerX(); val cy = capsuleRectF.centerY()
            val hw = capsuleRectF.width()  * capsuleScale / 2f
            val hh = capsuleRectF.height() * capsuleScale / 2f
            capsuleRectF.set(cx - hw, cy - hh, cx + hw, cy + hh)
        }

        // Fill
        val fillAlpha = lerp(20, 38, pressProgress)
        capsulePaint.color = if (isDark) Color.argb(fillAlpha, 255, 255, 255)
                             else        Color.argb(fillAlpha, 0,   0,   0  )
        canvas.drawRoundRect(capsuleRectF, capsuleRadius, capsuleRadius, capsulePaint)

        // Lens glow on press
        if (pressProgress > 0.02f) {
            val r = capsuleRectF.height() * 0.55f * pressProgress
            capsuleLensPaint.shader = RadialGradient(
                capsuleRectF.centerX(), capsuleRectF.centerY(), r,
                if (isDark) Color.argb((22 * pressProgress).toInt(), 255, 255, 255)
                else        Color.argb((35 * pressProgress).toInt(), 255, 255, 255),
                Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(capsuleRectF, capsuleRadius, capsuleRadius, capsuleLensPaint)
        }

        // Capsule border
        val bAlpha = lerp(15, 40, pressProgress)
        val bi = capsuleBorderPaint.strokeWidth / 2f
        tmpRectF.set(capsuleRectF); tmpRectF.inset(bi, bi)
        if (isDark) {
            capsuleBorderPaint.shader = null
            capsuleBorderPaint.color = Color.argb(bAlpha + 5, 255, 255, 255)
        } else {
            capsuleBorderPaint.shader = LinearGradient(
                capsuleRectF.left, capsuleRectF.top,
                capsuleRectF.left, capsuleRectF.bottom,
                intArrayOf(Color.argb(bAlpha + 15, 255, 255, 255),
                           Color.argb(bAlpha / 5,  0,   0,   0  )),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(tmpRectF, capsuleRadius - bi, capsuleRadius - bi, capsuleBorderPaint)
    }

    // ── Touch ────────────────────────────────────────────────────────────────

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = ev.x
                capsuleStartX = capsuleLeft
                isDragging = false
                triggerPress(true)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isDragging && Math.abs(ev.x - touchStartX) > touchSlop) {
                    isDragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                triggerPress(false)
            }
        }
        return isDragging
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val delta = ev.x - touchStartX
                    val minX = tabLeft(0)
                    val maxX = tabLeft(tabCount - 1)
                    capsuleLeft = (capsuleStartX + delta).coerceIn(minX, maxX)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    val tw = tabWidth()
                    val center = capsuleLeft + tw / 2f
                    val index = if (center < tabLeft(1)) 0 else 1
                    selectTab(index)
                    isDragging = false
                } else if (ev.actionMasked == MotionEvent.ACTION_UP) {
                    // Tap handling if intercept didn't trigger
                    val index = if (ev.x < width / 2f) 0 else 1
                    selectTab(index)
                }
                triggerPress(false)
            }
        }
        return true
    }

    // ── Spring animation for capsule ─────────────────────────────────────────

    private fun springCapsuleTo(target: Float) {
        capsuleSpring?.cancel()
        capsuleSpring = SpringAnimation(this, capsuleLeftProp, target).apply {
            spring = SpringForce(target).also {
                it.dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
                it.stiffness    = 650f
            }
            start()
        }
    }

    // ── Press feedback ───────────────────────────────────────────────────────

    private fun triggerPress(pressed: Boolean) {
        val targetProgress = if (pressed) 1f else 0f
        val targetScale    = if (pressed) 1.055f else 1f

        pressAnim?.cancel()
        pressAnim = ValueAnimator.ofFloat(pressProgress, targetProgress).apply {
            duration     = if (pressed) 180L else 380L
            interpolator = if (pressed) null else OvershootInterpolator(1.5f)
            addUpdateListener { pressProgress = it.animatedValue as Float; invalidate() }
            start()
        }

        scaleAnim?.cancel()
        scaleAnim = ValueAnimator.ofFloat(capsuleScale, targetScale).apply {
            duration     = if (pressed) 150L else 320L
            interpolator = if (pressed) null else OvershootInterpolator(2.5f)
            addUpdateListener { capsuleScale = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    // ── Tab colors ───────────────────────────────────────────────────────────

    private fun applyTabColors() {
        val active   = ContextCompat.getColor(context, R.color.primary_blue)
        val inactive = ContextCompat.getColor(context, R.color.nav_inactive)
        applyColor(recentTab,   selectedIndex == 0, active, inactive)
        applyColor(allToolsTab, selectedIndex == 1, active, inactive)
    }

    private fun applyColor(tab: LinearLayout, isActive: Boolean, active: Int, inactive: Int) {
        val icon  = tab.findViewWithTag<ImageView>("icon")
        val label = tab.findViewWithTag<TextView>("label")
        val to    = if (isActive) active else inactive

        icon.setColorFilter(to)
        label.setTextColor(to)
        label.typeface = if (isActive) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

        val iconScale = if (isActive) 1.12f else 1f
        icon.animate()
            .scaleX(iconScale).scaleY(iconScale)
            .setDuration(240)
            .setInterpolator(OvershootInterpolator(2.2f))
            .start()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun tabWidth()  = if (childCount > 0) getChildAt(0).measuredWidth.toFloat() else 0f
    private fun tabHeight() = if (childCount > 0) getChildAt(0).measuredHeight.toFloat() else 0f
    private fun tabLeft(i: Int) = tabPad + i * tabWidth()

    private fun lerp(a: Int, b: Int, t: Float) =
        (a + (b - a) * t.coerceIn(0f, 1f)).roundToInt()

    private fun nightMode() =
        (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
}
