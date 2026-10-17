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
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ImageCompressorActivity : AppCompatActivity() {

    private lateinit var btnClose: ImageButton
    private lateinit var cardPickImage: MaterialCardView
    private lateinit var layoutPickPlaceholder: LinearLayout
    private lateinit var ivPreview: ImageView
    private lateinit var tvOriginalSize: TextView
    private lateinit var tilTargetSize: TextInputLayout
    private lateinit var etTargetSize: TextInputEditText
    private lateinit var layoutQuality: LinearLayout
    private lateinit var tvQualityValue: TextView
    private lateinit var seekbarQuality: SeekBar
    private lateinit var btnCompress: Button
    private lateinit var cardResult: MaterialCardView
    private lateinit var ivCompressed: ImageView
    private lateinit var tvCompressedInfo: TextView
    private lateinit var btnSave: Button
    private lateinit var btnShare: Button

    private var originalBitmap: Bitmap? = null
    private var compressedBytes: ByteArray? = null
    private var originalFileSize: Long = 0

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { loadImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_compressor)
        initViews()
        setupListeners()

        // Handle image shared from share sheet
        val sharedUri = intent?.getParcelableExtra<Uri>("shared_image_uri")
        if (sharedUri != null) {
            loadImage(sharedUri)
        }
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
        cardPickImage = findViewById(R.id.card_pick_image)
        layoutPickPlaceholder = findViewById(R.id.layout_pick_placeholder)
        ivPreview = findViewById(R.id.iv_preview)
        tvOriginalSize = findViewById(R.id.tv_original_size)
        tilTargetSize = findViewById(R.id.til_target_size)
        etTargetSize = findViewById(R.id.et_target_size)
        layoutQuality = findViewById(R.id.layout_quality)
        tvQualityValue = findViewById(R.id.tv_quality_value)
        seekbarQuality = findViewById(R.id.seekbar_quality)
        btnCompress = findViewById(R.id.btn_compress)
        cardResult = findViewById(R.id.card_result)
        ivCompressed = findViewById(R.id.iv_compressed)
        tvCompressedInfo = findViewById(R.id.tv_compressed_info)
        btnSave = findViewById(R.id.btn_save)
        btnShare = findViewById(R.id.btn_share)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        cardPickImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        seekbarQuality.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvQualityValue.text = "${progress}%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnCompress.setOnClickListener { compressImage() }
        btnSave.setOnClickListener { saveCompressedImage() }
        btnShare.setOnClickListener { shareCompressedImage() }
    }

    private fun loadImage(uri: Uri) {
        try {
            // Get file size
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex >= 0) {
                        originalFileSize = it.getLong(sizeIndex)
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

            val sizeKB = originalFileSize / 1024
            tvOriginalSize.text = "Original: ${bitmap.width}×${bitmap.height} • ${formatFileSize(originalFileSize)}"
            tvOriginalSize.visibility = View.VISIBLE

            // Pre-fill target size to half of original
            etTargetSize.setText((sizeKB / 2).coerceAtLeast(1).toString())

            tilTargetSize.visibility = View.VISIBLE
            layoutQuality.visibility = View.VISIBLE
            seekbarQuality.visibility = View.VISIBLE
            btnCompress.visibility = View.VISIBLE
            cardResult.visibility = View.GONE

        } catch (e: Exception) {
            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun compressImage() {
        val bmp = originalBitmap
        if (bmp == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            return
        }

        val targetKB = etTargetSize.text.toString().toIntOrNull()
        val quality = seekbarQuality.progress.coerceIn(1, 100)

        if (targetKB != null && targetKB > 0) {
            // Iterative compression to hit target size
            compressedBytes = compressToTargetSize(bmp, targetKB)
        } else {
            // Just compress at the specified quality
            val stream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            compressedBytes = stream.toByteArray()
        }

        val bytes = compressedBytes ?: return
        val compressedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        ivCompressed.setImageBitmap(compressedBitmap)

        val reduction = if (originalFileSize > 0) {
            val pct = ((1.0 - bytes.size.toDouble() / originalFileSize) * 100).toInt()
            " • ${pct}% smaller"
        } else ""

        tvCompressedInfo.text = "Size: ${formatFileSize(bytes.size.toLong())}$reduction"
        cardResult.visibility = View.VISIBLE
        // Step 2: Compression done → sweep to 2/3rds
        sendBroadcast(Intent("com.essential.shortcuts.ACTION_STEP_2").setPackage(packageName))

        // Hide keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(etTargetSize.windowToken, 0)
    }

    private fun compressToTargetSize(bitmap: Bitmap, targetKB: Int): ByteArray {
        val targetBytes = targetKB * 1024L
        var quality = seekbarQuality.progress.coerceIn(1, 100)
        var stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)

        // Iteratively reduce quality until target is met or quality is too low
        while (stream.size() > targetBytes && quality > 5) {
            quality -= 5
            stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        }

        // If still too large, scale down the image
        if (stream.size() > targetBytes) {
            var scaleFactor = 0.9f
            var scaledBitmap = bitmap
            while (stream.size() > targetBytes && scaleFactor > 0.1f) {
                val newW = (bitmap.width * scaleFactor).toInt()
                val newH = (bitmap.height * scaleFactor).toInt()
                if (newW <= 0 || newH <= 0) break
                scaledBitmap = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
                stream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                scaleFactor -= 0.1f
            }
        }

        return stream.toByteArray()
    }

    private fun saveCompressedImage() {
        val bytes = compressedBytes ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "compressed_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
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
                val file = File(dir, "compressed_${System.currentTimeMillis()}.jpg")
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

    private fun shareCompressedImage() {
        val bytes = compressedBytes ?: return

        try {
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "compressed_share.jpg")
            FileOutputStream(file).use { stream ->
                stream.write(bytes)
            }

            val contentUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/jpeg"
            }
            startActivity(Intent.createChooser(shareIntent, "Share Compressed Image"))
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
