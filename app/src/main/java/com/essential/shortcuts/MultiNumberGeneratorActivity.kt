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
import com.google.android.material.textfield.TextInputEditText

class MultiNumberGeneratorActivity : AppCompatActivity() {

    private lateinit var btnClose: ImageButton
    private lateinit var etLower: TextInputEditText
    private lateinit var etUpper: TextInputEditText
    private lateinit var etCount: TextInputEditText
    private lateinit var btnGenerate: Button
    private lateinit var tvResults: TextView
    private lateinit var cardResult: View
    private lateinit var btnCopyAll: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_number_gen)
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
        etCount = findViewById(R.id.et_count)
        btnGenerate = findViewById(R.id.btn_generate)
        tvResults = findViewById(R.id.tv_results)
        cardResult = findViewById(R.id.card_result)
        btnCopyAll = findViewById(R.id.btn_copy_all)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        btnGenerate.setOnClickListener {
            val lower = etLower.text.toString().trim().toLongOrNull()
            val upper = etUpper.text.toString().trim().toLongOrNull()
            val count = etCount.text.toString().trim().toIntOrNull()

            if (lower == null || upper == null || count == null) {
                Toast.makeText(this, "Please fill all fields with valid numbers", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (lower > upper) {
                Toast.makeText(this, "Lower must be ≤ Upper", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (count < 1 || count > 100) {
                Toast.makeText(this, "Count must be between 1 and 100", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val numbers = (1..count).map { if (lower == upper) lower else (lower..upper).random() }
            tvResults.text = numbers.joinToString(", ")
            cardResult.visibility = View.VISIBLE
            setResult(RESULT_OK)   // ← mark as completed

            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(etCount.windowToken, 0)
        }

        btnCopyAll.setOnClickListener {
            val text = tvResults.text.toString()
            if (text.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("numbers", text))
                Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
