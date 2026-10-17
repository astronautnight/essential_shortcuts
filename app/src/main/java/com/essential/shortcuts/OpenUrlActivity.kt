package com.essential.shortcuts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class OpenUrlActivity : AppCompatActivity() {

    private lateinit var layoutUrlsList: LinearLayout
    private lateinit var layoutEmptyPlaceholder: LinearLayout
    private lateinit var btnAddUrlRow: LinearLayout
    private lateinit var btnHeaderAdd: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_url)

        initViews()
        setupListeners()
        loadAndDisplayUrls(animate = true)
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
        layoutUrlsList = findViewById(R.id.layout_urls_list)
        layoutEmptyPlaceholder = findViewById(R.id.layout_empty_placeholder)
        btnAddUrlRow = findViewById(R.id.btn_add_url_row)
        btnHeaderAdd = findViewById(R.id.btn_header_add)
    }

    private fun setupListeners() {
        btnAddUrlRow.setOnClickListener { showAddUrlDialog() }
        btnHeaderAdd.setOnClickListener { showAddUrlDialog() }
    }

    private fun loadAndDisplayUrls(animate: Boolean) {
        val savedUrls = OpenUrlManager.getSavedUrls(this)
        layoutUrlsList.removeAllViews()

        if (savedUrls.isEmpty()) {
            layoutEmptyPlaceholder.visibility = View.VISIBLE
        } else {
            layoutEmptyPlaceholder.visibility = View.GONE

            val inflater = LayoutInflater.from(this)
            for (savedUrl in savedUrls) {
                val itemView = inflater.inflate(R.layout.item_saved_url, layoutUrlsList, false)
                val tvTitle = itemView.findViewById<TextView>(R.id.tv_option_title)
                val tvSubtitle = itemView.findViewById<TextView>(R.id.tv_option_subtitle)
                val btnShare = itemView.findViewById<ImageView>(R.id.btn_share_url)
                val btnDelete = itemView.findViewById<ImageView>(R.id.btn_delete_option)
                val optionRow = itemView.findViewById<View>(R.id.option_row)

                tvTitle.text = savedUrl.title
                if (savedUrl.title != savedUrl.url) {
                    tvSubtitle.text = savedUrl.url
                    tvSubtitle.visibility = View.VISIBLE
                } else {
                    tvSubtitle.visibility = View.GONE
                }

                optionRow.setOnClickListener {
                    openUrl(savedUrl.url)
                }

                optionRow.setOnLongClickListener {
                    confirmDeleteUrl(savedUrl)
                    true
                }

                btnShare.setOnClickListener {
                    shareUrl(savedUrl.url)
                }

                btnDelete.setOnClickListener {
                    confirmDeleteUrl(savedUrl)
                }

                layoutUrlsList.addView(itemView)
            }
        }

        if (animate) {
            animateEntrance()
        }
    }

    private fun animateEntrance() {
        var delayIndex = 0
        for (i in 0 until layoutUrlsList.childCount) {
            val child = layoutUrlsList.getChildAt(i)
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

        // Animate the add row as well
        btnAddUrlRow.alpha = 0f
        btnAddUrlRow.translationY = 30f
        btnAddUrlRow.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(250)
            .setStartDelay(delayIndex * 35L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun openUrl(url: String) {
        val formattedUrl = OpenUrlManager.formatUrl(url)
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            setResult(RESULT_OK)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open URL: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareUrl(url: String) {
        val formattedUrl = OpenUrlManager.formatUrl(url)
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, formattedUrl)
            }
            startActivity(Intent.createChooser(sendIntent, "Share URL"))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not share URL: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteUrl(savedUrl: SavedUrl) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Saved URL")
            .setMessage("Are you sure you want to remove \"${savedUrl.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                OpenUrlManager.deleteUrl(this, savedUrl.id)
                loadAndDisplayUrls(animate = false)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddUrlDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_url, null)
        val etUrl = dialogView.findViewById<EditText>(R.id.et_url_input)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_title_input)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_save)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val urlText = etUrl.text.toString().trim()
            val titleText = etTitle.text.toString().trim()

            if (urlText.isEmpty()) {
                etUrl.error = "Please enter a URL"
                return@setOnClickListener
            }

            OpenUrlManager.saveUrl(this, titleText, urlText)
            dialog.dismiss()
            loadAndDisplayUrls(animate = true)
        }

        dialog.show()
        etUrl.requestFocus()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}
