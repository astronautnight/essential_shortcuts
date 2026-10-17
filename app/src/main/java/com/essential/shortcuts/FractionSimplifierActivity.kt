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

class FractionSimplifierActivity : AppCompatActivity() {

    private lateinit var etNumerator: TextInputEditText
    private lateinit var etDenominator: TextInputEditText
    private lateinit var btnSimplify: Button
    private lateinit var btnClose: Button
    private lateinit var cardResult: MaterialCardView
    private lateinit var tvResultValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fraction_simplifier)

        initViews()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun initViews() {
        etNumerator = findViewById(R.id.et_numerator)
        etDenominator = findViewById(R.id.et_denominator)
        btnSimplify = findViewById(R.id.btn_simplify)
        btnClose = findViewById(R.id.btn_close)
        cardResult = findViewById(R.id.card_result)
        tvResultValue = findViewById(R.id.tv_result_value)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        btnSimplify.setOnClickListener {
            val numStr = etNumerator.text.toString()
            val denStr = etDenominator.text.toString()

            if (numStr.isEmpty() || denStr.isEmpty()) {
                Toast.makeText(this, "Please fill both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val num = numStr.toLong()
                val den = denStr.toLong()

                if (den == 0L) {
                    Toast.makeText(this, "Denominator cannot be zero", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val gcd = findGCD(Math.abs(num), Math.abs(den))
                val simplifiedNum = num / gcd
                val simplifiedDen = den / gcd

                // Handle signs
                val finalNum = if (simplifiedDen < 0) -simplifiedNum else simplifiedNum
                val finalDen = Math.abs(simplifiedDen)

                if (finalDen == 1L) {
                    tvResultValue.text = "$finalNum"
                } else {
                    tvResultValue.text = "$finalNum/$finalDen"
                }
                
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

    private fun findGCD(a: Long, b: Long): Long {
        var num1 = a
        var num2 = b
        while (num2 != 0L) {
            val temp = num2
            num2 = num1 % num2
            num1 = temp
        }
        return num1
    }
}
