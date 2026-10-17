package com.essential.shortcuts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DefaultItemAnimator

class RecentFragment : Fragment() {

    // Single launcher for all tool cards in this fragment
    private lateinit var toolLauncher: ActivityResultLauncher<android.content.Intent>
    // Pending callback from whichever card was tapped last
    private var pendingLaunchCallback: ((Int) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toolLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            pendingLaunchCallback?.invoke(result.resultCode)
            pendingLaunchCallback = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recent, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply system window insets so the top bar, settings icon, and title
        // are positioned below the status bar
        val topBarArea = view.findViewById<eightbitlab.com.blurview.BlurView>(R.id.top_bar_area)
        val headerSpacer = view.findViewById<View>(R.id.header_spacer)
        val rvRecentTools = view.findViewById<RecyclerView>(R.id.rv_recent_tools)
        val tvEmptyState = view.findViewById<TextView>(R.id.tv_empty_state)
        val btnSettings = view.findViewById<android.widget.ImageView>(R.id.btn_settings)

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
        val tvSmallTitle = view.findViewById<android.widget.TextView>(R.id.tv_small_title)
        val decorView = requireActivity().window.decorView
        topBarArea.setupWith(view as android.view.ViewGroup, blurAlgorithm)
            .setFrameClearDrawable(decorView.background)
            .setBlurRadius(15f)

        // Fade in the small title when the large title scrolls out of view
        appbar.addOnOffsetChangedListener(com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            if (totalScrollRange == 0) return@OnOffsetChangedListener
            
            // verticalOffset is 0 when fully expanded, and -totalScrollRange when fully collapsed
            val percentage = Math.abs(verticalOffset).toFloat() / totalScrollRange.toFloat()
            
            // Fade in during the last 30% of the scroll
            var alpha = (percentage - 0.7f) / 0.3f
            if (alpha < 0f) alpha = 0f
            if (alpha > 1f) alpha = 1f
            
            tvSmallTitle.alpha = alpha
        })

        rvRecentTools.layoutManager = GridLayoutManager(requireContext(), 2)
        rvRecentTools.edgeEffectFactory = BounceEdgeEffectFactory()
        rvRecentTools.onFlingListener = SmoothScrollFlingListener(rvRecentTools)
        rvRecentTools.itemAnimator = DefaultItemAnimator().apply {
            addDuration = 300
            removeDuration = 300
            moveDuration = 300
        }

        loadRecentTools(rvRecentTools, tvEmptyState)

        // Settings button
        btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_recentFragment_to_settingsFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            val rvRecentTools = it.findViewById<RecyclerView>(R.id.rv_recent_tools)
            val tvEmptyState = it.findViewById<TextView>(R.id.tv_empty_state)
            loadRecentTools(rvRecentTools, tvEmptyState)
        }
    }

    private fun loadRecentTools(rvRecentTools: RecyclerView, tvEmptyState: TextView) {
        val recentIds = RecentToolsManager.getRecentTools(requireContext())
        
        if (recentIds.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            rvRecentTools.visibility = View.GONE
            return
        }

        val allTools = ToolsRepository.getAllTools(requireContext())
        
        // Map saved IDs back to Tool objects, preserving order
        val recentToolsList = recentIds.mapNotNull { savedId -> 
            allTools.find { it.id == savedId } 
        }

        if (recentToolsList.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            rvRecentTools.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            rvRecentTools.visibility = View.VISIBLE
            
            // "recent" is a special category ID handle to prevent coloring issues
            // but we can pass "essential" or just let ToolsAdapter fallback.
            val adapter = ToolsAdapter(
                tools = recentToolsList,
                categoryId = "recent",
                isHorizontal = false,
                launchRequestHandler = { request ->
                    if (request.intent != null) {
                        pendingLaunchCallback = request.onResult
                        toolLauncher.launch(request.intent)
                    } else {
                        request.onResult(android.app.Activity.RESULT_CANCELED)
                    }
                }
            ) { tool ->
                // Save to recents on click
                RecentToolsManager.saveRecentTool(requireContext(), tool.id)
            }
            rvRecentTools.adapter = adapter
        }
    }
}
