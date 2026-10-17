package com.essential.shortcuts

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.card.MaterialCardView
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

class RoundedImageActivity : AppCompatActivity() {

    private lateinit var btnClose: ImageButton
    private lateinit var cardPickImage: MaterialCardView
    private lateinit var layoutPickPlaceholder: LinearLayout
    private lateinit var ivPreview: ImageView
    private lateinit var btnMakeRound: Button
    private lateinit var cardResult: MaterialCardView
    private lateinit var ivRounded: ImageView
    private lateinit var btnSave: Button
    private lateinit var btnShare: Button

    private var originalBitmap: Bitmap? = null
    private var roundedBitmap: Bitmap? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { loadImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rounded_image)
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
        btnMakeRound = findViewById(R.id.btn_make_round)
        cardResult = findViewById(R.id.card_result)
        ivRounded = findViewById(R.id.iv_rounded)
        btnSave = findViewById(R.id.btn_save)
        btnShare = findViewById(R.id.btn_share)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        cardPickImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        btnMakeRound.setOnClickListener {
            makeImageRound()
        }

        btnSave.setOnClickListener {
            saveRoundedImage()
        }

        btnShare.setOnClickListener {
            shareRoundedImage()
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

            btnMakeRound.visibility = View.VISIBLE
            cardResult.visibility = View.GONE

        } catch (e: Exception) {
            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getRoundedCroppedBitmap(bitmap: Bitmap): Bitmap {
        val widthLight = bitmap.width
        val heightLight = bitmap.height
        
        val minDim = min(widthLight, heightLight)
        val cx = widthLight / 2
        val cy = heightLight / 2
        
        val output = Bitmap.createBitmap(minDim, minDim, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val paint = Paint()
        val srcRect = Rect(cx - minDim/2, cy - minDim/2, cx + minDim/2, cy + minDim/2)
        val dstRect = Rect(0, 0, minDim, minDim)
        val rectF = RectF(dstRect)

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = -0xbdbdbe
        canvas.drawOval(rectF, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)

        return output
    }

    private fun makeImageRound() {
        val bmp = originalBitmap
        if (bmp == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            roundedBitmap = getRoundedCroppedBitmap(bmp)
            ivRounded.setImageBitmap(roundedBitmap)
            cardResult.visibility = View.VISIBLE
            // Step 2: Rounding done → sweep to 2/3rds
            sendBroadcast(Intent("com.essential.shortcuts.ACTION_STEP_2").setPackage(packageName))
        } catch (e: Exception) {
            Toast.makeText(this, "Error processing: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveRoundedImage() {
        val bitmap = roundedBitmap ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "rounded_${System.currentTimeMillis()}.png")
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
                val file = File(dir, "rounded_${System.currentTimeMillis()}.png")
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

    private fun shareRoundedImage() {
        val bitmap = roundedBitmap ?: return

        try {
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "rounded_share.png")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            val contentUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
            }
            startActivity(Intent.createChooser(shareIntent, "Share Rounded Image"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error sharing: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
