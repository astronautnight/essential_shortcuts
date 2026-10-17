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
import java.util.concurrent.TimeUnit

class DaysUntilActivity : AppCompatActivity() {

    private lateinit var etTargetDate: TextInputEditText
    private lateinit var btnCalculate: Button
    private lateinit var btnClose: Button
    private lateinit var cardResult: MaterialCardView
    private lateinit var tvResultTitle: TextView
    private lateinit var tvResultDays: TextView

    private val calendar = Calendar.getInstance()
    private var selectedTargetDate: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_days_until)

        initViews()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        // Width 90% logic mirroring QR & Age pops
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun initViews() {
        etTargetDate = findViewById(R.id.et_target_date)
        btnCalculate = findViewById(R.id.btn_calculate)
        btnClose = findViewById(R.id.btn_close)
        cardResult = findViewById(R.id.card_result)
        tvResultTitle = findViewById(R.id.tv_result_title)
        tvResultDays = findViewById(R.id.tv_result_days)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        etTargetDate.setOnClickListener {
            showDatePicker()
        }

        btnCalculate.setOnClickListener {
            if (selectedTargetDate == null) {
                Toast.makeText(this, "Please select a target date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            calculateDays()
        }
    }

    private fun showDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            // Reset time to start of day to avoid partial-day edge cases
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            selectedTargetDate = calendar.clone() as Calendar

            val myFormat = "dd-MM-yyyy"
            val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
            etTargetDate.setText(sdf.format(calendar.time))
        }

        val dialog = DatePickerDialog(
            this, dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        // No min or max dates, users can check past "since" dates or future "until" dates
        dialog.show()
    }

    private fun calculateDays() {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val target = selectedTargetDate ?: return

        // Compute difference in milliseconds
        val diffMillis = target.timeInMillis - today.timeInMillis
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

        if (diffDays > 0) {
            tvResultTitle.text = "Days until target:"
            tvResultDays.text = "$diffDays Days"
        } else if (diffDays < 0) {
            tvResultTitle.text = "Days since target:"
            tvResultDays.text = "${kotlin.math.abs(diffDays)} Days"
        } else {
            tvResultTitle.text = "Target is:"
            tvResultDays.text = "Today"
        }

        cardResult.visibility = View.VISIBLE
        setResult(RESULT_OK)   // ← mark as completed
    }
}
