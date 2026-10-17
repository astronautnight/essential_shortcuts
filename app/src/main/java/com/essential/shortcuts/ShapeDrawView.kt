package com.essential.shortcuts

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

/**
 * Custom view that draws flat outline representations of 2D and 3D shapes.
 * The shapes are drawn as clean, labeled diagrams with dimension annotations.
 */
class ShapeDrawView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var shapeType: String = ""
    private var dimensions: Map<String, Float> = emptyMap()
    private var accentColor: Int = Color.parseColor("#5856D6")

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = accentColor
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(10f, 8f), 0f)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    fun setShape(type: String, dims: Map<String, Float>, color: Int) {
        shapeType = type
        dimensions = dims
        accentColor = color
        outlinePaint.color = color
        dashPaint.color = Color.argb(120, Color.red(color), Color.green(color), Color.blue(color))
        labelPaint.color = color
        dimPaint.color = Color.argb(150, Color.red(color), Color.green(color), Color.blue(color))
        fillPaint.color = Color.argb(20, Color.red(color), Color.green(color), Color.blue(color))
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (shapeType.isEmpty()) return

        val cx = width / 2f
        val cy = height / 2f
        val maxDim = minOf(width, height) * 0.35f

        when (shapeType) {
            // 2D Shapes
            "circle" -> drawCircle(canvas, cx, cy, maxDim)
            "rectangle" -> drawRectangle(canvas, cx, cy, maxDim)
            "triangle" -> drawTriangle(canvas, cx, cy, maxDim)
            "square" -> drawSquare(canvas, cx, cy, maxDim)
            "ellipse" -> drawEllipse(canvas, cx, cy, maxDim)
            "trapezoid" -> drawTrapezoid(canvas, cx, cy, maxDim)
            "parallelogram" -> drawParallelogram(canvas, cx, cy, maxDim)
            "rhombus" -> drawRhombus(canvas, cx, cy, maxDim)

            // 3D Shapes
            "sphere" -> drawSphere(canvas, cx, cy, maxDim)
            "cube" -> drawCube(canvas, cx, cy, maxDim)
            "cylinder" -> drawCylinder(canvas, cx, cy, maxDim)
            "cone" -> drawCone(canvas, cx, cy, maxDim)
            "cuboid" -> drawCuboid(canvas, cx, cy, maxDim)
            "pyramid" -> drawPyramid(canvas, cx, cy, maxDim)
        }
    }

    // ===== 2D SHAPES =====

    private fun drawCircle(canvas: Canvas, cx: Float, cy: Float, maxDim: Float) {
        val r = maxDim * 0.8f
        canvas.drawCircle(cx, cy, r, fillPaint)
        canvas.drawCircle(cx, cy, r, outlinePaint)
        // Radius line
        canvas.drawLine(cx, cy, cx + r, cy, dimPaint)
        canvas.drawCircle(cx, cy, 4f, outlinePaint)
        labelPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("r", cx + r / 2, cy - 10, labelPaint)
    }

    private fun drawRectangle(canvas: Canvas, cx: Float, cy: Float, maxDim: Float) {
        val w = maxDim * 1.4f
        val h = maxDim * 0.9f
        val rect = RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
        canvas.drawRect(rect, fillPaint)
        canvas.drawRect(rect, outlinePaint)
        // Labels
        canvas.drawText("w", cx, cy + h / 2 + 30, labelPaint)
        canvas.save()
        canvas.rotate(-90f, cx - w / 2 - 20, cy)
        canvas.drawText("h", cx - w / 2 - 20, cy, labelPaint)
        canvas.restore()
    }

    private fun drawTriangle(canvas: Canvas, cx: Float, cy: Float, maxDim: Float) {
        val base = maxDim * 1.4f
        val height = maxDim * 1.2f
        val path = Path().apply {
            moveTo(cx, cy - height / 2)
            lineTo(cx - base / 2, cy + height / 2)
            lineTo(cx + base / 2, cy + height / 2)
            close()
        }
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, outlinePaint)
        // Height dashed line
        canvas.drawLine(cx, cy - height / 2, cx, cy + height / 2, dashPaint)
        canvas.drawText("b", cx, cy + height / 2 + 30, labelPaint)
        canvas.drawText("h", cx + 16, cy, labelPaint)
    }

    private fun drawSquare(canvas: Canvas, cx: Float, cy: Float, maxDim: Float) {
        val s = maxDim * 1.1f
        val rect = RectF(cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2)
        canvas.drawRect(rect, fillPaint)
        canvas.drawRect(rect, outlinePaint)
        canvas.drawText("a", cx, cy + s / 2 + 30, labelPaint)
        // Small right angle marker
        val m = 12f
        canvas.drawLine(cx - s / 2 + m, cy + s / 2, cx - s / 2 + m, cy + s / 2 - m, dimPaint)
        canvas.drawLine(cx - s / 2, cy + s / 2 - m, cx - s / 2 + m, cy + s / 2 - m, dimPaint)
    }

    private fun drawEllipse(canvas: Canvas, cx: Float, cy: Float, maxDim: Float) {
        val a = maxDim * 1.2f
        val b = maxDim * 0.7f
        val rect = RectF(cx - a, cy - b, cx + a, cy + b)
        canvas.drawOval(rect, fillPaint)
        canvas.drawOval(rect, outlinePaint)
        canvas.drawLine(cx, cy, cx + a, cy, dimPaint)
        canvas.drawLine(cx, cy, cx, cy - b, dimPaint)
        canvas.drawCircle(cx, cy, 4f, outlinePaint)
        canvas.drawText("a", cx + a / 2, cy - 10, labelPaint)
        canvas.drawText("b", cx + 14, cy - b / 2, labelPaint)
    }

    private fun drawTrapezoid(canvas: Canvas, cx: Float, cy: Float, maxDim: Float) {
        val topW = maxDim * 0.8f
        val botW = maxDim * 1.6f
        val h = maxDim * 1.0f
        val path = Path().apply {
            moveTo(cx - topW / 2, cy - h / 2)
            lineTo(cx + topW / 2, cy - h / 2)
            lineTo(cx + botW / 2, cy + h / 2)
            lineTo(cx - botW / 2, cy + h / 2)
            close()
        }
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, outlinePaint)
        canvas.drawText("a", cx, cy - h / 2 - 10, labelPaint)
        canvas.drawText("b", cx, cy + h / 2 + 30, labelPaint)
        // Height dashed
        canvas.drawLine(cx - topW / 2, cy - h / 2, cx - topW / 2, cy + h / 2, dashPaint)
        canvas.drawText("h", cx - topW / 2 - 24, cy, labelPaint)
    }

    private fun drawParallelogram(canvas: Canvas, cx: Float, cy: Float, maxDim: Float) {
        val w = maxDim * 1.4f
        val h = maxDim * 0.8f
        val skew = maxDim * 0.35f
        val path = Path().apply {
            moveTo(cx - w / 2 + skew, cy - h / 2)
            lineTo(cx + w / 2 + skew, cy - h / 2)
            lineTo(cx + w / 2 - skew, cy + h / 2)
            lineTo(cx - w / 2 - skew, cy + h / 2)
            close()
        }
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, outlinePaint)
        canvas.drawText("b", cx, cy + h / 2 + 30, labelPaint)
        // Height dashed
        canvas.drawLine(cx - w / 2 + skew, cy - h / 2, cx - w / 2 + skew, cy + h / 2, dashPaint)
        canvas.drawText("h", cx - w / 2 + skew - 24, cy, labelPaint)
    }

    private fun drawRhombus(canvas: Canvas, cx: Float, cy: Float, maxDim: Float) {
        val d1 = maxDim * 1.4f  // horizontal diagonal
        val d2 = maxDim * 1.0f  // vertical diagonal
        val path = Path().apply {
            moveTo(cx, cy - d2 / 2)
            lineTo(cx + d1 / 2, cy)
            lineTo(cx, cy + d2 / 2)
            lineTo(cx - d1 / 2, cy)
            close()
        }
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, outlinePaint)
        // Diagonals
        canvas.drawLine(cx - d1 / 2, cy, cx + d1 / 2, cy, dashPaint)
        canvas.drawLine(cx, cy - d2 / 2, cx, cy + d2 / 2, dashPaint)
        canvas.drawText("d₁", cx + d1 / 4, cy - 10, labelPaint)
        canvas.drawText("d₂", cx + 14, cy - d2 / 4, labelPaint)
    }

    // ===== 3D SHAPES =====

    private fun drawSphere(canvas: Canvas, cx: Float, cy: Float, maxDim: Float) {
        val r = maxDim * 0.85f
        canvas.drawCircle(cx, cy, r, fillPaint)
        canvas.drawCircle(cx, cy, r, outlinePaint)
        // Equator ellipse (dashed)
        val ovalRect = RectF(cx - r, cy - r * 0.3f, cx + r, cy + r * 0.3f)
        canvas.drawOval(ovalRect, dashPaint)
        // Radius
        canvas.drawLine(cx, cy, cx + r, cy, dimPaint)
        canvas.drawCircle(cx, cy, 4f, outlinePaint)
        canvas.drawText("r", cx + r / 2, cy - 12, labelPaint)
    }

    private fun drawCube(canvas: Canvas, cx: Float, cy: Float, maxDim: Float) {
        val s = maxDim * 0.8f
        val off = s * 0.4f  // 3D offset

        // Back face
        val backPath = Path().apply {
            moveTo(cx - s / 2 + off, cy - s / 2 - off)
            lineTo(cx + s / 2 + off, cy - s / 2 - off)
            lineTo(cx + s / 2 + off, cy + s / 2 - off)
            lineTo(cx - s / 2 + off, cy + s / 2 - off)
            close()
        }
        canvas.drawPath(backPath, dashPaint)

        // Front face
        val frontRect = RectF(cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2)
        canvas.drawRect(frontRect, fillPaint)
        canvas.drawRect(frontRect, outlinePaint)

        // Connecting edges
        canvas.drawLine(cx - s / 2, cy - s / 2, cx - s / 2 + off, cy - s / 2 - off, outlinePaint)
        canvas.drawLine(cx + s / 2, cy - s / 2, cx + s / 2 + off, cy - s / 2 - off, outlinePaint)
        canvas.drawLine(cx + s / 2, cy + s / 2, cx + s / 2 + off, cy + s / 2 - off, outlinePaint)

        // Top face fill
        val topPath = Path().apply {
            moveTo(cx - s / 2, cy - s / 2)
            lineTo(cx - s / 2 + off, cy - s / 2 - off)
            lineTo(cx + s / 2 + off, cy - s / 2 - off)
            lineTo(cx + s / 2, cy - s / 2)
            close()
        }
        canvas.drawPath(topPath, fillPaint)
        canvas.drawPath(topPath, outlinePaint)

        // Right face fill
        val rightPath = Path().apply {
            moveTo(cx + s / 2, cy - s / 2)
            lineTo(cx + s / 2 + off, cy - s / 2 - off)
            lineTo(cx + s / 2 + off, cy + s / 2 - off)
            lineTo(cx + s / 2, cy + s / 2)
            close()
        }
        canvas.drawPath(rightPath, fillPaint)
        canvas.drawPath(rightPath, outlinePaint)

        canvas.drawText("a", cx, cy + s / 2 + 30, labelPaint)
    }

    private fun drawCylinder(canvas: Canvas, cx: Float, cy: Float, maxDim: Float) {
        val r = maxDim * 0.6f
        val h = maxDim * 1.2f
        val topY = cy - h / 2
        val botY = cy + h / 2
        val ovalH = r * 0.35f

        // Body
        canvas.drawLine(cx - r, topY, cx - r, botY, outlinePaint)
        canvas.drawLine(cx + r, topY, cx + r, botY, outlinePaint)

        // Bottom ellipse
        val botOval = RectF(cx - r, botY - ovalH, cx + r, botY + ovalH)
        canvas.drawOval(botOval, fillPaint)
        canvas.drawOval(botOval, outlinePaint)

        // Top ellipse (full)
        val topOval = RectF(cx - r, topY - ovalH, cx + r, topY + ovalH)
        canvas.drawOval(topOval, fillPaint)
        canvas.drawOval(topOval, outlinePaint)

        // Height dimension
        canvas.drawLine(cx + r + 20, topY, cx + r + 20, botY, dimPaint)
        canvas.drawText("h", cx + r + 40, cy, labelPaint)

        // Radius
        canvas.drawLine(cx, topY, cx + r, topY, dimPaint)
        canvas.drawText("r", cx + r / 2, topY - 14, labelPaint)
    }

    private fun drawCone(canvas: Canvas, cx: Float, cy: Float, maxDim: Float) {
        val r = maxDim * 0.7f
        val h = maxDim * 1.3f
        val topY = cy - h / 2
        val botY = cy + h / 2
        val ovalH = r * 0.3f

        // Sides
        canvas.drawLine(cx, topY, cx - r, botY, outlinePaint)
        canvas.drawLine(cx, topY, cx + r, botY, outlinePaint)

        // Body fill
        val bodyPath = Path().apply {
            moveTo(cx, topY)
            lineTo(cx - r, botY)
            lineTo(cx + r, botY)
            close()
        }
        canvas.drawPath(bodyPath, fillPaint)

        // Base ellipse
        val botOval = RectF(cx - r, botY - ovalH, cx + r, botY + ovalH)
        canvas.drawOval(botOval, fillPaint)
        canvas.drawOval(botOval, outlinePaint)

        // Height dashed
        canvas.drawLine(cx, topY, cx, botY, dashPaint)
        canvas.drawText("h", cx + 16, cy, labelPaint)

        // Slant
        canvas.drawText("r", cx + r / 2 + 8, botY + 28, labelPaint)
    }

    private fun drawCuboid(canvas: Canvas, cx: Float, cy: Float, maxDim: Float) {
        val w = maxDim * 1.1f
        val h = maxDim * 0.7f
        val d = maxDim * 0.35f

        // Front face
        val front = RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
        canvas.drawRect(front, fillPaint)
        canvas.drawRect(front, outlinePaint)

        // Top face
        val topPath = Path().apply {
            moveTo(cx - w / 2, cy - h / 2)
            lineTo(cx - w / 2 + d, cy - h / 2 - d)
            lineTo(cx + w / 2 + d, cy - h / 2 - d)
            lineTo(cx + w / 2, cy - h / 2)
            close()
        }
        canvas.drawPath(topPath, fillPaint)
        canvas.drawPath(topPath, outlinePaint)

        // Right face
        val rightPath = Path().apply {
            moveTo(cx + w / 2, cy - h / 2)
            lineTo(cx + w / 2 + d, cy - h / 2 - d)
            lineTo(cx + w / 2 + d, cy + h / 2 - d)
            lineTo(cx + w / 2, cy + h / 2)
            close()
        }
        canvas.drawPath(rightPath, fillPaint)
        canvas.drawPath(rightPath, outlinePaint)

        // Labels
        canvas.drawText("w", cx, cy + h / 2 + 30, labelPaint)
        canvas.save()
        canvas.rotate(-90f, cx - w / 2 - 20, cy)
        canvas.drawText("h", cx - w / 2 - 20, cy, labelPaint)
        canvas.restore()
        canvas.drawText("d", cx + w / 2 + d / 2 + 12, cy - h / 2 - d / 2, labelPaint)
    }

    private fun drawPyramid(canvas: Canvas, cx: Float, cy: Float, maxDim: Float) {
        val base = maxDim * 1.2f
        val h = maxDim * 1.3f
        val topY = cy - h / 2
        val botY = cy + h / 2
        val d = base * 0.3f

        // Front face
        val frontPath = Path().apply {
            moveTo(cx, topY)
            lineTo(cx - base / 2, botY)
            lineTo(cx + base / 2, botY)
            close()
        }
        canvas.drawPath(frontPath, fillPaint)
        canvas.drawPath(frontPath, outlinePaint)

        // Base parallelogram (3D effect)
        val basePath = Path().apply {
            moveTo(cx - base / 2, botY)
            lineTo(cx - base / 2 + d, botY - d * 0.6f)
            lineTo(cx + base / 2 + d, botY - d * 0.6f)
            lineTo(cx + base / 2, botY)
            close()
        }
        canvas.drawPath(basePath, fillPaint)
        canvas.drawPath(basePath, outlinePaint)

        // Right edge to apex
        canvas.drawLine(cx, topY, cx + base / 2 + d, botY - d * 0.6f, dashPaint)

        // Height dashed
        canvas.drawLine(cx, topY, cx, botY, dashPaint)
        canvas.drawText("h", cx + 16, cy, labelPaint)
        canvas.drawText("b", cx, botY + 30, labelPaint)
    }
}
