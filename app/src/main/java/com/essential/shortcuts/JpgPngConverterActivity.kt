package com.essential.shortcuts

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.card.MaterialCardView
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class JpgPngConverterActivity : AppCompatActivity() {

    private lateinit var btnClose: ImageButton
    private lateinit var cardPickImage: MaterialCardView
    private lateinit var layoutPickPlaceholder: LinearLayout
    private lateinit var ivPreview: ImageView
    private lateinit var tvOriginalInfo: TextView
    private lateinit var layoutControls: LinearLayout
    private lateinit var radioGroupFormat: RadioGroup
    private lateinit var radioJpg: RadioButton
    private lateinit var radioPng: RadioButton
    private lateinit var layoutQuality: LinearLayout
    private lateinit var tvQualityValue: TextView
    private lateinit var seekbarQuality: SeekBar
    private lateinit var btnConvert: Button
    
    private lateinit var cardResult: MaterialCardView
    private lateinit var ivConverted: ImageView
    private lateinit var tvConvertedInfo: TextView
    private lateinit var btnSave: Button
    private lateinit var btnShare: Button

    private var originalBitmap: Bitmap? = null
    private var convertedBytes: ByteArray? = null
    private var originalFormat: String = ""

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { loadImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jpg_png_converter)
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
        cardPickImage = findViewById(R.id.card_pick_image)
        layoutPickPlaceholder = findViewById(R.id.layout_pick_placeholder)
        ivPreview = findViewById(R.id.iv_preview)
        tvOriginalInfo = findViewById(R.id.tv_original_info)
        layoutControls = findViewById(R.id.layout_controls)
        radioGroupFormat = findViewById(R.id.radio_group_format)
        radioJpg = findViewById(R.id.radio_jpg)
        radioPng = findViewById(R.id.radio_png)
        layoutQuality = findViewById(R.id.layout_quality)
        tvQualityValue = findViewById(R.id.tv_quality_value)
        seekbarQuality = findViewById(R.id.seekbar_quality)
        btnConvert = findViewById(R.id.btn_convert)
        cardResult = findViewById(R.id.card_result)
        ivConverted = findViewById(R.id.iv_converted)
        tvConvertedInfo = findViewById(R.id.tv_converted_info)
        btnSave = findViewById(R.id.btn_save)
        btnShare = findViewById(R.id.btn_share)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        cardPickImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        radioGroupFormat.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radio_png) {
                // PNG is lossless, hide quality slider
                layoutQuality.visibility = View.GONE
                seekbarQuality.visibility = View.GONE
            } else {
                layoutQuality.visibility = View.VISIBLE
                seekbarQuality.visibility = View.VISIBLE
            }
        }

        seekbarQuality.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvQualityValue.text = "${progress}%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnConvert.setOnClickListener { convertImage() }
        btnSave.setOnClickListener { saveConvertedImage() }
        btnShare.setOnClickListener { shareConvertedImage() }
    }

    private fun loadImage(uri: Uri) {
        try {
            // Determine format
            val mimeType = contentResolver.getType(uri) ?: ""
            originalFormat = if (mimeType.contains("png", ignoreCase = true)) "PNG" else "JPG"
            
            // Set radio button to opposite format
            if (originalFormat == "PNG") {
                radioJpg.isChecked = true
            } else {
                radioPng.isChecked = true
            }

            var sizeBytes: Long = 0
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex >= 0) {
                        sizeBytes = it.getLong(sizeIndex)
                    }
                }
            }

            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                return
            }

            originalBitmap = bitmap
            layoutPickPlaceholder.visibility = View.GONE
            ivPreview.visibility = View.VISIBLE
            ivPreview.setImageBitmap(bitmap)

            tvOriginalInfo.text = "Original: $originalFormat • ${bitmap.width}×${bitmap.height} • ${formatFileSize(sizeBytes)}"
            tvOriginalInfo.visibility = View.VISIBLE

            layoutControls.visibility = View.VISIBLE
            cardResult.visibility = View.GONE

        } catch (e: Exception) {
            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertImage() {
        val bmp = originalBitmap
        if (bmp == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            return
        }

        val targetFormat = if (radioJpg.isChecked) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
        val quality = if (targetFormat == Bitmap.CompressFormat.JPEG) seekbarQuality.progress else 100

        val stream = ByteArrayOutputStream()
        bmp.compress(targetFormat, quality, stream)
        convertedBytes = stream.toByteArray()

        val bytes = convertedBytes ?: return
        val convertedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        ivConverted.setImageBitmap(convertedBitmap)
        
        val formatName = if (targetFormat == Bitmap.CompressFormat.JPEG) "JPG" else "PNG"
        tvConvertedInfo.text = "Converted: $formatName • Size: ${formatFileSize(bytes.size.toLong())}"
        cardResult.visibility = View.VISIBLE
        // Step 2: Conversion done → sweep to 2/3rds
        sendBroadcast(Intent("com.essential.shortcuts.ACTION_STEP_2").setPackage(packageName))
    }

    private fun saveConvertedImage() {
        val bytes = convertedBytes ?: return
        val formatName = if (radioJpg.isChecked) "jpg" else "png"
        val mimeType = if (radioJpg.isChecked) "image/jpeg" else "image/png"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "converted_${System.currentTimeMillis()}.$formatName")
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EssentialShortcuts")
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(bytes)
                    }
                    Toast.makeText(this, "Saved to Pictures/EssentialShortcuts", Toast.LENGTH_SHORT).show()
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "EssentialShortcuts")
                dir.mkdirs()
                val file = File(dir, "converted_${System.currentTimeMillis()}.$formatName")
                FileOutputStream(file).use { stream ->
                    stream.write(bytes)
                }
                Toast.makeText(this, "Saved to ${file.absolutePath}", Toast.LENGTH_SHORT).show()
            }
            // Step 3: Save done → sweep to full circle and auto-close
            setResult(RESULT_OK)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareConvertedImage() {
        val bytes = convertedBytes ?: return
        val formatName = if (radioJpg.isChecked) "jpg" else "png"
        val mimeType = if (radioJpg.isChecked) "image/jpeg" else "image/png"

        try {
            val cachePath = File(cacheDir, "converted")
            cachePath.mkdirs()
            val file = File(cachePath, "converted_share.$formatName")
            FileOutputStream(file).use { stream ->
                stream.write(bytes)
            }

            val contentUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = mimeType
            }
            startActivity(Intent.createChooser(shareIntent, "Share Converted Image"))
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
