package com.essential.shortcuts

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.DecimalFormat
import kotlin.math.*

class Shapes2DActivity : AppCompatActivity() {

    private lateinit var shapeDrawView: ShapeDrawView
    private lateinit var layoutInputs: LinearLayout
    private lateinit var layoutResults: LinearLayout
    private lateinit var cardResult: MaterialCardView
    private lateinit var layoutChips: LinearLayout

    private var selectedShape = "circle"
    private val inputFields = mutableMapOf<String, TextInputEditText>()
    private val df = DecimalFormat("#.####")
    private var accentColor: Int = 0

    data class Shape2D(
        val id: String,
        val name: String,
        val inputs: List<Pair<String, String>>,  // key, label
        val drawType: String
    )

    private val shapes = listOf(
        Shape2D("circle", "Circle", listOf("radius" to "Radius (r)"), "circle"),
        Shape2D("rectangle", "Rectangle", listOf("width" to "Width (w)", "height" to "Height (h)"), "rectangle"),
        Shape2D("triangle", "Triangle", listOf("base" to "Base (b)", "height" to "Height (h)", "side_a" to "Side A", "side_b" to "Side B", "side_c" to "Side C"), "triangle"),
        Shape2D("square", "Square", listOf("side" to "Side (a)"), "square"),
        Shape2D("ellipse", "Ellipse", listOf("semi_major" to "Semi-major (a)", "semi_minor" to "Semi-minor (b)"), "ellipse"),
        Shape2D("trapezoid", "Trapezoid", listOf("side_a" to "Parallel side a", "side_b" to "Parallel side b", "height" to "Height (h)", "leg1" to "Leg 1", "leg2" to "Leg 2"), "trapezoid"),
        Shape2D("parallelogram", "Parallelogram", listOf("base" to "Base (b)", "height" to "Height (h)", "side" to "Side (a)"), "parallelogram"),
        Shape2D("rhombus", "Rhombus", listOf("d1" to "Diagonal 1 (d₁)", "d2" to "Diagonal 2 (d₂)", "side" to "Side (a)"), "rhombus")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shapes_2d)

        accentColor = ContextCompat.getColor(this, R.color.category_calculator)

        shapeDrawView = findViewById(R.id.shape_draw_view)
        layoutInputs = findViewById(R.id.layout_inputs)
        layoutResults = findViewById(R.id.layout_results)
        cardResult = findViewById(R.id.card_result)
        layoutChips = findViewById(R.id.layout_shape_chips)

