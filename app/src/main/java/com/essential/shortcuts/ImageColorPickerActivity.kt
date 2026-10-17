package com.essential.shortcuts

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import kotlin.math.roundToInt

class ImageColorPickerActivity : AppCompatActivity() {

    private lateinit var btnClose: ImageButton
    private lateinit var cardPickImage: MaterialCardView
    private lateinit var layoutPickPlaceholder: LinearLayout
    private lateinit var ivPreview: ImageView
    private lateinit var btnChangeImage: Button
    
    private lateinit var layoutResult: LinearLayout
    private lateinit var viewColorSwatch: View
    private lateinit var tvHexCode: TextView
    private lateinit var tvRgbCode: TextView
    private lateinit var btnCopyHex: ImageButton
    private lateinit var btnCopyRgb: ImageButton

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            loadImage(it)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_color_picker)

        // Make window 90% of screen width (Popup theme)
        window.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Find views
        btnClose = findViewById(R.id.btn_close)
        cardPickImage = findViewById(R.id.card_pick_image)
        layoutPickPlaceholder = findViewById(R.id.layout_pick_placeholder)
        ivPreview = findViewById(R.id.iv_preview)
        btnChangeImage = findViewById(R.id.btn_change_image)
        
        layoutResult = findViewById(R.id.layout_result)
        viewColorSwatch = findViewById(R.id.view_color_swatch)
        tvHexCode = findViewById(R.id.tv_hex_code)
        tvRgbCode = findViewById(R.id.tv_rgb_code)
        btnCopyHex = findViewById(R.id.btn_copy_hex)
        btnCopyRgb = findViewById(R.id.btn_copy_rgb)

        // Setup listeners
        btnClose.setOnClickListener { finish() }
        
        cardPickImage.setOnClickListener {
            if (ivPreview.visibility == View.GONE) {
                openImagePicker()
            }
        }
        
        btnChangeImage.setOnClickListener {
            openImagePicker()
        }

        // Handle touch on image to pick color
        ivPreview.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                val bitmapDrawable = ivPreview.drawable as? BitmapDrawable
                val bitmap = bitmapDrawable?.bitmap
                
                if (bitmap != null) {
                    val inverse = Matrix()
                    ivPreview.imageMatrix.invert(inverse)
                    val touchPoint = floatArrayOf(event.x, event.y)
                    inverse.mapPoints(touchPoint)
                    
                    val x = touchPoint[0].toInt()
                    val y = touchPoint[1].toInt()
                    
                    if (x in 0 until bitmap.width && y in 0 until bitmap.height) {
                        // We must request unparented intercept to allow dragging inside ScrollView
                        view.parent.requestDisallowInterceptTouchEvent(true)
                        
                        val pixelColor = bitmap.getPixel(x, y)
                        updateColorResult(pixelColor)
                    }
                }
            } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                view.parent.requestDisallowInterceptTouchEvent(false)
            }
            true
        }

        btnCopyHex.setOnClickListener {
            copyToClipboard("HEX Color", tvHexCode.text.toString())
        }

        btnCopyRgb.setOnClickListener {
            copyToClipboard("RGB Color", tvRgbCode.text.toString())
        }
    }

    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

    private fun loadImage(uri: Uri) {
        layoutPickPlaceholder.visibility = View.GONE
        ivPreview.visibility = View.VISIBLE
        btnChangeImage.visibility = View.VISIBLE
        layoutResult.visibility = View.VISIBLE
        
        ivPreview.setImageURI(uri)
        
        // Wait till image is rendered to set initial color
        ivPreview.post {
            val bitmap = (ivPreview.drawable as? BitmapDrawable)?.bitmap
            if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
                val centerColor = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
                updateColorResult(centerColor)
            }
        }
    }

    private fun updateColorResult(color: Int) {
        // Update color swatch
        viewColorSwatch.setBackgroundColor(color)

        // Convert to HEX
        val hexFormat = String.format("#%06X", (0xFFFFFF and color))
        tvHexCode.text = hexFormat

        // Convert to RGB
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        tvRgbCode.text = "rgb($red, $green, $blue)"
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied $text to clipboard", Toast.LENGTH_SHORT).show()
    }
}
