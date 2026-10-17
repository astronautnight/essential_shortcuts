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
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream

class ImageResizerActivity : AppCompatActivity() {

    private lateinit var btnClose: ImageButton
    private lateinit var cardPickImage: MaterialCardView
    private lateinit var layoutPickPlaceholder: LinearLayout
    private lateinit var ivPreview: ImageView
    private lateinit var tvOriginalSize: TextView
    private lateinit var layoutDimensions: LinearLayout
    private lateinit var etWidth: TextInputEditText
    private lateinit var etHeight: TextInputEditText
    private lateinit var cbAspectRatio: CheckBox
    private lateinit var btnResize: Button
    private lateinit var cardResult: MaterialCardView
    private lateinit var ivResized: ImageView
    private lateinit var tvResizedSize: TextView
    private lateinit var btnSave: Button
    private lateinit var btnShare: Button

    private var originalBitmap: Bitmap? = null
    private var resizedBitmap: Bitmap? = null
    private var isUpdatingDimensions = false

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { loadImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_resizer)
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
        layoutDimensions = findViewById(R.id.layout_dimensions)
        etWidth = findViewById(R.id.et_width)
        etHeight = findViewById(R.id.et_height)
        cbAspectRatio = findViewById(R.id.cb_aspect_ratio)
        btnResize = findViewById(R.id.btn_resize)
        cardResult = findViewById(R.id.card_result)
        ivResized = findViewById(R.id.iv_resized)
        tvResizedSize = findViewById(R.id.tv_resized_size)
        btnSave = findViewById(R.id.btn_save)
        btnShare = findViewById(R.id.btn_share)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        cardPickImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // Aspect ratio: when width changes, auto-update height
        etWidth.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingDimensions || !cbAspectRatio.isChecked) return
                val bmp = originalBitmap ?: return
                val w = s.toString().toIntOrNull() ?: return
                if (w <= 0) return
                val ratio = bmp.height.toFloat() / bmp.width.toFloat()
                val h = (w * ratio).toInt()
                isUpdatingDimensions = true
                etHeight.setText(h.toString())
                isUpdatingDimensions = false
            }
        })

        etHeight.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingDimensions || !cbAspectRatio.isChecked) return
                val bmp = originalBitmap ?: return
                val h = s.toString().toIntOrNull() ?: return
                if (h <= 0) return
                val ratio = bmp.width.toFloat() / bmp.height.toFloat()
                val w = (h * ratio).toInt()
                isUpdatingDimensions = true
                etWidth.setText(w.toString())
                isUpdatingDimensions = false
            }
        })

        btnResize.setOnClickListener {
            resizeImage()
        }

        btnSave.setOnClickListener {
            saveResizedImage()
        }

        btnShare.setOnClickListener {
            shareResizedImage()
        }
    }

    private fun loadImage(uri: Uri) {
        try {
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

            tvOriginalSize.text = "Original: ${bitmap.width} × ${bitmap.height} px"
            tvOriginalSize.visibility = View.VISIBLE

            // Pre-fill dimensions
            isUpdatingDimensions = true
            etWidth.setText(bitmap.width.toString())
            etHeight.setText(bitmap.height.toString())
            isUpdatingDimensions = false

            layoutDimensions.visibility = View.VISIBLE
            cbAspectRatio.visibility = View.VISIBLE
            btnResize.visibility = View.VISIBLE
            cardResult.visibility = View.GONE

        } catch (e: Exception) {
            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resizeImage() {
        val bmp = originalBitmap
        if (bmp == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            return
        }

        val targetW = etWidth.text.toString().toIntOrNull()
        val targetH = etHeight.text.toString().toIntOrNull()

        if (targetW == null || targetH == null || targetW <= 0 || targetH <= 0) {
            Toast.makeText(this, "Enter valid width and height", Toast.LENGTH_SHORT).show()
            return
        }

        if (targetW > 10000 || targetH > 10000) {
            Toast.makeText(this, "Maximum dimension is 10000px", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            resizedBitmap = Bitmap.createScaledBitmap(bmp, targetW, targetH, true)
            ivResized.setImageBitmap(resizedBitmap)
            tvResizedSize.text = "Resized: ${targetW} × ${targetH} px"
            cardResult.visibility = View.VISIBLE
            // Step 2: Resize done → sweep to 2/3rds
            sendBroadcast(Intent("com.essential.shortcuts.ACTION_STEP_2").setPackage(packageName))

            // Hide keyboard
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(etWidth.windowToken, 0)
        } catch (e: Exception) {
            Toast.makeText(this, "Error resizing: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveResizedImage() {
        val bitmap = resizedBitmap ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "resized_${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EssentialShortcuts")
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                    Toast.makeText(this, "Saved to Pictures/EssentialShortcuts", Toast.LENGTH_SHORT).show()
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "EssentialShortcuts")
                dir.mkdirs()
                val file = File(dir, "resized_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                Toast.makeText(this, "Saved to ${file.absolutePath}", Toast.LENGTH_SHORT).show()
            }
            setResult(RESULT_OK)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareResizedImage() {
        val bitmap = resizedBitmap ?: return

        try {
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "resized_share.png")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            val contentUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
            }
            startActivity(Intent.createChooser(shareIntent, "Share Resized Image"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error sharing: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
