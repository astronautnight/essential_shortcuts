package com.essential.shortcuts

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class FlipCoinActivity : AppCompatActivity() {

    private lateinit var btnClose: ImageButton
    private lateinit var ivCoinFace: ImageView
    private lateinit var btnFlip: Button
    private lateinit var tvHeadsCount: TextView
    private lateinit var tvTailsCount: TextView

    private var headsCount = 0
    private var tailsCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flip_coin)
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
        ivCoinFace = findViewById(R.id.iv_coin_face)
        btnFlip = findViewById(R.id.btn_flip)
        tvHeadsCount = findViewById(R.id.tv_heads_count)
        tvTailsCount = findViewById(R.id.tv_tails_count)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        btnFlip.setOnClickListener {
            btnFlip.isEnabled = false

            // Set camera distance for a true 3D perspective effect during rotation
            val distance = 8000f
            val scale = resources.displayMetrics.density
            ivCoinFace.cameraDistance = distance * scale

            val isHeads = Math.random() < 0.5
            val duration = 1200L

            // 1. Rotation animation (multiple spins)
            val rotateX = ObjectAnimator.ofFloat(ivCoinFace, "rotationX", 0f, 1800f)
            rotateX.duration = duration
            rotateX.interpolator = AccelerateDecelerateInterpolator()

            // 2. Fly up and down animation
            val flyUp = ObjectAnimator.ofFloat(ivCoinFace, "translationY", 0f, -400f)
            flyUp.duration = duration / 2
            flyUp.interpolator = DecelerateInterpolator()

            val flyDown = ObjectAnimator.ofFloat(ivCoinFace, "translationY", -400f, 0f)
            flyDown.duration = duration / 2
            flyDown.interpolator = AccelerateDecelerateInterpolator()

            // 3. Scale animation (give a sense of height/closeness)
            val scaleXUp = ObjectAnimator.ofFloat(ivCoinFace, "scaleX", 1.0f, 1.5f)
            val scaleYUp = ObjectAnimator.ofFloat(ivCoinFace, "scaleY", 1.0f, 1.5f)
            scaleXUp.duration = duration / 2
            scaleYUp.duration = duration / 2

            val scaleXDown = ObjectAnimator.ofFloat(ivCoinFace, "scaleX", 1.5f, 1.0f)
            val scaleYDown = ObjectAnimator.ofFloat(ivCoinFace, "scaleY", 1.5f, 1.0f)
            scaleXDown.duration = duration / 2
            scaleYDown.duration = duration / 2

            // Combine animations
            val flyUpSet = AnimatorSet().apply {
                playTogether(flyUp, scaleXUp, scaleYUp)
            }
            val flyDownSet = AnimatorSet().apply {
                playTogether(flyDown, scaleXDown, scaleYDown)
            }

            val fullSet = AnimatorSet().apply {
                play(rotateX)
                play(flyUpSet).before(flyDownSet)
                
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        ivCoinFace.rotationX = 0f
                        ivCoinFace.translationY = 0f
                        ivCoinFace.scaleX = 1.0f
                        ivCoinFace.scaleY = 1.0f
                        
                        if (isHeads) {
                            ivCoinFace.setImageResource(R.drawable.ic_coin_heads)
                            headsCount++
                        } else {
                            ivCoinFace.setImageResource(R.drawable.ic_coin_tails)
                            tailsCount++
                        }
                        tvHeadsCount.text = "Heads: $headsCount"
                        tvTailsCount.text = "Tails: $tailsCount"
                        setResult(RESULT_OK)   // ← mark as completed
                        btnFlip.isEnabled = true
                    }
                })
            }

            fullSet.start()
        }
    }
}
