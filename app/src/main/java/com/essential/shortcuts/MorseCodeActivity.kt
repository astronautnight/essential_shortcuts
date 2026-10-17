package com.essential.shortcuts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class MorseCodeActivity : AppCompatActivity() {

    private lateinit var btnClose: ImageButton
    private lateinit var tilInput: TextInputLayout
    private lateinit var etInput: TextInputEditText
    private lateinit var btnToMorse: MaterialCardView
    private lateinit var btnToText: MaterialCardView
    private lateinit var cardResult: MaterialCardView
    private lateinit var tvResultLabel: TextView
    private lateinit var tvResult: TextView
    private lateinit var btnCopy: Button

    private val textToMorseMap = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.",
        'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
        'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
        'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
        'Y' to "-.--", 'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.", '!' to "-.-.--",
        '/' to "-..-.", '(' to "-.--.", ')' to "-.--.-", '&' to ".-...", ':' to "---...",
        ';' to "-.-.-.", '=' to "-...-", '+' to ".-.-.", '-' to "-....-", '_' to "..--.-",
        '"' to ".-..-.", '$' to "...-..-", '@' to ".--.-."
    )

    private val morseToTextMap = textToMorseMap.entries.associateBy({ it.value }) { it.key }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_morse_code)
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
        tilInput = findViewById(R.id.til_input)
        etInput = findViewById(R.id.et_input)
        btnToMorse = findViewById(R.id.btn_to_morse)
        btnToText = findViewById(R.id.btn_to_text)
        cardResult = findViewById(R.id.card_result)
        tvResultLabel = findViewById(R.id.tv_result_label)
        tvResult = findViewById(R.id.tv_result)
        btnCopy = findViewById(R.id.btn_copy)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        btnToMorse.setOnClickListener {
            convert("to_morse")
        }

        btnToText.setOnClickListener {
            convert("to_text")
        }

        btnCopy.setOnClickListener {
            val text = tvResult.text.toString()
            if (text.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("morse_result", text))
                Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show()
                finish() // Finish after copy to complete the widget animation
            }
        }
    }

    private fun convert(mode: String) {
        val input = etInput.text.toString().trim()
        if (input.isEmpty()) {
            tilInput.error = "Please enter some text"
            return
        }
        tilInput.error = null

        var result = ""
        var label = ""

        if (mode == "to_morse") {
            val words = input.uppercase().split(Regex("\\s+"))
            val morseWords = words.map { word ->
                word.map { char ->
                    textToMorseMap[char] ?: ""
                }.filter { it.isNotEmpty() }.joinToString(" ")
            }
            result = morseWords.joinToString(" / ")
            label = "Converted to Morse Code"
        } else {
            // Morse to text
            val normalizedInput = input
                .replace("•", ".")
                .replace("·", ".")
                .replace("*", ".")
                .replace("−", "-")
                .replace("—", "-")
                .replace("–", "-")
                .replace("_", "-")

            val words = normalizedInput.split(" / ", "/", "   ", " | ", "|")
            val textWords = words.map { word ->
                word.trim().split(Regex("\\s+")).map { char ->
                    morseToTextMap[char] ?: ""
                }.joinToString("")
            }
            result = textWords.joinToString(" ")
            label = "Converted to Text"
        }

        tvResultLabel.text = label
        tvResult.text = result
        cardResult.visibility = View.VISIBLE
        setResult(RESULT_OK)
        sendBroadcast(android.content.Intent("com.essential.shortcuts.ACTION_STEP_2").apply {
            setPackage(packageName)
        })

        // Hide keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(etInput.windowToken, 0)
    }
}
