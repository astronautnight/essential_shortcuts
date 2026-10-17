package com.essential.shortcuts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton

class RepeatTextActivity : AppCompatActivity() {

    private lateinit var btnClose: ImageButton
    private lateinit var etText: TextInputEditText
    private lateinit var etCount: TextInputEditText
    private lateinit var cbSpace: CheckBox
    private lateinit var cbNewline: CheckBox
    private lateinit var etOutput: TextInputEditText
    private lateinit var btnRepeat: Button
    private lateinit var btnCopy: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repeat_text)

        // Standard popup width
        val width = (resources.displayMetrics.widthPixels * 0.92).toInt()
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnClose = findViewById(R.id.btn_close)
        etText = findViewById(R.id.et_text)
        etCount = findViewById(R.id.et_count)
        cbSpace = findViewById(R.id.cb_space)
        cbNewline = findViewById(R.id.cb_newline)
        etOutput = findViewById(R.id.et_output)
        btnRepeat = findViewById(R.id.btn_repeat)
        btnCopy = findViewById(R.id.btn_copy)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        btnRepeat.setOnClickListener {
            repeatText()
        }

        btnCopy.setOnClickListener {
            copyToClipboard()
        }
    }

    private fun repeatText() {
        val textToRepeat = etText.text.toString()
        if (textToRepeat.isEmpty()) {
            Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show()
            return
        }

        val countStr = etCount.text.toString()
        val count = countStr.toIntOrNull() ?: 0
        if (count <= 0) {
            Toast.makeText(this, "Please enter a valid repeat count", Toast.LENGTH_SHORT).show()
            return
        }

        // Limit count to avoid OOM or ANR for extreme values
        if (count > 50000) {
            Toast.makeText(this, "Max limit is 50,000", Toast.LENGTH_SHORT).show()
            return
        }

        val addSpace = cbSpace.isChecked
        val addNewLine = cbNewline.isChecked

        val sb = StringBuilder()
        for (i in 0 until count) {
            sb.append(textToRepeat)
            if (i < count - 1) {
                if (addSpace) sb.append(" ")
                if (addNewLine) sb.append("\n")
            }
        }

        etOutput.setText(sb.toString())
    }

    private fun copyToClipboard() {
        val result = etOutput.text.toString()
        if (result.isEmpty()) {
            Toast.makeText(this, "Nothing to copy", Toast.LENGTH_SHORT).show()
            return
        }

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Repeated Text", result)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}
