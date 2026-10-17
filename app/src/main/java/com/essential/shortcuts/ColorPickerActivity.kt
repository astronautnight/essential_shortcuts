package com.essential.shortcuts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class ColorPickerActivity : AppCompatActivity() {

    private lateinit var viewColorSwatch: View
    private lateinit var seekRed: SeekBar
    private lateinit var seekGreen: SeekBar
    private lateinit var seekBlue: SeekBar
    private lateinit var tvRedVal: TextView
    private lateinit var tvGreenVal: TextView
    private lateinit var tvBlueVal: TextView

    private lateinit var seekHue: SeekBar
    private lateinit var seekSat: SeekBar
    private lateinit var seekVal: SeekBar
    private lateinit var tvHueVal: TextView
    private lateinit var tvSatVal: TextView
    private lateinit var tvValVal: TextView

    private lateinit var etHexInput: TextInputEditText
    private lateinit var tvHexCode: TextView
    private lateinit var tvRgbCode: TextView
    private lateinit var btnCopyHex: Button
    private lateinit var btnCopyRgb: Button
    private lateinit var btnClose: Button

    private var currentRed = 0
    private var currentGreen = 0
    private var currentBlue = 0

    private var currentHue = 0f
    private var currentSat = 0f
    private var currentVal = 0f

    private var isUpdatingFromRGB = false
    private var isUpdatingFromHSV = false
    private var isUpdatingFromHex = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_picker)

        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        viewColorSwatch = findViewById(R.id.view_color_swatch)
        seekRed = findViewById(R.id.seek_red)
        seekGreen = findViewById(R.id.seek_green)
        seekBlue = findViewById(R.id.seek_blue)
        tvRedVal = findViewById(R.id.tv_red_val)
        tvGreenVal = findViewById(R.id.tv_green_val)
        tvBlueVal = findViewById(R.id.tv_blue_val)

        seekHue = findViewById(R.id.seek_hue)
        seekSat = findViewById(R.id.seek_sat)
        seekVal = findViewById(R.id.seek_val)
        tvHueVal = findViewById(R.id.tv_hue_val)
        tvSatVal = findViewById(R.id.tv_sat_val)
        tvValVal = findViewById(R.id.tv_val_val)

        etHexInput = findViewById(R.id.et_hex_input)
        tvHexCode = findViewById(R.id.tv_hex_code)
        tvRgbCode = findViewById(R.id.tv_rgb_code)
        btnCopyHex = findViewById(R.id.btn_copy_hex)
        btnCopyRgb = findViewById(R.id.btn_copy_rgb)
        btnClose = findViewById(R.id.btn_close)

        // Pre-set default
        currentRed = 82
        currentGreen = 113
        currentBlue = 255
        
        updateFromRGB()

        val rgbSeekBarListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser || isUpdatingFromHSV || isUpdatingFromHex) return
                when (seekBar?.id) {
                    R.id.seek_red -> currentRed = progress
                    R.id.seek_green -> currentGreen = progress
                    R.id.seek_blue -> currentBlue = progress
                }
                updateFromRGB()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
        
        seekRed.setOnSeekBarChangeListener(rgbSeekBarListener)
        seekGreen.setOnSeekBarChangeListener(rgbSeekBarListener)
        seekBlue.setOnSeekBarChangeListener(rgbSeekBarListener)

        val hsvSeekBarListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser || isUpdatingFromRGB || isUpdatingFromHex) return
                when (seekBar?.id) {
                    R.id.seek_hue -> currentHue = progress.toFloat()
                    R.id.seek_sat -> currentSat = progress / 100f
                    R.id.seek_val -> currentVal = progress / 100f
                }
                updateFromHSV()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        seekHue.setOnSeekBarChangeListener(hsvSeekBarListener)
        seekSat.setOnSeekBarChangeListener(hsvSeekBarListener)
        seekVal.setOnSeekBarChangeListener(hsvSeekBarListener)

        etHexInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingFromRGB || isUpdatingFromHSV) return
                val hexStr = s.toString().trim()
                if (hexStr.length in 4..9) {
                    try {
                        val formattedHex = if (!hexStr.startsWith("#")) "#$hexStr" else hexStr
                        val color = Color.parseColor(formattedHex)
                        currentRed = Color.red(color)
                        currentGreen = Color.green(color)
                        currentBlue = Color.blue(color)
                        updateFromHex()
                    } catch (e: Exception) {
                        // ignore invalid partial inputs
                    }
                }
            }
        })

        btnCopyHex.setOnClickListener {
            copyToClipboard("HEX Color", tvHexCode.text.toString())
        }
        btnCopyRgb.setOnClickListener {
            copyToClipboard("RGB Color", tvRgbCode.text.toString())
        }
        btnClose.setOnClickListener {
            finish()
        }
    }

    private fun updateFromRGB() {
        isUpdatingFromRGB = true
        
        val hsv = FloatArray(3)
        Color.RGBToHSV(currentRed, currentGreen, currentBlue, hsv)
        currentHue = hsv[0]
        currentSat = hsv[1]
        currentVal = hsv[2]
        
        updateUI()
        isUpdatingFromRGB = false
    }

    private fun updateFromHSV() {
        isUpdatingFromHSV = true
        
        val color = Color.HSVToColor(floatArrayOf(currentHue, currentSat, currentVal))
        currentRed = Color.red(color)
        currentGreen = Color.green(color)
        currentBlue = Color.blue(color)

        updateUI()
        isUpdatingFromHSV = false
    }

    private fun updateFromHex() {
        isUpdatingFromHex = true
        val hsv = FloatArray(3)
        Color.RGBToHSV(currentRed, currentGreen, currentBlue, hsv)
        currentHue = hsv[0]
        currentSat = hsv[1]
        currentVal = hsv[2]
        updateUI(skipHexInputUpdate = true)
        isUpdatingFromHex = false
    }

    private fun updateUI(skipHexInputUpdate: Boolean = false) {
        seekRed.progress = currentRed
        seekGreen.progress = currentGreen
        seekBlue.progress = currentBlue
        tvRedVal.text = currentRed.toString()
        tvGreenVal.text = currentGreen.toString()
        tvBlueVal.text = currentBlue.toString()

        seekHue.progress = currentHue.toInt()
        seekSat.progress = (currentSat * 100).toInt()
        seekVal.progress = (currentVal * 100).toInt()
        
        tvHueVal.text = "${currentHue.toInt()}°"
        tvSatVal.text = "${(currentSat * 100).toInt()}%"
        tvValVal.text = "${(currentVal * 100).toInt()}%"

        val color = Color.rgb(currentRed, currentGreen, currentBlue)
        viewColorSwatch.setBackgroundColor(color)

        val hexFormat = String.format("#%06X", (0xFFFFFF and color))
        tvHexCode.text = hexFormat
        tvRgbCode.text = "rgb($currentRed, $currentGreen, $currentBlue)"

        if (!skipHexInputUpdate) {
            isUpdatingFromRGB = true // Temporarily suppress watcher
            val selStart = etHexInput.selectionStart
            val selEnd = etHexInput.selectionEnd
            
            // Only update hex field if it doesn't match roughly to avoid jumping text
            val currentText = etHexInput.text.toString()
            val parsedTextHex = try {
                String.format("#%06X", (0xFFFFFF and Color.parseColor(if (currentText.startsWith("#")) currentText else "#$currentText")))
            } catch (e: Exception) { "" }
            
            if (parsedTextHex != hexFormat) {
                etHexInput.setText(hexFormat)
                try {
                    etHexInput.setSelection(hexFormat.length)
                } catch(e: Exception) {}
            }
            
            isUpdatingFromRGB = false
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied $text to clipboard", Toast.LENGTH_SHORT).show()
    }
}
