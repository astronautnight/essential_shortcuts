package com.essential.shortcuts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.security.SecureRandom

class PasswordGeneratorActivity : AppCompatActivity() {

    private lateinit var viewFlipper: ViewFlipper

    // Step 0: Home
    private lateinit var cardGenerate: MaterialCardView
    private lateinit var cardViewSaved: MaterialCardView
    private lateinit var btnCloseHome: Button

    // Step 1: Generate
    private lateinit var btnBackFromGenerate: ImageButton
    private lateinit var tilUsername: TextInputLayout
    private lateinit var etUsername: TextInputEditText
    private lateinit var tilLength: TextInputLayout
    private lateinit var etLength: TextInputEditText
    private lateinit var cbUppercase: CheckBox
    private lateinit var cbLowercase: CheckBox
    private lateinit var cbDigits: CheckBox
    private lateinit var cbSymbols: CheckBox
    private lateinit var btnGeneratePassword: Button
    private lateinit var cardResult: MaterialCardView
    private lateinit var tvGeneratedPassword: TextView
    private lateinit var btnCopyPassword: Button
    private lateinit var btnSavePassword: Button

    // Step 2: Saved
    private lateinit var btnBackFromSaved: ImageButton
    private lateinit var tvEmptyMessage: TextView
    private lateinit var rvSavedPasswords: RecyclerView
    private lateinit var savedAdapter: SavedPasswordsAdapter

    private var lastGeneratedPassword: String = ""
    private var lastUsername: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_generator)

        initViews()
        setupListeners()
        setupBackNavigation()
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun initViews() {
        viewFlipper = findViewById(R.id.view_flipper)

        // Step 0
        cardGenerate = findViewById(R.id.card_generate)
        cardViewSaved = findViewById(R.id.card_view_saved)
        btnCloseHome = findViewById(R.id.btn_close_home)

        // Step 1
        btnBackFromGenerate = findViewById(R.id.btn_back_from_generate)
        tilUsername = findViewById(R.id.til_username)
        etUsername = findViewById(R.id.et_username)
        tilLength = findViewById(R.id.til_length)
        etLength = findViewById(R.id.et_length)
        cbUppercase = findViewById(R.id.cb_uppercase)
        cbLowercase = findViewById(R.id.cb_lowercase)
        cbDigits = findViewById(R.id.cb_digits)
        cbSymbols = findViewById(R.id.cb_symbols)
        btnGeneratePassword = findViewById(R.id.btn_generate_password)
        cardResult = findViewById(R.id.card_result)
        tvGeneratedPassword = findViewById(R.id.tv_generated_password)
        btnCopyPassword = findViewById(R.id.btn_copy_password)
        btnSavePassword = findViewById(R.id.btn_save_password)

        // Step 2
        btnBackFromSaved = findViewById(R.id.btn_back_from_saved)
        tvEmptyMessage = findViewById(R.id.tv_empty_message)
        rvSavedPasswords = findViewById(R.id.rv_saved_passwords)

        savedAdapter = SavedPasswordsAdapter(mutableListOf()) { entry ->
            PasswordManager.deletePassword(this, entry.timestamp)
            refreshSavedList()
            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
        }
        rvSavedPasswords.layoutManager = LinearLayoutManager(this)
        rvSavedPasswords.adapter = savedAdapter
    }

    private fun setupListeners() {
        // Step 0
        cardGenerate.setOnClickListener { viewFlipper.displayedChild = 1 }
        cardViewSaved.setOnClickListener {
            refreshSavedList()
            viewFlipper.displayedChild = 2
        }
        btnCloseHome.setOnClickListener { finish() }

        // Step 1
        btnBackFromGenerate.setOnClickListener {
            hideKeyboard()
            cardResult.visibility = View.GONE
            viewFlipper.displayedChild = 0
        }

        btnGeneratePassword.setOnClickListener {
            val username = etUsername.text.toString().trim()
            if (username.isEmpty()) {
                tilUsername.error = "Please enter a username or label"
                return@setOnClickListener
            }
            tilUsername.error = null

            val lengthStr = etLength.text.toString().trim()
            val length = lengthStr.toIntOrNull()
            if (length == null || length < 4 || length > 128) {
                tilLength.error = "Enter a length between 4 and 128"
                return@setOnClickListener
            }
            tilLength.error = null

            if (!cbUppercase.isChecked && !cbLowercase.isChecked && !cbDigits.isChecked && !cbSymbols.isChecked) {
                Toast.makeText(this, "Select at least one character type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lastUsername = username
            lastGeneratedPassword = generatePassword(length)
            tvGeneratedPassword.text = lastGeneratedPassword
            cardResult.visibility = View.VISIBLE
            hideKeyboard()
        }

        btnCopyPassword.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("password", lastGeneratedPassword))
            setResult(RESULT_OK)   // ← mark as completed
            Toast.makeText(this, "Password copied!", Toast.LENGTH_SHORT).show()
        }

        btnSavePassword.setOnClickListener {
            if (lastGeneratedPassword.isNotEmpty() && lastUsername.isNotEmpty()) {
                PasswordManager.savePassword(this, lastUsername, lastGeneratedPassword)
                setResult(RESULT_OK)   // ← mark as completed
                Toast.makeText(this, "Password saved!", Toast.LENGTH_SHORT).show()
                btnSavePassword.isEnabled = false
                btnSavePassword.text = "Saved ✓"
            }
        }

        // Step 2
        btnBackFromSaved.setOnClickListener { viewFlipper.displayedChild = 0 }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewFlipper.displayedChild > 0) {
                    if (viewFlipper.displayedChild == 1) {
                        cardResult.visibility = View.GONE
                    }
                    viewFlipper.displayedChild = 0
                } else {
                    finish()
                }
            }
        })
    }

    private fun generatePassword(length: Int): String {
        var chars = ""
        if (cbUppercase.isChecked) chars += "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        if (cbLowercase.isChecked) chars += "abcdefghijklmnopqrstuvwxyz"
        if (cbDigits.isChecked) chars += "0123456789"
        if (cbSymbols.isChecked) chars += "!@#\$%^&*()-_=+[]{}|;:',.<>?/"

        val random = SecureRandom()
        val sb = StringBuilder(length)
        for (i in 0 until length) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        return sb.toString()
    }

    private fun refreshSavedList() {
        val passwords = PasswordManager.getSavedPasswords(this)
        savedAdapter.updatePasswords(passwords)
        tvEmptyMessage.visibility = if (passwords.isEmpty()) View.VISIBLE else View.GONE
        rvSavedPasswords.visibility = if (passwords.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken ?: etUsername.windowToken, 0)
    }
}
