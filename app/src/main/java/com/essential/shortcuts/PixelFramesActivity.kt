package com.essential.shortcuts

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
import com.google.android.material.checkbox.MaterialCheckBox
import android.media.MediaMetadataRetriever
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.VideoView
import io.microshow.rxffmpeg.RxFFmpegInvoke
import io.microshow.rxffmpeg.RxFFmpegSubscriber
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class PixelFramesActivity : AppCompatActivity() {

    private lateinit var btnClose: ImageButton
    private lateinit var cardPickImage: MaterialCardView
    private lateinit var layoutPickPlaceholder: LinearLayout
    private lateinit var ivPreview: ImageView
    private lateinit var layoutColorSection: LinearLayout
    private lateinit var layoutPaddingSection: LinearLayout
    private lateinit var etHexColor: TextInputEditText
    private lateinit var seekbarPadding: SeekBar
    private lateinit var tvPaddingValue: TextView
    private lateinit var btnGenerate: Button
    private lateinit var cardResult: MaterialCardView
    private lateinit var ivResult: ImageView
    private lateinit var btnSave: Button
    private lateinit var btnShare: Button

    // Preset color chips
    private lateinit var chipColor1: MaterialCardView
    private lateinit var chipColor2: MaterialCardView
    private lateinit var chipColor3: MaterialCardView
    private lateinit var chipColor4: MaterialCardView
    private lateinit var chipColor5: MaterialCardView
    private lateinit var chipColor6: MaterialCardView
    private lateinit var chipColorCustom: MaterialCardView

    private lateinit var vvPreview: VideoView
    private lateinit var vvResult: VideoView
    private lateinit var layoutProgress: FrameLayout
    private lateinit var tvProgress: TextView

    private lateinit var cbFullQuality: MaterialCheckBox

    private var isVideo: Boolean = false
    private var videoUri: Uri? = null
    private var resultVideoFile: File? = null
    private var isProcessingVideo: Boolean = false
    private val watchdogHandler = Handler(Looper.getMainLooper())
    private var watchdogRunnable: Runnable? = null
    private var hasReceivedProgress = false
    private var ffmpegSubscriber: RxFFmpegSubscriber? = null

    private var screenshotBitmap: Bitmap? = null
    private var resultBitmap: Bitmap? = null
    private var selectedColor: Int = Color.WHITE
    private var paddingPercent: Int = 40
    private var isUpdatingHex = false

    // Preset colors
    private val presetColors = intArrayOf(
        Color.WHITE,            // #FFFFFF
        Color.BLACK,            // #000000
        Color.parseColor("#E8F5E9"),  // Mint green
        Color.parseColor("#E3F2FD"),  // Light blue
        Color.parseColor("#FFF3E0"),  // Light orange
        Color.parseColor("#F3E5F5")   // Light purple
    )

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { loadMedia(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pixel_frames)
        initViews()
        setupListeners()

        // Handle image or video shared from share sheet
        val sharedUri = intent?.getParcelableExtra<Uri>("shared_image_uri")
        if (sharedUri != null) {
            loadMedia(sharedUri)
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
        layoutColorSection = findViewById(R.id.layout_color_section)
        layoutPaddingSection = findViewById(R.id.layout_padding_section)
        etHexColor = findViewById(R.id.et_hex_color)
        seekbarPadding = findViewById(R.id.seekbar_padding)
        tvPaddingValue = findViewById(R.id.tv_padding_value)
        btnGenerate = findViewById(R.id.btn_generate)
        cardResult = findViewById(R.id.card_result)
        ivResult = findViewById(R.id.iv_result)
        vvPreview = findViewById(R.id.vv_preview)
        vvResult = findViewById(R.id.vv_result)
        layoutProgress = findViewById(R.id.layout_progress)
        tvProgress = findViewById(R.id.tv_progress)
        btnSave = findViewById(R.id.btn_save)
        btnShare = findViewById(R.id.btn_share)

        chipColor1 = findViewById(R.id.chip_color_1)
        chipColor2 = findViewById(R.id.chip_color_2)
        chipColor3 = findViewById(R.id.chip_color_3)
        chipColor4 = findViewById(R.id.chip_color_4)
        chipColor5 = findViewById(R.id.chip_color_5)
        chipColor6 = findViewById(R.id.chip_color_6)
        chipColorCustom = findViewById(R.id.chip_color_custom)
        cbFullQuality = findViewById(R.id.cb_full_quality)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        cardPickImage.setOnClickListener {
            imagePickerLauncher.launch("*/*")
        }

        // Preset color chip listeners
        val chipViews = listOf(chipColor1, chipColor2, chipColor3, chipColor4, chipColor5, chipColor6)
        chipViews.forEachIndexed { index, chip ->
            chip.setOnClickListener {
                selectColor(presetColors[index])
                updateChipSelection(chip, chipViews)
            }
        }

        chipColorCustom.setOnClickListener {
            // Just focus the hex input so the user can type a custom color
            etHexColor.requestFocus()
        }

        // Hex color input
        etHexColor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingHex) return
                val hex = s.toString().trim()
                try {
                    val color = Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
                    selectedColor = color
                    // Deselect all preset chips
                    val chipViews = listOf(chipColor1, chipColor2, chipColor3, chipColor4, chipColor5, chipColor6)
                    chipViews.forEach { it.strokeWidth = 2.dpToPx() }
                    chipViews.forEach { it.strokeColor = Color.TRANSPARENT }
                    // Show custom color on the custom chip
                    chipColorCustom.setCardBackgroundColor(color)
                    chipColorCustom.strokeColor = androidx.core.content.ContextCompat.getColor(this@PixelFramesActivity, R.color.category_essential)
                    chipColorCustom.strokeWidth = 3.dpToPx()
                } catch (_: Exception) {
                    // Invalid color — ignorero
                }
            }
        })

        // Padding slider
        seekbarPadding.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                paddingPercent = progress
                tvPaddingValue.text = "${progress}%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnGenerate.setOnClickListener { generateFrame() }
        btnSave.setOnClickListener { saveResult() }
        btnShare.setOnClickListener { shareResult() }

        // Select first color chip by default
        updateChipSelection(chipColor1, listOf(chipColor1, chipColor2, chipColor3, chipColor4, chipColor5, chipColor6))
    }

    private fun selectColor(color: Int) {
        selectedColor = color
        isUpdatingHex = true
        etHexColor.setText(String.format("#%06X", 0xFFFFFF and color))
        isUpdatingHex = false
    }

    private fun updateChipSelection(selected: MaterialCardView, allChips: List<MaterialCardView>) {
        // Reset all chip strokes
        allChips.forEach { chip ->
            chip.strokeColor = Color.TRANSPARENT
            chip.strokeWidth = 2.dpToPx()
        }
        // Also reset custom chip
        chipColorCustom.strokeColor = androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary)
        chipColorCustom.strokeWidth = 1.dpToPx()

        // Highlight selected
        selected.strokeColor = androidx.core.content.ContextCompat.getColor(this, R.color.category_essential)
        selected.strokeWidth = 3.dpToPx()

        // Special case: white chip always has a visible border
        chipColor1.strokeColor = if (selected == chipColor1) {
            androidx.core.content.ContextCompat.getColor(this, R.color.category_essential)
        } else {
            androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary)
        }
    }

    private fun loadMedia(uri: Uri) {
        try {
            val mimeType = contentResolver.getType(uri)
            isVideo = mimeType?.startsWith("video/") == true

            if (isVideo) {
                videoUri = uri
                layoutPickPlaceholder.visibility = View.GONE
                ivPreview.visibility = View.GONE
                vvPreview.visibility = View.VISIBLE
                vvPreview.setVideoURI(uri)
                vvPreview.setOnPreparedListener { mp ->
                    mp.isLooping = true
                    vvPreview.start()
                }
            } else {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap == null) {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                    return
                }
                screenshotBitmap = bitmap
                layoutPickPlaceholder.visibility = View.GONE
                vvPreview.visibility = View.GONE
                ivPreview.visibility = View.VISIBLE
                ivPreview.setImageBitmap(bitmap)
            }

            // Show controls
            layoutColorSection.visibility = View.VISIBLE
            layoutPaddingSection.visibility = View.VISIBLE
            btnGenerate.visibility = View.VISIBLE
            cardResult.visibility = View.GONE
            
            if (isVideo) {
                cbFullQuality.visibility = View.VISIBLE
            } else {
                cbFullQuality.visibility = View.GONE
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error loading media: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateFrame() {
        if (isVideo) {
            if (videoUri == null) {
                Toast.makeText(this, "Please select a video first", Toast.LENGTH_SHORT).show()
                return
            }
            if (isProcessingVideo) {
                Toast.makeText(this, "Video is already being processed", Toast.LENGTH_SHORT).show()
                return
            }
            processVideoFrame()
        } else {
            val screenshot = screenshotBitmap
            if (screenshot == null) {
                Toast.makeText(this, "Please select a screenshot first", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                resultBitmap = createPixelFrame(screenshot, selectedColor, paddingPercent, false)
                vvResult.visibility = View.GONE
                ivResult.visibility = View.VISIBLE
                ivResult.setImageBitmap(resultBitmap)
                cardResult.visibility = View.VISIBLE
                // Step 2: Frame generated → sweep to 2/3rds
                sendBroadcast(Intent("com.essential.shortcuts.ACTION_STEP_2").setPackage(packageName))

                // Hide keyboard
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(etHexColor.windowToken, 0)
            } catch (e: Exception) {
                Toast.makeText(this, "Error generating frame: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopWatchdog() {
        watchdogRunnable?.let { watchdogHandler.removeCallbacks(it) }
        watchdogRunnable = null
    }

    private fun startWatchdog() {
        stopWatchdog()
        hasReceivedProgress = false
        watchdogRunnable = Runnable {
            if (isProcessingVideo && !hasReceivedProgress) {
                Log.e("PixelFrames", "Watchdog: FFmpeg stuck at 0% for 30s, killing...")
                // Dispose RxJava subscription + kill native process
                ffmpegSubscriber?.dispose()
                try { RxFFmpegInvoke.getInstance().exit() } catch (_: Exception) {}
                isProcessingVideo = false
                btnGenerate.isEnabled = true
                btnGenerate.alpha = 1f
                layoutProgress.visibility = View.GONE
                Toast.makeText(this, "Video rendering timed out. Please try again.", Toast.LENGTH_LONG).show()
            }
        }
        // 30 second timeout if no progress is received
        watchdogHandler.postDelayed(watchdogRunnable!!, 30_000L)
    }

    private fun finishProcessing() {
        isProcessingVideo = false
        stopWatchdog()
        runOnUiThread {
            btnGenerate.isEnabled = true
            btnGenerate.alpha = 1f
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWatchdog()
        ffmpegSubscriber?.dispose()
        try { RxFFmpegInvoke.getInstance().exit() } catch (_: Exception) {}
    }

    private fun processVideoFrame() {
        isProcessingVideo = true
        layoutProgress.visibility = View.VISIBLE
        btnGenerate.isEnabled = false
        btnGenerate.alpha = 0.5f

        // Hide keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(etHexColor.windowToken, 0)

        Thread {
            try {
                // 1. Get Video Dimensions
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(this@PixelFramesActivity, videoUri)
                val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                retriever.release()

                val durationMs = durationStr?.toLongOrNull() ?: 1000L
                val totalMicroseconds = durationMs * 1000L

                var videoW = widthStr?.toFloatOrNull() ?: 1080f
                var videoH = heightStr?.toFloatOrNull() ?: 1920f
                val rotation = rotationStr?.toIntOrNull() ?: 0
                if (rotation == 90 || rotation == 270) {
                    val temp = videoW
                    videoW = videoH
                    videoH = temp
                }

                // Ensure even dimensions for video codec compatibility
                var videoWInt = videoW.roundToInt()
                var videoHInt = videoH.roundToInt()
                if (videoWInt % 2 != 0) videoWInt++
                if (videoHInt % 2 != 0) videoHInt++
                videoW = videoWInt.toFloat()
                videoH = videoHInt.toFloat()

                val isFullQuality = cbFullQuality.isChecked

                if (!isFullQuality) {
                    // Downscale large screen recordings for performance
                    val maxDim = Math.max(videoW, videoH)
                    val MAX_DIM = 960f
                    if (maxDim > MAX_DIM) {
                        val scaleFactor = MAX_DIM / maxDim
                        videoW *= scaleFactor
                        videoH *= scaleFactor

                        var scaledW = videoW.roundToInt()
                        var scaledH = videoH.roundToInt()
                        if (scaledW % 2 != 0) scaledW++
                        if (scaledH % 2 != 0) scaledH++
                        videoW = scaledW.toFloat()
                        videoH = scaledH.toFloat()
                    }
                }

                // 2. Generate Transparent Frame Template
                val dummyBitmap = Bitmap.createBitmap(videoW.toInt(), videoH.toInt(), Bitmap.Config.ARGB_8888)
                val frameTemplate = createPixelFrame(dummyBitmap, selectedColor, paddingPercent, true)

                val cacheDir = File(cacheDir, "pixel_frames")
                cacheDir.mkdirs()

                val templateFile = File(cacheDir, "template.png")
                FileOutputStream(templateFile).use { out ->
                    frameTemplate.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                // Calculate metrics for FFmpeg
                val screenW = videoW
                val bezelThickness = screenW * 0.022f
                val frameThickness = screenW * 0.030f
                val phoneW = screenW + 2 * (bezelThickness + frameThickness)
                val phoneH = videoH + 2 * (bezelThickness + frameThickness)

                val paddingFraction = paddingPercent / 100f
                val padX = (phoneW * paddingFraction * 0.5f)
                val padY = (phoneH * paddingFraction * 0.5f)

                val canvasW = frameTemplate.width
                val canvasH = frameTemplate.height

                val screenX = (padX + frameThickness + bezelThickness).roundToInt()
                val screenY = (padY + frameThickness + bezelThickness).roundToInt()

                // Create input file from URI
                val inputFile = File(cacheDir, "input.mp4")
                contentResolver.openInputStream(videoUri!!)?.use { input ->
                    FileOutputStream(inputFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val outputFile = File(cacheDir, "output_${System.currentTimeMillis()}.mp4")

                val progressTick = findViewById<ProgressTickView>(R.id.progress_tick)
                runOnUiThread {
                    tvProgress.text = "Rendering Video..."
                    progressTick.setProgress(0f, androidx.core.content.ContextCompat.getColor(this@PixelFramesActivity, R.color.category_essential))
                }

                val filterComplex = if (isFullQuality) {
                    "[0:v]scale=${videoW.roundToInt()}:${videoH.roundToInt()}[v_scaled]; [v_scaled]pad=$canvasW:$canvasH:$screenX:$screenY:black[v_padded]; [v_padded][1:v]overlay=0:0,format=yuv420p[out]"
                } else {
                    "[0:v]fps=30,scale=${videoW.roundToInt()}:${videoH.roundToInt()}[v_scaled]; [v_scaled]pad=$canvasW:$canvasH:$screenX:$screenY:black[v_padded]; [v_padded][1:v]overlay=0:0,format=yuv420p[out]"
                }
                val crfVal = if (isFullQuality) "23" else "28"

                val commands = arrayOf("-y", "-nostdin", "-i", inputFile.absolutePath, "-i", templateFile.absolutePath,
                        "-filter_complex", filterComplex,
                        "-map", "[out]", "-map", "0:a?", "-c:v", "libx264", "-preset", "ultrafast", "-threads", "0", "-crf", crfVal, "-c:a", "copy", outputFile.absolutePath)

                Log.d("PixelFrames", "FFmpeg command: ${commands.joinToString(" ")}")
                Log.d("PixelFrames", "Input exists: ${inputFile.exists()}, size: ${inputFile.length()}")
                Log.d("PixelFrames", "Template exists: ${templateFile.exists()}, size: ${templateFile.length()}")
                Log.d("PixelFrames", "Canvas: ${canvasW}x${canvasH}, Screen offset: ${screenX},${screenY}")

                // Enable native FFmpeg debug logging
                RxFFmpegInvoke.getInstance().setDebug(true)

                // Start watchdog timer
                runOnUiThread { startWatchdog() }

                // Use RxJava API (recommended by library) — non-blocking, callbacks on main thread
                ffmpegSubscriber = object : RxFFmpegSubscriber() {
                    override fun onFinish() {
                        if (!isProcessingVideo) return // Watchdog already handled this
                        finishProcessing()
                        layoutProgress.visibility = View.GONE
                        if (outputFile.exists() && outputFile.length() > 0) {
                            resultVideoFile = outputFile
                            ivResult.visibility = View.GONE
                            vvResult.visibility = View.VISIBLE
                            vvResult.setVideoPath(outputFile.absolutePath)
                            vvResult.setOnPreparedListener { mp ->
                                mp.isLooping = true
                                vvResult.start()
                            }
                            cardResult.visibility = View.VISIBLE
                            sendBroadcast(Intent("com.essential.shortcuts.ACTION_STEP_2").setPackage(packageName))
                        } else {
                            Toast.makeText(this@PixelFramesActivity, "Video processing completed but output file is missing", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onProgress(progress: Int, progressTime: Long) {
                        if (!isProcessingVideo) return
                        hasReceivedProgress = true
                        val percent = progressTime.toFloat() / totalMicroseconds.toFloat()
                        val clamped = percent.coerceIn(0f, 1f)
                        progressTick.setProgress(clamped, androidx.core.content.ContextCompat.getColor(this@PixelFramesActivity, R.color.category_essential))
                        tvProgress.text = "Rendering Video... ${(clamped * 100).toInt()}%"
                    }

                    override fun onCancel() {
                        if (!isProcessingVideo) return
                        finishProcessing()
                        layoutProgress.visibility = View.GONE
                        Toast.makeText(this@PixelFramesActivity, "Video processing cancelled", Toast.LENGTH_SHORT).show()
                    }

                    override fun onError(message: String?) {
                        if (!isProcessingVideo) return
                        Log.e("PixelFrames", "FFmpeg error: $message")
                        finishProcessing()
                        layoutProgress.visibility = View.GONE
                        Toast.makeText(this@PixelFramesActivity, "Video processing error: $message", Toast.LENGTH_LONG).show()
                    }
                }

                RxFFmpegInvoke.getInstance()
                    .runCommandRxJava(commands)
                    .subscribe(ffmpegSubscriber!!)

            } catch (e: Exception) {
                Log.e("PixelFrames", "Error in processVideoFrame", e)
                finishProcessing()
                runOnUiThread {
                    layoutProgress.visibility = View.GONE
                    Toast.makeText(this, "Error processing video: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    /**
     * Creates a framed screenshot by drawing the Pixel 10 phone cutout around the image.
     *
     * Creates a framed screenshot by drawing the Pixel 10 Pro XL phone cutout around the image.
     *
     * Precise proportions based on Pixel 10 Pro XL (derived from Pixel 9 Pro XL measurements):
     * - Phone body:  76.6 × 162.8 mm  (width × height)
     * - Side bezels:  ~2.8 mm each
     * - Top bezel:    ~3.0 mm
     * - Bottom bezel: ~3.0 mm
     * - Screen-to-body: ~89.5%
     * - Display resolution: 1344 × 2992 (aspect ratio ~2.226:1)
     * - Phone corner radius: ~9.0 mm
     * - Display corner radius: ~7.5 mm
     * - Selfie punch-hole: ~5 mm diameter, centered, ~8 mm from top edge
     * - Metal frame thickness (visible edge): ~1.5 mm
     *
     * All proportions are expressed as ratios of phone width for scale-independence.
     */
    private fun createPixelFrame(screenshot: Bitmap, bgColor: Int, paddingPct: Int, isVideoTemplate: Boolean): Bitmap {
        val screenW = screenshot.width.toFloat()
        val screenH = screenshot.height.toFloat()

        // Match the reference image: thicker frame, extremely rounded corners
        val bezelThickness = screenW * 0.022f // Reduced: thinner black bezel around the screen
        val frameThickness = screenW * 0.030f // Slightly thicker to allow for a better 3D rounded gloss effect

        val phoneW = screenW + 2 * (bezelThickness + frameThickness)
        val phoneH = screenH + 2 * (bezelThickness + frameThickness)

        // Calculate padding around the phone
        val paddingFraction = paddingPct / 100f
        val padX = (phoneW * paddingFraction * 0.5f).toInt()
        val padY = (phoneH * paddingFraction * 0.5f).toInt()

        // Final canvas size (ensure even for h264 yuv420p compatibility)
        var canvasW = phoneW.toInt() + padX * 2
        var canvasH = phoneH.toInt() + padY * 2
        if (canvasW % 2 != 0) canvasW++
        if (canvasH % 2 != 0) canvasH++

        val result = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 1. Draw solid background
        canvas.drawColor(bgColor)

        val phoneLeft = padX.toFloat()
        val phoneTop = padY.toFloat()
        val phoneRight = phoneLeft + phoneW
        val phoneBottom = phoneTop + phoneH

        // Significantly more rounded corners matching Pixel 10 Pro
        val outerRadius = phoneW * 0.17f
        val bezelRadius = outerRadius - frameThickness
        val screenRadius = bezelRadius - bezelThickness

        // 2. Shiny Dark Metal Frame (Outer Body)
        // We use a high-contrast base gradient to give it a 3D curved cylinder look from the front
        val metalBaseGradient = android.graphics.LinearGradient(
            phoneLeft, phoneTop, phoneRight, phoneBottom,
            intArrayOf(
                Color.parseColor("#9C9C9E"), // Bright top-left rim reflection
                Color.parseColor("#2A2A2C"), // Mid body
                Color.parseColor("#151515"), // Deep curve shadow
                Color.parseColor("#3C3C3E"), // Secondary bounce light
                Color.parseColor("#8E8E90")  // Bottom-right rim reflection
            ),
            floatArrayOf(0.0f, 0.15f, 0.6f, 0.85f, 1.0f),
            android.graphics.Shader.TileMode.CLAMP
        )
        val metalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = metalBaseGradient
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(phoneLeft, phoneTop, phoneRight, phoneBottom),
            outerRadius, outerRadius, metalPaint
        )

        // 3. 3D Rounded Specular Highlights (Double Rim)
        // Outer glossy highlight
        val outerGlossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(
                phoneLeft, phoneTop, phoneRight, phoneBottom,
                intArrayOf(Color.parseColor("#FFFFFF"), Color.TRANSPARENT, Color.TRANSPARENT, Color.parseColor("#C0C0C5")),
                floatArrayOf(0.0f, 0.25f, 0.75f, 1.0f),
                android.graphics.Shader.TileMode.CLAMP
            )
            style = Paint.Style.STROKE
            strokeWidth = phoneW * 0.006f
        }
        val outerGlossInset = phoneW * 0.003f
        canvas.drawRoundRect(
            RectF(phoneLeft + outerGlossInset, phoneTop + outerGlossInset, phoneRight - outerGlossInset, phoneBottom - outerGlossInset),
            outerRadius - outerGlossInset, outerRadius - outerGlossInset, outerGlossPaint
        )

        // Inner edge highlight (where metal meets the screen assembly)
        val innerGlossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(
                phoneRight, phoneTop, phoneLeft, phoneBottom, // Opposite diagonal for complex light interplay
                intArrayOf(Color.parseColor("#60FFFFFF"), Color.TRANSPARENT, Color.parseColor("#40FFFFFF")),
                floatArrayOf(0.0f, 0.5f, 1.0f),
                android.graphics.Shader.TileMode.CLAMP
            )
            style = Paint.Style.STROKE
            strokeWidth = phoneW * 0.004f
        }
        val innerGlossInset = frameThickness - phoneW * 0.002f
        canvas.drawRoundRect(
            RectF(phoneLeft + innerGlossInset, phoneTop + innerGlossInset, phoneRight - innerGlossInset, phoneBottom - innerGlossInset),
            outerRadius - innerGlossInset, outerRadius - innerGlossInset, innerGlossPaint
        )

        // 4. Antenna Bands
        // Typical placement: two on top, two on bottom, one on each side
        val antennaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#151515") // Deeper matte plastic for high contrast
            strokeWidth = phoneW * 0.008f
            style = Paint.Style.FILL
        }
        val bandInset = phoneW * 0.25f
        // Top bands
        canvas.drawLine(phoneLeft + bandInset, phoneTop, phoneLeft + bandInset, phoneTop + frameThickness, antennaPaint)
        canvas.drawLine(phoneRight - bandInset, phoneTop, phoneRight - bandInset, phoneTop + frameThickness, antennaPaint)
        // Bottom bands
        canvas.drawLine(phoneLeft + bandInset, phoneBottom - frameThickness, phoneLeft + bandInset, phoneBottom, antennaPaint)
        canvas.drawLine(phoneRight - bandInset, phoneBottom - frameThickness, phoneRight - bandInset, phoneBottom, antennaPaint)
        // Side bands (near top)
        canvas.drawLine(phoneLeft, phoneTop + bandInset, phoneLeft + frameThickness, phoneTop + bandInset, antennaPaint)
        canvas.drawLine(phoneRight - frameThickness, phoneTop + bandInset, phoneRight, phoneTop + bandInset, antennaPaint)

        // 5. Inner Bezel (The black border around the screen)
        val bezelLeft = phoneLeft + frameThickness
        val bezelTop = phoneTop + frameThickness
        val bezelRight = phoneRight - frameThickness
        val bezelBottom = phoneBottom - frameThickness

        val bezelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#050505") // Deep black
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(bezelLeft, bezelTop, bezelRight, bezelBottom),
            bezelRadius, bezelRadius, bezelPaint
        )

        // 6. Draw Screenshot or Clear Screen Area
        val screenLeft = bezelLeft + bezelThickness
        val screenTop = bezelTop + bezelThickness
        val screenRight = bezelRight - bezelThickness
        val screenBottom = bezelBottom - bezelThickness

        val screenPath = Path()
        screenPath.addRoundRect(
            RectF(screenLeft, screenTop, screenRight, screenBottom),
            screenRadius, screenRadius, Path.Direction.CW
        )
        
        if (isVideoTemplate) {
            // Cut a transparent hole where the video will be
            val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
            }
            canvas.drawPath(screenPath, clearPaint)
        } else {
            canvas.save()
            canvas.clipPath(screenPath)
            canvas.drawBitmap(
                screenshot,
                null,
                RectF(screenLeft, screenTop, screenRight, screenBottom),
                Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            )
            canvas.restore()
        }

        // 7. Glass Sheen / Reflection Overlay
        // Adds a premium glossy look across the whole screen/bezel area
        val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(
                phoneLeft, phoneTop, phoneRight, phoneBottom,
                intArrayOf(
                    Color.parseColor("#1AFFFFFF"),
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.parseColor("#08FFFFFF")
                ),
                floatArrayOf(0.0f, 0.4f, 0.65f, 1.0f),
                android.graphics.Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(bezelLeft, bezelTop, bezelRight, bezelBottom),
            bezelRadius, bezelRadius, sheenPaint
        )

        // 8. Selfie Camera Punch-Hole
        val cameraCx = phoneLeft + phoneW * 0.5f
        val cameraCy = screenTop + screenW * 0.055f // Moved slightly lower due to larger size
        val cameraRadius = screenW * 0.032f // Significantly Increased: matching real size

        // Camera lens housing (subtle dark grey edge)
        val cameraOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A1A1C")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cameraCx, cameraCy, cameraRadius * 1.08f, cameraOuterPaint)

        // Camera hole (deep black)
        val cameraInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#020202")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cameraCx, cameraCy, cameraRadius, cameraInnerPaint)

        // Subtle blueish/greenish lens reflection inside
        val lensReflectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0A151A")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cameraCx, cameraCy, cameraRadius * 0.65f, lensReflectPaint)

        // Tiny specular highlight dot
        val specularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#50FFFFFF")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cameraCx - cameraRadius * 0.25f, cameraCy - cameraRadius * 0.25f, cameraRadius * 0.2f, specularPaint)

        // 9. Side Buttons (Shiny Metal)
        val btnWidth = phoneW * 0.008f
        val btnRadius = btnWidth * 0.5f
        val buttonGradient = android.graphics.LinearGradient(
            phoneRight, phoneTop, phoneRight + btnWidth, phoneTop, // horizontal gradient for buttons
            intArrayOf(
                Color.parseColor("#8E8E93"),
                Color.parseColor("#D1D1D6"),
                Color.parseColor("#3A3A3C")
            ),
            null,
            android.graphics.Shader.TileMode.CLAMP
        )
        val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = buttonGradient
            style = Paint.Style.FILL
        }

        // Power button
        val pwrTop = phoneTop + phoneH * 0.22f
        val pwrBot = phoneTop + phoneH * 0.29f
        canvas.drawRoundRect(
            RectF(phoneRight - 1f, pwrTop, phoneRight + btnWidth, pwrBot),
            btnRadius, btnRadius, buttonPaint
        )

        // Volume rocker
        val volTop = phoneTop + phoneH * 0.33f
        val volBot = phoneTop + phoneH * 0.45f
        canvas.drawRoundRect(
            RectF(phoneRight - 1f, volTop, phoneRight + btnWidth, volBot),
            btnRadius, btnRadius, buttonPaint
        )

        return result
    }

    private fun saveResult() {
        if (isVideo) {
            val videoFile = resultVideoFile ?: return
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, "pixel_frame_${System.currentTimeMillis()}.mp4")
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/EssentialShortcuts")
                    }
                    val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        contentResolver.openOutputStream(uri)?.use { stream ->
                            videoFile.inputStream().copyTo(stream)
                        }
                        Toast.makeText(this, "Saved to Movies/EssentialShortcuts", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val dir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                        "EssentialShortcuts"
                    )
                    dir.mkdirs()
                    val file = File(dir, "pixel_frame_${System.currentTimeMillis()}.mp4")
                    videoFile.copyTo(file, overwrite = true)
                    Toast.makeText(this, "Saved to ${file.absolutePath}", Toast.LENGTH_SHORT).show()
                }
                setResult(RESULT_OK)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this, "Error saving video: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            val bitmap = resultBitmap ?: return

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "pixel_frame_${System.currentTimeMillis()}.png")
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
                    val dir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "EssentialShortcuts"
                    )
                    dir.mkdirs()
                    val file = File(dir, "pixel_frame_${System.currentTimeMillis()}.png")
                    FileOutputStream(file).use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
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
    }

    private fun shareResult() {
        if (isVideo) {
            val videoFile = resultVideoFile ?: return
            try {
                val contentUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", videoFile)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    type = "video/mp4"
                }
                startActivity(Intent.createChooser(shareIntent, "Share Framed Video"))
            } catch (e: Exception) {
                Toast.makeText(this, "Error sharing: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            val bitmap = resultBitmap ?: return

            try {
                val cachePath = File(cacheDir, "images")
                cachePath.mkdirs()
                val file = File(cachePath, "pixel_frame_share.png")
                FileOutputStream(file).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }

                val contentUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    type = "image/png"
                }
                startActivity(Intent.createChooser(shareIntent, "Share Framed Screenshot"))
            } catch (e: Exception) {
                Toast.makeText(this, "Error sharing: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
