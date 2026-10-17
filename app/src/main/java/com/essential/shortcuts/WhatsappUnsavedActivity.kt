package com.essential.shortcuts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class WhatsappUnsavedActivity : AppCompatActivity() {

    private lateinit var etPhone: TextInputEditText
    private lateinit var btnOpenWhatsapp: Button
    private lateinit var btnClose: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whatsapp_unsaved)

        // Make window background transparent for popup
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        etPhone = findViewById(R.id.et_phone)
        btnOpenWhatsapp = findViewById(R.id.btn_open_whatsapp)
        btnClose = findViewById(R.id.btn_close)

        btnOpenWhatsapp.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            if (phone.isNotEmpty()) {
                openWhatsapp(phone)
            } else {
                etPhone.error = "Please enter a phone number"
            }
        }

        btnClose.setOnClickListener {
            finish()
        }
    }

    private fun openWhatsapp(phoneNumber: String) {
        // Hide keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etPhone.windowToken, 0)

        // Remove any non-digit characters (except maybe leading '+')
        var formattedNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        
        // WhatsApp API requires the number without '+' mostly, but handles it fine. Let's just pass it to the api.
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedNumber")
        }
        
        // Always try to start it, if WhatsApp isn't installed the system will open a browser which will ask them to install it.
        startActivity(intent)
        // Finish this popup since they are moving to WhatsApp
        finish()
    }
}
