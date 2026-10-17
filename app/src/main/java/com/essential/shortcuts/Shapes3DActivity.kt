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

class Shapes3DActivity : AppCompatActivity() {

    private lateinit var shapeDrawView: ShapeDrawView
    private lateinit var layoutInputs: LinearLayout
    private lateinit var layoutResults: LinearLayout
    private lateinit var cardResult: MaterialCardView
    private lateinit var layoutChips: LinearLayout

    private var selectedShape = "sphere"
    private val inputFields = mutableMapOf<String, TextInputEditText>()
    private val df = DecimalFormat("#.####")
    private var accentColor: Int = 0

    data class Shape3D(
        val id: String,
        val name: String,
        val inputs: List<Pair<String, String>>,
        val drawType: String
    )

    private val shapes = listOf(
        Shape3D("sphere", "Sphere", listOf("radius" to "Radius (r)"), "sphere"),
        Shape3D("cube", "Cube", listOf("side" to "Side (a)"), "cube"),
        Shape3D("cuboid", "Cuboid", listOf("length" to "Length (l)", "width" to "Width (w)", "height" to "Height (h)"), "cuboid"),
        Shape3D("cylinder", "Cylinder", listOf("radius" to "Radius (r)", "height" to "Height (h)"), "cylinder"),
        Shape3D("cone", "Cone", listOf("radius" to "Radius (r)", "height" to "Height (h)"), "cone"),
        Shape3D("pyramid", "Pyramid", listOf("base" to "Base side (b)", "height" to "Height (h)"), "pyramid")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shapes_3d)

        accentColor = ContextCompat.getColor(this, R.color.category_calculator)

        shapeDrawView = findViewById(R.id.shape_draw_view)
        layoutInputs = findViewById(R.id.layout_inputs)
        layoutResults = findViewById(R.id.layout_results)
        cardResult = findViewById(R.id.card_result)
        layoutChips = findViewById(R.id.layout_shape_chips)

        findViewById<ImageButton>(R.id.btn_close).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_calculate).setOnClickListener { calculate() }

        setupShapeChips()
        selectShape("sphere")
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
                setTextColor(ContextCompat.getColor(this@Shapes3DActivity, R.color.text_primary))
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
                setTextColor(ContextCompat.getColor(this@Shapes3DActivity, R.color.text_primary))
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
            "sphere" -> {
                val r = getInput("radius") ?: return showError("Enter the radius")
                results.add("Volume" to df.format((4.0 / 3.0) * PI * r.pow(3)))
                results.add("Surface Area" to df.format(4 * PI * r * r))
                results.add("Diameter" to df.format(2 * r))
                results.add("Great Circle Area" to df.format(PI * r * r))
                results.add("Circumference" to df.format(2 * PI * r))
            }
            "cube" -> {
                val a = getInput("side") ?: return showError("Enter the side length")
                results.add("Volume" to df.format(a.pow(3)))
                results.add("Surface Area" to df.format(6 * a * a))
                results.add("Face Diagonal" to df.format(a * sqrt(2.0)))
                results.add("Space Diagonal" to df.format(a * sqrt(3.0)))
                results.add("Edge Length Total" to df.format(12 * a))
            }
            "cuboid" -> {
                val l = getInput("length") ?: return showError("Enter the length")
                val w = getInput("width") ?: return showError("Enter the width")
                val h = getInput("height") ?: return showError("Enter the height")
                results.add("Volume" to df.format(l * w * h))
                results.add("Surface Area" to df.format(2 * (l * w + w * h + h * l)))
                results.add("Space Diagonal" to df.format(sqrt(l * l + w * w + h * h)))
                results.add("Lateral Surface Area" to df.format(2 * h * (l + w)))
                results.add("Base Area" to df.format(l * w))
            }
            "cylinder" -> {
                val r = getInput("radius") ?: return showError("Enter the radius")
                val h = getInput("height") ?: return showError("Enter the height")
                val slant = sqrt(r * r + h * h)
                results.add("Volume" to df.format(PI * r * r * h))
                results.add("Total Surface Area" to df.format(2 * PI * r * (r + h)))
                results.add("Lateral Surface Area" to df.format(2 * PI * r * h))
                results.add("Base Area" to df.format(PI * r * r))
                results.add("Base Circumference" to df.format(2 * PI * r))
            }
            "cone" -> {
                val r = getInput("radius") ?: return showError("Enter the radius")
                val h = getInput("height") ?: return showError("Enter the height")
                val slant = sqrt(r * r + h * h)
                results.add("Volume" to df.format((1.0 / 3.0) * PI * r * r * h))
                results.add("Total Surface Area" to df.format(PI * r * (r + slant)))
                results.add("Lateral Surface Area" to df.format(PI * r * slant))
                results.add("Slant Height" to df.format(slant))
                results.add("Base Area" to df.format(PI * r * r))
            }
            "pyramid" -> {
                val b = getInput("base") ?: return showError("Enter the base side")
                val h = getInput("height") ?: return showError("Enter the height")
                val slant = sqrt(h * h + (b / 2) * (b / 2))
                results.add("Volume" to df.format((1.0 / 3.0) * b * b * h))
                results.add("Total Surface Area" to df.format(b * b + 2 * b * slant))
                results.add("Lateral Surface Area" to df.format(2 * b * slant))
                results.add("Slant Height" to df.format(slant))
                results.add("Base Area" to df.format(b * b))
                results.add("Base Perimeter" to df.format(4 * b))
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
                setTextColor(ContextCompat.getColor(this@Shapes3DActivity, R.color.calc_display_text_secondary))
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvValue = TextView(this).apply {
                text = value
                setTextColor(ContextCompat.getColor(this@Shapes3DActivity, R.color.calc_display_text_primary))
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
                    setBackgroundColor(ContextCompat.getColor(this@Shapes3DActivity, R.color.calc_divider))
                }
                layoutResults.addView(divider)
            }
        }
        cardResult.visibility = View.VISIBLE
        setResult(RESULT_OK)   // ← mark as completed
    }
}
