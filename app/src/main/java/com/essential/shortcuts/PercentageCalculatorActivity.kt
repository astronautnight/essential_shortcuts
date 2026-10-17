package com.essential.shortcuts

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import java.text.DecimalFormat

class PercentageCalculatorActivity : AppCompatActivity() {

    private lateinit var etPercentage: TextInputEditText
    private lateinit var etValue: TextInputEditText
    private lateinit var btnCalculate: Button
    private lateinit var btnClose: Button
    private lateinit var cardResult: MaterialCardView
    private lateinit var tvResultValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_percentage_calculator)

        initViews()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun initViews() {
        etPercentage = findViewById(R.id.et_percentage)
        etValue = findViewById(R.id.et_value)
        btnCalculate = findViewById(R.id.btn_calculate)
        btnClose = findViewById(R.id.btn_close)
        cardResult = findViewById(R.id.card_result)
        tvResultValue = findViewById(R.id.tv_result_value)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        btnCalculate.setOnClickListener {
            val percStr = etPercentage.text.toString()
            val valStr = etValue.text.toString()

            if (percStr.isEmpty() || valStr.isEmpty()) {
                Toast.makeText(this, "Please fill both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val percentage = percStr.toDouble()
                val value = valStr.toDouble()

                val result = (percentage / 100.0) * value

                val df = DecimalFormat("#.####")
                tvResultValue.text = df.format(result)
                cardResult.visibility = View.VISIBLE
                setResult(RESULT_OK)   // ← mark as completed

                // Hide keyboard
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
                
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Invalid numbers entered", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
