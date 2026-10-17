package com.essential.shortcuts

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.Html
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.ContentInfoCompat
import androidx.core.view.OnReceiveContentListener
import androidx.core.view.ViewCompat
import org.json.JSONArray
import org.json.JSONObject
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.io.File
import java.util.UUID

/**
 * GmailRouterActivity — Material Design 2014 compose screen.
 *
 *  ┌──────────────────────────────────────────┐
 *  │ [←]  New Message                  [▶]   │  ← primary-blue MD2014 toolbar
 *  │▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░│  ← send progress bar
 *  ├──────────────────────────────────────────┤
 *  │  From   user@gmail.com               ›  │
 *  │  To     [recipients]                    │
 *  │  Cc/Bcc [tap to expand]                 │
 *  │  Subject                                │
 *  │  [body — fills remaining space]         │
 *  ├──────────────────────────────────────────┤
 *  │  📎              🗂   👤               │
 *  └──────────────────────────────────────────┘
 */
class GmailRouterActivity : AppCompatActivity() {

    // ── Views ──────────────────────────────────────────────────────────────
    private lateinit var flipper:         ViewFlipper
    private lateinit var tvFromAccount:   TextView
    private lateinit var etTo:            EditText
    private lateinit var etCc:            EditText
    private lateinit var etBcc:           EditText
    private lateinit var etSubject:       EditText
    private lateinit var layoutBodyBlocks: LinearLayout
    private lateinit var layoutCc:        LinearLayout
    private lateinit var layoutBcc:       LinearLayout
    private lateinit var chipGroupAtt:    ViewGroup
    private lateinit var tvAttachCount:   TextView
    private lateinit var rvTemplates:     RecyclerView
    private lateinit var tvNoTemplates:   TextView
    private lateinit var pbSendProgress:  ProgressBar
    private lateinit var btnSend:         ImageView

    // ── State ──────────────────────────────────────────────────────────────
    private val attachedUris = mutableListOf<Uri>()
    private lateinit var templateAdapter: GmailTemplateAdapter
    private var progressAnimator: ValueAnimator? = null

    // ── Block Editor State ──────────────────────────────────────────────────
    sealed class Block {
        data class Text(val editText: EditText) : Block()
        data class Image(
            val container: View,
            val imageView: ImageView,
            var uri: Uri,
            var width: Int,
            var height: Int
        ) : Block()
    }
    private val blocks = mutableListOf<Block>()

