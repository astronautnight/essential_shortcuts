package com.essential.shortcuts

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.google.zxing.BarcodeFormat
import java.io.File
import java.io.FileOutputStream

class BarcodeGeneratorActivity : AppCompatActivity() {

    private lateinit var viewFlipper: ViewFlipper

    // Step 1: Input
    private lateinit var btnCloseMain: ImageButton
    private lateinit var tilContent: TextInputLayout
    private lateinit var etContent: TextInputEditText
    private lateinit var btnGenerate: Button

    // Step 2: Result
    private lateinit var ivBarcode: ImageView
    private lateinit var btnShare: Button
    private lateinit var btnDone: Button

    private var currentBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_generator)

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
        btnCloseMain = findViewById(R.id.btn_close_main)
        tilContent = findViewById(R.id.til_content)
        etContent = findViewById(R.id.et_content)
        btnGenerate = findViewById(R.id.btn_generate)

        // Step 2
        ivBarcode = findViewById(R.id.iv_barcode)
        btnShare = findViewById(R.id.btn_share)
        btnDone = findViewById(R.id.btn_done)
    }

    private fun setupListeners() {
        btnCloseMain.setOnClickListener { finish() }

        btnGenerate.setOnClickListener {
            val content = etContent.text.toString().trim()
            if (content.isEmpty()) {
                tilContent.error = "Please enter content"
                return@setOnClickListener
            }
            tilContent.error = null
            generateBarcode(content)
        }

        // Result Actions
        btnShare.setOnClickListener { shareBarcode() }
        btnDone.setOnClickListener { finish() }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewFlipper.displayedChild > 0) {
                    viewFlipper.displayedChild = viewFlipper.displayedChild - 1
                } else {
                    finish()
                }
            }
        })
    }

    private fun generateBarcode(input: String) {
        try {
            val barcodeEncoder = BarcodeEncoder()
            // Code 128 is a very versatile barcode format
            val bitmap = barcodeEncoder.encodeBitmap(input, BarcodeFormat.CODE_128, 600, 200)
            currentBitmap = bitmap
            ivBarcode.setImageBitmap(bitmap)
            
            setResult(RESULT_OK)   // ← mark as completed
            hideKeyboard()
            viewFlipper.displayedChild = 1 // Move to Result step
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error generating Barcode: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(etContent.windowToken, 0)
    }

    private fun shareBarcode() {
        if (currentBitmap == null) return

        try {
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs() 
            val stream = FileOutputStream("$cachePath/barcode_share.png") 
            currentBitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val newFile = File(cachePath, "barcode_share.png")
            val contentUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", newFile)

            if (contentUri != null) {
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                shareIntent.setDataAndType(contentUri, contentResolver.getType(contentUri))
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
                shareIntent.type = "image/png"
                startActivity(Intent.createChooser(shareIntent, "Share Barcode"))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error sharing image", Toast.LENGTH_SHORT).show()
        }
    }
}
