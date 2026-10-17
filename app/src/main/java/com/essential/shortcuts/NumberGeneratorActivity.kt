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

class NumberGeneratorActivity : AppCompatActivity() {

    private lateinit var btnClose: ImageButton
    private lateinit var etLower: TextInputEditText
    private lateinit var etUpper: TextInputEditText
    private lateinit var btnGenerate: Button
    private lateinit var cardResult: MaterialCardView
    private lateinit var tvResult: TextView
    private lateinit var btnCopy: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_number_generator)
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
        etLower = findViewById(R.id.et_lower)
        etUpper = findViewById(R.id.et_upper)
        btnGenerate = findViewById(R.id.btn_generate)
        cardResult = findViewById(R.id.card_result)
        tvResult = findViewById(R.id.tv_result)
        btnCopy = findViewById(R.id.btn_copy)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        btnGenerate.setOnClickListener {
            val lowerStr = etLower.text.toString().trim()
            val upperStr = etUpper.text.toString().trim()

            if (lowerStr.isEmpty() || upperStr.isEmpty()) {
                Toast.makeText(this, "Please fill both limits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val lower = lowerStr.toLongOrNull()
            val upper = upperStr.toLongOrNull()

            if (lower == null || upper == null) {
                Toast.makeText(this, "Invalid numbers", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (lower > upper) {
                Toast.makeText(this, "Lower limit must be ≤ upper limit", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val result = if (lower == upper) lower else (lower..upper).random()

            tvResult.text = result.toString()
            cardResult.visibility = View.VISIBLE
            setResult(RESULT_OK)   // ← mark as completed

            // Hide keyboard
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(etUpper.windowToken, 0)
        }

        btnCopy.setOnClickListener {
            val text = tvResult.text.toString()
            if (text.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("number", text))
                Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
