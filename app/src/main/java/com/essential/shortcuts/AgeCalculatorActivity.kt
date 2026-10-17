package com.essential.shortcuts

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class AgeCalculatorActivity : AppCompatActivity() {

    private lateinit var etDob: TextInputEditText
    private lateinit var btnCalculate: Button
    private lateinit var btnClose: Button
    private lateinit var cardResult: MaterialCardView
    private lateinit var tvResultAge: TextView

    private val calendar = Calendar.getInstance()
    private var selectedDob: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_age_calculator)

        initViews()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        // Width 90% logic from QR Code popup
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun initViews() {
        etDob = findViewById(R.id.et_dob)
        btnCalculate = findViewById(R.id.btn_calculate)
        btnClose = findViewById(R.id.btn_close)
        cardResult = findViewById(R.id.card_result)
        tvResultAge = findViewById(R.id.tv_result_age)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        etDob.setOnClickListener {
            showDatePicker()
        }

        btnCalculate.setOnClickListener {
            if (selectedDob == null) {
                Toast.makeText(this, "Please select your date of birth", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            calculateAge()
        }
    }

    private fun showDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            
            selectedDob = calendar.clone() as Calendar

            val myFormat = "dd-MM-yyyy" // Example format
            val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
            etDob.setText(sdf.format(calendar.time))
        }

        val dialog = DatePickerDialog(
            this, dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        // Ensure they cannot pick a future date of birth
        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.show()
    }

    private fun calculateAge() {
        val today = Calendar.getInstance()
        val dob = selectedDob ?: return

        var years = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        var months = today.get(Calendar.MONTH) - dob.get(Calendar.MONTH)
        var days = today.get(Calendar.DAY_OF_MONTH) - dob.get(Calendar.DAY_OF_MONTH)

        if (days < 0) {
            val maxDayOfMonth = today.getActualMaximum(Calendar.DAY_OF_MONTH)
            days += maxDayOfMonth
            months--

        }

        if (months < 0) {
            months += 12
            years--
        }

        tvResultAge.text = "$years Years, $months Months, $days Days"
        cardResult.visibility = View.VISIBLE
        setResult(RESULT_OK)   // ← mark as completed
    }
}
