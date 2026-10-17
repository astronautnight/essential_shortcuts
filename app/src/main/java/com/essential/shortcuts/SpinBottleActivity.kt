package com.essential.shortcuts

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SpinBottleActivity : AppCompatActivity() {

    private lateinit var btnClose: ImageButton
    private lateinit var ivBottle: ImageView
    private lateinit var btnSpin: Button
    private lateinit var tvDirection: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spin_bottle)
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
        ivBottle = findViewById(R.id.iv_bottle)
        btnSpin = findViewById(R.id.btn_spin)
        tvDirection = findViewById(R.id.tv_direction)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        btnSpin.setOnClickListener {
            btnSpin.isEnabled = false
            val randomDegrees = (360 * 4) + (Math.random() * 360).toFloat()

            val animator = ObjectAnimator.ofFloat(
                ivBottle, "rotation",
                ivBottle.rotation,
                ivBottle.rotation + randomDegrees.toFloat()
            )
            animator.duration = 2500
            animator.interpolator = DecelerateInterpolator(2.5f)
            animator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    btnSpin.isEnabled = true
                    val finalAngle = ivBottle.rotation % 360
                    
                    // Recalibrated for bottle where 0 degrees = straight up (North)
                    val direction = when {
                        finalAngle < 22.5 || finalAngle >= 337.5 -> "⬆️ North"
                        finalAngle < 67.5 -> "↗️ North-East"
                        finalAngle < 112.5 -> "➡️ East"
                        finalAngle < 157.5 -> "↘️ South-East"
                        finalAngle < 202.5 -> "⬇️ South"
                        finalAngle < 247.5 -> "↙️ South-West"
                        finalAngle < 292.5 -> "⬅️ West"
                        else -> "↖️ North-West"
                    }
                    tvDirection.text = direction
                    setResult(RESULT_OK)   // ← mark as completed
                }
            })
            animator.start()
        }
    }
}
