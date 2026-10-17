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

class LaptopFramesActivity : AppCompatActivity() {

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
    private lateinit var cardModelWindows: MaterialCardView
    private lateinit var cardModelMacos: MaterialCardView

    private var isVideo: Boolean = false
    private var videoUri: Uri? = null
    private var resultVideoFile: File? = null
    private var isMacOsModel: Boolean = true
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
        setContentView(R.layout.activity_laptop_frames)
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
        cardModelWindows = findViewById(R.id.card_model_windows)
        cardModelMacos = findViewById(R.id.card_model_macos)
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
                    val chipViews = listOf(chipColor1, chipColor2, chipColor3, chipColor4, chipColor5, chipColor6)
                    chipViews.forEach { it.strokeWidth = 2.dpToPx() }
                    chipViews.forEach { it.strokeColor = Color.TRANSPARENT }
                    chipColorCustom.setCardBackgroundColor(color)
                    chipColorCustom.strokeColor = androidx.core.content.ContextCompat.getColor(this@LaptopFramesActivity, R.color.category_essential)
                    chipColorCustom.strokeWidth = 3.dpToPx()
                } catch (_: Exception) {
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

        // Model selection listeners
        cardModelWindows.setOnClickListener {
            isMacOsModel = false
            cardModelWindows.strokeColor = androidx.core.content.ContextCompat.getColor(this, R.color.category_essential)
            cardModelWindows.strokeWidth = 2.dpToPx()
            cardModelMacos.strokeColor = androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary)
            cardModelMacos.strokeWidth = 1.dpToPx()
        }

        cardModelMacos.setOnClickListener {
            isMacOsModel = true
            cardModelMacos.strokeColor = androidx.core.content.ContextCompat.getColor(this, R.color.category_essential)
            cardModelMacos.strokeWidth = 2.dpToPx()
            cardModelWindows.strokeColor = androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary)
            cardModelWindows.strokeWidth = 1.dpToPx()
        }

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
        allChips.forEach { chip ->
            chip.strokeColor = Color.TRANSPARENT
            chip.strokeWidth = 2.dpToPx()
        }
        chipColorCustom.strokeColor = androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary)
        chipColorCustom.strokeWidth = 1.dpToPx()

        selected.strokeColor = androidx.core.content.ContextCompat.getColor(this, R.color.category_essential)
        selected.strokeWidth = 3.dpToPx()

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
                resultBitmap = createLaptopFrame(screenshot, selectedColor, paddingPercent, false)
                vvResult.visibility = View.GONE
                ivResult.visibility = View.VISIBLE
                ivResult.setImageBitmap(resultBitmap)
                cardResult.visibility = View.VISIBLE
                sendBroadcast(Intent("com.essential.shortcuts.ACTION_STEP_2").setPackage(packageName))

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
                Log.e("LaptopFrames", "Watchdog: FFmpeg stuck at 0% for 30s, killing...")
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

        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(etHexColor.windowToken, 0)

        Thread {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(this@LaptopFramesActivity, videoUri)
                val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                retriever.release()

                val durationMs = durationStr?.toLongOrNull() ?: 1000L
                val totalMicroseconds = durationMs * 1000L

                var videoW = widthStr?.toFloatOrNull() ?: 1920f
                var videoH = heightStr?.toFloatOrNull() ?: 1080f
                val rotation = rotationStr?.toIntOrNull() ?: 0
                if (rotation == 90 || rotation == 270) {
                    val temp = videoW
                    videoW = videoH
                    videoH = temp
                }

                var videoWInt = videoW.roundToInt()
                var videoHInt = videoH.roundToInt()
                if (videoWInt % 2 != 0) videoWInt++
                if (videoHInt % 2 != 0) videoHInt++
                videoW = videoWInt.toFloat()
                videoH = videoHInt.toFloat()

                val isFullQuality = cbFullQuality.isChecked

                if (!isFullQuality) {
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

                val dummyBitmap = Bitmap.createBitmap(videoW.toInt(), videoH.toInt(), Bitmap.Config.ARGB_8888)
                val frameTemplate = createLaptopFrame(dummyBitmap, selectedColor, paddingPercent, true)

                val cacheDir = File(cacheDir, "laptop_frames")
                cacheDir.mkdirs()

                val templateFile = File(cacheDir, "template.png")
                FileOutputStream(templateFile).use { out ->
                    frameTemplate.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                // Calculate metrics for FFmpeg — must match createLaptopFrame exactly
                val origW = videoW
                val origH = videoH

                val canvasW = frameTemplate.width
                val canvasH = frameTemplate.height

                val screenX: Int
                val screenY: Int
                val canvasScreenW: Int
                val canvasScreenH: Int

                if (isMacOsModel) {
                    val templateW = 1024f
                    val templateH = 620f
                    val screenLeft = 104f
                    val screenRight = 920f
                    val screenTop = 21f
                    val screenBottom = 552f
                    val screenW = screenRight - screenLeft
                    val screenH = screenBottom - screenTop

                    val scale = if (origW / origH > screenW / screenH) {
                        origW / screenW
                    } else {
                        origH / screenH
                    }

                    val laptopW = templateW * scale
                    val laptopH = templateH * scale

                    val paddingFraction = paddingPercent / 100f
                    val padX = (laptopW * paddingFraction * 0.5f)
                    val padY = (laptopH * paddingFraction * 0.5f)

                    val drawLeft = padX
                    val drawTop = padY

                    val canvasScreenLeft = drawLeft + screenLeft * scale
                    val canvasScreenTop = drawTop + screenTop * scale
                    canvasScreenW = (screenW * scale).roundToInt()
                    canvasScreenH = (screenH * scale).roundToInt()

                    screenX = canvasScreenLeft.roundToInt()
                    screenY = canvasScreenTop.roundToInt()
                } else {
                    // Windows model metrics
                    val targetRatio = 1.6f
                    var screenW = origW
                    var screenH = origH
                    if (origW / origH > targetRatio) {
                        screenH = origW / targetRatio
                    } else {
                        screenW = origH * targetRatio
                    }

                    val bezelThickness = screenW * 0.01f
                    val lidW = screenW + 2 * bezelThickness
                    val lidH = screenH + 2 * bezelThickness

                    val baseExtensionX = screenW * 0.11f
                    val baseW = lidW + 2 * baseExtensionX
                    val baseH = screenH * 0.05f

                    val footH = baseH * 0.35f
                    val laptopW = baseW
                    val laptopH = lidH + baseH + footH

                    val paddingFraction = paddingPercent / 100f
                    val padX = (laptopW * paddingFraction * 0.5f)
                    val padY = (laptopH * paddingFraction * 0.5f)

                    val centerX = padX + laptopW / 2

                    val lidLeft = centerX - lidW / 2

                    screenX = (lidLeft + bezelThickness).roundToInt()
                    screenY = (padY + bezelThickness).roundToInt()
                    canvasScreenW = screenW.roundToInt()
                    canvasScreenH = screenH.roundToInt()
                }

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
                    progressTick.setProgress(0f, androidx.core.content.ContextCompat.getColor(this@LaptopFramesActivity, R.color.category_essential))
                }

                val filterComplex = if (isFullQuality) {
                    "[0:v]scale=$canvasScreenW:$canvasScreenH:force_original_aspect_ratio=decrease,pad=$canvasScreenW:$canvasScreenH:(ow-iw)/2:(oh-ih)/2,format=yuv420p[v_scaled]; [v_scaled]pad=$canvasW:$canvasH:$screenX:$screenY:black[v_padded]; [v_padded][1:v]overlay=0:0,format=yuv420p[out]"
                } else {
                    "[0:v]fps=30,scale=$canvasScreenW:$canvasScreenH:force_original_aspect_ratio=decrease,pad=$canvasScreenW:$canvasScreenH:(ow-iw)/2:(oh-ih)/2,format=yuv420p[v_scaled]; [v_scaled]pad=$canvasW:$canvasH:$screenX:$screenY:black[v_padded]; [v_padded][1:v]overlay=0:0,format=yuv420p[out]"
                }
                val crfVal = if (isFullQuality) "23" else "28"

                val commands = arrayOf("-y", "-nostdin", "-i", inputFile.absolutePath, "-i", templateFile.absolutePath,
                        "-filter_complex", filterComplex,
                        "-map", "[out]", "-map", "0:a?", "-c:v", "libx264", "-preset", "ultrafast", "-threads", "0", "-crf", crfVal, "-c:a", "copy", outputFile.absolutePath)

                Log.d("LaptopFrames", "FFmpeg command: ${commands.joinToString(" ")}")
                Log.d("LaptopFrames", "Input exists: ${inputFile.exists()}, size: ${inputFile.length()}")
                Log.d("LaptopFrames", "Template exists: ${templateFile.exists()}, size: ${templateFile.length()}")
                Log.d("LaptopFrames", "Canvas: ${canvasW}x${canvasH}, Screen offset: ${screenX},${screenY}")

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
                            Toast.makeText(this@LaptopFramesActivity, "Video processing completed but output file is missing", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onProgress(progress: Int, progressTime: Long) {
                        if (!isProcessingVideo) return
                        hasReceivedProgress = true
                        val percent = progressTime.toFloat() / totalMicroseconds.toFloat()
                        val clamped = percent.coerceIn(0f, 1f)
                        progressTick.setProgress(clamped, androidx.core.content.ContextCompat.getColor(this@LaptopFramesActivity, R.color.category_essential))
                        tvProgress.text = "Rendering Video... ${(clamped * 100).toInt()}%"
                    }

                    override fun onCancel() {
                        if (!isProcessingVideo) return
                        finishProcessing()
                        layoutProgress.visibility = View.GONE
                        Toast.makeText(this@LaptopFramesActivity, "Video processing cancelled", Toast.LENGTH_SHORT).show()
                    }

                    override fun onError(message: String?) {
                        if (!isProcessingVideo) return
                        Log.e("LaptopFrames", "FFmpeg error: $message")
                        finishProcessing()
                        layoutProgress.visibility = View.GONE
                        Toast.makeText(this@LaptopFramesActivity, "Video processing error: $message", Toast.LENGTH_LONG).show()
                    }
                }

                RxFFmpegInvoke.getInstance()
                    .runCommandRxJava(commands)
                    .subscribe(ffmpegSubscriber!!)

            } catch (e: Exception) {
                Log.e("LaptopFrames", "Error in processVideoFrame", e)
                finishProcessing()
                runOnUiThread {
                    layoutProgress.visibility = View.GONE
                    Toast.makeText(this, "Error processing video: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    /**
     * Creates a laptop frame mockup matching a MacBook-style design:
     * - Thin black bezels, equal on all sides around the screen
     * - Small webcam notch at top center of the bezel
     * - Silver/gray chin (laptop base/hinge) at the bottom — no keyboard
     * - Subtle rounded corners on the body
     * - Glass sheen overlay for premium look
     */
    private fun createLaptopFrame(screenshot: Bitmap, bgColor: Int, paddingPct: Int, isVideoTemplate: Boolean): Bitmap {
        val origW = screenshot.width.toFloat()
        val origH = screenshot.height.toFloat()

        if (isMacOsModel) {
            // Load custom template image with Options inScaled = false to prevent scaling on high density screens!
            val options = android.graphics.BitmapFactory.Options().apply {
                inScaled = false
            }
            val template = android.graphics.BitmapFactory.decodeResource(resources, R.drawable.mockup_laptop, options)
            val templateW = template.width.toFloat()
            val templateH = template.height.toFloat()

            // Bounding box of screen in template:
            val screenLeft = 104f
            val screenRight = 920f
            val screenTop = 21f
            val screenBottom = 552f
            val screenW = screenRight - screenLeft
            val screenH = screenBottom - screenTop

            // Scale factor for the template to preserve high-res input
            val scale = if (origW / origH > screenW / screenH) {
                origW / screenW
            } else {
                origH / screenH
            }

            val laptopW = templateW * scale
            val laptopH = templateH * scale

            // Calculate padding
            val paddingFraction = paddingPct / 100f
            val padX = (laptopW * paddingFraction * 0.5f).toInt()
            val padY = (laptopH * paddingFraction * 0.5f).toInt()

            // Canvas size (ensure even for h264 yuv420p compatibility)
            var canvasW = laptopW.toInt() + padX * 2
            var canvasH = laptopH.toInt() + padY * 2
            if (canvasW % 2 != 0) canvasW++
            if (canvasH % 2 != 0) canvasH++

            val result = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)

            // Draw solid background
            canvas.drawColor(bgColor)

            // Offset coordinates for drawing template
            val drawLeft = padX.toFloat()
            val drawTop = padY.toFloat()

            val canvasScreenLeft = drawLeft + screenLeft * scale
            val canvasScreenTop = drawTop + screenTop * scale
            val canvasScreenRight = drawLeft + screenRight * scale
            val canvasScreenBottom = drawTop + screenBottom * scale
            val canvasScreenW = screenW * scale
            val canvasScreenH = screenH * scale

            if (isVideoTemplate) {
                // Create a transparent hole in the template
                val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                }
                val screenRadius = canvasScreenW * (8f / 816f) // Rounded screen corners
                canvas.drawRoundRect(
                    RectF(canvasScreenLeft, canvasScreenTop, canvasScreenRight, canvasScreenBottom),
                    screenRadius, screenRadius,
                    clearPaint
                )
            } else {
                // Draw the screenshot centered inside the canvas screen box
                canvas.save()
                val screenRadius = canvasScreenW * (8f / 816f)
                val screenPath = Path().apply {
                    addRoundRect(
                        RectF(canvasScreenLeft, canvasScreenTop, canvasScreenRight, canvasScreenBottom),
                        screenRadius, screenRadius, Path.Direction.CW
                    )
                }
                canvas.clipPath(screenPath)
                
                // Draw background black (for letterbox/pillarbox)
                canvas.drawColor(Color.BLACK)
                
                // Center the screenshot
                val sScale: Float
                var dx = 0f
                var dy = 0f
                if (origW * canvasScreenH > canvasScreenW * origH) {
                    sScale = canvasScreenW / origW
                    dy = (canvasScreenH - origH * sScale) * 0.5f
                } else {
                    sScale = canvasScreenH / origH
                    dx = (canvasScreenW - origW * sScale) * 0.5f
                }
                val matrix = android.graphics.Matrix()
                matrix.postScale(sScale, sScale)
                matrix.postTranslate(canvasScreenLeft + dx, canvasScreenTop + dy)
                canvas.drawBitmap(screenshot, matrix, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
                canvas.restore()
            }

            // Draw the scaled template on top (so notch/bezels/chin overlay correctly)
            val destRect = RectF(drawLeft, drawTop, drawLeft + laptopW, drawTop + laptopH)
            canvas.drawBitmap(template, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))

            // Clean up decoded template resource
            template.recycle()

            return result
        } else {
            // Draw Windows Laptop programmatically
            // 1. Calculate dimensions (force 16:10)
            val targetRatio = 1.6f
            var screenW = origW
            var screenH = origH
            if (origW / origH > targetRatio) {
                screenH = origW / targetRatio
            } else {
                screenW = origH * targetRatio
            }

            val bezelThickness = screenW * 0.01f
            val lidW = screenW + 2 * bezelThickness
            val lidH = screenH + 2 * bezelThickness

            val baseExtensionX = screenW * 0.11f
            val baseW = lidW + 2 * baseExtensionX
            val baseH = screenH * 0.05f

            val footH = baseH * 0.35f
            val laptopW = baseW
            val laptopH = lidH + baseH + footH

            // Calculate padding
            val paddingFraction = paddingPct / 100f
            val padX = (laptopW * paddingFraction * 0.5f).toInt()
            val padY = (laptopH * paddingFraction * 0.5f).toInt()

            // Canvas size (ensure even for h264 yuv420p compatibility)
            var canvasW = laptopW.toInt() + padX * 2
            var canvasH = laptopH.toInt() + padY * 2
            if (canvasW % 2 != 0) canvasW++
            if (canvasH % 2 != 0) canvasH++

            val result = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)

            // Draw solid background
            canvas.drawColor(bgColor)

            val laptopLeft = padX.toFloat()
            val laptopTop = padY.toFloat()

            // Center coordinates
            val centerX = laptopLeft + laptopW / 2

            // --- 2. Lid / Screen Frame ---
            val lidLeft = centerX - lidW / 2
            val lidTop = laptopTop
            val lidRight = centerX + lidW / 2
            val lidBottom = lidTop + lidH
            val lidRadius = screenW * 0.025f

            // Draw black lid background (the bezel)
            val bezelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#151515") // Very dark gray/black
                style = Paint.Style.FILL
            }
            
            val lidRoundedPath = Path().apply {
                // Only top corners rounded
                addRoundRect(
                    RectF(lidLeft, lidTop, lidRight, lidBottom),
                    floatArrayOf(lidRadius, lidRadius, lidRadius, lidRadius, 0f, 0f, 0f, 0f),
                    Path.Direction.CW
                )
            }
            canvas.drawPath(lidRoundedPath, bezelPaint)

            // --- 3. Screen Area ---
            val screenLeft = lidLeft + bezelThickness
            val screenTop = lidTop + bezelThickness
            val screenRight = lidRight - bezelThickness
            val screenBottom = lidBottom - bezelThickness
            val screenRadius = lidRadius * 0.6f

            val screenPath = Path().apply {
                // Only top corners rounded
                addRoundRect(
                    RectF(screenLeft, screenTop, screenRight, screenBottom),
                    floatArrayOf(screenRadius, screenRadius, screenRadius, screenRadius, 0f, 0f, 0f, 0f),
                    Path.Direction.CW
                )
            }

            if (isVideoTemplate) {
                val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                }
                canvas.drawPath(screenPath, clearPaint)
            } else {
                canvas.save()
                canvas.clipPath(screenPath)
                
                // Fill background with black (letterbox/pillarbox)
                canvas.drawColor(Color.BLACK)
                
                // Calculate matrix to center and scale screenshot to fit within screenPath
                val scale: Float
                var dx = 0f
                var dy = 0f

                if (origW * screenH > screenW * origH) {
                    // Image is wider than 16:10 (pillarbox - fit to width)
                    scale = screenW / origW
                    dy = (screenH - origH * scale) * 0.5f
                } else {
                    // Image is taller than 16:10 (letterbox - fit to height)
                    scale = screenH / origH
                    dx = (screenW - origW * scale) * 0.5f
                }

                val matrix = android.graphics.Matrix()
                matrix.postScale(scale, scale)
                matrix.postTranslate(screenLeft + dx, screenTop + dy)
                
                canvas.drawBitmap(
                    screenshot,
                    matrix,
                    Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                )
                canvas.restore()
            }

            // --- 4. Camera (Top Center Bezel) ---
            // Simple webcam dot inside the top bezel
            val camRadius = bezelThickness * 0.15f
            val camCy = lidTop + bezelThickness / 2
            val camPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#080808") }
            canvas.drawCircle(centerX, camCy, camRadius, camPaint)
            val specPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#20FFFFFF") }
            canvas.drawCircle(centerX - camRadius * 0.2f, camCy - camRadius * 0.2f, camRadius * 0.3f, specPaint)

            // --- 5. Base / Deck ---
            val baseLeft = laptopLeft
            val baseRight = laptopLeft + laptopW
            val baseTop = lidBottom // Exactly touching the lid to ensure even bezels
            val baseBottom = baseTop + baseH
            val baseRadius = baseH * 0.6f

            // Base gradient (silver/grey metallic)
            val baseGradient = android.graphics.LinearGradient(
                baseLeft, baseTop, baseRight, baseBottom,
                intArrayOf(
                    Color.parseColor("#D4D4D8"), // Top highlight
                    Color.parseColor("#9CA3AF"), // Mid grey
                    Color.parseColor("#6B7280"), // Darker edge
                    Color.parseColor("#4B5563")  // Shadow
                ),
                floatArrayOf(0.0f, 0.3f, 0.8f, 1.0f),
                android.graphics.Shader.TileMode.CLAMP
            )
            val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = baseGradient
                style = Paint.Style.FILL
            }

            // Draw base (only bottom corners are rounded)
            val basePath = Path().apply {
                addRoundRect(
                    RectF(baseLeft, baseTop, baseRight, baseBottom),
                    floatArrayOf(0f, 0f, 0f, 0f, baseRadius, baseRadius, baseRadius, baseRadius),
                    Path.Direction.CW
                )
            }
            canvas.drawPath(basePath, basePaint)

            // Thumb scoop (cutout at the top center of the base)
            val scoopW = screenW * 0.15f
            val scoopH = baseH * 0.15f
            val scoopLeft = centerX - scoopW / 2
            val scoopRight = centerX + scoopW / 2
            val scoopTop = baseTop
            val scoopBottom = baseTop + scoopH

            // Draw a shadow/darker grey for the scoop
            val scoopPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#6B7280")
                style = Paint.Style.FILL
            }
            val scoopPath = Path().apply {
                moveTo(scoopLeft, scoopTop)
                quadTo(scoopLeft + scoopW * 0.1f, scoopBottom, centerX, scoopBottom)
                quadTo(scoopRight - scoopW * 0.1f, scoopBottom, scoopRight, scoopTop)
                close()
            }
            canvas.drawPath(scoopPath, scoopPaint)

            // Base top edge highlight
            val hingePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E5E7EB")
                strokeWidth = baseH * 0.05f
                style = Paint.Style.STROKE
            }
            canvas.drawLine(baseLeft, baseTop, baseRight, baseTop, hingePaint)
            
            // --- 5. Rubber Feet ---
            val footW = laptopW * 0.06f
            val footOffset = laptopW * 0.14f

            val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1C1C1E")
                style = Paint.Style.FILL
            }

            // Left foot (rounded at bottom)
            val leftFootPath = Path().apply {
                addRoundRect(
                    RectF(baseLeft + footOffset, baseBottom, baseLeft + footOffset + footW, baseBottom + footH),
                    floatArrayOf(0f, 0f, 0f, 0f, footH, footH, footH, footH),
                    Path.Direction.CW
                )
            }
            canvas.drawPath(leftFootPath, footPaint)

            // Right foot (rounded at bottom)
            val rightFootPath = Path().apply {
                addRoundRect(
                    RectF(baseRight - footOffset - footW, baseBottom, baseRight - footOffset, baseBottom + footH),
                    floatArrayOf(0f, 0f, 0f, 0f, footH, footH, footH, footH),
                    Path.Direction.CW
                )
            }
            canvas.drawPath(rightFootPath, footPaint)

            return result
        }
    }

    private fun saveResult() {
        if (isVideo) {
            val videoFile = resultVideoFile ?: return
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, "laptop_frame_${System.currentTimeMillis()}.mp4")
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
                    val file = File(dir, "laptop_frame_${System.currentTimeMillis()}.mp4")
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
                        put(MediaStore.Images.Media.DISPLAY_NAME, "laptop_frame_${System.currentTimeMillis()}.png")
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
                    val file = File(dir, "laptop_frame_${System.currentTimeMillis()}.png")
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
                val file = File(cachePath, "laptop_frame_share.png")
                FileOutputStream(file).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }

                val contentUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    type = "image/png"
                }
                startActivity(Intent.createChooser(shareIntent, "Share Laptop Frame"))
            } catch (e: Exception) {
                Toast.makeText(this, "Error sharing: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
