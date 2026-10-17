package com.essential.shortcuts

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews

class ToolsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (appWidgetId in appWidgetIds) {
            editor.remove(PREF_PREFIX_TOOL_IDS + appWidgetId)
            editor.remove(PREF_PREFIX_SIZE + appWidgetId)
        }
        editor.apply()
    }

    companion object {
        const val PREFS_NAME = "widget_prefs"
        const val PREF_PREFIX_TOOL_IDS = "widget_tool_ids_"
        const val PREF_PREFIX_SIZE = "widget_size_"
        const val ACTION_TOOL_CLICK = "com.essential.shortcuts.ACTION_TOOL_CLICK"
        const val EXTRA_TOOL_ID = "extra_tool_id"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val toolIdsStr = prefs.getString(PREF_PREFIX_TOOL_IDS + appWidgetId, null) ?: return
            val size = prefs.getInt(PREF_PREFIX_SIZE + appWidgetId, 1)
            val toolIds = toolIdsStr.split(",")

            val allTools = ToolsRepository.getAllTools(context)
            val selectedTools = toolIds.mapNotNull { id -> allTools.find { it.id == id } }
            if (selectedTools.isEmpty()) return

            val layoutId = when (size) {
                1 -> R.layout.widget_layout_single
                2 -> R.layout.widget_layout_double
                4 -> R.layout.widget_layout_quad
                else -> R.layout.widget_layout_single
            }

            val views = RemoteViews(context.packageName, layoutId)

            for (i in selectedTools.indices) {
                val tool = selectedTools[i]
                val slotIndex = i + 1 // 1-based

                val nameId = getResId(context, "widget_name_$slotIndex", "id")
                val iconId = getResId(context, "widget_icon_$slotIndex", "id")
                val containerId = getResId(context, "widget_tool_$slotIndex", "id")

                if (nameId == 0 || iconId == 0 || containerId == 0) continue

                views.setTextViewText(nameId, tool.name)
                views.setImageViewResource(iconId, tool.icon)
                views.setInt(iconId, "setColorFilter", Color.WHITE)

                // Set gradient background based on category
                val gradientRes = getWidgetGradient(tool.categoryId)
                views.setInt(containerId, "setBackgroundResource", gradientRes)

                // Set click intent to open the tool
                val clickIntent = Intent(context, ToolsWidgetProvider::class.java).apply {
                    action = ACTION_TOOL_CLICK
                    putExtra(EXTRA_TOOL_ID, tool.id)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    tool.id.hashCode(),
                    clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(containerId, pendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun getResId(context: Context, name: String, defType: String): Int {
            return context.resources.getIdentifier(name, defType, context.packageName)
        }

        fun getWidgetGradient(categoryId: String): Int {
            return when (categoryId) {
                "calculator" -> R.drawable.widget_gradient_calculator
                "text_tools" -> R.drawable.widget_gradient_text_tools
                "generator" -> R.drawable.widget_gradient_generator
                "image_tools" -> R.drawable.widget_gradient_image
                "converter" -> R.drawable.widget_gradient_converter
                "color_tools" -> R.drawable.widget_gradient_color_tools
                "essential" -> R.drawable.widget_gradient_essential
                "playground" -> R.drawable.widget_gradient_playground
                else -> R.drawable.widget_gradient_essential
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_TOOL_CLICK) {
            val toolId = intent.getStringExtra(EXTRA_TOOL_ID) ?: return
            val allTools = ToolsRepository.getAllTools(context)
            val tool = allTools.find { it.id == toolId } ?: return

            // Save to recents
            RecentToolsManager.saveRecentTool(context, tool.id)

            // Route through WidgetLaunchActivity so the tick animation plays
            val bridgeIntent = Intent(context, WidgetLaunchActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
                putExtra(WidgetLaunchActivity.EXTRA_TOOL_ID,      tool.id)
                putExtra(WidgetLaunchActivity.EXTRA_TOOL_NAME,    tool.name)
                putExtra(WidgetLaunchActivity.EXTRA_TOOL_ICON,    tool.icon)
                putExtra(WidgetLaunchActivity.EXTRA_GRADIENT_RES, getWidgetGradient(tool.categoryId))
            }
            context.startActivity(bridgeIntent)
        }
    }

    private fun getToolActivityIntent(context: Context, tool: Tool): Intent? {
        return when (tool.id) {
            "calc_percentage" -> Intent(context, PercentageCalculatorActivity::class.java)
            "calc_average" -> Intent(context, AverageCalculatorActivity::class.java)
            "calc_days_until" -> Intent(context, DaysUntilActivity::class.java)
            "calc_age" -> Intent(context, AgeCalculatorActivity::class.java)
            "calc_fraction" -> Intent(context, FractionSimplifierActivity::class.java)
            "calc_factorial" -> Intent(context, FactorialCalculatorActivity::class.java)
            "calc_basic" -> Intent(context, BasicCalculatorActivity::class.java)
            "calc_scientific" -> Intent(context, ScientificCalculatorActivity::class.java)
            "gen_qr" -> Intent(context, QrCodeGeneratorActivity::class.java)
            "gen_barcode" -> Intent(context, BarcodeGeneratorActivity::class.java)
            "gen_password" -> Intent(context, PasswordGeneratorActivity::class.java)
            "text_case" -> Intent(context, CaseConverterActivity::class.java)
            "text_repeat" -> Intent(context, RepeatTextActivity::class.java)
            "gen_number" -> Intent(context, NumberGeneratorActivity::class.java)
            "gen_scanner" -> Intent(context, ScannerActivity::class.java)
            "img_resize" -> Intent(context, ImageResizerActivity::class.java)
            "img_rounded" -> Intent(context, RoundedImageActivity::class.java)
            "img_compress" -> Intent(context, ImageCompressorActivity::class.java)
            "play_lucky_wheel" -> Intent(context, LuckyWheelActivity::class.java)
            "play_multi_number" -> Intent(context, MultiNumberGeneratorActivity::class.java)
            "play_flip_coin" -> Intent(context, FlipCoinActivity::class.java)
            "play_rps" -> Intent(context, RockPaperScissorActivity::class.java)
            "play_spin_bottle" -> Intent(context, SpinBottleActivity::class.java)
            "play_roll_dice" -> Intent(context, RollDiceActivity::class.java)
            "conv_jpg_png" -> Intent(context, JpgPngConverterActivity::class.java)
            "conv_img_pdf" -> Intent(context, ImageToPdfActivity::class.java)
            "conv_img_webp" -> Intent(context, ImageToWebpActivity::class.java)
            "conv_word_pdf",
            "conv_pdf_word" -> Intent(context, WordPdfConverterActivity::class.java)
            "conv_txt_pdf" -> Intent(context, TextToPdfActivity::class.java)
            "ess_open_url" -> Intent(context, OpenUrlActivity::class.java)
            "ess_open_file" -> Intent(context, OpenFileActivity::class.java)
            "ess_url" -> Intent(context, UrlShortenerActivity::class.java)
            "ess_whatsapp" -> Intent(context, WhatsappUnsavedActivity::class.java)
            "ess_gmail" -> Intent(context, GmailRouterActivity::class.java)
            "ess_compass" -> Intent(context, CompassActivity::class.java)

            "color_picker" -> Intent(context, ColorPickerActivity::class.java)
            "color_from_image" -> Intent(context, ImageColorPickerActivity::class.java)
            "color_blend" -> Intent(context, BlendColorsActivity::class.java)
            "ess_support" -> Intent(context, SupportActivity::class.java)
            "calc_2d" -> Intent(context, Shapes2DActivity::class.java)
            "calc_3d" -> Intent(context, Shapes3DActivity::class.java)
            else -> {
                // For tools without a dedicated activity, open the main app
                Intent(context, MainActivity::class.java)
            }
        }
    }
}
