package com.essential.shortcuts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class ThemeModeFragment : Fragment() {

    companion object {
        private const val PREFS_NAME = "theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        const val MODE_LIGHT = 0
        const val MODE_DARK = 1
        const val MODE_SYSTEM = 2
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_theme_mode, container, false)
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
        recyclerView.adapter = ThemeModeAdapter()
        recyclerView.edgeEffectFactory = BounceEdgeEffectFactory()
        recyclerView.onFlingListener = SmoothScrollFlingListener(recyclerView)
    }

    inner class ThemeModeAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_theme_mode_content, parent, false)
            setupClickListeners(view)
            return object : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {}
        }
        override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {}
        override fun getItemCount() = 1
    }

    private fun setupClickListeners(contentView: View) {
        val checkLight = contentView.findViewById<ImageView>(R.id.check_light)
        val checkDark = contentView.findViewById<ImageView>(R.id.check_dark)
        val checkSystem = contentView.findViewById<ImageView>(R.id.check_system)

        // Load current preference
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)
        val currentMode = prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM)
        updateChecks(currentMode, checkLight, checkDark, checkSystem)

        // Light
        contentView.findViewById<View>(R.id.row_light)?.setOnClickListener {
            setTheme(MODE_LIGHT, checkLight, checkDark, checkSystem)
        }

        // Dark
        contentView.findViewById<View>(R.id.row_dark)?.setOnClickListener {
            setTheme(MODE_DARK, checkLight, checkDark, checkSystem)
        }

        // System
        contentView.findViewById<View>(R.id.row_system)?.setOnClickListener {
            setTheme(MODE_SYSTEM, checkLight, checkDark, checkSystem)
        }
    }

    private fun setTheme(mode: Int, checkLight: ImageView, checkDark: ImageView, checkSystem: ImageView) {
        // Save preference
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()

        // Update check visuals
        updateChecks(mode, checkLight, checkDark, checkSystem)

        // Apply theme
        val nightMode = when (mode) {
            MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            MODE_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    private fun updateChecks(mode: Int, checkLight: ImageView, checkDark: ImageView, checkSystem: ImageView) {
        checkLight.visibility = if (mode == MODE_LIGHT) View.VISIBLE else View.GONE
        checkDark.visibility = if (mode == MODE_DARK) View.VISIBLE else View.GONE
        checkSystem.visibility = if (mode == MODE_SYSTEM) View.VISIBLE else View.GONE
    }
}
