package com.essential.shortcuts

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.card.MaterialCardView
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ImageConverterActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var layoutPickPlaceholder: LinearLayout
    private lateinit var layoutFormatPicker: LinearLayout
    private lateinit var layoutConverting: LinearLayout
    private lateinit var tvConvertingStatus: TextView
    private lateinit var layoutResult: LinearLayout

    private lateinit var tvConvertedInfo: TextView
    private lateinit var tvSavedSpace: TextView
    private lateinit var btnSave: Button
    private lateinit var btnShare: Button

    private var selectedUris: List<Uri> = emptyList()
    private var convertedFiles: List<File> = emptyList()
    private var targetFormat: String = ""

    private val multipleImagePicker = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri>? ->
        if (uris != null && uris.isNotEmpty()) {
            selectedUris = uris
            switchToFormatState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_converter)
        initViews()
        setupListeners()

        // Handle shared image(s) if any
        handleSharedIntent()
    }

    override fun onStart() {
        super.onStart()
        window?.apply {
            setGravity(Gravity.TOP)
            val layoutParams = attributes
            val density = resources.displayMetrics.density
            
            var statusBarHeight = 0
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                statusBarHeight = resources.getDimensionPixelSize(resourceId)
            }
            
            layoutParams.y = statusBarHeight + (56 * density).toInt()
            
            // Almost edge-to-edge (97% width)
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
        tvTitle = findViewById(R.id.tv_title)
        layoutPickPlaceholder = findViewById(R.id.layout_pick_placeholder)
        layoutFormatPicker = findViewById(R.id.layout_format_picker)
        layoutConverting = findViewById(R.id.layout_converting)
        tvConvertingStatus = findViewById(R.id.tv_converting_status)
        layoutResult = findViewById(R.id.layout_result)

        tvConvertedInfo = findViewById(R.id.tv_converted_info)
        tvSavedSpace = findViewById(R.id.tv_saved_space)
        btnSave = findViewById(R.id.btn_save)
        btnShare = findViewById(R.id.btn_share)
    }

    private fun setupListeners() {
        layoutPickPlaceholder.setOnClickListener {
            multipleImagePicker.launch("image/*")
        }

        findViewById<TextView>(R.id.btn_format_jpg).setOnClickListener { performConversion("JPG") }
        findViewById<TextView>(R.id.btn_format_png).setOnClickListener { performConversion("PNG") }
        findViewById<TextView>(R.id.btn_format_webp).setOnClickListener { performConversion("WEBP") }
        findViewById<TextView>(R.id.btn_format_pdf).setOnClickListener { performConversion("PDF") }

        btnSave.setOnClickListener { saveConvertedFiles() }
        btnShare.setOnClickListener { shareConvertedFiles() }
    }

    private fun handleSharedIntent() {
        val action = intent.action
        val type = intent.type
        if (action == Intent.ACTION_SEND && type?.startsWith("image/") == true) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (uri != null) {
                selectedUris = listOf(uri)
                switchToFormatState()
            }
        } else if (action == Intent.ACTION_SEND_MULTIPLE && type?.startsWith("image/") == true) {
            val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            if (uris != null && uris.isNotEmpty()) {
                selectedUris = uris
                switchToFormatState()
            }
        } else {
            val uri = intent.getParcelableExtra<Uri>("shared_image_uri")
            if (uri != null) {
                selectedUris = listOf(uri)
                switchToFormatState()
            }
        }
    }

    private fun switchToFormatState() {
        tvTitle.text = "Convert Image"
        layoutPickPlaceholder.visibility = View.GONE
        layoutFormatPicker.visibility = View.VISIBLE
        layoutConverting.visibility = View.GONE
        layoutResult.visibility = View.GONE

        // Cascading animation for the option buttons
        val container = layoutFormatPicker
        var delayIndex = 0
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is TextView && child.visibility == View.VISIBLE) {
                child.alpha = 0f
                child.translationY = 30f
                child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(250)
                    .setStartDelay(delayIndex * 35L)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
                delayIndex++
            }
        }
    }

    private fun performConversion(format: String) {
        targetFormat = format
        tvTitle.text = "Converting"
        layoutPickPlaceholder.visibility = View.GONE
        layoutFormatPicker.visibility = View.GONE
        layoutConverting.visibility = View.VISIBLE
        layoutResult.visibility = View.GONE

        tvConvertingStatus.text = "Converting ${selectedUris.size} image(s) to $format..."

        Thread {
            val convertedList = mutableListOf<File>()
            var totalOriginalSize: Long = 0
            var totalConvertedSize: Long = 0

            try {
                for (uri in selectedUris) {
                    contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                        totalOriginalSize += it.length
                    }
                }

                if (format == "PDF") {
                    val pdfDocument = PdfDocument()
                    for ((index, uri) in selectedUris.withIndex()) {
                        val inputStream = contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()
                        if (bitmap == null) continue

                        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                        val page = pdfDocument.startPage(pageInfo)
                        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        pdfDocument.finishPage(page)
                    }

                    val cacheDir = File(cacheDir, "converted")
                    cacheDir.mkdirs()
                    val pdfFile = File(cacheDir, "compiled_${System.currentTimeMillis()}.pdf")
                    FileOutputStream(pdfFile).use { out ->
                        pdfDocument.writeTo(out)
                    }
                    pdfDocument.close()
                    convertedList.add(pdfFile)
                    totalConvertedSize = pdfFile.length()
                } else {
                    val compressFormat = when (format) {
                        "JPG" -> Bitmap.CompressFormat.JPEG
                        "PNG" -> Bitmap.CompressFormat.PNG
                        else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Bitmap.CompressFormat.WEBP_LOSSY
                        } else {
                            @Suppress("DEPRECATION")
                            Bitmap.CompressFormat.WEBP
                        }
                    }
                    val ext = format.lowercase()

                    for ((index, uri) in selectedUris.withIndex()) {
                        val inputStream = contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()
                        if (bitmap == null) continue

                        val cacheDir = File(cacheDir, "converted")
                        cacheDir.mkdirs()
                        val file = File(cacheDir, "converted_${index}_${System.currentTimeMillis()}.$ext")
                        FileOutputStream(file).use { out ->
                            bitmap.compress(compressFormat, 90, out)
                        }
                        convertedList.add(file)
                        totalConvertedSize += file.length()
                    }
                }

                convertedFiles = convertedList

                runOnUiThread {
                    tvTitle.text = "Conversion Complete"
                    layoutConverting.visibility = View.GONE
                    layoutResult.visibility = View.VISIBLE

                    tvConvertedInfo.text = "Successfully converted ${selectedUris.size} image(s) to $format!"
                    
                    val spaceDiff = totalOriginalSize - totalConvertedSize
                    if (spaceDiff > 0 && format != "PDF") {
                        tvSavedSpace.text = "Saved ${formatFileSize(spaceDiff)} of storage space"
                        tvSavedSpace.visibility = View.VISIBLE
                    } else {
                        tvSavedSpace.visibility = View.GONE
                    }
                    // Step 2: Conversion done → sweep to 2/3rds
                    sendBroadcast(Intent("com.essential.shortcuts.ACTION_STEP_2").setPackage(packageName))
                }

            } catch (e: Exception) {
                runOnUiThread {
                    switchToFormatState()
                    Toast.makeText(this@ImageConverterActivity, "Conversion failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun saveConvertedFiles() {
        if (convertedFiles.isEmpty()) return
        var successCount = 0

        try {
            for (file in convertedFiles) {
                val mimeType = when (targetFormat) {
                    "JPG" -> "image/jpeg"
                    "PNG" -> "image/png"
                    "WEBP" -> "image/webp"
                    else -> "application/pdf"
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        if (targetFormat == "PDF") {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/EssentialShortcuts")
                        } else {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EssentialShortcuts")
                        }
                    }

                    val uri = if (targetFormat == "PDF") {
                        contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    } else {
                        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    }

                    if (uri != null) {
                        contentResolver.openOutputStream(uri)?.use { outputStream ->
                            file.inputStream().use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        successCount++
                    }
                } else {
                    val targetDir = if (targetFormat == "PDF") {
                        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "EssentialShortcuts")
                    } else {
                        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "EssentialShortcuts")
                    }
                    targetDir.mkdirs()
                    val targetFile = File(targetDir, file.name)
                    file.inputStream().use { inputStream ->
                        targetFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    successCount++
                }
            }

            val folderName = if (targetFormat == "PDF") "Downloads/EssentialShortcuts" else "Pictures/EssentialShortcuts"
            Toast.makeText(this, "Saved $successCount files to $folderName", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareConvertedFiles() {
        if (convertedFiles.isEmpty()) return

        try {
            val uris = ArrayList<Uri>()
            for (file in convertedFiles) {
                uris.add(FileProvider.getUriForFile(this, "$packageName.fileprovider", file))
            }

            val shareIntent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_STREAM, uris[0])
                    type = when (targetFormat) {
                        "JPG" -> "image/jpeg"
                        "PNG" -> "image/png"
                        "WEBP" -> "image/webp"
                        else -> "application/pdf"
                    }
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    type = when (targetFormat) {
                        "PDF" -> "application/pdf"
                        else -> "image/*"
                    }
                }
            }
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(shareIntent, "Share Converted Files"))
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
