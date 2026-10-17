package com.essential.shortcuts

import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.StyleSpan
import android.util.Xml
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class WordPdfConverterActivity : AppCompatActivity() {

    private data class SelectedDoc(
        val uri: Uri,
        val name: String,
        val size: Long
    )

    private var selectedDocs: MutableList<SelectedDoc> = mutableListOf()
    private var convertedFiles: MutableList<File> = mutableListOf()
    private var targetFormat: String = ""

    // UI elements
    private lateinit var tvTitle: TextView
    private lateinit var layoutPickPlaceholder: LinearLayout
    private lateinit var layoutFormatPicker: LinearLayout
    private lateinit var tvSelectedSummary: TextView
    private lateinit var btnClearSelection: ImageButton
    private lateinit var layoutOptionsContainer: LinearLayout
    private lateinit var btnFormatWord: TextView
    private lateinit var dividerFormat: View
    private lateinit var btnFormatPdf: TextView
    private lateinit var layoutConverting: LinearLayout
    private lateinit var tvConvertingStatus: TextView
    private lateinit var layoutResult: LinearLayout
    private lateinit var tvConvertedInfo: TextView
    private lateinit var tvConvertedDetail: TextView
    private lateinit var btnSave: Button
    private lateinit var btnShare: Button

    private val multipleFilePicker = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri>? ->
        if (!uris.isNullOrEmpty()) {
            onFilesSelected(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_word_pdf_converter)

        PDFBoxResourceLoader.init(applicationContext)

        initViews()
        setupListeners()
        handleIncomingIntent()
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

            // Almost edge-to-edge (97% width) matching ImageConverterActivity
            val width = (resources.displayMetrics.widthPixels * 0.97).toInt()
            layoutParams.width = width
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            attributes = layoutParams

            val isFromShare = intent.hasExtra("shared_image_uri") ||
                    intent.action == Intent.ACTION_SEND ||
                    intent.action == Intent.ACTION_SEND_MULTIPLE
            if (isFromShare) {
                clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
        }
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tv_title)
        layoutPickPlaceholder = findViewById(R.id.layout_pick_placeholder)
        layoutFormatPicker = findViewById(R.id.layout_format_picker)
        tvSelectedSummary = findViewById(R.id.tv_selected_summary)
        btnClearSelection = findViewById(R.id.btn_clear_selection)
        layoutOptionsContainer = findViewById(R.id.layout_options_container)
        btnFormatWord = findViewById(R.id.btn_format_word)
        dividerFormat = findViewById(R.id.divider_format)
        btnFormatPdf = findViewById(R.id.btn_format_pdf)
        layoutConverting = findViewById(R.id.layout_converting)
        tvConvertingStatus = findViewById(R.id.tv_converting_status)
        layoutResult = findViewById(R.id.layout_result)
        tvConvertedInfo = findViewById(R.id.tv_converted_info)
        tvConvertedDetail = findViewById(R.id.tv_converted_detail)
        btnSave = findViewById(R.id.btn_save)
        btnShare = findViewById(R.id.btn_share)
    }

    private fun setupListeners() {
        layoutPickPlaceholder.setOnClickListener {
            multipleFilePicker.launch("*/*")
        }

        btnClearSelection.setOnClickListener {
            switchToPickState()
        }

        btnFormatWord.setOnClickListener { performConversion("WORD") }
        btnFormatPdf.setOnClickListener { performConversion("PDF") }

        btnSave.setOnClickListener { saveConvertedFiles() }
        btnShare.setOnClickListener { shareConvertedFiles() }
    }

    private fun handleIncomingIntent() {
        val extraMode = intent.getStringExtra("converter_mode")

        val uris = mutableListOf<Uri>()

        val streamList = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        if (!streamList.isNullOrEmpty()) {
            uris.addAll(streamList)
        }

        val singleStream = intent.getParcelableExtra<Uri>("shared_image_uri")
            ?: intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            ?: intent.data
        if (singleStream != null && !uris.contains(singleStream)) {
            uris.add(singleStream)
        }

        intent.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) {
                val itemUri = clip.getItemAt(i).uri
                if (itemUri != null && !uris.contains(itemUri)) {
                    uris.add(itemUri)
                }
            }
        }

        if (uris.isNotEmpty()) {
            onFilesSelected(uris, extraMode)
        }
    }

    private fun onFilesSelected(uris: List<Uri>, forcedMode: String? = null) {
        val newDocs = uris.map { uri ->
            val (name, size) = queryFileInfo(uri)
            SelectedDoc(uri, name, size)
        }
        selectedDocs = newDocs.toMutableList()

        if (forcedMode == "pdf_to_word") {
            performConversion("WORD")
        } else if (forcedMode == "word_to_pdf") {
            performConversion("PDF")
        } else {
            switchToFormatState()
        }
    }

    private fun switchToPickState() {
        selectedDocs.clear()
        convertedFiles.clear()
        tvTitle.text = "Convert Documents"
        layoutPickPlaceholder.visibility = View.VISIBLE
        layoutFormatPicker.visibility = View.GONE
        layoutConverting.visibility = View.GONE
        layoutResult.visibility = View.GONE
    }

    private fun switchToFormatState() {
        tvTitle.text = "Convert Documents"
        layoutPickPlaceholder.visibility = View.GONE
        layoutFormatPicker.visibility = View.VISIBLE
        layoutConverting.visibility = View.GONE
        layoutResult.visibility = View.GONE

        // Update summary text
        if (selectedDocs.size == 1) {
            val doc = selectedDocs[0]
            tvSelectedSummary.text = "${doc.name} (${formatFileSize(doc.size)})"
        } else {
            val totalSize = selectedDocs.sumOf { it.size }
            tvSelectedSummary.text = "${selectedDocs.size} files selected • ${formatFileSize(totalSize)}"
        }

        // Auto-filter options based on selected file formats
        val pdfCount = selectedDocs.count { it.name.lowercase().endsWith(".pdf") }
        val wordCount = selectedDocs.count { it.name.lowercase().endsWith(".docx") || it.name.lowercase().endsWith(".doc") }

        if (pdfCount > 0 && wordCount == 0) {
            // Only PDFs selected -> show "Convert to Word"
            btnFormatWord.visibility = View.VISIBLE
            btnFormatWord.text = if (selectedDocs.size > 1) "Convert ${selectedDocs.size} PDFs to Word (.docx)" else "Convert to Word (.docx)"
            dividerFormat.visibility = View.GONE
            btnFormatPdf.visibility = View.GONE
        } else if (wordCount > 0 && pdfCount == 0) {
            // Only Word files selected -> show "Convert to PDF"
            btnFormatPdf.visibility = View.VISIBLE
            btnFormatPdf.text = if (selectedDocs.size > 1) "Convert ${selectedDocs.size} Word files to PDF (.pdf)" else "Convert to PDF (.pdf)"
            dividerFormat.visibility = View.GONE
            btnFormatWord.visibility = View.GONE
        } else {
            // Mixed or unspecific -> show both options
            btnFormatWord.visibility = View.VISIBLE
            btnFormatWord.text = "Convert to Word (.docx)"
            dividerFormat.visibility = View.VISIBLE
            btnFormatPdf.visibility = View.VISIBLE
            btnFormatPdf.text = "Convert to PDF (.pdf)"
        }

        // Cascading entrance animation for vertical options
        val container = layoutOptionsContainer
        var delayIndex = 0
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
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

    private fun performConversion(format: String) {
        if (selectedDocs.isEmpty()) return
        targetFormat = format

        tvTitle.text = "Converting"
        layoutPickPlaceholder.visibility = View.GONE
        layoutFormatPicker.visibility = View.GONE
        layoutConverting.visibility = View.VISIBLE
        layoutResult.visibility = View.GONE
        convertedFiles.clear()

        val isWordTarget = format == "WORD"
        val count = selectedDocs.size

        Thread {
            try {
                val outputList = mutableListOf<File>()
                for ((index, doc) in selectedDocs.withIndex()) {
                    runOnUiThread {
                        tvConvertingStatus.text = if (count == 1) {
                            if (isWordTarget) "Converting PDF to Word..." else "Converting Word to PDF..."
                        } else {
                            "Converting ${index + 1} of $count:\n${doc.name}"
                        }
                    }

                    val cacheDir = File(cacheDir, "converted").apply { mkdirs() }
                    val baseName = doc.name.substringBeforeLast(".", "document_${index + 1}")

                    if (isWordTarget) {
                        val outputFile = File(cacheDir, "${baseName}.docx")
                        convertSinglePdfToWord(doc.uri, outputFile)
                        outputList.add(outputFile)
                    } else {
                        val outputFile = File(cacheDir, "${baseName}.pdf")
                        convertSingleWordToPdf(doc.uri, doc.name, outputFile)
                        outputList.add(outputFile)
                    }
                }

                convertedFiles = outputList

                runOnUiThread {
                    tvTitle.text = "Conversion Complete"
                    layoutConverting.visibility = View.GONE
                    layoutResult.visibility = View.VISIBLE

                    val totalOutputSize = convertedFiles.sumOf { it.length() }
                    if (convertedFiles.size == 1) {
                        val f = convertedFiles[0]
                        tvConvertedInfo.text = "Successfully converted to ${f.name}!"
                        tvConvertedDetail.text = "Size: ${formatFileSize(f.length())}"
                        btnSave.text = "Save"
                        btnShare.text = "Share"
                    } else {
                        val targetExt = if (isWordTarget) "Word" else "PDF"
                        tvConvertedInfo.text = "Successfully converted ${convertedFiles.size} files to $targetExt!"
                        tvConvertedDetail.text = "Total Size: ${formatFileSize(totalOutputSize)}"
                        btnSave.text = "Save All"
                        btnShare.text = "Share All"
                    }

                    sendBroadcast(Intent("com.essential.shortcuts.ACTION_STEP_2").setPackage(packageName))
                }
            } catch (e: Exception) {
                runOnUiThread {
                    switchToFormatState()
                    Toast.makeText(this@WordPdfConverterActivity, "Conversion failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun convertSinglePdfToWord(uri: Uri, outputFile: File) {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open PDF file stream")

        val pdfDocument = PDDocument.load(inputStream)
        val totalPages = pdfDocument.numberOfPages
        val stripper = PDFTextStripper().apply {
            sortByPosition = true
        }

        val pageContents = mutableListOf<String>()
        for (p in 1..totalPages) {
            stripper.startPage = p
            stripper.endPage = p
            val pageText = stripper.getText(pdfDocument)
            pageContents.add(pageText)
        }
        pdfDocument.close()
        inputStream.close()

        buildDocxFile(pageContents, outputFile)
    }

    private fun buildDocxFile(pages: List<String>, outputFile: File) {
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>"""
            zos.write(contentTypesXml.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("_rels/.rels"))
            val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
            zos.write(relsXml.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
            val docRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
            zos.write(docRelsXml.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("word/styles.xml"))
            val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault>
      <w:rPr>
        <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/>
        <w:sz w:val="22"/>
      </w:rPr>
    </w:rPrDefault>
  </w:docDefaults>
</w:styles>"""
            zos.write(stylesXml.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("word/document.xml"))
            val sb = java.lang.StringBuilder()
            sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            sb.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
            sb.append("<w:body>")

            for ((index, pageText) in pages.withIndex()) {
                val lines = pageText.split("\n")
                for (line in lines) {
                    val trimmed = line.trimEnd()
                    if (trimmed.isEmpty()) {
                        sb.append("<w:p/>")
                    } else {
                        sb.append("<w:p><w:pPr><w:spacing w:after=\"120\" w:line=\"240\" w:lineRule=\"auto\"/></w:pPr>")
                        sb.append("<w:r><w:t xml:space=\"preserve\">")
                        sb.append(escapeXml(trimmed))
                        sb.append("</w:t></w:r></w:p>")
                    }
                }
                if (index < pages.size - 1) {
                    sb.append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>")
                }
            }

            sb.append("""<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="720" w:footer="720" w:gutter="0"/></w:sectPr>""")
            sb.append("</w:body></w:document>")

            zos.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }
    }

    private fun convertSingleWordToPdf(uri: Uri, originalName: String, outputFile: File) {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open Word file stream")

        val isDocx = originalName.lowercase().endsWith(".docx") || try {
            val bis = java.io.BufferedInputStream(contentResolver.openInputStream(uri))
            bis.mark(4)
            val b1 = bis.read()
            val b2 = bis.read()
            bis.close()
            b1 == 0x50 && b2 == 0x4B
        } catch (e: Exception) {
            true
        }

        val paragraphs = if (isDocx) {
            parseDocxParagraphs(inputStream)
        } else {
            parseDocPlainText(inputStream)
        }
        inputStream.close()

        if (paragraphs.isEmpty()) {
            throw IllegalStateException("Document contains no readable text: $originalName")
        }

        renderParagraphsToPdf(paragraphs, outputFile)
    }

    private data class ParsedRun(
        val text: String,
        val isBold: Boolean,
        val isItalic: Boolean,
        val sizePt: Float
    )

    private data class ParsedParagraph(
        val runs: List<ParsedRun>,
        val isPageBreak: Boolean = false
    )

    private fun parseDocxParagraphs(inputStream: InputStream): List<ParsedParagraph> {
        val paragraphs = mutableListOf<ParsedParagraph>()
        val zis = ZipInputStream(inputStream)
        var entry = zis.nextEntry

        var documentXmlFound = false
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                documentXmlFound = true
                val parser = Xml.newPullParser()
                parser.setInput(zis, "UTF-8")

                var currentRuns = mutableListOf<ParsedRun>()
                var runText = StringBuilder()
                var inRun = false
                var isBold = false
                var isItalic = false
                var sizePt = 11f
                var isPageBreak = false

                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name) {
                                "p" -> {
                                    currentRuns = mutableListOf()
                                    isPageBreak = false
                                }
                                "r" -> {
                                    inRun = true
                                    runText.clear()
                                    isBold = false
                                    isItalic = false
                                    sizePt = 11f
                                }
                                "b" -> if (inRun) isBold = true
                                "i" -> if (inRun) isItalic = true
                                "sz" -> {
                                    val szVal = parser.getAttributeValue(null, "val")?.toIntOrNull()
                                    if (szVal != null) {
                                        sizePt = szVal / 2f
                                    }
                                }
                                "br" -> {
                                    val type = parser.getAttributeValue(null, "type")
                                    if (type == "page") {
                                        isPageBreak = true
                                    } else {
                                        runText.append("\n")
                                    }
                                }
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inRun) {
                                runText.append(parser.text)
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            when (parser.name) {
                                "r" -> {
                                    if (runText.isNotEmpty()) {
                                        currentRuns.add(ParsedRun(runText.toString(), isBold, isItalic, sizePt))
                                    }
                                    inRun = false
                                }
                                "p" -> {
                                    if (currentRuns.isNotEmpty() || isPageBreak) {
                                        paragraphs.add(ParsedParagraph(currentRuns, isPageBreak))
                                    }
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }
                break
            }
            entry = zis.nextEntry
        }

        if (!documentXmlFound) {
            throw IllegalArgumentException("Invalid .docx file: word/document.xml not found")
        }
        return paragraphs
    }

    private fun parseDocPlainText(inputStream: InputStream): List<ParsedParagraph> {
        val bytes = inputStream.readBytes()
        val text = String(bytes, StandardCharsets.ISO_8859_1)
        val clean = text.filter { it in ' '..'~' || it == '\n' || it == '\r' || it == '\t' }
        val paragraphs = mutableListOf<ParsedParagraph>()
        clean.split("\n").forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                paragraphs.add(ParsedParagraph(listOf(ParsedRun(trimmed, isBold = false, isItalic = false, sizePt = 11f))))
            }
        }
        return paragraphs
    }

    private fun renderParagraphsToPdf(paragraphs: List<ParsedParagraph>, outputFile: File): Int {
        val pdfDocument = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842
        val marginX = 50f
        val marginY = 50f
        val printableWidth = (pageWidth - marginX * 2).toInt()
        val maxPageY = pageHeight - marginY

        var pageIndex = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas
        var currentY = marginY

        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        for (paragraph in paragraphs) {
            if (paragraph.isPageBreak) {
                canvas.drawText("Page $pageIndex", pageWidth - marginX, pageHeight - 25f, footerPaint)
                pdfDocument.finishPage(currentPage)
                pageIndex++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                currentY = marginY
                continue
            }

            if (paragraph.runs.isEmpty()) {
                currentY += 12f
                continue
            }

            val spannable = SpannableStringBuilder()
            for (run in paragraph.runs) {
                val start = spannable.length
                spannable.append(run.text)
                val end = spannable.length

                if (run.isBold && run.isItalic) {
                    spannable.setSpan(StyleSpan(Typeface.BOLD_ITALIC), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else if (run.isBold) {
                    spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else if (run.isItalic) {
                    spannable.setSpan(StyleSpan(Typeface.ITALIC), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            val textPaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 11f
                isAntiAlias = true
            }

            val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(spannable, 0, spannable.length, textPaint, printableWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.15f)
                    .setIncludePad(false)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(spannable, textPaint, printableWidth, Layout.Alignment.ALIGN_NORMAL, 1.15f, 0f, false)
            }

            val layoutHeight = staticLayout.height.toFloat()

            if (currentY + layoutHeight > maxPageY) {
                canvas.drawText("Page $pageIndex", pageWidth - marginX, pageHeight - 25f, footerPaint)
                pdfDocument.finishPage(currentPage)
                pageIndex++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                currentY = marginY
            }

            canvas.save()
            canvas.translate(marginX, currentY)
            staticLayout.draw(canvas)
            canvas.restore()

            currentY += layoutHeight + 8f
        }

        canvas.drawText("Page $pageIndex", pageWidth - marginX, pageHeight - 25f, footerPaint)
        pdfDocument.finishPage(currentPage)

        FileOutputStream(outputFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()

        return pageIndex
    }

    private fun saveConvertedFiles() {
        if (convertedFiles.isEmpty()) return
        var successCount = 0

        try {
            for (file in convertedFiles) {
                val isDocx = file.name.endsWith(".docx")
                val mimeType = if (isDocx) {
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                } else {
                    "application/pdf"
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                        put(MediaStore.Downloads.MIME_TYPE, mimeType)
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/EssentialShortcuts")
                    }
                    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        contentResolver.openOutputStream(uri)?.use { os ->
                            FileInputStream(file).use { fis ->
                                fis.copyTo(os)
                            }
                        }
                        successCount++
                    }
                } else {
                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "EssentialShortcuts")
                    dir.mkdirs()
                    val destFile = File(dir, file.name)
                    file.copyTo(destFile, overwrite = true)
                    successCount++
                }
            }

            val msg = if (successCount == 1) {
                "Saved to Downloads/EssentialShortcuts"
            } else {
                "Saved $successCount files to Downloads/EssentialShortcuts"
            }
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareConvertedFiles() {
        if (convertedFiles.isEmpty()) return

        try {
            val isDocx = convertedFiles[0].name.endsWith(".docx")
            val mimeType = if (isDocx) {
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            } else {
                "application/pdf"
            }

            val uris = ArrayList<Uri>()
            for (file in convertedFiles) {
                uris.add(FileProvider.getUriForFile(this, "$packageName.fileprovider", file))
            }

            val shareIntent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_STREAM, uris[0])
                    type = mimeType
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    type = mimeType
                }
            }
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(shareIntent, "Share Converted Files"))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun queryFileInfo(uri: Uri): Pair<String, Long> {
        var name = "document"
        var size = 0L

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) name = cursor.getString(nameIndex) ?: "document"
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }
        if (size == 0L) {
            try {
                contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                    size = it.length
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        if (!name.contains(".")) {
            val type = contentResolver.getType(uri) ?: ""
            if (type.contains("pdf")) {
                name += ".pdf"
            } else if (type.contains("word") || type.contains("officedocument") || type.contains("msword")) {
                name += ".docx"
            }
        }
        return Pair(name, size)
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
