package com.essential.shortcuts

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

class WidgetConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var selectedSize = 1
    private val selectedToolIds = mutableListOf<String>()
    private val toolViewMap = mutableMapOf<String, View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set result to CANCELED in case the user backs out
        setResult(RESULT_CANCELED)

        // Get the widget ID from the intent
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContentView(R.layout.activity_widget_config)

        setupSizeButtons()
        populateTools()
        setupConfirmButton()
    }

    private fun setupSizeButtons() {
        val btn1 = findViewById<TextView>(R.id.btn_size_1)
        val btn2 = findViewById<TextView>(R.id.btn_size_2)
        val btn4 = findViewById<TextView>(R.id.btn_size_4)

        // Highlight initially selected (1)
        updateSizeButtonStates(btn1, btn2, btn4)

        btn1.setOnClickListener {
            selectedSize = 1
            selectedToolIds.clear()
            updateSizeButtonStates(btn1, btn2, btn4)
            updateSelectionCount()
            clearToolSelections()
        }
        btn2.setOnClickListener {
            selectedSize = 2
            selectedToolIds.clear()
            updateSizeButtonStates(btn1, btn2, btn4)
            updateSelectionCount()
            clearToolSelections()
        }
        btn4.setOnClickListener {
            selectedSize = 4
            selectedToolIds.clear()
            updateSizeButtonStates(btn1, btn2, btn4)
            updateSelectionCount()
            clearToolSelections()
        }
    }

    private fun updateSizeButtonStates(btn1: TextView, btn2: TextView, btn4: TextView) {
        val selectedAlpha = 1.0f
        val unselectedAlpha = 0.4f

        btn1.alpha = if (selectedSize == 1) selectedAlpha else unselectedAlpha
        btn2.alpha = if (selectedSize == 2) selectedAlpha else unselectedAlpha
        btn4.alpha = if (selectedSize == 4) selectedAlpha else unselectedAlpha
    }

    private fun populateTools() {
        val container = findViewById<LinearLayout>(R.id.tools_container)
        val categories = ToolsRepository.getAllCategories(this)

        for (category in categories) {
            // Category header
            val header = TextView(this).apply {
                text = category.name
                setTextColor(Color.WHITE)
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 24, 0, 12)
            }
            container.addView(header)

            // Tools grid (2 columns)
            var rowLayout: LinearLayout? = null
            for ((index, tool) in category.tools.withIndex()) {
                if (index % 2 == 0) {
                    rowLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = 8
                        }
                    }
                    container.addView(rowLayout)
                }

                val toolView = createToolView(tool, category.id)
                val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (index % 2 == 0) marginEnd = 4 else marginStart = 4
                }
                toolView.layoutParams = params
                rowLayout?.addView(toolView)
                toolViewMap[tool.id] = toolView
            }
        }
    }

    private fun createToolView(tool: Tool, categoryId: String): FrameLayout {
        val container = FrameLayout(this).apply {
            val gradientRes = ToolsWidgetProvider.getWidgetGradient(categoryId)
            background = ContextCompat.getDrawable(this@WidgetConfigActivity, gradientRes)
            setPadding(24, 20, 24, 20)
            minimumHeight = 180
        }

        val icon = ImageView(this).apply {
            setImageResource(tool.icon)
            setColorFilter(Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(56, 56).apply {
                gravity = Gravity.TOP or Gravity.START
            }
        }

        val name = TextView(this).apply {
            text = tool.name
            setTextColor(Color.WHITE)
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            maxLines = 2
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.START
            }
        }

        // Selection checkmark overlay
        val checkmark = TextView(this).apply {
            text = "✓"
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
            }
            tag = "checkmark"
        }

        container.addView(icon)
        container.addView(name)
        container.addView(checkmark)

        container.setOnClickListener {
            toggleToolSelection(tool.id, container)
        }

        return container
    }

    private fun toggleToolSelection(toolId: String, view: View) {
        if (selectedToolIds.contains(toolId)) {
            // Deselect
            selectedToolIds.remove(toolId)
            view.alpha = 1.0f
            val checkmark = (view as FrameLayout).findViewWithTag<View>("checkmark")
            checkmark?.visibility = View.GONE
        } else {
            if (selectedToolIds.size >= selectedSize) {
                Toast.makeText(
                    this,
                    "You can only select $selectedSize tool${if (selectedSize > 1) "s" else ""}",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            // Select
            selectedToolIds.add(toolId)
            view.alpha = 0.8f
            val checkmark = (view as FrameLayout).findViewWithTag<View>("checkmark")
            checkmark?.visibility = View.VISIBLE
        }
        updateSelectionCount()
    }

    private fun clearToolSelections() {
        for ((_, view) in toolViewMap) {
            view.alpha = 1.0f
            val checkmark = (view as? FrameLayout)?.findViewWithTag<View>("checkmark")
            checkmark?.visibility = View.GONE
        }
    }

    private fun updateSelectionCount() {
        val tv = findViewById<TextView>(R.id.tv_selection_count)
        val remaining = selectedSize - selectedToolIds.size
        tv.text = if (remaining > 0) {
            "Select $remaining more tool${if (remaining > 1) "s" else ""}"
        } else {
            "All tools selected ✓"
        }
    }

    private fun setupConfirmButton() {
        findViewById<TextView>(R.id.btn_add_widget).setOnClickListener {
            if (selectedToolIds.size != selectedSize) {
                Toast.makeText(
                    this,
                    "Please select exactly $selectedSize tool${if (selectedSize > 1) "s" else ""}",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Save configuration
            val prefs = getSharedPreferences(ToolsWidgetProvider.PREFS_NAME, MODE_PRIVATE)
            prefs.edit().apply {
                putString(
                    ToolsWidgetProvider.PREF_PREFIX_TOOL_IDS + appWidgetId,
                    selectedToolIds.joinToString(",")
                )
                putInt(ToolsWidgetProvider.PREF_PREFIX_SIZE + appWidgetId, selectedSize)
                apply()
            }

            // Update the widget
            val appWidgetManager = AppWidgetManager.getInstance(this)
            ToolsWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)

            // Return success
            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}
