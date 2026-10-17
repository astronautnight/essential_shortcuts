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

class AverageCalculatorActivity : AppCompatActivity() {

    private lateinit var etNumbers: TextInputEditText
    private lateinit var btnCalculate: Button
    private lateinit var btnClose: Button
    private lateinit var cardResult: MaterialCardView
    private lateinit var tvResultCount: TextView
    private lateinit var tvResultSum: TextView
    private lateinit var tvResultAvg: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_average_calculator)

        initViews()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun initViews() {
        etNumbers = findViewById(R.id.et_numbers)
        btnCalculate = findViewById(R.id.btn_calculate)
        btnClose = findViewById(R.id.btn_close)
        cardResult = findViewById(R.id.card_result)
        tvResultCount = findViewById(R.id.tv_result_count)
        tvResultSum = findViewById(R.id.tv_result_sum)
        tvResultAvg = findViewById(R.id.tv_result_avg)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        btnCalculate.setOnClickListener {
            val inputStr = etNumbers.text.toString().trim()
            if (inputStr.isEmpty()) {
                Toast.makeText(this, "Please enter some numbers", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                // Split by comma, remove whitespace, and parse to double
                val numbers = inputStr.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { it.toDouble() }

                if (numbers.isEmpty()) {
                    Toast.makeText(this, "No valid numbers found", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val count = numbers.size
                val sum = numbers.sum()
                val average = sum / count

                val df = DecimalFormat("#.####")

                tvResultCount.text = "Count: $count"
                tvResultSum.text = "Sum: ${df.format(sum)}"
                tvResultAvg.text = "Average: ${df.format(average)}"
                
                cardResult.visibility = View.VISIBLE
                setResult(RESULT_OK)   // ← mark as completed

                // Hide keyboard
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Invalid characters detected. Only use numbers and commas.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
