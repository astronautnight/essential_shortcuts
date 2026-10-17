package com.essential.shortcuts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams

class MainActivity : AppCompatActivity() {

    private lateinit var navController: androidx.navigation.NavController
    private lateinit var bottomNavBar: LiquidGlassBottomBar

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme before super.onCreate
        applySavedTheme()

        super.onCreate(savedInstanceState)

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as androidx.navigation.fragment.NavHostFragment
        navController = navHostFragment.navController

        initViews()
        setupNavigation()
    }

    private fun applySavedTheme() {
        val prefs = getSharedPreferences("theme_prefs", 0)
        val mode = prefs.getInt("theme_mode", 2) // default: System
        val nightMode = when (mode) {
            0 -> AppCompatDelegate.MODE_NIGHT_NO
            1 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    private fun initViews() {
        bottomNavBar = findViewById(R.id.bottom_nav_container)

        // Set initial tab selection
        bottomNavBar.setSelectedTabWithoutCallback(0)

        // Setup real-time frosted glass blur matching top bar
        val mainRoot = findViewById<ViewGroup>(R.id.main_root) ?: (bottomNavBar.parent as? ViewGroup) ?: (window.decorView as ViewGroup)
        bottomNavBar.setupBlur(mainRoot)

        // Handle window insets to avoid overlap with system navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavBar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<MarginLayoutParams> {
                // Add system navigation bar height to the existing bottom margin (24dp)
                // We convert 24dp to pixels first
                val baseMarginBottom = (24 * view.resources.displayMetrics.density).toInt()
                bottomMargin = baseMarginBottom + insets.bottom
            }
            windowInsets
        }
    }

    private fun setupNavigation() {
        bottomNavBar.onTabSelected = { index ->
            val navOptions = when (index) {
                1 -> { // Switching to All Tools
                    androidx.navigation.navOptions {
                        anim {
                            enter = R.anim.slide_in_right
                            exit = R.anim.slide_out_left
                            popEnter = R.anim.slide_in_left
                            popExit = R.anim.slide_out_right
                        }
                        // Pop up to the start destination to avoid building up a large back stack
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                else -> { // Switching to Recent
                    androidx.navigation.navOptions {
                        anim {
                            enter = R.anim.slide_in_left
                            exit = R.anim.slide_out_right
                            popEnter = R.anim.slide_in_right
                            popExit = R.anim.slide_out_left
                        }
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }

            when (index) {
                0 -> {
                    if (navController.currentDestination?.id != R.id.recentFragment) {
                        navController.navigate(R.id.recentFragment, null, navOptions)
                    }
                }
                1 -> {
                    if (navController.currentDestination?.id != R.id.allToolsFragment) {
                        navController.navigate(R.id.allToolsFragment, null, navOptions)
                    }
                }
            }
        }

        // Listen to destination changes to sync UI (e.g. back button press)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.recentFragment -> {
                    bottomNavBar.setSelectedTabWithoutCallback(0)
                    bottomNavBar.visibility = View.VISIBLE
                }
                R.id.allToolsFragment -> {
                    bottomNavBar.setSelectedTabWithoutCallback(1)
                    bottomNavBar.visibility = View.VISIBLE
                }
                else -> {
                    // Hide bottom bar on settings/theme fragments
                    bottomNavBar.visibility = View.GONE
                }
            }
        }
    }
}
