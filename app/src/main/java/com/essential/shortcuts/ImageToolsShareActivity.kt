package com.essential.shortcuts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.ViewGroup
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

/**
 * Receives files via the Android share sheet (ACTION_SEND, ACTION_SEND_MULTIPLE).
 *
 * Displays a chooser popup asking "What do you want to do with this file?"
 * with vertical item layouts. On selection, forwards the file URI to the chosen
 * tool activity.
 */
class ImageToolsShareActivity : AppCompatActivity() {

    private var sharedUris: List<Uri> = emptyList()
    private var isPdfShare = false
    private var isWordShare = false
    private var isVideoShare = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_tools_share)

        // Extract single or multiple shared URIs
        val action = intent?.action
        val type = intent?.type

        if (action == Intent.ACTION_SEND) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
                ?: intent.data
            if (uri != null) {
                sharedUris = listOf(uri)
            }
        } else if (action == Intent.ACTION_SEND_MULTIPLE) {
            val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            if (uris != null) {
                sharedUris = uris
            } else if (intent.clipData != null) {
                val list = mutableListOf<Uri>()
                val count = intent.clipData?.itemCount ?: 0
                for (i in 0 until count) {
                    intent.clipData?.getItemAt(i)?.uri?.let { list.add(it) }
                }
                sharedUris = list
            }
        }

        if (sharedUris.isEmpty()) {
            Toast.makeText(this, "No files received", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        detectFileType(type)
        setupToolCards()
        configureShareLayout()
        animateEntrance()
    }

    private fun detectFileType(intentType: String?) {
        var pdfCount = 0
        var wordCount = 0
        var videoCount = 0
        var imageCount = 0

        for (uri in sharedUris) {
            var fileName = ""
            var uriMime: String? = null
            try {
                uriMime = contentResolver.getType(uri)
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx != -1) fileName = cursor.getString(idx) ?: ""
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
            if (fileName.isEmpty()) {
                fileName = uri.lastPathSegment ?: ""
            }

            val lowerName = fileName.lowercase()
            val effectiveType = (intentType ?: uriMime ?: "").lowercase()

            when {
                effectiveType.contains("pdf") || lowerName.endsWith(".pdf") -> pdfCount++
                effectiveType.contains("word") ||
                        effectiveType.contains("officedocument.wordprocessingml") ||
                        effectiveType.contains("msword") ||
                        lowerName.endsWith(".docx") ||
                        lowerName.endsWith(".doc") -> wordCount++
                effectiveType.startsWith("video/") || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") -> videoCount++
                else -> imageCount++
            }
        }

        isPdfShare = pdfCount > 0 && pdfCount >= wordCount
        isWordShare = wordCount > 0 && wordCount >= pdfCount
        isVideoShare = videoCount > 0 && videoCount >= imageCount
    }

    override fun onStart() {
        super.onStart()
        window?.apply {
            setGravity(android.view.Gravity.TOP)
            
            val layoutParams = attributes
            val density = resources.displayMetrics.density
            
            // Get dynamic status bar height
            var statusBarHeight = 0
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                statusBarHeight = resources.getDimensionPixelSize(resourceId)
            }
            
            // Start the window at the very top (y = 0) to allow drawing around the camera
            layoutParams.y = 0
            
            // Almost edge-to-edge (97% width)
            val width = (resources.displayMetrics.widthPixels * 0.97).toInt()
            layoutParams.width = width
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            
            attributes = layoutParams

            // Shift dialog card container below status bar programmatically
            val cardContainer = findViewById<View>(R.id.dialog_card_container)
            val cardParams = cardContainer.layoutParams as FrameLayout.LayoutParams
            cardParams.topMargin = statusBarHeight + (6 * density).toInt()
            cardContainer.layoutParams = cardParams

            // Position the Dynamic Island pill at the very top to align with the camera
            val pill = findViewById<View>(R.id.dynamic_island_pill)
            val pillParams = pill.layoutParams as FrameLayout.LayoutParams
            pillParams.topMargin = (2 * density).toInt()
            pill.layoutParams = pillParams
        }
    }

    private val REQUEST_TOOL = 1001

    private fun setupToolCards() {
        // Image Tools
        findViewById<TextView>(R.id.card_resize).setOnClickListener {
            launchTool(ImageResizerActivity::class.java)
        }
        findViewById<TextView>(R.id.card_rounded).setOnClickListener {
            launchTool(RoundedImageActivity::class.java)
        }
        findViewById<TextView>(R.id.card_compress).setOnClickListener {
            launchTool(ImageCompressorActivity::class.java)
        }
        findViewById<TextView>(R.id.card_pixel_frame).setOnClickListener {
            launchTool(PixelFramesActivity::class.java)
        }
        findViewById<TextView>(R.id.card_laptop_frame).setOnClickListener {
            launchTool(LaptopFramesActivity::class.java)
        }
        findViewById<TextView>(R.id.card_meme).setOnClickListener {
            launchTool(MemeGeneratorActivity::class.java)
        }

        // Converter Tools
        findViewById<TextView>(R.id.card_convert_image).setOnClickListener {
            launchTool(ImageConverterActivity::class.java)
        }
        findViewById<TextView>(R.id.card_pdf_combine).setOnClickListener {
            launchTool(PdfCombineActivity::class.java)
        }

        // Word & PDF Converter Tools
        findViewById<TextView>(R.id.card_pdf_to_word).setOnClickListener {
            launchTool(WordPdfConverterActivity::class.java, "pdf_to_word")
        }
        findViewById<TextView>(R.id.card_word_to_pdf).setOnClickListener {
            launchTool(WordPdfConverterActivity::class.java, "word_to_pdf")
        }
    }

    private fun configureShareLayout() {
        val container = findViewById<ViewGroup>(R.id.options_container)
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            when {
                isWordShare -> {
                    // Word Share: Show ONLY "Convert to PDF"
                    child.visibility = if (child.id == R.id.card_word_to_pdf) View.VISIBLE else View.GONE
                }
                isPdfShare -> {
                    // PDF Share: Show "Convert to Word" and "Combine PDFs"
                    child.visibility = when (child.id) {
                        R.id.card_pdf_to_word,
                        R.id.divider_pdf_to_word,
                        R.id.card_pdf_combine -> View.VISIBLE
                        else -> View.GONE
                    }
                }
                isVideoShare -> {
                    // Video share: Show ONLY Pixel Frames and Laptop Frames
                    child.visibility = if (child.id == R.id.card_pixel_frame || child.id == R.id.card_laptop_frame) View.VISIBLE else View.GONE
                }
                else -> {
                    // Image Share: Show image tools, Hide PDF/Word options
                    val isPdfOrWord = child.id == R.id.card_pdf_combine ||
                            child.id == R.id.card_pdf_to_word ||
                            child.id == R.id.divider_pdf_to_word ||
                            child.id == R.id.divider_word_to_pdf ||
                            child.id == R.id.card_word_to_pdf

                    if (isPdfOrWord) {
                        child.visibility = View.GONE
                    } else {
                        // If multiple images are shared, hide all tools except "Convert Image"
                        if (sharedUris.size > 1 && child.id != R.id.card_convert_image) {
                            child.visibility = View.GONE
                        } else {
                            child.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun animateEntrance() {
        val container = findViewById<ViewGroup>(R.id.options_container)
        var delayIndex = 0
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            // Only animate visible options
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

    private fun launchTool(activityClass: Class<*>, mode: String? = null) {
        val cardContainer = findViewById<View>(R.id.dialog_card_container)
        cardContainer.animate()
            .alpha(0f)
            .scaleX(0.7f)
            .scaleY(0.7f)
            .setDuration(250)
            .withEndAction {
                cardContainer.visibility = View.INVISIBLE
            }
            .start()

        // Configure Dynamic Island Pill
        val pillContainer = findViewById<View>(R.id.dynamic_island_pill)
        val pillIcon = findViewById<ImageView>(R.id.pill_tool_icon)
        val closeBtn = findViewById<View>(R.id.pill_close_btn)

        // Set tool icon
        val iconRes = when (activityClass) {
            ImageResizerActivity::class.java -> android.R.drawable.ic_menu_crop
            RoundedImageActivity::class.java -> R.drawable.ic_rounded_image
            ImageCompressorActivity::class.java -> R.drawable.ic_compress
            PixelFramesActivity::class.java -> R.drawable.ic_pixel_frame
            LaptopFramesActivity::class.java -> R.drawable.ic_laptop_frame
            MemeGeneratorActivity::class.java -> android.R.drawable.ic_menu_edit
            ImageConverterActivity::class.java -> R.drawable.ic_jpg_png
            PdfCombineActivity::class.java -> R.drawable.ic_pdf_combine
            WordPdfConverterActivity::class.java -> R.drawable.ic_word_pdf
            else -> R.drawable.ic_launcher_foreground
        }
        pillIcon.setImageResource(iconRes)

        // Initial setup for Dynamic Island animation (pop/bounce entrance like iPhone)
        pillContainer.visibility = View.VISIBLE
        pillContainer.alpha = 0f
        pillContainer.scaleX = 0.5f
        pillContainer.scaleY = 0.5f
        pillContainer.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
            .start()

        val progressTick = findViewById<ProgressTickView>(R.id.progress_tick)
        
        closeBtn.setOnClickListener {
            progressTick.animateRevert {
                finish()
            }
        }

        progressTick.animateStart {
            val toolIntent = Intent(this, activityClass).apply {
                if (mode != null) {
                    putExtra("converter_mode", mode)
                }
                if (sharedUris.size == 1) {
                    putExtra("shared_image_uri", sharedUris[0])
                    putExtra(Intent.EXTRA_STREAM, sharedUris[0])
                    data = sharedUris[0]
                    action = Intent.ACTION_SEND
                    clipData = android.content.ClipData.newRawUri("shared_file", sharedUris[0])
                } else {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(sharedUris))
                    action = Intent.ACTION_SEND_MULTIPLE
                    type = when {
                        isPdfShare -> "application/pdf"
                        isWordShare -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        else -> "image/*"
                    }
                    if (sharedUris.isNotEmpty()) {
                        val clip = android.content.ClipData.newRawUri("shared_files", sharedUris[0])
                        for (i in 1 until sharedUris.size) {
                            clip.addItem(android.content.ClipData.Item(sharedUris[i]))
                        }
                        clipData = clip
                    }
                }
                // Grant read permission so the tool activity can access the shared URIs
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            @Suppress("DEPRECATION")
            startActivityForResult(toolIntent, REQUEST_TOOL)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TOOL) {
            val progressTick = findViewById<ProgressTickView>(R.id.progress_tick)
            if (resultCode == RESULT_OK) {
                progressTick.animateComplete {
                    finish()
                }
            } else {
                progressTick.animateRevert {
                    finish()
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}
