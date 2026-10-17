package com.essential.shortcuts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DefaultItemAnimator

class AllToolsFragment : Fragment() {

    private lateinit var categoriesAdapter: CategoriesAdapter
    private var allCategories: List<Category> = emptyList()

    // Single launcher for all tool cards in this fragment
    private lateinit var toolLauncher: ActivityResultLauncher<android.content.Intent>
    // Pending callback from whichever card was tapped last
    private var pendingLaunchCallback: ((Int) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must be registered before STARTED — do it here, not in onViewCreated
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
        return inflater.inflate(R.layout.fragment_all_tools, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply system window insets so the top bar, settings icon, and title
        // are positioned below the status bar
        val topBarArea = view.findViewById<eightbitlab.com.blurview.BlurView>(R.id.top_bar_area)
        val headerSpacer = view.findViewById<View>(R.id.header_spacer)
        val rvCategories = view.findViewById<RecyclerView>(R.id.rv_categories)
        val etSearch = view.findViewById<android.widget.EditText>(R.id.et_category_search)
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

        // Setup Search Listener
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterCategories(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // 1. Setup RecyclerView
        rvCategories.layoutManager = LinearLayoutManager(requireContext())
        rvCategories.edgeEffectFactory = BounceEdgeEffectFactory()
        rvCategories.onFlingListener = SmoothScrollFlingListener(rvCategories)
        rvCategories.itemAnimator = DefaultItemAnimator().apply {
            addDuration = 300
            removeDuration = 300
            moveDuration = 300
        }
        categoriesAdapter = CategoriesAdapter(
            categories = emptyList(),
            launchRequestHandler = { request ->
                if (request.intent != null) {
                    pendingLaunchCallback = request.onResult
                    toolLauncher.launch(request.intent)
                } else {
                    // No intent = special-case (BottomSheet etc.): treat as cancelled
                    request.onResult(android.app.Activity.RESULT_CANCELED)
                }
            }
        )
        rvCategories.adapter = categoriesAdapter

        loadCategories()

        // Settings button
        btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_allToolsFragment_to_settingsFragment)
        }
    }

    private fun loadCategories() {
        allCategories = ToolsRepository.getAllCategories(requireContext())
        categoriesAdapter.updateCategories(allCategories)
    }

    private fun filterCategories(query: String) {
        if (query.isEmpty()) {
            categoriesAdapter.updateCategories(allCategories, query)
            return
        }
        
        val lowerCaseQuery = query.lowercase()
        val filteredList = mutableListOf<Category>()
        
        for (category in allCategories) {
            // Check if any tool matches the query
            val matchedTools = category.tools.filter { 
                it.name.lowercase().contains(lowerCaseQuery) 
            }
            
            // If the category matches or it has matching tools, add it
            if (category.name.lowercase().contains(lowerCaseQuery) || matchedTools.isNotEmpty()) {
                filteredList.add(category.copy(tools = matchedTools.ifEmpty { category.tools }))
            }
        }
        
        categoriesAdapter.updateCategories(filteredList, query)
    }
}