        findViewById<ImageButton>(R.id.btn_close).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_calculate).setOnClickListener { calculate() }

        setupShapeChips()
        selectShape("circle")
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun setupShapeChips() {
        shapes.forEach { shape ->
            val chip = Chip(this).apply {
                text = shape.name
                isCheckable = true
                isCheckedIconVisible = false
                setChipBackgroundColorResource(R.color.pwd_result_bg)
                setTextColor(ContextCompat.getColor(this@Shapes2DActivity, R.color.text_primary))
                chipStrokeWidth = 2f
                @Suppress("DEPRECATION")
                chipCornerRadius = 50f
                setOnClickListener {
                    selectShape(shape.id)
                }
            }
            layoutChips.addView(chip)
            val params = chip.layoutParams as LinearLayout.LayoutParams
            params.marginEnd = 8
            chip.layoutParams = params
        }
    }

    private fun selectShape(shapeId: String) {
        selectedShape = shapeId
        cardResult.visibility = View.GONE
        inputFields.clear()
        layoutInputs.removeAllViews()

        val shape = shapes.first { it.id == shapeId }

        // Update chip states
        for (i in 0 until layoutChips.childCount) {
            val chip = layoutChips.getChildAt(i) as Chip
            chip.isChecked = shapes[i].id == shapeId
            if (chip.isChecked) {
                chip.setChipBackgroundColorResource(R.color.category_calculator)
                chip.setTextColor(Color.WHITE)
            } else {
                chip.setChipBackgroundColorResource(R.color.pwd_result_bg)
                chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            }
        }

        // Draw shape preview
        shapeDrawView.setShape(shape.drawType, emptyMap(), accentColor)

        // Create input fields
        shape.inputs.forEach { (key, label) ->
            val til = TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                hint = label
                setBoxCornerRadii(12f, 12f, 12f, 12f)
            }
            val et = TextInputEditText(til.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                setTextColor(ContextCompat.getColor(this@Shapes2DActivity, R.color.text_primary))
            }
            til.addView(et)
            layoutInputs.addView(til)
            inputFields[key] = et
        }
    }

    private fun getInput(key: String): Double? {
        return inputFields[key]?.text?.toString()?.toDoubleOrNull()
    }

    private fun calculate() {
        // Hide keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

        val results = mutableListOf<Pair<String, String>>()

        when (selectedShape) {
            "circle" -> {
                val r = getInput("radius") ?: return showError("Enter the radius")
                results.add("Area" to df.format(PI * r * r))
                results.add("Circumference" to df.format(2 * PI * r))
                results.add("Diameter" to df.format(2 * r))
            }
            "rectangle" -> {
                val w = getInput("width") ?: return showError("Enter the width")
                val h = getInput("height") ?: return showError("Enter the height")
                results.add("Area" to df.format(w * h))
                results.add("Perimeter" to df.format(2 * (w + h)))
                results.add("Diagonal" to df.format(sqrt(w * w + h * h)))
            }
            "triangle" -> {
                val b = getInput("base")
                val h = getInput("height")
                val a = getInput("side_a")
                val sb = getInput("side_b")
                val sc = getInput("side_c")
                if (b != null && h != null) {
                    results.add("Area" to df.format(0.5 * b * h))
                }
                if (a != null && sb != null && sc != null) {
                    val s = (a + sb + sc) / 2
                    val heronArea = sqrt(s * (s - a) * (s - sb) * (s - sc))
                    results.add("Area (Heron's)" to df.format(heronArea))
                    results.add("Perimeter" to df.format(a + sb + sc))
                }
                if (results.isEmpty()) return showError("Enter base & height, or all 3 sides")
            }
            "square" -> {
                val s = getInput("side") ?: return showError("Enter the side length")
                results.add("Area" to df.format(s * s))
                results.add("Perimeter" to df.format(4 * s))
                results.add("Diagonal" to df.format(s * sqrt(2.0)))
            }
            "ellipse" -> {
                val a = getInput("semi_major") ?: return showError("Enter semi-major axis")
                val b = getInput("semi_minor") ?: return showError("Enter semi-minor axis")
                results.add("Area" to df.format(PI * a * b))
                // Ramanujan approximation for circumference
                val h = ((a - b) * (a - b)) / ((a + b) * (a + b))
                results.add("Circumference ≈" to df.format(PI * (a + b) * (1 + 3 * h / (10 + sqrt(4 - 3 * h)))))
                results.add("Eccentricity" to df.format(sqrt(1 - (b * b) / (a * a))))
            }
            "trapezoid" -> {
                val a = getInput("side_a") ?: return showError("Enter parallel side a")
                val b = getInput("side_b") ?: return showError("Enter parallel side b")
                val h = getInput("height") ?: return showError("Enter the height")
                results.add("Area" to df.format(0.5 * (a + b) * h))
                val l1 = getInput("leg1")
                val l2 = getInput("leg2")
                if (l1 != null && l2 != null) {
                    results.add("Perimeter" to df.format(a + b + l1 + l2))
                }
            }
            "parallelogram" -> {
                val b = getInput("base") ?: return showError("Enter the base")
                val h = getInput("height") ?: return showError("Enter the height")
                results.add("Area" to df.format(b * h))
                val s = getInput("side")
                if (s != null) {
                    results.add("Perimeter" to df.format(2 * (b + s)))
                }
            }
            "rhombus" -> {
                val d1 = getInput("d1")
                val d2 = getInput("d2")
                val s = getInput("side")
                if (d1 != null && d2 != null) {
                    results.add("Area" to df.format(d1 * d2 / 2))
                }
                if (s != null) {
                    results.add("Perimeter" to df.format(4 * s))
                }
                if (d1 == null && d2 == null && s == null) return showError("Enter diagonals or side")
            }
        }

        showResults(results)
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun showResults(results: List<Pair<String, String>>) {
        layoutResults.removeAllViews()
        results.forEachIndexed { index, (label, value) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (index < results.size - 1) bottomMargin = 12
                }
            }

            val tvLabel = TextView(this).apply {
                text = label
                setTextColor(ContextCompat.getColor(this@Shapes2DActivity, R.color.calc_display_text_secondary))
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvValue = TextView(this).apply {
                text = value
                setTextColor(ContextCompat.getColor(this@Shapes2DActivity, R.color.calc_display_text_primary))
                textSize = 18f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.END
            }

            row.addView(tvLabel)
            row.addView(tvValue)
            layoutResults.addView(row)

            // Divider
            if (index < results.size - 1) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    ).apply { topMargin = 12 }
                    setBackgroundColor(ContextCompat.getColor(this@Shapes2DActivity, R.color.calc_divider))
                }
                layoutResults.addView(divider)
            }
        }
        cardResult.visibility = View.VISIBLE
        setResult(RESULT_OK)   // ← mark as completed
    }
}
