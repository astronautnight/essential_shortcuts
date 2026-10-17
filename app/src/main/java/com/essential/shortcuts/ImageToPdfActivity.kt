package com.essential.shortcuts

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.card.MaterialCardView
import java.io.File
import java.io.FileOutputStream

class ImageToPdfActivity : AppCompatActivity() {

    private lateinit var btnClose: ImageButton
    private lateinit var cardPickImages: MaterialCardView
    private lateinit var tvSelectionInfo: TextView
    private lateinit var layoutControls: LinearLayout
    private lateinit var radioGroupSize: RadioGroup
    private lateinit var radioA4: RadioButton
    private lateinit var btnConvert: Button
    
    private lateinit var cardResult: MaterialCardView
    private lateinit var tvConvertedInfo: TextView
    private lateinit var btnSave: Button
    private lateinit var btnShare: Button

    private var selectedUris: List<Uri> = emptyList()
    private var generatedPdfFile: File? = null

    // Constant for A4 size in 1/72th of an inch (points)
    private val A4_WIDTH = 595
    private val A4_HEIGHT = 842

    private val multipleImagePicker = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri>? ->
        if (uris != null && uris.isNotEmpty()) {
            selectedUris = uris
            updateSelectionUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_to_pdf)
        initViews()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.92).toInt()
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun initViews() {
        btnClose = findViewById(R.id.btn_close)
        cardPickImages = findViewById(R.id.card_pick_images)
        tvSelectionInfo = findViewById(R.id.tv_selection_info)
        layoutControls = findViewById(R.id.layout_controls)
        radioGroupSize = findViewById(R.id.radio_group_size)
        radioA4 = findViewById(R.id.radio_a4)
        btnConvert = findViewById(R.id.btn_convert)
        cardResult = findViewById(R.id.card_result)
        tvConvertedInfo = findViewById(R.id.tv_converted_info)
        btnSave = findViewById(R.id.btn_save)
        btnShare = findViewById(R.id.btn_share)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        cardPickImages.setOnClickListener {
            multipleImagePicker.launch("image/*")
        }

        btnConvert.setOnClickListener { createPdf() }
        btnSave.setOnClickListener { savePdf() }
        btnShare.setOnClickListener { sharePdf() }
    }

    private fun updateSelectionUI() {
        if (selectedUris.isEmpty()) {
            tvSelectionInfo.visibility = View.GONE
            layoutControls.visibility = View.GONE
            return
        }

        tvSelectionInfo.text = "${selectedUris.size} image(s) selected"
        tvSelectionInfo.visibility = View.VISIBLE
        layoutControls.visibility = View.VISIBLE
        cardResult.visibility = View.GONE
    }

    private fun createPdf() {
        if (selectedUris.isEmpty()) return

        btnConvert.isEnabled = false
        btnConvert.text = "Creating PDF..."

        Thread {
            try {
                val pdfDocument = PdfDocument()
                val useA4 = radioA4.isChecked

                for ((index, uri) in selectedUris.withIndex()) {
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (bitmap == null) continue

                    val pageInfo = if (useA4) {
                        PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, index + 1).create()
                    } else {
                        PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                    }

                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas

                    if (useA4) {
                        // Fill background white
                        val paint = Paint()
                        paint.color = Color.WHITE
                        canvas.drawRect(0f, 0f, A4_WIDTH.toFloat(), A4_HEIGHT.toFloat(), paint)

                        // Scale image to fit A4 while maintaining aspect ratio
                        val ratio = Math.min(
                            A4_WIDTH.toFloat() / bitmap.width,
                            A4_HEIGHT.toFloat() / bitmap.height
                        )
                        val scaledWidth = bitmap.width * ratio
                        val scaledHeight = bitmap.height * ratio
                        val x = (A4_WIDTH - scaledWidth) / 2
                        val y = (A4_HEIGHT - scaledHeight) / 2

                        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth.toInt(), scaledHeight.toInt(), true)
                        canvas.drawBitmap(scaledBitmap, x, y, null)
                    } else {
                        canvas.drawBitmap(bitmap, 0f, 0f, null)
                    }

                    pdfDocument.finishPage(page)
                }

                // Save to cache dir first
                val cachePath = File(cacheDir, "converted")
                cachePath.mkdirs()
                val tempFile = File(cachePath, "document_${System.currentTimeMillis()}.pdf")
                val fos = FileOutputStream(tempFile)
                pdfDocument.writeTo(fos)
                pdfDocument.close()
                fos.close()

                generatedPdfFile = tempFile

                runOnUiThread {
                    tvConvertedInfo.text = "PDF Created Successfully!\nSize: ${formatFileSize(tempFile.length())}"
                    cardResult.visibility = View.VISIBLE
                    // Step 2: PDF created → sweep to 2/3rds
                    sendBroadcast(Intent("com.essential.shortcuts.ACTION_STEP_2").setPackage(packageName))
                    btnConvert.isEnabled = true
                    btnConvert.text = "Create PDF"
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ImageToPdfActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    btnConvert.isEnabled = true
                    btnConvert.text = "Create PDF"
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
            startActivity(Intent.createChooser(shareIntent, "Share PDF Document"))
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
