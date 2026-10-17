package com.essential.shortcuts

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RockPaperScissorActivity : AppCompatActivity() {

    private lateinit var btnClose: ImageButton
    private lateinit var ivBtnRock: ImageView
    private lateinit var ivBtnPaper: ImageView
    private lateinit var ivBtnScissors: ImageView
    private lateinit var ivYourChoice: ImageView
    private lateinit var ivCpuChoice: ImageView
    private lateinit var tvResultText: TextView
    private lateinit var tvWins: TextView
    private lateinit var tvLosses: TextView
    private lateinit var tvDraws: TextView

    private var wins = 0
    private var losses = 0
    private var draws = 0

    private val choices = listOf(
        R.drawable.ic_rps_rock,
        R.drawable.ic_rps_paper,
        R.drawable.ic_rps_scissors
    )
    
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rps)
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
        ivBtnRock = findViewById(R.id.iv_btn_rock)
        ivBtnPaper = findViewById(R.id.iv_btn_paper)
        ivBtnScissors = findViewById(R.id.iv_btn_scissors)
        ivYourChoice = findViewById(R.id.iv_your_choice)
        ivCpuChoice = findViewById(R.id.iv_cpu_choice)
        tvResultText = findViewById(R.id.tv_result_text)
        tvWins = findViewById(R.id.tv_wins)
        tvLosses = findViewById(R.id.tv_losses)
        tvDraws = findViewById(R.id.tv_draws)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        ivBtnRock.setOnClickListener { play(0) }
        ivBtnPaper.setOnClickListener { play(1) }
        ivBtnScissors.setOnClickListener { play(2) }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        ivBtnRock.isEnabled = enabled
        ivBtnPaper.isEnabled = enabled
        ivBtnScissors.isEnabled = enabled
    }

    private fun play(playerChoice: Int) {
        if (isPlaying) return
        isPlaying = true
        setButtonsEnabled(false)
        tvResultText.text = "Opponent is picking..."

        val cpuChoice = (0..2).random()

        // Reveal Player's Choice
        ivYourChoice.animate().scaleX(0f).scaleY(0f).setDuration(100).withEndAction {
            ivYourChoice.setImageResource(choices[playerChoice])
            ivYourChoice.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
        }.start()

        // CPU Shake Animation (starts with Rock as the closed fist)
        ivCpuChoice.setImageResource(R.drawable.ic_rps_rock)
        
        // A bump animation translating Y up and down
        val shakeAnimator = ObjectAnimator.ofFloat(ivCpuChoice, "translationY", 0f, -40f, 0f)
        shakeAnimator.duration = 300
        shakeAnimator.repeatCount = 2 // 3 shakes total
        
        shakeAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // After shaking, reveal actual CPU choice
                ivCpuChoice.animate().scaleX(0f).scaleY(0f).setDuration(150).withEndAction {
                    ivCpuChoice.setImageResource(choices[cpuChoice])
                    ivCpuChoice.animate().scaleX(1f).scaleY(1f).setDuration(200).withEndAction {
                        // Determine winner
                        val result = when {
                            playerChoice == cpuChoice -> {
                                draws++
                                "🤝 It's a Draw!"
                            }
                            (playerChoice == 0 && cpuChoice == 2) ||
                            (playerChoice == 1 && cpuChoice == 0) ||
                            (playerChoice == 2 && cpuChoice == 1) -> {
                                wins++
                                "🎉 You Win!"
                            }
                            else -> {
                                losses++
                                "😢 You Lose!"
                            }
                        }

                        tvResultText.text = result
                        tvWins.text = "W: $wins"
                        tvLosses.text = "L: $losses"
                        tvDraws.text = "D: $draws"
                        setResult(RESULT_OK)   // ← mark as completed
                        
                        isPlaying = false
                        setButtonsEnabled(true)
                    }.start()
                }.start()
            }
        })
        
        shakeAnimator.start()
    }
}