    // ── File picker ────────────────────────────────────────────────────────
    private val pickFiles = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.also { data ->
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                data.clipData?.let { clip ->
                    for (i in 0 until clip.itemCount) {
                        val uri = clip.getItemAt(i).uri
                        try {
                            contentResolver.takePersistableUriPermission(uri, flags)
                        } catch (e: Exception) {}
                        attachedUris.add(uri)
                    }
                } ?: data.data?.let { uri ->
                    try {
                        contentResolver.takePersistableUriPermission(uri, flags)
                    } catch (e: Exception) {}
                    attachedUris.add(uri)
                }
            }
            refreshAttachmentChips()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gmail_router)

        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setLayout(
            (resources.displayMetrics.widthPixels  * 0.93).toInt(),
            (resources.displayMetrics.heightPixels * 0.88).toInt()
        )

        bindViews()
        initBodyBlocks()
        refreshFromLabel()
        setupCcBccToggle()
        setupTemplatesView()
        setupToolbar()
        setupBottomBar()
    }

    override fun onResume() {
        super.onResume()
        refreshFromLabel()
    }

    // ── Bind ───────────────────────────────────────────────────────────────
    private fun bindViews() {
        flipper        = findViewById(R.id.view_flipper)
        tvFromAccount  = findViewById(R.id.tv_from_account)
        etTo           = findViewById(R.id.et_to)
        etCc           = findViewById(R.id.et_cc)
        etBcc          = findViewById(R.id.et_bcc)
        etSubject      = findViewById(R.id.et_subject)
        layoutBodyBlocks = findViewById(R.id.layout_body_blocks)
        layoutCc       = findViewById(R.id.layout_cc)
        layoutBcc      = findViewById(R.id.layout_bcc)
        chipGroupAtt   = findViewById(R.id.chip_group_attachments)
        tvAttachCount  = findViewById(R.id.tv_attach_count)
        rvTemplates    = findViewById(R.id.rv_templates)
        tvNoTemplates  = findViewById(R.id.tv_no_templates)
        pbSendProgress = findViewById(R.id.pb_send_progress)
        btnSend        = findViewById(R.id.btn_send)
    }

    private fun refreshFromLabel() {
        tvFromAccount.text = if (SmtpAccountManager.isConfigured(this))
            SmtpAccountManager.getEmail(this)
        else
            "Tap to configure account"
        tvFromAccount.setTextColor(
            if (SmtpAccountManager.isConfigured(this))
                getColor(android.R.color.tab_indicator_text)
            else getColor(android.R.color.darker_gray)
        )
    }

    // ── CC / BCC toggle ────────────────────────────────────────────────────
    private fun setupCcBccToggle() {
        var ccOpen = false; var bccOpen = false
        findViewById<LinearLayout>(R.id.row_cc_toggle).setOnClickListener {
            if (!ccOpen) { layoutCc.visibility  = View.VISIBLE; ccOpen  = true }
            else         { layoutBcc.visibility = View.VISIBLE; bccOpen = true }
        }
    }

    // ── Attachment chips ───────────────────────────────────────────────────
    private fun refreshAttachmentChips() {
        chipGroupAtt.removeAllViews()
        if (attachedUris.isEmpty()) {
            chipGroupAtt.visibility  = View.GONE
            tvAttachCount.visibility = View.GONE
            return
        }
        chipGroupAtt.visibility  = View.VISIBLE
        tvAttachCount.visibility = View.VISIBLE
        tvAttachCount.text       = "(${attachedUris.size})"
        attachedUris.forEachIndexed { idx, uri ->
            Chip(this).also { chip ->
                chip.text = getFileName(uri)
                chip.isCloseIconVisible = true
                chip.setOnCloseIconClickListener { attachedUris.removeAt(idx); refreshAttachmentChips() }
                chipGroupAtt.addView(chip)
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "file"
        try {
            contentResolver.query(uri, null, null, null, null)?.use {
                val col = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && col >= 0) name = it.getString(col)
            }
        } catch (e: Exception) {
            name = uri.lastPathSegment ?: "file"
        }
        return name
    }

    // ── Templates panel ────────────────────────────────────────────────────
    private fun setupTemplatesView() {
        templateAdapter = GmailTemplateAdapter(
            items    = GmailTemplateManager.getAll(this),
            onUse    = { t -> applyTemplate(t); flipper.displayedChild = 0 },
            onRename = { t -> showRenameDialog(t) },
            onDelete = { t -> confirmDelete(t) }
        )
        rvTemplates.layoutManager = LinearLayoutManager(this)
        rvTemplates.adapter = templateAdapter
        updateTemplateEmptyState()

        findViewById<View>(R.id.card_save_current_template).setOnClickListener {
            showSaveTemplateDialog()
        }
    }

    private fun refreshTemplates() {
        templateAdapter.reload(GmailTemplateManager.getAll(this))
        updateTemplateEmptyState()
    }

    private fun updateTemplateEmptyState() {
        val empty = templateAdapter.itemCount == 0
        tvNoTemplates.visibility = if (empty) View.VISIBLE else View.GONE
        rvTemplates.visibility   = if (empty) View.GONE    else View.VISIBLE
    }

    private fun applyTemplate(t: GmailTemplate) {
        etSubject.setText(t.subject)
        loadSerializedBody(t.body)
        if (t.attachmentUris.isNotEmpty()) {
            attachedUris.clear()
            attachedUris.addAll(t.attachmentUris)
            refreshAttachmentChips()
        }
    }

    private fun showRenameDialog(t: GmailTemplate) {
        val et = EditText(this).apply { setText(t.name); selectAll() }
        AlertDialog.Builder(this)
            .setTitle("Rename template")
            .setView(et)
            .setPositiveButton("Save") { _, _ ->
                GmailTemplateManager.rename(this, t.id, et.text.toString().trim().ifEmpty { t.name })
                refreshTemplates()
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun confirmDelete(t: GmailTemplate) {
        AlertDialog.Builder(this)
            .setTitle("Delete \"${t.name}\"?")
            .setMessage("This template will be permanently removed.")
            .setPositiveButton("Delete") { _, _ ->
                deleteTemplateLocalImages(t)
                GmailTemplateManager.delete(this, t.id)
                refreshTemplates()
            }
            .setNegativeButton("Cancel", null).show()
    }

    // ── Toolbar ────────────────────────────────────────────────────────────
    private fun setupToolbar() {
        // Cancel: ImageView with ic_arrow_back
        findViewById<ImageView>(R.id.btn_cancel).setOnClickListener {
            setResult(RESULT_CANCELED); finish()
        }
        // Send: ImageView with ic_send
        btnSend.setOnClickListener { onSendTapped() }

        // Schedule Send: ImageView with ic_calendar
        findViewById<View>(R.id.btn_schedule).setOnClickListener {
            showDateTimePicker()
        }

        // From row → account setup
        findViewById<LinearLayout>(R.id.row_account).setOnClickListener { showAccountDialog() }

        // Templates back: ImageView with ic_arrow_back
        findViewById<ImageView>(R.id.btn_back_compose).setOnClickListener {
            flipper.displayedChild = 0
        }
    }

    // ── Bottom bar ─────────────────────────────────────────────────────────
    private fun setupBottomBar() {
        findViewById<View>(R.id.btn_attach).setOnClickListener {
            pickFiles.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            })
        }
        findViewById<View>(R.id.btn_save_template).setOnClickListener {
            if (flipper.displayedChild == 1) {
                flipper.displayedChild = 0
            } else {
                refreshTemplates()
                flipper.displayedChild = 1
            }
        }
        findViewById<View>(R.id.btn_save_template).setOnLongClickListener {
            refreshTemplates()
            flipper.displayedChild = 1
            true
        }
        findViewById<View>(R.id.btn_account).setOnClickListener { showAccountDialog() }
    }

    // ── Account dialog ─────────────────────────────────────────────────────
    private fun showAccountDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val etEmail = EditText(this).apply {
            hint = "Gmail address"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(SmtpAccountManager.getEmail(this@GmailRouterActivity))
        }
        val etPass = EditText(this).apply {
            hint = "App Password (16 chars)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(SmtpAccountManager.getPassword(this@GmailRouterActivity))
        }
        val hint = TextView(this).apply {
            text = "Generate at: myaccount.google.com → Security → App Passwords"
            textSize = 11f
            setPadding(0, 12, 0, 0)
            setTextColor(0xFF9E9E9E.toInt())
        }
        container.addView(etEmail); container.addView(etPass); container.addView(hint)
        AlertDialog.Builder(this)
            .setTitle("Sending Account")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val email = etEmail.text.toString().trim()
                val pass  = etPass.text.toString().trim()
                if (email.isNotEmpty() && pass.isNotEmpty()) {
                    SmtpAccountManager.save(this, email, pass)
                    refreshFromLabel()
                    Toast.makeText(this, "Account saved", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Remove") { _, _ -> SmtpAccountManager.clear(this); refreshFromLabel() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Save template dialog ───────────────────────────────────────────────
    private fun saveUriToInternalStorage(uri: Uri): Uri {
        val templateDir = File(filesDir, "gmail_templates").apply { mkdirs() }
        if (uri.scheme == "file") {
            val path = uri.path
            if (path != null && path.startsWith(templateDir.absolutePath)) {
                return uri
            }
        }
        try {
            val localFile = File(templateDir, "img_${UUID.randomUUID()}.png")
            contentResolver.openInputStream(uri)?.use { input ->
                localFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return Uri.fromFile(localFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return uri
    }

    private fun deleteTemplateLocalImages(t: GmailTemplate) {
        try {
            val bodyStr = t.body
            if (bodyStr.startsWith("[") && bodyStr.endsWith("]")) {
                val arr = JSONArray(bodyStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.optString("type") == "image") {
                        val uriStr = obj.optString("uri")
                        if (uriStr.isNotEmpty()) {
                            val uri = Uri.parse(uriStr)
                            if (uri.scheme == "file") {
                                val path = uri.path
                                if (path != null) {
                                    val file = File(path)
                                    val templateDir = File(filesDir, "gmail_templates")
                                    if (file.absolutePath.startsWith(templateDir.absolutePath)) {
                                        file.delete()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showSaveTemplateDialog() {
        val subject = etSubject.text.toString().trim()
        val totalTextLength = blocks.filterIsInstance<Block.Text>().sumOf { it.editText.text.length }
        val hasImages = blocks.any { it is Block.Image }
        if (subject.isEmpty() && totalTextLength == 0 && !hasImages) {
            Toast.makeText(this, "Add a subject or body first", Toast.LENGTH_SHORT).show()
            return
        }
        val from = SmtpAccountManager.getEmail(this)
        val etName = EditText(this).apply { hint = "Template name" }
        AlertDialog.Builder(this)
            .setTitle("Save Template")
            .setView(etName)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                    .ifEmpty { "Template ${System.currentTimeMillis()}" }

                // Copy any block images to internal storage so they persist across sessions
                blocks.forEach { block ->
                    if (block is Block.Image) {
                        block.uri = saveUriToInternalStorage(block.uri)
                    }
                }
                val body = getBodySerialized()

                GmailTemplateManager.add(this, GmailTemplate(
                    id             = UUID.randomUUID().toString(),
                    name           = name,
                    from           = from,
                    subject        = subject,
                    body           = body,
                    attachmentUris = attachedUris.toList()
                ))
                refreshTemplates()
                val attMsg = if (attachedUris.isNotEmpty())
                    " (${attachedUris.size} attachment${if (attachedUris.size > 1) "s" else ""})"
                else ""
                Toast.makeText(this, "\"$name\"$attMsg saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null).show()
    }

    // ── Send logic ─────────────────────────────────────────────────────────
    private fun onSendTapped() {
        val to  = etTo.text.toString().trim()
        val bcc = etBcc.text.toString().trim()
        if (to.isEmpty() && bcc.isEmpty()) { etTo.error = "Add at least one recipient"; return }

        if (SmtpAccountManager.isConfigured(this)) {
            sendViaSMTP(to)
        } else {
            AlertDialog.Builder(this)
                .setTitle("No sending account")
                .setMessage("Configure your Gmail account to send directly, or open Gmail app instead?")
                .setPositiveButton("Configure") { _, _ -> showAccountDialog() }
                .setNegativeButton("Open Gmail") { _, _ -> openGmailApp(to) }
                .show()
        }
    }

    // ── Animated SMTP send ─────────────────────────────────────────────────
    private fun sendViaSMTP(to: String) {
        // Disable send button to prevent double-tap
        btnSend.isEnabled = false

        // 1. Show progress bar at 0
        pbSendProgress.progress = 0
        pbSendProgress.visibility = View.VISIBLE

        // 2. Play Apple's swoosh sound immediately
        playSendSound()

        // 3. Animate 0 → 95% over 4.5 seconds completely LINEARLY (simulates progress without stopping/sticking)
        progressAnimator = ValueAnimator.ofInt(0, 95).apply {
            duration = 4500L
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener { anim ->
                pbSendProgress.progress = anim.animatedValue as Int
            }
            start()
        }

        val htmlBody = getHTMLBody()
        val inlineImages = getInlineImageUris()
        SmtpEmailSender.sendAsync(
            context      = this,
            fromEmail    = SmtpAccountManager.getEmail(this),
            appPassword  = SmtpAccountManager.getPassword(this),
            to           = to,
            cc           = etCc.text.toString().trim(),
            bcc          = etBcc.text.toString().trim(),
            subject      = etSubject.text.toString().trim(),
            body         = htmlBody,
            isHtml       = true,
            inlineImages = inlineImages,
            attachments  = attachedUris.toList(),
            onSuccess = {
                // Stop the linear progress and sprint smoothly to 100%
                progressAnimator?.cancel()
                val from = pbSendProgress.progress
                ValueAnimator.ofInt(from, 100).apply {
                    duration = 300L
                    interpolator = android.view.animation.LinearInterpolator()
                    addUpdateListener { pbSendProgress.progress = it.animatedValue as Int }
                    start()
                }
                // Brief pause at 100% so user sees completion, then finish
                Handler(Looper.getMainLooper()).postDelayed({
                    Toast.makeText(this, "Message sent!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK); finish()
                }, 400L)
            },
            onError = { msg ->
                progressAnimator?.cancel()
                pbSendProgress.visibility = View.GONE
                pbSendProgress.progress   = 0
                btnSend.isEnabled = true
                AlertDialog.Builder(this)
                    .setTitle("Send failed")
                    .setMessage(msg)
                    .setPositiveButton("OK", null)
                    .show()
            }
        )
    }

    /**
     * Programmatically synthesizes the legendary Apple Mail "swoosh" sound
     * using AudioTrack (airy white noise + sine frequency upward sweep).
     */
    private fun playSendSound() {
        Thread {
            try {
                val sampleRate = 44100
                val durationMs = 150
                val numSamples = sampleRate * durationMs / 1000
                val generatedSnd = ByteArray(numSamples * 2)

                for (i in 0 until numSamples) {

                    val t = i.toDouble() / sampleRate
                    val progress = i.toDouble() / numSamples

                    // Smooth pitch rise
                    val freq = 650.0 + 550.0 * progress

                    val phase = 2.0 * Math.PI * freq * t

                    // Main tone
                    val fundamental = Math.sin(phase)

                    // Subtle harmonics for richness
                    val harmonic2 = 0.25 * Math.sin(phase * 2.0)
                    val harmonic3 = 0.08 * Math.sin(phase * 3.0)

                    var wave = fundamental + harmonic2 + harmonic3

                    // Soft attack
                    val attack =
                        if (progress < 0.08)
                            progress / 0.08
                        else
                            1.0

                    // Exponential fade
                    val decay = Math.exp(-4.0 * progress)

                    val envelope = attack * decay

                    wave *= envelope * 0.35

                    val sample = (wave * Short.MAX_VALUE).toInt()

                    generatedSnd[i * 2] = (sample and 0xFF).toByte()
                    generatedSnd[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
                }

                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    generatedSnd.size,
                    AudioTrack.MODE_STATIC
                )

                audioTrack.write(generatedSnd, 0, generatedSnd.size)
                audioTrack.play()

                Thread.sleep(durationMs.toLong() + 50)

                audioTrack.release()

            } catch (_: Exception) {
            }
        }.start()
    }

    // ── Gmail fallback ─────────────────────────────────────────────────────
    private fun openGmailApp(to: String) {
        val cc      = etCc.text.toString().trim()
        val bcc     = etBcc.text.toString().trim()
        val subject = etSubject.text.toString().trim()
        val plainTextBody = blocks.filterIsInstance<Block.Text>().joinToString("\n\n") { it.editText.text.toString() }
        val allAttachments = ArrayList<Uri>().apply {
            addAll(attachedUris)
            addAll(getInlineImageUris())
        }

        val intent: Intent = if (allAttachments.isEmpty()) {
            val mailto = Uri.Builder().scheme("mailto")
                .opaquePart(Uri.encode(to))
                .appendQueryParameter("subject", subject)
                .appendQueryParameter("body", plainTextBody)
                .apply {
                    if (cc.isNotEmpty())  appendQueryParameter("cc", cc)
                    if (bcc.isNotEmpty()) appendQueryParameter("bcc", bcc)
                }.build()
            Intent(Intent.ACTION_SENDTO, mailto).apply { setPackage("com.google.android.gm") }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"; setPackage("com.google.android.gm")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
                if (cc.isNotEmpty())  putExtra(Intent.EXTRA_CC,  arrayOf(cc))
                if (bcc.isNotEmpty()) putExtra(Intent.EXTRA_BCC, arrayOf(bcc))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, plainTextBody)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, allAttachments)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        runCatching { startActivity(intent) }
            .onFailure {
                startActivity(Intent.createChooser(intent.apply { setPackage(null) }, "Open with…"))
            }
        setResult(RESULT_OK); finish()
    }

    // ── Block Editor Logic & Dynamic Views ───────────────────────────────────

    private fun initBodyBlocks() {
        layoutBodyBlocks.removeAllViews()
        blocks.clear()
        val firstEt = createTextViewBlock("")
        layoutBodyBlocks.addView(firstEt)
        blocks.add(Block.Text(firstEt))
        updateHintState()
    }

    private fun createTextViewBlock(initialText: String): EditText {
        val et = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            background = null
            textSize = 15f
            setTextColor(0xFF212121.toInt())
            setHintTextColor(0xFF9E9E9E.toInt())
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
            setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                        android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(initialText)

            ViewCompat.setOnReceiveContentListener(
                this,
                arrayOf("image/*"),
                OnReceiveContentListener { _, payload ->
                    val pair = payload.partition { item -> item.uri != null }
                    val uriContent = pair.first
                    val remaining = pair.second

                    if (uriContent != null) {
                        val clip = uriContent.clip
                        for (i in 0 until clip.itemCount) {
                            val uri = clip.getItemAt(i).uri
                            if (uri != null) {
                                val cursor = selectionStart
                                val textStr = text.toString()
                                val before = if (cursor >= 0 && cursor <= textStr.length) textStr.substring(0, cursor) else textStr
                                val after = if (cursor >= 0 && cursor <= textStr.length) textStr.substring(cursor) else ""

                                setText(before)

                                val currentBlock = blocks.find { it is Block.Text && it.editText == this }
                                val idx = blocks.indexOf(currentBlock)

                                val imgBlockView = createImageBlockView(uri, -1, -1)
                                layoutBodyBlocks.addView(imgBlockView, idx + 1)

                                val nextEt = createTextViewBlock(after)
                                layoutBodyBlocks.addView(nextEt, idx + 2)

                                if (idx >= 0) {
                                    val imgW = dpToPx(240)
                                    val imgH = dpToPx(180)
                                    blocks.add(idx + 1, Block.Image(imgBlockView, imgBlockView.findViewWithTag("image_view"), uri, imgW, imgH))
                                    blocks.add(idx + 2, Block.Text(nextEt))
                                }

                                nextEt.requestFocus()
                                nextEt.setSelection(0)
                                break
                            }
                        }
                    }
                    remaining
                }
            )

            setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL && event.action == android.view.KeyEvent.ACTION_DOWN) {
                    if (selectionStart == 0 && selectionEnd == 0) {
                        val currentBlock = blocks.find { it is Block.Text && it.editText == this }
                        val idx = blocks.indexOf(currentBlock)
                        if (idx > 0) {
                            val blockAbove = blocks[idx - 1]
                            if (blockAbove is Block.Image) {
                                layoutBodyBlocks.removeView(blockAbove.container)
                                blocks.removeAt(idx - 1)

                                if (idx - 2 >= 0) {
                                    val textBlockAbove = blocks[idx - 2]
                                    if (textBlockAbove is Block.Text) {
                                        val originalText = textBlockAbove.editText.text.toString()
                                        val thisText = text.toString()
                                        textBlockAbove.editText.setText(originalText + thisText)
                                        textBlockAbove.editText.requestFocus()
                                        textBlockAbove.editText.setSelection(originalText.length)

                                        layoutBodyBlocks.removeView(this)
                                        blocks.remove(currentBlock)
                                    }
                                }
                                updateHintState()
                                return@setOnKeyListener true
                            }
                        }
                    }
                }
                false
            }

            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    updateHintState()
                }
            })
        }
        return et
    }

    private fun createImageBlockView(uri: Uri, targetWidth: Int, targetHeight: Int): View {
        val root = RelativeLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
        }

        val imageContainer = RelativeLayout(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        var originalWidth = 400
        var originalHeight = 300
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            if (options.outWidth > 0 && options.outHeight > 0) {
                originalWidth = options.outWidth
                originalHeight = options.outHeight
            }
        } catch (e: Exception) {}
        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()

        val initialWidth = if (targetWidth > 0) targetWidth else dpToPx(240)
        val initialHeight = if (targetHeight > 0) targetHeight else (initialWidth / aspectRatio).toInt()

        val imageViewId = View.generateViewId()
        val imageView = ImageView(this).apply {
            id = imageViewId
            tag = "image_view"
            layoutParams = RelativeLayout.LayoutParams(initialWidth, initialHeight).apply {
                addRule(RelativeLayout.ALIGN_PARENT_TOP)
                addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, dpToPx(12).toFloat())
                }
            }

            val bmp = loadScaledBitmap(uri, dpToPx(400), dpToPx(400))
            if (bmp != null) {
                setImageBitmap(bmp)
            } else {
                setImageURI(uri)
            }
        }
        imageContainer.addView(imageView)

        val btnDelete = FrameLayout(this).apply {
            layoutParams = RelativeLayout.LayoutParams(dpToPx(28), dpToPx(28)).apply {
                addRule(RelativeLayout.ALIGN_RIGHT, imageViewId)
                addRule(RelativeLayout.ALIGN_TOP, imageViewId)
                setMargins(0, dpToPx(6), dpToPx(6), 0)
            }
            background = getDrawable(R.drawable.bg_circle_dark)?.apply {
                setTint(0xCC757575.toInt())
            }
            isClickable = true
            isFocusable = true

            val closeIcon = ImageView(this@GmailRouterActivity).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(12), dpToPx(12)).apply {
                    gravity = android.view.Gravity.CENTER
                }
                setImageDrawable(getDrawable(R.drawable.ic_close))
            }
            addView(closeIcon)
        }

        val resizeHandle = FrameLayout(this).apply {
            layoutParams = RelativeLayout.LayoutParams(dpToPx(24), dpToPx(24)).apply {
                addRule(RelativeLayout.ALIGN_RIGHT, imageViewId)
                addRule(RelativeLayout.ALIGN_BOTTOM, imageViewId)
                setMargins(0, 0, dpToPx(6), dpToPx(6))
            }
            background = getDrawable(R.drawable.bg_circle_dark)?.apply {
                setTint(getColor(R.color.primary_orange))
            }
            isClickable = true
            isFocusable = true

            val dot = View(this@GmailRouterActivity).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(6), dpToPx(6)).apply {
                    gravity = android.view.Gravity.CENTER
                }
                background = getDrawable(R.drawable.bg_circle_dark)?.apply {
                    setTint(0xFFFFFFFF.toInt())
                }
            }
            addView(dot)
        }
        imageContainer.addView(resizeHandle)
        imageContainer.addView(btnDelete)

        root.addView(imageContainer)

        var initialTouchX = 0f
        var startW = 0

        resizeHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchX = event.rawX
                    startW = imageView.width
                    (parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dX = event.rawX - initialTouchX
                    var newW = (startW + dX).toInt()

                    val minW = dpToPx(80)
                    val maxW = root.width - dpToPx(32)
                    if (newW < minW) newW = minW
                    if (newW > maxW) newW = maxW

                    val newH = (newW / aspectRatio).toInt()

                    imageView.layoutParams = RelativeLayout.LayoutParams(newW, newH).apply {
                        addRule(RelativeLayout.ALIGN_PARENT_TOP)
                        addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                    }
                    imageView.requestLayout()

                    val currentBlock = blocks.find { it is Block.Image && it.container == root }
                    if (currentBlock is Block.Image) {
                        currentBlock.width = newW
                        currentBlock.height = newH
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    (parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(false)
                    true
                }
                else -> false
            }
        }

        btnDelete.setOnClickListener {
            layoutBodyBlocks.removeView(root)
            val currentBlock = blocks.find { it is Block.Image && it.container == root }
            if (currentBlock != null) {
                val idx = blocks.indexOf(currentBlock)
                blocks.remove(currentBlock)

                if (idx > 0 && idx < blocks.size) {
                    val blockBefore = blocks[idx - 1]
                    val blockAfter = blocks[idx]
                    if (blockBefore is Block.Text && blockAfter is Block.Text) {
                        val textBefore = blockBefore.editText.text.toString()
                        val textAfter = blockAfter.editText.text.toString()

                        blockBefore.editText.setText(textBefore + textAfter)
                        blockBefore.editText.requestFocus()
                        blockBefore.editText.setSelection(textBefore.length)

                        layoutBodyBlocks.removeView(blockAfter.editText)
                        blocks.removeAt(idx)
                    }
                }
            }
            updateHintState()
        }

        return root
    }

    private fun insertImageIntoBody(uri: Uri) {
        var focusedBlock = blocks.find { it is Block.Text && it.editText.isFocused }
        if (focusedBlock == null) {
            focusedBlock = blocks.findLast { it is Block.Text }
        }

        if (focusedBlock is Block.Text) {
            val et = focusedBlock.editText
            val cursor = et.selectionStart
            val textStr = et.text.toString()
            val before = if (cursor >= 0 && cursor <= textStr.length) textStr.substring(0, cursor) else textStr
            val after = if (cursor >= 0 && cursor <= textStr.length) textStr.substring(cursor) else ""

            et.setText(before)

            val idx = blocks.indexOf(focusedBlock)

            val imgBlockView = createImageBlockView(uri, -1, -1)
            layoutBodyBlocks.addView(imgBlockView, idx + 1)

            val nextEt = createTextViewBlock(after)
            layoutBodyBlocks.addView(nextEt, idx + 2)

            if (idx >= 0) {
                val imgW = dpToPx(240)
                val imgH = dpToPx(180)
                blocks.add(idx + 1, Block.Image(imgBlockView, imgBlockView.findViewWithTag("image_view"), uri, imgW, imgH))
                blocks.add(idx + 2, Block.Text(nextEt))
            }

            nextEt.requestFocus()
            nextEt.setSelection(0)
        } else {
            val imgBlockView = createImageBlockView(uri, -1, -1)
            layoutBodyBlocks.addView(imgBlockView)

            val nextEt = createTextViewBlock("")
            layoutBodyBlocks.addView(nextEt)

            val imgW = dpToPx(240)
            val imgH = dpToPx(180)
            blocks.add(Block.Image(imgBlockView, imgBlockView.findViewWithTag("image_view"), uri, imgW, imgH))
            blocks.add(Block.Text(nextEt))

            nextEt.requestFocus()
        }
        updateHintState()
    }

    private fun loadScaledBitmap(uri: Uri, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            var scale = 1
            while (options.outWidth / scale / 2 >= targetWidth && options.outHeight / scale / 2 >= targetHeight) {
                scale *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun pxToDp(px: Int): Int {
        val density = resources.displayMetrics.density
        return if (density > 0) (px / density).toInt() else px
    }

    private fun getBodySerialized(): String {
        val arr = JSONArray()
        blocks.forEach { block ->
            val obj = JSONObject()
            when (block) {
                is Block.Text -> {
                    obj.put("type", "text")
                    obj.put("content", block.editText.text.toString())
                }
                is Block.Image -> {
                    obj.put("type", "image")
                    obj.put("uri", block.uri.toString())
                    obj.put("width", block.width)
                    obj.put("height", block.height)
                }
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun loadSerializedBody(bodyStr: String) {
        layoutBodyBlocks.removeAllViews()
        blocks.clear()

        try {
            if (bodyStr.startsWith("[") && bodyStr.endsWith("]")) {
                val arr = JSONArray(bodyStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val type = obj.getString("type")
                    if (type == "text") {
                        val content = obj.optString("content", "")
                        val et = createTextViewBlock(content)
                        layoutBodyBlocks.addView(et)
                        blocks.add(Block.Text(et))
                    } else if (type == "image") {
                        val uri = Uri.parse(obj.getString("uri"))
                        val w = obj.getInt("width")
                        val h = obj.getInt("height")
                        val imgBlockView = createImageBlockView(uri, w, h)
                        layoutBodyBlocks.addView(imgBlockView)
                        blocks.add(Block.Image(imgBlockView, imgBlockView.findViewWithTag("image_view"), uri, w, h))
                    }
                }
            } else {
                val et = createTextViewBlock(bodyStr)
                layoutBodyBlocks.addView(et)
                blocks.add(Block.Text(et))
            }
        } catch (e: Exception) {
            val et = createTextViewBlock(bodyStr)
            layoutBodyBlocks.addView(et)
            blocks.add(Block.Text(et))
        }

        if (blocks.isEmpty()) {
            initBodyBlocks()
        }
        updateHintState()
    }

    private fun getHTMLBody(): String {
        val sb = StringBuilder()
        sb.append("<html><body style=\"font-family: sans-serif; font-size: 15px; color: #212121;\">")
        var inlineIdx = 0
        blocks.forEach { block ->
            when (block) {
                is Block.Text -> {
                    val txt = block.editText.text.toString()
                    val paragraphs = txt.split("\n")
                    paragraphs.forEach { p ->
                        if (p.isNotEmpty()) {
                            sb.append("<p>").append(Html.escapeHtml(p)).append("</p>")
                        } else {
                            sb.append("<br/>")
                        }
                    }
                }
                is Block.Image -> {
                    sb.append("<div style=\"margin: 12px 0;\">")
                    sb.append("<img src=\"cid:inline_img_$inlineIdx\" style=\"width:")
                      .append(pxToDp(block.width)).append("px; height:")
                      .append(pxToDp(block.height)).append("px; max-width: 100%; border-radius: 8px;\" />")
                    sb.append("</div>")
                    inlineIdx++
                }
            }
        }
        sb.append("</body></html>")
        return sb.toString()
    }

    private fun getInlineImageUris(): List<Uri> {
        val uris = mutableListOf<Uri>()
        blocks.forEach { block ->
            if (block is Block.Image) {
                uris.add(block.uri)
            }
        }
        return uris
    }

    private fun updateHintState() {
        val totalTextLength = blocks.filterIsInstance<Block.Text>().sumOf { it.editText.text.length }
        val hasImages = blocks.any { it is Block.Image }

        blocks.filterIsInstance<Block.Text>().forEachIndexed { index, textBlock ->
            if (index == 0 && totalTextLength == 0 && !hasImages) {
                textBlock.editText.hint = "Compose email"
            } else {
                textBlock.editText.hint = ""
            }
        }
    }

    private fun showDateTimePicker() {
        val c = java.util.Calendar.getInstance()
        val year = c.get(java.util.Calendar.YEAR)
        val month = c.get(java.util.Calendar.MONTH)
        val day = c.get(java.util.Calendar.DAY_OF_MONTH)

        val datePicker = android.app.DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val hour = c.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = c.get(java.util.Calendar.MINUTE)

            val timePicker = android.app.TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                val scheduleCalendar = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.YEAR, selectedYear)
                    set(java.util.Calendar.MONTH, selectedMonth)
                    set(java.util.Calendar.DAY_OF_MONTH, selectedDay)
                    set(java.util.Calendar.HOUR_OF_DAY, selectedHour)
                    set(java.util.Calendar.MINUTE, selectedMinute)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }

                val timeMs = scheduleCalendar.timeInMillis
                if (timeMs <= System.currentTimeMillis()) {
                    Toast.makeText(this, "Please select a future time", Toast.LENGTH_SHORT).show()
                } else {
                    showScheduleConfirmationDialog(timeMs)
                }
            }, hour, minute, true)
            timePicker.show()
        }, year, month, day)

        // Set minimum date to today
        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
        datePicker.show()
    }

    private fun showScheduleConfirmationDialog(scheduledTimeMs: Long) {
        val sdf = java.text.SimpleDateFormat("EEEE, dd MMM yyyy 'at' HH:mm", java.util.Locale.getDefault())
        val formatted = sdf.format(java.util.Date(scheduledTimeMs))

        val to = etTo.text.toString().trim()
        val bcc = etBcc.text.toString().trim()
        if (to.isEmpty() && bcc.isEmpty()) {
            etTo.error = "Add at least one recipient"
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm Schedule")
            .setMessage("Do you want to schedule this email to be sent on:\n\n$formatted?")
            .setPositiveButton("Schedule") { _, _ ->
                val email = ScheduledEmail(
                    to = to,
                    cc = etCc.text.toString().trim(),
                    bcc = bcc,
                    subject = etSubject.text.toString().trim(),
                    body = getHTMLBody(),
                    isHtml = true,
                    inlineImageUris = getInlineImageUris().map { it.toString() },
                    attachmentUris = attachedUris.map { it.toString() },
                    scheduledTimeMs = scheduledTimeMs
                )

                ScheduledEmailManager.schedule(this, email)
                Toast.makeText(this, "Email scheduled!", Toast.LENGTH_LONG).show()
                setResult(RESULT_OK)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
