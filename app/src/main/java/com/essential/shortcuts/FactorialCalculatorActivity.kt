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
import java.math.BigInteger
import kotlinx.coroutines.*

class FactorialCalculatorActivity : AppCompatActivity() {

    private lateinit var etNumber: TextInputEditText
    private lateinit var btnCalculate: Button
    private lateinit var btnClose: Button
    private lateinit var cardResult: MaterialCardView
    private lateinit var tvResultValue: TextView

    private var calculationJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_factorial_calculator)

        initViews()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDestroy() {
        super.onDestroy()
        calculationJob?.cancel()
    }

    private fun initViews() {
        etNumber = findViewById(R.id.et_number)
        btnCalculate = findViewById(R.id.btn_calculate)
        btnClose = findViewById(R.id.btn_close)
        cardResult = findViewById(R.id.card_result)
        tvResultValue = findViewById(R.id.tv_result_value)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        btnCalculate.setOnClickListener {
            val numStr = etNumber.text.toString()

            if (numStr.isEmpty()) {
                Toast.makeText(this, "Please enter a number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val num = numStr.toInt()

                if (num < 0) {
                    Toast.makeText(this, "Factorial is not defined for negative numbers", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (num > 50000) {
                    Toast.makeText(this, "Number is too large, please enter a number up to 50000", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                // Hide keyboard
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
                
                // Show calculating state
                cardResult.visibility = View.VISIBLE
                setResult(RESULT_OK)   // ← mark as completed
                tvResultValue.text = "Calculating..."
                btnCalculate.isEnabled = false

                // Calculate in background to avoid freezing UI for large numbers
                calculationJob?.cancel()
                calculationJob = CoroutineScope(Dispatchers.Main).launch {
                    val result = withContext(Dispatchers.Default) {
                        calculateFactorial(num)
                    }
                    tvResultValue.text = result
                    btnCalculate.isEnabled = true
                }
                
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Invalid number entered", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculateFactorial(n: Int): String {
        var result = BigInteger.ONE
        for (i in 2..n) {
            result = result.multiply(BigInteger.valueOf(i.toLong()))
        }
        return result.toString()
    }
}
