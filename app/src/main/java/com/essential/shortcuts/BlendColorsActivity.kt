package com.essential.shortcuts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class BlendColorsActivity : AppCompatActivity() {

    private lateinit var viewBlendResult: View
    private lateinit var containerColors: LinearLayout
    private lateinit var btnAddColor: Button
    
    private lateinit var tvHexCode: TextView
    private lateinit var tvRgbCode: TextView
    private lateinit var btnCopyHex: Button
    private lateinit var btnCopyRgb: Button
    private lateinit var btnClose: Button

    private data class ColorInput(
        val view: View,
        val etHex: TextInputEditText,
        val viewIndicator: View,
        val seekWeight: SeekBar,
        val tvWeight: TextView,
        val btnRemove: ImageButton,
        var parsedColor: Int = Color.BLACK,
        var isValid: Boolean = true
    )

    private val colorInputs = mutableListOf<ColorInput>()
    private val maxColors = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blend_colors)

        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        viewBlendResult = findViewById(R.id.view_blend_result)
        containerColors = findViewById(R.id.container_colors)
        btnAddColor = findViewById(R.id.btn_add_color)
        tvHexCode = findViewById(R.id.tv_hex_code)
        tvRgbCode = findViewById(R.id.tv_rgb_code)
        btnCopyHex = findViewById(R.id.btn_copy_hex)
        btnCopyRgb = findViewById(R.id.btn_copy_rgb)
        btnClose = findViewById(R.id.btn_close)

        btnAddColor.setOnClickListener {
            if (colorInputs.size < maxColors) {
                addColorRow("#FFFFFF")
                updateBlend()
                updateAddButtonState()
                updateRemoveButtonsState()
            }
        }

        btnCopyHex.setOnClickListener {
            copyToClipboard("Blended HEX", tvHexCode.text.toString())
        }
        btnCopyRgb.setOnClickListener {
            copyToClipboard("Blended RGB", tvRgbCode.text.toString())
        }
        btnClose.setOnClickListener { finish() }

        // Initial setup with 2 colors
        addColorRow("#FF0000")
        addColorRow("#0000FF")
        updateBlend()
        updateAddButtonState()
        updateRemoveButtonsState()
    }

    private fun addColorRow(initialHex: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_blend_color_row, containerColors, false)
        val etHex = view.findViewById<TextInputEditText>(R.id.et_hex)
        val viewIndicator = view.findViewById<View>(R.id.view_indicator)
        val seekWeight = view.findViewById<SeekBar>(R.id.seek_weight)
        val tvWeight = view.findViewById<TextView>(R.id.tv_weight)
        val btnRemove = view.findViewById<ImageButton>(R.id.btn_remove)

        val inputData = ColorInput(view, etHex, viewIndicator, seekWeight, tvWeight, btnRemove)
        colorInputs.add(inputData)
        containerColors.addView(view)

        etHex.setText(initialHex)
        syncInputColor(inputData)

        etHex.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                syncInputColor(inputData)
                updateBlend()
            }
        })

        seekWeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvWeight.text = "$progress%"
                updateBlend()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        btnRemove.setOnClickListener {
            if (colorInputs.size > 2) {
                containerColors.removeView(view)
                colorInputs.remove(inputData)
                updateBlend()
                updateAddButtonState()
                updateRemoveButtonsState()
            } else {
                Toast.makeText(this, "Minimum 2 colors required to blend", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun syncInputColor(input: ColorInput) {
        val s = input.etHex.text.toString().trim()
        try {
            if (s.length in 4..9) {
                val formatted = if (!s.startsWith("#")) "#$s" else s
                input.parsedColor = Color.parseColor(formatted)
                input.viewIndicator.setBackgroundColor(input.parsedColor)
                input.isValid = true
            } else {
                input.isValid = false
            }
        } catch (e: Exception) {
            input.isValid = false
        }
    }

    private fun updateAddButtonState() {
        if (colorInputs.size >= maxColors) {
            btnAddColor.isEnabled = false
            btnAddColor.text = "Maximum Colors Reached"
        } else {
            btnAddColor.isEnabled = true
            btnAddColor.text = "+ Add Another Color"
        }
    }

    private fun updateRemoveButtonsState() {
        val canRemove = colorInputs.size > 2
        for (input in colorInputs) {
            input.btnRemove.alpha = if (canRemove) 1.0f else 0.3f
            input.btnRemove.isEnabled = canRemove
        }
    }

    private fun updateBlend() {
        var totalWeight = 0f
        var totalR = 0f
        var totalG = 0f
        var totalB = 0f
        var totalA = 0f

        for (input in colorInputs) {
            if (input.isValid) {
                val weight = input.seekWeight.progress.toFloat()
                if (weight > 0) {
                    val color = input.parsedColor
                    totalA += Color.alpha(color) * weight
                    totalR += Color.red(color) * weight
                    totalG += Color.green(color) * weight
                    totalB += Color.blue(color) * weight
                    totalWeight += weight
                }
            }
        }

        val blendedColor = if (totalWeight > 0) {
            val a = (totalA / totalWeight).toInt().coerceIn(0, 255)
            val r = (totalR / totalWeight).toInt().coerceIn(0, 255)
            val g = (totalG / totalWeight).toInt().coerceIn(0, 255)
            val b = (totalB / totalWeight).toInt().coerceIn(0, 255)
            Color.argb(a, r, g, b)
        } else {
            Color.TRANSPARENT // if all weights are 0
        }

        viewBlendResult.setBackgroundColor(blendedColor)
        
        val a = Color.alpha(blendedColor)
        val r = Color.red(blendedColor)
        val g = Color.green(blendedColor)
        val b = Color.blue(blendedColor)

        val hexFormat = if (a == 255) {
            String.format("#%06X", (0xFFFFFF and blendedColor))
        } else {
            String.format("#%08X", blendedColor)
        }
        
        if (totalWeight > 0) {
            tvHexCode.text = hexFormat
            tvRgbCode.text = "rgb($r, $g, $b)"
        } else {
            tvHexCode.text = "N/A"
            tvRgbCode.text = "N/A"
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        if (text == "N/A") return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied $text to clipboard", Toast.LENGTH_SHORT).show()
    }
}
