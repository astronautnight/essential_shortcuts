package com.essential.shortcuts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class OpenFileActivity : AppCompatActivity() {

    private lateinit var layoutFilesList: LinearLayout
    private lateinit var layoutEmptyPlaceholder: LinearLayout
    private lateinit var btnAddFileRow: LinearLayout
    private lateinit var btnHeaderAdd: ImageView

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            handleSelectedFile(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_file)

        initViews()
        setupListeners()
        loadAndDisplayFiles(animate = true)
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

            // Almost edge-to-edge (97% width)
            val width = (resources.displayMetrics.widthPixels * 0.97).toInt()
            layoutParams.width = width
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            attributes = layoutParams
        }
    }

    private fun initViews() {
        layoutFilesList = findViewById(R.id.layout_files_list)
        layoutEmptyPlaceholder = findViewById(R.id.layout_empty_placeholder)
        btnAddFileRow = findViewById(R.id.btn_add_file_row)
        btnHeaderAdd = findViewById(R.id.btn_header_add)
    }

    private fun setupListeners() {
        btnAddFileRow.setOnClickListener { launchFilePicker() }
        btnHeaderAdd.setOnClickListener { launchFilePicker() }
    }

    private fun launchFilePicker() {
        filePickerLauncher.launch(arrayOf("*/*"))
    }

    private fun handleSelectedFile(uri: Uri) {
        try {
            // Take persistable URI permission so file can be reopened across reboots
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        var fileName = "Document"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val mimeType = contentResolver.getType(uri) ?: "*/*"
        OpenFileManager.saveFile(this, fileName, uri.toString(), mimeType)
        loadAndDisplayFiles(animate = true)
    }

    private fun loadAndDisplayFiles(animate: Boolean) {
        val savedFiles = OpenFileManager.getSavedFiles(this)
        layoutFilesList.removeAllViews()

        if (savedFiles.isEmpty()) {
            layoutEmptyPlaceholder.visibility = View.VISIBLE
        } else {
            layoutEmptyPlaceholder.visibility = View.GONE

            val inflater = LayoutInflater.from(this)
            for (savedFile in savedFiles) {
                val itemView = inflater.inflate(R.layout.item_converter_option, layoutFilesList, false)
                val tvTitle = itemView.findViewById<TextView>(R.id.tv_option_title)
                val tvSubtitle = itemView.findViewById<TextView>(R.id.tv_option_subtitle)
                val btnDelete = itemView.findViewById<ImageView>(R.id.btn_delete_option)
                val optionRow = itemView.findViewById<View>(R.id.option_row)

                // Show the name of the already saved file
                tvTitle.text = savedFile.name

                val subtitle = formatSubtitle(savedFile)
                if (subtitle.isNotEmpty()) {
                    tvSubtitle.text = subtitle
                    tvSubtitle.visibility = View.VISIBLE
                } else {
                    tvSubtitle.visibility = View.GONE
                }

                optionRow.setOnClickListener {
                    openFile(savedFile)
                }

                optionRow.setOnLongClickListener {
                    confirmDeleteFile(savedFile)
                    true
                }

                btnDelete.setOnClickListener {
                    confirmDeleteFile(savedFile)
                }

                layoutFilesList.addView(itemView)
            }
        }

        if (animate) {
            animateEntrance()
        }
    }

    private fun formatSubtitle(savedFile: SavedFile): String {
        val ext = savedFile.name.substringAfterLast('.', "").uppercase()
        return if (ext.isNotEmpty() && ext.length <= 6) {
            ext
        } else {
            savedFile.mimeType.takeIf { it != "*/*" } ?: ""
        }
    }

    private fun animateEntrance() {
        var delayIndex = 0
        for (i in 0 until layoutFilesList.childCount) {
            val child = layoutFilesList.getChildAt(i)
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

        btnAddFileRow.alpha = 0f
        btnAddFileRow.translationY = 30f
        btnAddFileRow.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(250)
            .setStartDelay(delayIndex * 35L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun openFile(savedFile: SavedFile) {
        val uri = Uri.parse(savedFile.uriString)
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val mime = if (savedFile.mimeType.isNotEmpty()) savedFile.mimeType else "*/*"
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            setResult(RESULT_OK)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteFile(savedFile: SavedFile) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Saved File")
            .setMessage("Are you sure you want to remove \"${savedFile.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                OpenFileManager.deleteFile(this, savedFile.id)
                loadAndDisplayFiles(animate = false)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}
