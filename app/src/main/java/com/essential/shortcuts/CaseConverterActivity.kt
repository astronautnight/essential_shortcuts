package com.essential.shortcuts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class CaseConverterActivity : AppCompatActivity() {

    private lateinit var btnClose: ImageButton
    private lateinit var tilInput: TextInputLayout
    private lateinit var etInput: TextInputEditText
    private lateinit var btnLowercase: MaterialCardView
    private lateinit var btnUppercase: MaterialCardView
    private lateinit var btnInvert: MaterialCardView
    private lateinit var cardResult: MaterialCardView
    private lateinit var tvResultLabel: TextView
    private lateinit var tvResult: TextView
    private lateinit var btnCopy: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_case_converter)
        initViews()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun initViews() {
        btnClose = findViewById(R.id.btn_close)
        tilInput = findViewById(R.id.til_input)
        etInput = findViewById(R.id.et_input)
        btnLowercase = findViewById(R.id.btn_lowercase)
        btnUppercase = findViewById(R.id.btn_uppercase)
        btnInvert = findViewById(R.id.btn_invert)
        cardResult = findViewById(R.id.card_result)
        tvResultLabel = findViewById(R.id.tv_result_label)
        tvResult = findViewById(R.id.tv_result)
        btnCopy = findViewById(R.id.btn_copy)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        btnLowercase.setOnClickListener {
            convertCase("lowercase")
        }

        btnUppercase.setOnClickListener {
            convertCase("uppercase")
        }

        btnInvert.setOnClickListener {
            convertCase("invert")
        }

        btnCopy.setOnClickListener {
            val text = tvResult.text.toString()
            if (text.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("converted_text", text))
                Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun convertCase(mode: String) {
        val input = etInput.text.toString()
        if (input.isEmpty()) {
            tilInput.error = "Please enter some text"
            return
        }
        tilInput.error = null

        val result = when (mode) {
            "lowercase" -> input.lowercase()
            "uppercase" -> input.uppercase()
            "invert" -> input.map {
                if (it.isUpperCase()) it.lowercaseChar()
                else if (it.isLowerCase()) it.uppercaseChar()
                else it
            }.joinToString("")
            else -> input
        }

        val label = when (mode) {
            "lowercase" -> "Converted to lowercase"
            "uppercase" -> "Converted to UPPERCASE"
            "invert" -> "Cases inverted"
            else -> "Result"
        }

        tvResultLabel.text = label
        tvResult.text = result
        cardResult.visibility = View.VISIBLE
        setResult(RESULT_OK)   // ← mark as completed

        // Hide keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(etInput.windowToken, 0)
    }
}
