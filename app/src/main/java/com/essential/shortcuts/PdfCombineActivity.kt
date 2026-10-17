package com.essential.shortcuts

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.card.MaterialCardView
import java.io.File
import java.io.FileOutputStream

class PdfCombineActivity : AppCompatActivity() {

    private lateinit var btnClose: ImageButton
    private lateinit var cardPickPdfs: MaterialCardView
    private lateinit var tvSelectionInfo: TextView
    private lateinit var layoutControls: LinearLayout
    private lateinit var btnConvert: Button
    
    private lateinit var cardResult: MaterialCardView
    private lateinit var tvConvertedInfo: TextView
    private lateinit var btnSave: Button
    private lateinit var btnShare: Button

    private var selectedUris: List<Uri> = emptyList()
    private var generatedPdfFile: File? = null

    private val multiplePdfPicker = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri>? ->
        if (uris != null && uris.isNotEmpty()) {
            selectedUris = uris
            updateSelectionUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_combine)
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)
        initViews()
        setupListeners()
        handleIncomingIntent()
    }

    override fun onStart() {
        super.onStart()
        window?.apply {
            setGravity(android.view.Gravity.TOP)
            val layoutParams = attributes
            val density = resources.displayMetrics.density
            
            var statusBarHeight = 0
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                statusBarHeight = resources.getDimensionPixelSize(resourceId)
            }
            
            layoutParams.y = statusBarHeight + (56 * density).toInt()
            
            val width = (resources.displayMetrics.widthPixels * 0.97).toInt()
            layoutParams.width = width
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            attributes = layoutParams

            val isFromShare = intent.hasExtra("shared_image_uri") || intent.action == Intent.ACTION_SEND_MULTIPLE
            if (isFromShare) {
                clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
        }
    }

    private fun initViews() {
        btnClose = findViewById(R.id.btn_close)
        cardPickPdfs = findViewById(R.id.card_pick_pdfs)
        tvSelectionInfo = findViewById(R.id.tv_selection_info)
        layoutControls = findViewById(R.id.layout_controls)
        btnConvert = findViewById(R.id.btn_convert)
        cardResult = findViewById(R.id.card_result)
        tvConvertedInfo = findViewById(R.id.tv_converted_info)
        btnSave = findViewById(R.id.btn_save)
        btnShare = findViewById(R.id.btn_share)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        cardPickPdfs.setOnClickListener {
            multiplePdfPicker.launch("application/pdf")
        }

        btnConvert.setOnClickListener { combinePdfs() }
        btnSave.setOnClickListener { savePdf() }
        btnShare.setOnClickListener { sharePdf() }
    }

    private fun handleIncomingIntent() {
        val action = intent?.action
        val type = intent?.type
        if (action == Intent.ACTION_SEND && type?.startsWith("application/pdf") == true) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: intent.getParcelableExtra<Uri>("shared_image_uri")
            if (uri != null) {
                selectedUris = listOf(uri)
                updateSelectionUI()
            }
        } else if (action == Intent.ACTION_SEND_MULTIPLE && type?.startsWith("application/pdf") == true) {
            val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            if (uris != null) {
                selectedUris = uris
                updateSelectionUI()
            }
        } else {
            val singleUri = intent?.getParcelableExtra<Uri>("shared_image_uri")
            if (singleUri != null) {
                selectedUris = listOf(singleUri)
                updateSelectionUI()
            }
        }
    }

    private fun updateSelectionUI() {
        if (selectedUris.isEmpty()) {
            tvSelectionInfo.visibility = View.GONE
            layoutControls.visibility = View.GONE
            return
        }

        tvSelectionInfo.text = "${selectedUris.size} PDF document(s) selected"
        tvSelectionInfo.visibility = View.VISIBLE
        layoutControls.visibility = View.VISIBLE
        cardResult.visibility = View.GONE
    }

    private fun combinePdfs() {
        if (selectedUris.isEmpty()) return

        btnConvert.isEnabled = false
        btnConvert.text = "Combining PDFs..."

        Thread {
            try {
                val cacheDir = File(cacheDir, "combined")
                cacheDir.mkdirs()
                val tempFile = File(cacheDir, "combined_${System.currentTimeMillis()}.pdf")
                
                val merger = com.tom_roush.pdfbox.multipdf.PDFMergerUtility()
                merger.destinationFileName = tempFile.absolutePath

                val inputStreams = mutableListOf<java.io.InputStream>()
                val pfds = mutableListOf<android.os.ParcelFileDescriptor>()
                
                for (uri in selectedUris) {
                    val pfd = contentResolver.openFileDescriptor(uri, "r")
                    if (pfd != null) {
                        pfds.add(pfd)
                        val fis = java.io.FileInputStream(pfd.fileDescriptor)
                        inputStreams.add(fis)
                        merger.addSource(fis)
                    }
                }

                merger.mergeDocuments(com.tom_roush.pdfbox.io.MemoryUsageSetting.setupMainMemoryOnly())

                for (stream in inputStreams) {
                    try { stream.close() } catch (e: Exception) {}
                }
                for (pfd in pfds) {
                    try { pfd.close() } catch (e: Exception) {}
                }

                generatedPdfFile = tempFile

                runOnUiThread {
                    tvConvertedInfo.text = "PDFs Combined Successfully!\nSize: ${formatFileSize(tempFile.length())}"
                    cardResult.visibility = View.VISIBLE
                    // Step 2: PDFs combined → sweep to 2/3rds
                    sendBroadcast(Intent("com.essential.shortcuts.ACTION_STEP_2").setPackage(packageName))
                    btnConvert.isEnabled = true
                    btnConvert.text = "Combine PDFs"
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@PdfCombineActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    btnConvert.isEnabled = true
                    btnConvert.text = "Combine PDFs"
                }
            }
        }.start()
    }

    private fun savePdf() {
        val file = generatedPdfFile ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/EssentialShortcuts")
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { stream ->
                        file.inputStream().use { input ->
                            input.copyTo(stream)
                        }
                    }
                    Toast.makeText(this, "Saved to Downloads/EssentialShortcuts", Toast.LENGTH_SHORT).show()
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "EssentialShortcuts")
                dir.mkdirs()
                val destFile = File(dir, file.name)
                file.copyTo(destFile, overwrite = true)
                Toast.makeText(this, "Saved to ${destFile.absolutePath}", Toast.LENGTH_SHORT).show()
            }
            // Step 3: Save done → sweep to full circle and auto-close
            setResult(RESULT_OK)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharePdf() {
        val file = generatedPdfFile ?: return

        try {
            val contentUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "application/pdf"
            }
            startActivity(Intent.createChooser(shareIntent, "Share Combined PDF"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error sharing: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes} B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
