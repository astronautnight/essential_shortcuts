package com.essential.shortcuts

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.URLUtil
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.ScaleAnimation
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.google.zxing.BarcodeFormat
import java.io.File
import java.io.FileOutputStream
class QrCodeGeneratorActivity : AppCompatActivity() {

    private lateinit var viewFlipper: ViewFlipper
    
    // Step 1: Types
    private lateinit var cardTypeText: MaterialCardView
    private lateinit var cardTypeEmail: MaterialCardView
    private lateinit var cardTypeContact: MaterialCardView
    private lateinit var cardTypeSms: MaterialCardView
    private lateinit var btnCloseMain: Button

    // Step 2: Input
    private lateinit var btnBackToType: ImageButton
    private lateinit var tvInputTitle: TextView
    private lateinit var tilContent: TextInputLayout
    private lateinit var etContent: TextInputEditText
    private lateinit var tilContactName: TextInputLayout
    private lateinit var etContactName: TextInputEditText
    private lateinit var tilContactEmail: TextInputLayout
    private lateinit var etContactEmail: TextInputEditText
    private lateinit var btnGenerate: Button

    // Step 3: Result
    private lateinit var ivQrCode: ImageView
    private lateinit var cardTlyAffiliate: MaterialCardView
    private lateinit var btnShare: Button
    private lateinit var btnDone: Button

