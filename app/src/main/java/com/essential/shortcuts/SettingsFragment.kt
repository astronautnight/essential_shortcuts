package com.essential.shortcuts

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment() {

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { exportDataToUri(it) }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importDataFromUri(it) }
    }

    private fun exportDataToUri(uri: Uri) {
        try {
            val json = JSONObject()
            
            // Passwords
            val passwords = PasswordManager.getSavedPasswords(requireContext())
            val passwordsArray = JSONArray()
            passwords.forEach { 
                val obj = JSONObject()
                obj.put("username", it.username)
                obj.put("password", it.password)
                obj.put("timestamp", it.timestamp)
                passwordsArray.put(obj)
            }
            json.put("passwords", passwordsArray)
            
            // Templates
            val templates = GmailTemplateManager.getAll(requireContext())
            val templatesArray = JSONArray()
            templates.forEach { templatesArray.put(it.toJson()) }
            json.put("gmail_templates", templatesArray)
            
            requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json.toString())
                }
            }
            Toast.makeText(requireContext(), "Data exported successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to export data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importDataFromUri(uri: Uri) {
        try {
            var jsonString = ""
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    jsonString = reader.readText()
                }
            }
            
            val json = JSONObject(jsonString)
            
            // Import Passwords
            if (json.has("passwords")) {
                val passwordsArray = json.getJSONArray("passwords")
                val currentPasswords = PasswordManager.getSavedPasswords(requireContext()).toMutableList()
                var addedPasswords = 0
                for (i in 0 until passwordsArray.length()) {
                    val obj = passwordsArray.getJSONObject(i)
                    val username = obj.optString("username", "")
                    val pwd = obj.optString("password", "")
                    val time = obj.optLong("timestamp", System.currentTimeMillis())
                    
                    if (pwd.isNotEmpty() && currentPasswords.none { it.password == pwd && it.username == username }) {
                        currentPasswords.add(SavedPassword(username, pwd, time))
                        addedPasswords++
                    }
                }
                if (addedPasswords > 0) {
                    PasswordManager.persistPasswords(requireContext(), currentPasswords)
                }
            }
            
            // Import Templates
            if (json.has("gmail_templates")) {
                val templatesArray = json.getJSONArray("gmail_templates")
                val currentTemplates = GmailTemplateManager.getAll(requireContext())
                var addedTemplates = 0
                for (i in 0 until templatesArray.length()) {
                    val obj = templatesArray.getJSONObject(i)
                    val template = GmailTemplate.fromJson(obj)
                    if (currentTemplates.none { it.id == template.id }) {
                        currentTemplates.add(template)
                        addedTemplates++
                    }
                }
                if (addedTemplates > 0) {
                    GmailTemplateManager.save(requireContext(), currentTemplates)
                }
            }
            
            Toast.makeText(requireContext(), "Data imported successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to import data", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        view.findViewById<View>(R.id.btn_back_container).setOnClickListener {
            findNavController().navigateUp()
        }

        // Apply system window insets so the top bar and title are positioned below the status bar
        val topBarArea = view.findViewById<eightbitlab.com.blurview.BlurView>(R.id.top_bar_area)
        val headerSpacer = view.findViewById<android.widget.Space>(R.id.header_spacer)
        
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
            val statusBarHeight = windowInsets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.statusBars()
            ).top

            val barHeight = statusBarHeight + (64 * resources.displayMetrics.density).toInt()

            // Extend top bar area to cover status bar area
            val topBarParams = topBarArea.layoutParams as androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
            topBarParams.height = barHeight
            topBarArea.layoutParams = topBarParams

            // Make the scrollable spacer exactly the same height so title starts below it
            val spacerParams = headerSpacer.layoutParams as android.widget.LinearLayout.LayoutParams
            spacerParams.height = barHeight
            headerSpacer.layoutParams = spacerParams

            windowInsets
        }

        // Setup BlurView
        val blurAlgorithm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            eightbitlab.com.blurview.RenderEffectBlur()
        } else {
            eightbitlab.com.blurview.RenderScriptBlur(requireContext())
        }
        val appbar = view.findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appbar)
        val tvSmallTitle = view.findViewById<TextView>(R.id.tv_small_title)
        val decorView = requireActivity().window.decorView
        topBarArea.setupWith(view as ViewGroup, blurAlgorithm)
            .setFrameClearDrawable(decorView.background)
            .setBlurRadius(15f)

        // Fade in the small title when the large title scrolls out of view
        appbar.addOnOffsetChangedListener(com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            if (totalScrollRange == 0) return@OnOffsetChangedListener
            
            val percentage = Math.abs(verticalOffset).toFloat() / totalScrollRange.toFloat()
            
            var alpha = (percentage - 0.7f) / 0.3f
            if (alpha < 0f) alpha = 0f
            if (alpha > 1f) alpha = 1f
            
            tvSmallTitle.alpha = alpha
        })

        // Setup RecyclerView for bouncy animations
        val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.scroll_view)
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        recyclerView.adapter = SettingsAdapter()
        recyclerView.edgeEffectFactory = BounceEdgeEffectFactory()
        recyclerView.onFlingListener = SmoothScrollFlingListener(recyclerView)
    }

    inner class SettingsAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_settings_content, parent, false)
            setupClickListeners(view)
            return object : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {}
        }
        override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {}
        override fun getItemCount() = 1
    }

    private fun setupClickListeners(contentView: View) {
        // Theme Mode
        contentView.findViewById<View>(R.id.row_theme_mode)?.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_themeModeFragment)
        }

        // Rate Us
        contentView.findViewById<View>(R.id.row_rate_us)?.setOnClickListener {
            val playStoreUrl = "https://play.google.com/store/apps/details?id=com.essential.shortcuts"
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.essential.shortcuts"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl)))
            }
        }
        
        // Premium Banner
        contentView.findViewById<View>(R.id.row_premium)?.setOnClickListener {
            SupportBottomSheet().show(childFragmentManager, SupportBottomSheet.TAG)
        }

        // Share App
        contentView.findViewById<View>(R.id.row_share)?.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this app: https://play.google.com/store/apps/details?id=com.essential.shortcuts")
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }

        // Contact
        contentView.findViewById<View>(R.id.row_contact)?.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO)
            emailIntent.data = Uri.parse("mailto:rajassberde@gmail.com")
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "App Feedback")
            try {
                startActivity(emailIntent)
            } catch (e: Exception) {
                // No email app found
            }
        }

        // Patreon
        contentView.findViewById<View>(R.id.btn_patreon)?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://patreon.com")))
        }

        // Ko-fi
        contentView.findViewById<View>(R.id.btn_kofi)?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/rbstudioz")))
        }

        // UPI
        contentView.findViewById<View>(R.id.btn_upi)?.setOnClickListener {
            val upiId = "rajassberde@oksbi"
            val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("UPI ID", upiId))
            android.widget.Toast.makeText(requireContext(), "UPI ID copied! 📋", android.widget.Toast.LENGTH_SHORT).show()

            try {
                val uri = Uri.parse("upi://pay?pa=$upiId&pn=Essential%20Shortcuts&cu=INR")
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (e: Exception) {
                // Ignore if no UPI app
            }
        }

        // Reddit
        contentView.findViewById<View>(R.id.row_reddit)?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.reddit.com/r/Essential_Shortcuts/")))
        }

        // Export Data
        contentView.findViewById<View>(R.id.row_export_data)?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Export Data")
                .setMessage("This data will be exported:\n• Passwords\n• Email Templates")
                .setPositiveButton("Export") { _, _ ->
                    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    val fileName = "Essential_Shortcuts_Backup_${sdf.format(Date())}.json"
                    exportLauncher.launch(fileName)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Import Data
        contentView.findViewById<View>(R.id.row_import_data)?.setOnClickListener {
            importLauncher.launch(arrayOf("application/json", "*/*"))
        }
    }
}
