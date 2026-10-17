package com.essential.shortcuts

import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

class TextToPdfActivity : AppCompatActivity() {

    private lateinit var btnClose: ImageButton
    private lateinit var cardPickFile: MaterialCardView
    private lateinit var tvFileStatus: TextView
    private lateinit var etTitle: TextInputEditText
    private lateinit var etContent: TextInputEditText
    private lateinit var btnConvert: Button
    
    private lateinit var cardResult: MaterialCardView
    private lateinit var tvConvertedInfo: TextView
    private lateinit var btnSave: Button
    private lateinit var btnShare: Button

    private var generatedPdfFile: File? = null

    private val textFilePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { loadTextFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_to_pdf)
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
        cardPickFile = findViewById(R.id.card_pick_file)
        tvFileStatus = findViewById(R.id.tv_file_status)
        etTitle = findViewById(R.id.et_title)
        etContent = findViewById(R.id.et_content)
        btnConvert = findViewById(R.id.btn_convert)
        cardResult = findViewById(R.id.card_result)
        tvConvertedInfo = findViewById(R.id.tv_converted_info)
        btnSave = findViewById(R.id.btn_save)
        btnShare = findViewById(R.id.btn_share)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        cardPickFile.setOnClickListener {
            textFilePicker.launch("text/plain")
        }

        btnConvert.setOnClickListener { createPdf() }
        btnSave.setOnClickListener { savePdf() }
        btnShare.setOnClickListener { sharePdf() }
    }

    private fun loadTextFile(uri: Uri) {
        try {
            val contentResolver = applicationContext.contentResolver
            val stringBuilder = StringBuilder()
            
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        stringBuilder.append('\n')
                        line = reader.readLine()
                    }
                }
            }

            // Extract filename to use as default title
            var fileName = "Document"
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        val fullName = it.getString(nameIndex)
                        fileName = fullName.substringBeforeLast(".")
                    }
                }
            }

            etTitle.setText(fileName)
            etContent.setText(stringBuilder.toString())
            tvFileStatus.text = "File loaded successfully"
            tvFileStatus.setTextColor(getColor(R.color.category_generator)) // Green

        } catch (e: Exception) {
            Toast.makeText(this, "Error reading file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createPdf() {
        val content = etContent.text.toString()
        if (content.isBlank()) {
            Toast.makeText(this, "Please enter some text or load a file", Toast.LENGTH_SHORT).show()
            return
        }

        val title = etTitle.text.toString().ifBlank { "Text Document" }

        btnConvert.isEnabled = false
        btnConvert.text = "Creating PDF..."

        Thread {
            try {
                val pdfDocument = PdfDocument()
                val pageWidth = 595 // A4
                val pageHeight = 842 // A4
                val margin = 50

                // Paints
                val titlePaint = TextPaint().apply {
                    color = Color.BLACK
                    textSize = 24f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                
                val bodyPaint = TextPaint().apply {
                    color = Color.DKGRAY
                    textSize = 14f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }

                // Layout for body text to handle wrapping
                val textWidth = pageWidth - (margin * 2)
                
                val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    StaticLayout.Builder.obtain(content, 0, content.length, bodyPaint, textWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(1f, 1.2f)
                        .setIncludePad(false)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    StaticLayout(content, bodyPaint, textWidth, Layout.Alignment.ALIGN_NORMAL, 1.2f, 1f, false)
                }

                var currentLineIndex = 0
                var pageNumber = 1
                
                while (currentLineIndex < staticLayout.lineCount) {
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas

                    canvas.drawColor(Color.WHITE) // White background

                    var currentY = margin.toFloat()

                    // Draw title only on first page
                    if (pageNumber == 1) {
                        canvas.drawText(title, margin.toFloat(), currentY + titlePaint.textSize, titlePaint)
                        currentY += titlePaint.textSize + 30f // Space after title
                    } else {
                        currentY += 20f
                    }

                    // Draw lines for this page
                    while (currentLineIndex < staticLayout.lineCount) {
                        val lineBottom = currentY + staticLayout.getLineBottom(currentLineIndex) - staticLayout.getLineTop(currentLineIndex)
                        
                        // If line exceeds page height (minus margin), break to new page
                        if (lineBottom > pageHeight - margin) {
                            break
                        }

                        val lineStart = staticLayout.getLineStart(currentLineIndex)
                        val lineEnd = staticLayout.getLineEnd(currentLineIndex)
                        val lineText = content.substring(lineStart, lineEnd).trimEnd('\n', '\r')
                        
                        canvas.drawText(lineText, margin.toFloat(), currentY + staticLayout.getLineBaseline(currentLineIndex) - staticLayout.getLineTop(currentLineIndex), bodyPaint)
                        
                        currentY += staticLayout.getLineBottom(currentLineIndex) - staticLayout.getLineTop(currentLineIndex)
                        currentLineIndex++
                    }

                    pdfDocument.finishPage(page)
                    pageNumber++
                }

                // Save to cache dir first
                val cachePath = File(cacheDir, "converted")
                cachePath.mkdirs()
                val safeTitle = title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                val tempFile = File(cachePath, "${safeTitle}_${System.currentTimeMillis()}.pdf")
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
                    Toast.makeText(this@TextToPdfActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