    private var currentType = "text"
    private var currentBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code_generator)

        // Set the window size programmatically just in case, though Dialog theme handles it
        // window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        initViews()
        setupListeners()
        setupBackNavigation()
    }

    override fun onStart() {
        super.onStart()
        // Make the dialog width 90% of screen width to look like a proper popup
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun initViews() {
        viewFlipper = findViewById(R.id.view_flipper)

        // Step 1
        cardTypeText = findViewById(R.id.card_type_text)
        cardTypeEmail = findViewById(R.id.card_type_email)
        cardTypeContact = findViewById(R.id.card_type_contact)
        cardTypeSms = findViewById(R.id.card_type_sms)
        btnCloseMain = findViewById(R.id.btn_close_main)

        // Step 2
        btnBackToType = findViewById(R.id.btn_back_to_type)
        tvInputTitle = findViewById(R.id.tv_input_title)
        tilContent = findViewById(R.id.til_content)
        etContent = findViewById(R.id.et_content)
        tilContactName = findViewById(R.id.til_contact_name)
        etContactName = findViewById(R.id.et_contact_name)
        tilContactEmail = findViewById(R.id.til_contact_email)
        etContactEmail = findViewById(R.id.et_contact_email)
        btnGenerate = findViewById(R.id.btn_generate)

        // Step 3
        ivQrCode = findViewById(R.id.iv_qr_code)
        cardTlyAffiliate = findViewById(R.id.card_tly_affiliate)
        btnShare = findViewById(R.id.btn_share)
        btnDone = findViewById(R.id.btn_done)
    }

    private fun setupListeners() {
        // Type Selection — tap with scale bounce matching shortcut animation
        cardTypeText.setOnClickListener    { animateTap(it) { selectType("text") } }
        cardTypeEmail.setOnClickListener   { animateTap(it) { selectType("email") } }
        cardTypeContact.setOnClickListener { animateTap(it) { selectType("contact") } }
        cardTypeSms.setOnClickListener     { animateTap(it) { selectType("sms") } }
        
        btnCloseMain.setOnClickListener { finish() }

        // Input Navigation
        btnBackToType.setOnClickListener {
            hideKeyboard()
            flipBack()
        }

        btnGenerate.setOnClickListener {
            val content = etContent.text.toString().trim()
            if (content.isEmpty()) {
                if (currentType == "contact") {
                    tilContent.error = "Please enter a phone number"
                } else {
                    tilContent.error = "Please enter content"
                }
                return@setOnClickListener
            }
            tilContent.error = null
            generateQrCode(content)
        }

        // Result Actions
        cardTlyAffiliate.setOnClickListener {
            animateTap(it) {
                openTlyLink()
            }
        }
        btnShare.setOnClickListener { shareQrCode() }
        btnDone.setOnClickListener { finish() }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewFlipper.displayedChild > 0) {
                    hideKeyboard()
                    flipBack()
                } else {
                    finish()
                }
            }
        })
    }

    private fun selectType(type: String) {
        currentType = type
        etContent.text = null
        etContactName.text = null
        etContactEmail.text = null
        tilContent.error = null
        tilContactName.error = null
        tilContactEmail.error = null
        
        if (type == "contact") {
            tilContactName.visibility = View.VISIBLE
            tilContactEmail.visibility = View.VISIBLE
        } else {
            tilContactName.visibility = View.GONE
            tilContactEmail.visibility = View.GONE
        }
        
        when (type) {
            "text" -> {
                tvInputTitle.text = "Enter Text or URL"
                tilContent.hint = "https://example.com"
                etContent.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
            }
            "email" -> {
                tvInputTitle.text = "Enter Email Address"
                tilContent.hint = "user@example.com"
                etContent.inputType = EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }
            "contact" -> {
                tvInputTitle.text = "Enter Contact Details"
                tilContent.hint = "Phone Number"
                etContent.inputType = EditorInfo.TYPE_CLASS_PHONE
            }
            "sms" -> {
                tvInputTitle.text = "Enter SMS Number"
                tilContent.hint = "+1-555-010-9999"
                etContent.inputType = EditorInfo.TYPE_CLASS_PHONE
            }
        }
        
        flipForward()
    }

    private fun generateQrCode(input: String) {
        val dataToEncode = when (currentType) {
            "email" -> if (input.startsWith("mailto:")) input else "mailto:$input"
            "contact" -> {
                val name = etContactName.text.toString().trim()
                val email = etContactEmail.text.toString().trim()
                "BEGIN:VCARD\nVERSION:3.0\nN:$name\nFN:$name\nTEL:$input\nEMAIL:$email\nEND:VCARD"
            }
            "sms" -> if (input.startsWith("smsto:")) input else "smsto:$input"
            else -> input
        }

        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(dataToEncode, BarcodeFormat.QR_CODE, 600, 600)
            currentBitmap = bitmap
            ivQrCode.setImageBitmap(bitmap)
            
            // Only show T.LY affiliate recommendation if the generated QR contains a valid HTTP/HTTPS URL
            val isHttpUrl = isValidHttpUrl(dataToEncode)
            cardTlyAffiliate.visibility = if (isHttpUrl) View.VISIBLE else View.GONE

            setResult(RESULT_OK)   // ← mark as completed
            hideKeyboard()
            flipForward() // Move to Result step
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error generating QR Code: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ── ViewFlipper transitions ───────────────────────────────────────────────

    /** Forward: type-select → input → result (content drifts left) */
    private fun flipForward() {
        viewFlipper.inAnimation  = AnimationUtils.loadAnimation(this, R.anim.qr_slide_in_forward)
        viewFlipper.outAnimation = AnimationUtils.loadAnimation(this, R.anim.qr_slide_out_forward)
        viewFlipper.showNext()
    }

    /** Back: input → type-select (content drifts right) */
    private fun flipBack() {
        viewFlipper.inAnimation  = AnimationUtils.loadAnimation(this, R.anim.qr_slide_in_back)
        viewFlipper.outAnimation = AnimationUtils.loadAnimation(this, R.anim.qr_slide_out_back)
        viewFlipper.showPrevious()
    }

    // ── Card tap bounce ──────────────────────────────────────────────────────

    /**
     * Plays the same 80ms scale-bounce used by shortcut cards, then runs [action].
     */
    private fun animateTap(view: View, action: () -> Unit) {
        val anim = ScaleAnimation(
            1f, 0.96f, 1f, 0.96f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration    = 80
            repeatCount = 1
            repeatMode  = Animation.REVERSE
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(a: Animation?) {}
                override fun onAnimationRepeat(a: Animation?) {}
                override fun onAnimationEnd(a: Animation?) { action() }
            })
        }
        view.startAnimation(anim)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(etContent.windowToken, 0)
    }

    private fun shareQrCode() {
        if (currentBitmap == null) return

        try {
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs() 
            val stream = FileOutputStream("$cachePath/qr_share.png") 
            currentBitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val newFile = File(cachePath, "qr_share.png")
            val contentUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", newFile)

            if (contentUri != null) {
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                shareIntent.setDataAndType(contentUri, contentResolver.getType(contentUri))
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
                shareIntent.type = "image/png"
                startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error sharing image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isValidHttpUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim()
        val isHttpOrHttps = trimmed.startsWith("http://", ignoreCase = true) ||
                            trimmed.startsWith("https://", ignoreCase = true)
        if (!isHttpOrHttps) return false
        return try {
            val uri = Uri.parse(trimmed)
            val scheme = uri.scheme?.lowercase()
            val host = uri.host
            (scheme == "http" || scheme == "https") && !host.isNullOrBlank() && URLUtil.isValidUrl(trimmed)
        } catch (_: Exception) {
            false
        }
    }

    private fun openTlyLink() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TLY_AFFILIATE_URL))
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TLY_AFFILIATE_URL = "https://t.ly/register?via=rajas"
    }
}
