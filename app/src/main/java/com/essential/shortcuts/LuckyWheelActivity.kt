package com.essential.shortcuts

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class LuckyWheelActivity : AppCompatActivity() {

    private lateinit var btnClose: ImageButton
    private lateinit var wheelView: WheelCanvasView
    private lateinit var btnSpin: Button
    private lateinit var tvResult: TextView
    private lateinit var etItems: TextInputEditText
    private lateinit var btnSetItems: Button

    private var items = mutableListOf("Option 1", "Option 2", "Option 3", "Option 4", "Option 5", "Option 6")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lucky_wheel)
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
        wheelView = findViewById(R.id.wheel_view)
        btnSpin = findViewById(R.id.btn_spin)
        tvResult = findViewById(R.id.tv_result)
        etItems = findViewById(R.id.et_items)
        btnSetItems = findViewById(R.id.btn_set_items)

        etItems.setText(items.joinToString(", "))
        wheelView.setItems(items)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        btnSetItems.setOnClickListener {
            val text = etItems.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "Please enter items separated by commas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            items = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            if (items.size < 2) {
                Toast.makeText(this, "Enter at least 2 items", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            wheelView.setItems(items)
            tvResult.text = ""
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(etItems.windowToken, 0)
        }

        btnSpin.setOnClickListener {
            if (items.size < 2) {
                Toast.makeText(this, "Add at least 2 items", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            btnSpin.isEnabled = false
            val randomDegrees = (360 * 5) + (Math.random() * 360).toFloat()
            val animator = ObjectAnimator.ofFloat(wheelView, "rotation", wheelView.rotation, wheelView.rotation + randomDegrees.toFloat())
            animator.duration = 3000
            animator.interpolator = DecelerateInterpolator(2f)
            animator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    btnSpin.isEnabled = true
                    val finalRotation = wheelView.rotation % 360
                    val segmentAngle = 360f / items.size
                    val pointerAngle = (360 - finalRotation % 360) % 360
                    val index = (pointerAngle / segmentAngle).toInt() % items.size
                    tvResult.text = "🎯  ${items[index]}"
                    tvResult.visibility = View.VISIBLE
                }
            })
            animator.start()
        }
    }
}
