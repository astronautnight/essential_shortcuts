package com.essential.shortcuts

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlay: ScannerOverlayView
    private lateinit var tvInstruction: TextView
    private lateinit var btnFlash: ImageButton

    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var flashOn = false

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var hasScanned = false

    // ── Store extracted data immediately (Barcode objects can go stale) ──
    private var scannedRawValue: String = ""
    private var scannedValueType: Int = -1
    private var scannedUrl: String? = null
    private var scannedPhone: String? = null
    private var scannedEmailAddress: String? = null
    private var scannedEmailSubject: String? = null
    private var scannedEmailBody: String? = null
    private var scannedSmsNumber: String? = null
    private var scannedSmsBody: String? = null
    private var scannedWifiSsid: String? = null
    private var scannedWifiPassword: String? = null
    private var scannedGeoLat: Double = 0.0
    private var scannedGeoLng: Double = 0.0
    private var scannedContactName: String? = null
    private var scannedContactPhone: String? = null
    private var scannedContactEmail: String? = null

    // ── Store the view-mapped bounding box ──
    private var scannedBoundingBox: android.graphics.RectF? = null

    // ML Kit scanner
    private val scanner: BarcodeScanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_PDF417,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else {
                Toast.makeText(this, "Camera permission is required to scan", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    // ═════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ═════════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        previewView = findViewById(R.id.preview_view)
        overlay = findViewById(R.id.scanner_overlay)
        tvInstruction = findViewById(R.id.tv_instruction)
        btnFlash = findViewById(R.id.btn_flash)

        findViewById<ImageButton>(R.id.btn_close).setOnClickListener { finish() }
        btnFlash.setOnClickListener { toggleFlash() }

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        cameraExecutor.shutdown()
        try { scanner.close() } catch (_: Exception) {}
        super.onDestroy()
    }

    // ═════════════════════════════════════════════════════════════════
    //  Camera
    // ═════════════════════════════════════════════════════════════════

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                cameraProvider = future.get()
                bindCamera()
            } catch (e: Exception) {
                Log.e("Scanner", "Camera init failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCamera() {
        val provider = cameraProvider ?: return
        val preview = Preview.Builder().build()
            .also { it.surfaceProvider = previewView.surfaceProvider }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(cameraExecutor, ::analyzeFrame) }

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
            )
        } catch (e: Exception) {
            Log.e("Scanner", "Camera bind failed", e)
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  Frame analysis
    // ═════════════════════════════════════════════════════════════════

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun analyzeFrame(proxy: ImageProxy) {
        val img = proxy.image
        if (img == null) { proxy.close(); return }

        val input = InputImage.fromMediaImage(img, proxy.imageInfo.rotationDegrees)

        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    val bc = barcodes.first()
                    
                    // ── Compute mapped bounding box continuously ──
                    try {
                        val box = bc.boundingBox
                        if (box != null) {
                            val rotation = proxy.imageInfo.rotationDegrees
                            val isPortrait = rotation == 90 || rotation == 270
                            val imageW = if (isPortrait) img.height.toFloat() else img.width.toFloat()
                            val imageH = if (isPortrait) img.width.toFloat() else img.height.toFloat()
                            val viewW = previewView.width.toFloat()
                            val viewH = previewView.height.toFloat()

                            val scale = kotlin.math.max(viewW / imageW, viewH / imageH)
                            val offsetX = (viewW - imageW * scale) / 2f
                            val offsetY = (viewH - imageH * scale) / 2f

                            scannedBoundingBox = android.graphics.RectF(
                                box.left * scale + offsetX,
                                box.top * scale + offsetY,
                                box.right * scale + offsetX,
                                box.bottom * scale + offsetY
                            )
                            
                            // Continually update the overlay to stick animations to the moving QR
                            if (hasScanned) {
                                overlay.updateTargetBoundingBox(scannedBoundingBox)
                            }
                        }
                    } catch (_: Exception) {}

                    val raw = bc.rawValue

                    if (!hasScanned && !raw.isNullOrBlank()) {
                        hasScanned = true

                        // ─── Extract ALL data from the Barcode NOW ───
                        // (the Barcode object may become invalid after this callback)
                        scannedRawValue = raw
                        scannedValueType = bc.valueType

                        try { scannedUrl = bc.url?.url } catch (_: Exception) {}
                        try { scannedPhone = bc.phone?.number } catch (_: Exception) {}
                        try {
                            scannedEmailAddress = bc.email?.address
                            scannedEmailSubject = bc.email?.subject
                            scannedEmailBody = bc.email?.body
                        } catch (_: Exception) {}
                        try {
                            scannedSmsNumber = bc.sms?.phoneNumber
                            scannedSmsBody = bc.sms?.message
                        } catch (_: Exception) {}
                        try {
                            scannedWifiSsid = bc.wifi?.ssid
                            scannedWifiPassword = bc.wifi?.password
                        } catch (_: Exception) {}
                        try {
                            scannedGeoLat = bc.geoPoint?.lat ?: 0.0
                            scannedGeoLng = bc.geoPoint?.lng ?: 0.0
                        } catch (_: Exception) {}
                        try {
                            scannedContactName = bc.contactInfo?.name?.formattedName
                            scannedContactPhone = bc.contactInfo?.phones?.firstOrNull()?.number
                            scannedContactEmail = bc.contactInfo?.emails?.firstOrNull()?.address
                        } catch (_: Exception) {}

                        mainHandler.post { onBarcodeDetected() }
                    }
                }
            }
            .addOnFailureListener { /* transient */ }
            .addOnCompleteListener { proxy.close() }
    }

    // ═════════════════════════════════════════════════════════════════
    //  Animation sequence → then open
    // ═════════════════════════════════════════════════════════════════

    private fun onBarcodeDetected() {
        if (isFinishing || isDestroyed) return

        // Phase 1 — start decode animation
        overlay.startDecodingAnimation(scannedBoundingBox)
        tvInstruction.text = "Decoding…"

        try { overlay.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS) }
        catch (_: Exception) {}

        // Phase 2 — after 1.8s show success
        mainHandler.postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed
            overlay.showSuccess()
            tvInstruction.text = "Scanned ✓"

            // Phase 3 — after 0.6s open result
            mainHandler.postDelayed({
                if (isFinishing || isDestroyed) return@postDelayed
                openScannedResult()
            }, 600)
        }, 1800)
    }

    // ═════════════════════════════════════════════════════════════════
    //  Result handling (uses extracted primitive fields, not Barcode)
    // ═════════════════════════════════════════════════════════════════

    private fun openScannedResult() {
        if (isFinishing || isDestroyed) return

        Log.d("Scanner", "Opening result: type=$scannedValueType raw=$scannedRawValue url=$scannedUrl")

        try {
            when (scannedValueType) {

                Barcode.TYPE_URL -> {
                    val url = scannedUrl ?: scannedRawValue
                    openUrl(url)
                }

                Barcode.TYPE_WIFI -> {
                    copyAndToast(
                        "Wi-Fi: ${scannedWifiSsid ?: "?"} / ${scannedWifiPassword ?: "?"}",
                        "Wi-Fi credentials copied"
                    )
                }

                Barcode.TYPE_EMAIL -> {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:${scannedEmailAddress ?: ""}")
                        putExtra(Intent.EXTRA_SUBJECT, scannedEmailSubject ?: "")
                        putExtra(Intent.EXTRA_TEXT, scannedEmailBody ?: "")
                    }
                    safeStart(intent)
                }

                Barcode.TYPE_PHONE -> {
                    val phone = scannedPhone ?: scannedRawValue
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    safeStart(intent)
                }

                Barcode.TYPE_SMS -> {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("smsto:${scannedSmsNumber ?: ""}")
                        putExtra("sms_body", scannedSmsBody ?: "")
                    }
                    safeStart(intent)
                }

                Barcode.TYPE_GEO -> {
                    val intent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("geo:$scannedGeoLat,$scannedGeoLng"))
                    safeStart(intent)
                }

                Barcode.TYPE_CONTACT_INFO -> {
                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        type = ContactsContract.Contacts.CONTENT_TYPE
                        putExtra(ContactsContract.Intents.Insert.NAME, scannedContactName)
                        putExtra(ContactsContract.Intents.Insert.PHONE, scannedContactPhone)
                        putExtra(ContactsContract.Intents.Insert.EMAIL, scannedContactEmail)
                    }
                    safeStart(intent)
                }

                else -> {
                    // Smart URL detection — match any text that looks like a website
                    val raw = scannedRawValue.trim()
                    if (looksLikeUrl(raw)) {
                        val url = when {
                            raw.startsWith("http://", true) || raw.startsWith("https://", true) -> raw
                            else -> "https://$raw"
                        }
                        openUrl(url)
                    } else {
                        copyAndToast(raw, "Scanned text copied")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Scanner", "Error opening result", e)
            copyAndToast(scannedRawValue, "Scanned content copied")
        }
    }

    private fun openUrl(url: String) {
        Log.d("Scanner", "Opening URL: $url")
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("Scanner", "Failed to open URL", e)
            copyAndToast(url, "URL copied (no browser found)")
        }
    }

    private fun safeStart(intent: Intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("Scanner", "Failed to start intent", e)
            copyAndToast(scannedRawValue, "Content copied (no app to handle)")
        }
    }

    private fun copyAndToast(text: String, msg: String) {
        try {
            val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cb.setPrimaryClip(ClipData.newPlainText("scan_result", text))
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        } catch (_: Exception) {}
        finish()
    }

    // ═════════════════════════════════════════════════════════════════
    //  Flash toggle
    // ═════════════════════════════════════════════════════════════════

    private fun toggleFlash() {
        flashOn = !flashOn
        camera?.cameraControl?.enableTorch(flashOn)
        btnFlash.setColorFilter(
            if (flashOn) android.graphics.Color.YELLOW else android.graphics.Color.WHITE
        )
    }

    // ═════════════════════════════════════════════════════════════════
    //  URL detection
    // ═════════════════════════════════════════════════════════════════

    /** Returns true if the text looks like a website URL (with or without http:// prefix). */
    private fun looksLikeUrl(text: String): Boolean {
        if (text.isBlank()) return false
        // Obvious protocols
        if (text.startsWith("http://", true) || text.startsWith("https://", true)) return true
        if (text.startsWith("www.", true)) return true
        // Match domain + TLD pattern: something.tld or something.tld/path
        // Covers all common TLDs including 2-letter country codes
        val urlPattern = Regex(
            """^[a-zA-Z0-9]([a-zA-Z0-9\-]*[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9\-]*[a-zA-Z0-9])?)*\.(com|org|net|edu|gov|mil|int|io|co|app|dev|me|info|biz|name|pro|museum|aero|coop|travel|jobs|mobi|tel|cat|asia|post|xxx|in|uk|us|au|ca|de|fr|jp|cn|ru|br|za|kr|it|es|nl|se|no|fi|dk|pl|cz|at|ch|be|pt|ie|nz|mx|ar|cl|co\.uk|co\.in|ac\.in|org\.in|net\.in|gov\.in|ac\.uk|org\.uk|[a-z]{2})([\/\?#].*)?$""",
            RegexOption.IGNORE_CASE
        )
        return urlPattern.matches(text.trim())
    }
}
