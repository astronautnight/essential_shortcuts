package com.essential.shortcuts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

class SupportActivity : AppCompatActivity() {

    // ── Replace these with your real payment links / UPI ID ──
    private val kofiUrl    = "https://ko-fi.com/rbstudioz"
    private val patreonUrl = "https://www.patreon.com/"
    private val upiId      = "rajassberde@oksbi"
    // ─────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_support)

        setupButtons()
        animateCards()
    }

    private fun setupButtons() {
        // Back button
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // Ko-fi
        findViewById<LinearLayout>(R.id.card_kofi).setOnClickListener {
            openUrl(kofiUrl)
        }

        // Patreon
        findViewById<LinearLayout>(R.id.card_patreon).setOnClickListener {
            openUrl(patreonUrl)
        }

        // UPI – copy ID to clipboard
        val cardUpi = findViewById<LinearLayout>(R.id.card_upi)
        val tvUpiId = findViewById<TextView>(R.id.tv_upi_id)
        tvUpiId.text = upiId

        cardUpi.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("UPI ID", upiId)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "UPI ID copied! 📋", Toast.LENGTH_SHORT).show()

            // Try to open UPI payment app
            try {
                val upiIntent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("upi://pay?pa=$upiId&pn=Essential%20Shortcuts&cu=INR"))
                upiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(upiIntent)
            } catch (e: Exception) {
                // UPI app not found — clipboard copy is enough
            }
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun animateCards() {
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        val delay = 80L
        val cards = listOf(
            R.id.card_kofi,
            R.id.card_patreon,
            R.id.card_upi
        )
        cards.forEachIndexed { index, id ->
            val view = findViewById<LinearLayout>(id)
            view.alpha = 0f
            view.postDelayed({
                view.alpha = 1f
                view.startAnimation(slideUp)
            }, index * delay)
        }
    }
}
