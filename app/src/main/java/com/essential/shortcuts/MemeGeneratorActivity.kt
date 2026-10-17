package com.essential.shortcuts

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.card.MaterialCardView
import java.io.File
import java.io.FileOutputStream

class MemeGeneratorActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var layoutPickPlaceholder: LinearLayout
    private lateinit var layoutLinesPicker: LinearLayout
    private lateinit var layoutTextInputs: LinearLayout
    private lateinit var etTopText: EditText
    private lateinit var etBottomText: EditText
    private lateinit var btnGenerateMeme: Button
    private lateinit var layoutGenerating: LinearLayout

    // Quick Look Overlay Views
    private lateinit var layoutQuickLook: RelativeLayout
    private lateinit var ivMemePreview: ImageView
    private lateinit var btnQuickLookDone: TextView
    private lateinit var btnQuickLookShare: ImageView
    private lateinit var btnQuickLookSave: ImageView

    private var selectedUri: Uri? = null
    private var originalBitmap: Bitmap? = null
    private var generatedBitmap: Bitmap? = null
    private var selectedLineCountMode: Int = 2 // 1 = Top, 2 = Bottom, 3 = Both

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            loadSelectedImage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meme_generator)
        initViews()
        setupListeners()

        // Handle shared image if launched via Share Sheet or App's selector
        val sharedUri = intent?.getParcelableExtra<Uri>("shared_image_uri")
        if (sharedUri != null) {
            selectedUri = sharedUri
            loadSelectedImage(sharedUri)
        } else if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            val streamUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (streamUri != null) {
                selectedUri = streamUri
                loadSelectedImage(streamUri)
            }
        }
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

            val width = (resources.displayMetrics.widthPixels * 0.97).toInt()
            layoutParams.width = width
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            attributes = layoutParams

            val isFromShare = intent.hasExtra("shared_image_uri")
            if (isFromShare) {
                clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
        }
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tv_title)
        layoutPickPlaceholder = findViewById(R.id.layout_pick_placeholder)
        layoutLinesPicker = findViewById(R.id.layout_lines_picker)
        layoutTextInputs = findViewById(R.id.layout_text_inputs)
        etTopText = findViewById(R.id.et_top_text)
        etBottomText = findViewById(R.id.et_bottom_text)
        btnGenerateMeme = findViewById(R.id.btn_generate_meme)
        layoutGenerating = findViewById(R.id.layout_generating)

        layoutQuickLook = findViewById(R.id.layout_quick_look)
        ivMemePreview = findViewById(R.id.iv_meme_preview)
        btnQuickLookDone = findViewById(R.id.btn_quick_look_done)
        btnQuickLookShare = findViewById(R.id.btn_quick_look_share)
        btnQuickLookSave = findViewById(R.id.btn_quick_look_save)
    }

    private fun setupListeners() {
        layoutPickPlaceholder.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        findViewById<TextView>(R.id.btn_lines_1_top).setOnClickListener {
            selectedLineCountMode = 1
            switchToInputState()
        }

        findViewById<TextView>(R.id.btn_lines_1_bottom).setOnClickListener {
            selectedLineCountMode = 2
            switchToInputState()
        }

        findViewById<TextView>(R.id.btn_lines_2).setOnClickListener {
            selectedLineCountMode = 3
            switchToInputState()
        }

        btnGenerateMeme.setOnClickListener {
            performMemeGeneration()
        }

        btnQuickLookDone.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }

        btnQuickLookShare.setOnClickListener {
            shareMeme()
        }

        btnQuickLookSave.setOnClickListener {
            saveMemeToStorage()
        }
    }

    private fun loadSelectedImage(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                originalBitmap = BitmapFactory.decodeStream(stream)
            }
            if (originalBitmap != null) {
                switchToLinesSelectorState()
            } else {
                Toast.makeText(this, "Failed to decode image", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun switchToLinesSelectorState() {
        tvTitle.text = "How many lines of text?"
        layoutPickPlaceholder.visibility = View.GONE
        layoutLinesPicker.visibility = View.VISIBLE
        layoutTextInputs.visibility = View.GONE
        layoutGenerating.visibility = View.GONE

        // Cascading animation for options
        var delayIndex = 0
        for (i in 0 until layoutLinesPicker.childCount) {
            val child = layoutLinesPicker.getChildAt(i)
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

    private fun switchToInputState() {
        tvTitle.text = "Enter Meme Text"
        layoutPickPlaceholder.visibility = View.GONE
        layoutLinesPicker.visibility = View.GONE
        layoutTextInputs.visibility = View.VISIBLE
        layoutGenerating.visibility = View.GONE

        when (selectedLineCountMode) {
            1 -> {
                etTopText.visibility = View.VISIBLE
                etBottomText.visibility = View.GONE
            }
            2 -> {
                etTopText.visibility = View.GONE
                etBottomText.visibility = View.VISIBLE
            }
            3 -> {
                etTopText.visibility = View.VISIBLE
                etBottomText.visibility = View.VISIBLE
            }
        }
    }

    private fun performMemeGeneration() {
        val original = originalBitmap ?: return
        tvTitle.text = "Generating Meme"
        layoutTextInputs.visibility = View.GONE
        layoutGenerating.visibility = View.VISIBLE

        Thread {
            try {
                val mutableBmp = original.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutableBmp)

                val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    style = Paint.Style.STROKE
                    strokeWidth = (mutableBmp.height / 60f)
                    textSize = (mutableBmp.height / 12f)
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }

                val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                    textSize = (mutableBmp.height / 12f)
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }

                val topText = etTopText.text.toString().uppercase()
                val bottomText = etBottomText.text.toString().uppercase()

                // Draw Top Text
                if (selectedLineCountMode == 1 || selectedLineCountMode == 3) {
                    val x = mutableBmp.width / 2f
                    val y = paintFill.textSize * 1.2f
                    canvas.drawText(topText, x, y, paintStroke)
                    canvas.drawText(topText, x, y, paintFill)
                }

                // Draw Bottom Text
                if (selectedLineCountMode == 2 || selectedLineCountMode == 3) {
                    val x = mutableBmp.width / 2f
                    val y = mutableBmp.height - (paintFill.textSize * 0.5f)
                    canvas.drawText(bottomText, x, y, paintStroke)
                    canvas.drawText(bottomText, x, y, paintFill)
                }

                generatedBitmap = mutableBmp

                runOnUiThread {
                    layoutGenerating.visibility = View.GONE
                    findViewById<View>(R.id.dialog_scroll_container).visibility = View.GONE

                    // Configure window to be full screen for Quick Look overlay
                    window?.apply {
                        setGravity(Gravity.CENTER)
                        val layoutParams = attributes
                        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                        attributes = layoutParams
                    }

                    ivMemePreview.setImageBitmap(generatedBitmap)
                    layoutQuickLook.visibility = View.VISIBLE

                    // Notify hosting progress view that step 2 is complete
                    sendBroadcast(Intent("com.essential.shortcuts.ACTION_STEP_2").setPackage(packageName))
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Generation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    switchToInputState()
                }
            }
        }.start()
    }

    private fun shareMeme() {
        val bitmap = generatedBitmap ?: return
        try {
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "meme_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val contentUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
            }
            startActivity(Intent.createChooser(shareIntent, "Share Meme"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error sharing: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveMemeToStorage() {
        val bitmap = generatedBitmap ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "meme_${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EssentialShortcuts")
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    Toast.makeText(this, "Saved to Pictures/EssentialShortcuts", Toast.LENGTH_SHORT).show()
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "EssentialShortcuts")
                dir.mkdirs()
                val file = File(dir, "meme_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Toast.makeText(this, "Saved to ${file.absolutePath}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
