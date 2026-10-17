package com.essential.shortcuts

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.ViewGroup
import android.view.animation.BounceInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class RollDiceActivity : AppCompatActivity() {

    private lateinit var btnClose: ImageButton
    private lateinit var tvDice1: TextView
    private lateinit var tvDice2: TextView
    private lateinit var tvDice3: TextView
    private lateinit var btnRoll: Button
    private lateinit var btnDiceCount1: TextView
    private lateinit var btnDiceCount2: TextView
    private lateinit var btnDiceCount3: TextView
    private lateinit var tvTotal: TextView
    private lateinit var dice2Container: LinearLayout
    private lateinit var dice3Container: LinearLayout

    private var diceCount = 1
    private val diceFaces = arrayOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_roll_dice)
        initViews()
        setupListeners()
        updateDiceVisibility()
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun initViews() {
        btnClose = findViewById(R.id.btn_close)
        tvDice1 = findViewById(R.id.tv_dice_1)
        tvDice2 = findViewById(R.id.tv_dice_2)
        tvDice3 = findViewById(R.id.tv_dice_3)
        btnRoll = findViewById(R.id.btn_roll)
        btnDiceCount1 = findViewById(R.id.btn_dice_count_1)
        btnDiceCount2 = findViewById(R.id.btn_dice_count_2)
        btnDiceCount3 = findViewById(R.id.btn_dice_count_3)
        tvTotal = findViewById(R.id.tv_total)
        dice2Container = findViewById(R.id.dice_2_container)
        dice3Container = findViewById(R.id.dice_3_container)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        btnDiceCount1.setOnClickListener {
            diceCount = 1
            updateDiceVisibility()
            updateDiceCountButtons()
        }
        btnDiceCount2.setOnClickListener {
            diceCount = 2
            updateDiceVisibility()
            updateDiceCountButtons()
        }
        btnDiceCount3.setOnClickListener {
            diceCount = 3
            updateDiceVisibility()
            updateDiceCountButtons()
        }

        btnRoll.setOnClickListener {
            btnRoll.isEnabled = false
            var total = 0

            val diceViews = mutableListOf(tvDice1)
            if (diceCount >= 2) diceViews.add(tvDice2)
            if (diceCount >= 3) diceViews.add(tvDice3)

            for (diceView in diceViews) {
                val value = (1..6).random()
                total += value

                // Bounce animation
                val bounceAnim = ObjectAnimator.ofFloat(diceView, "translationY", 0f, -60f, 0f)
                bounceAnim.duration = 600
                bounceAnim.interpolator = BounceInterpolator()

                // Spin animation
                val spinAnim = ObjectAnimator.ofFloat(diceView, "rotationY", 0f, 720f)
                spinAnim.duration = 600

                diceView.text = diceFaces[value - 1]
                bounceAnim.start()
                spinAnim.start()
            }

            tvTotal.postDelayed({
                tvTotal.text = "Total: $total"
                setResult(RESULT_OK)   // ← mark as completed
                btnRoll.isEnabled = true
            }, 650)
        }

        updateDiceCountButtons()
    }

    private fun updateDiceVisibility() {
        dice2Container.visibility = if (diceCount >= 2) LinearLayout.VISIBLE else LinearLayout.GONE
        dice3Container.visibility = if (diceCount >= 3) LinearLayout.VISIBLE else LinearLayout.GONE
    }

    private fun updateDiceCountButtons() {
        val selectedAlpha = 1.0f
        val unselectedAlpha = 0.4f
        btnDiceCount1.alpha = if (diceCount == 1) selectedAlpha else unselectedAlpha
        btnDiceCount2.alpha = if (diceCount == 2) selectedAlpha else unselectedAlpha
        btnDiceCount3.alpha = if (diceCount == 3) selectedAlpha else unselectedAlpha
    }
}
