package com.essential.shortcuts

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

/**
 * WidgetLaunchActivity — a fully transparent, zero-chrome bridge activity.
 *
 * When a widget shortcut is tapped it shows ONLY the ProgressTickView
 * (tick + circular arc) floating on a transparent screen — giving the
 * impression the animation is playing directly on the widget card.
 *
 * Flow:
 *   Widget tap → this activity (transparent, no dim)
 *     → animateStart()  — tick fades in, arc to 1/3
 *     → startActivityForResult(toolIntent)
 *         ← RESULT_OK       → arc completes 360°, fades out, finish()
 *         ← RESULT_CANCELED → arc reverts, fades out, finish()
 */
class WidgetLaunchActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TOOL_ID      = "widget_tool_id"
        const val EXTRA_TOOL_NAME    = "widget_tool_name"
        const val EXTRA_TOOL_ICON    = "widget_tool_icon"
        const val EXTRA_GRADIENT_RES = "widget_gradient_res"
        private const val REQUEST_TOOL = 1001
    }

    private lateinit var progressTick: ProgressTickView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val toolId = intent.getStringExtra(EXTRA_TOOL_ID) ?: run { finish(); return }

        val allTools = ToolsRepository.getAllTools(this)
        val tool = allTools.find { it.id == toolId } ?: run { finish(); return }
        val toolIntent = buildToolIntent(tool)

        val dp = resources.displayMetrics.density

        // Full-screen transparent root — homescreen shows straight through
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        // Only the ProgressTickView — 56×56dp, top-right corner with margin
        progressTick = ProgressTickView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                (56 * dp).toInt(),
                (56 * dp).toInt()
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                val margin = (16 * dp).toInt()
                setMargins(margin, margin, margin, margin)
            }
        }

        root.addView(progressTick)
        setContentView(root)

        // Tapping anywhere outside the tick dismisses with revert
        root.setOnClickListener { progressTick.animateRevert { finish() } }

        // Start: tick fades in + arc sweeps to 1/3, then launch tool
        progressTick.animateStart {
            if (toolIntent != null) {
                @Suppress("DEPRECATION")
                startActivityForResult(toolIntent, REQUEST_TOOL)
            } else {
                progressTick.animateRevert { finish() }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TOOL) {
            if (resultCode == RESULT_OK) {
                progressTick.animateComplete { finish() }
            } else {
                progressTick.animateRevert { finish() }
            }
        }
    }

    // No transition animation — appears/disappears instantly
    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    private fun buildToolIntent(tool: Tool): Intent? = when (tool.id) {
        "calc_percentage"   -> Intent(this, PercentageCalculatorActivity::class.java)
        "calc_average"      -> Intent(this, AverageCalculatorActivity::class.java)
        "calc_days_until"   -> Intent(this, DaysUntilActivity::class.java)
        "calc_age"          -> Intent(this, AgeCalculatorActivity::class.java)
        "calc_fraction"     -> Intent(this, FractionSimplifierActivity::class.java)
        "calc_factorial"    -> Intent(this, FactorialCalculatorActivity::class.java)
        "calc_basic"        -> Intent(this, BasicCalculatorActivity::class.java)
        "calc_scientific"   -> Intent(this, ScientificCalculatorActivity::class.java)
        "gen_qr"            -> Intent(this, QrCodeGeneratorActivity::class.java)
        "gen_barcode"       -> Intent(this, BarcodeGeneratorActivity::class.java)
        "gen_password"      -> Intent(this, PasswordGeneratorActivity::class.java)
        "text_case"         -> Intent(this, CaseConverterActivity::class.java)
        "text_morse"        -> Intent(this, MorseCodeActivity::class.java)
        "text_repeat"       -> Intent(this, RepeatTextActivity::class.java)
        "gen_number"        -> Intent(this, NumberGeneratorActivity::class.java)
        "gen_scanner"       -> Intent(this, ScannerActivity::class.java)
        "img_resize"        -> Intent(this, ImageResizerActivity::class.java)
        "img_rounded"       -> Intent(this, RoundedImageActivity::class.java)
        "img_compress"      -> Intent(this, ImageCompressorActivity::class.java)
        "img_pixel_frame"   -> Intent(this, PixelFramesActivity::class.java)
        "img_laptop_frame"  -> Intent(this, LaptopFramesActivity::class.java)
        "play_lucky_wheel"  -> Intent(this, LuckyWheelActivity::class.java)
        "play_multi_number" -> Intent(this, MultiNumberGeneratorActivity::class.java)
        "play_flip_coin"    -> Intent(this, FlipCoinActivity::class.java)
        "play_rps"          -> Intent(this, RockPaperScissorActivity::class.java)
        "play_spin_bottle"  -> Intent(this, SpinBottleActivity::class.java)
        "play_roll_dice"    -> Intent(this, RollDiceActivity::class.java)
        "conv_image"        -> Intent(this, ImageConverterActivity::class.java)
        "conv_word_pdf",
        "conv_pdf_word"     -> Intent(this, WordPdfConverterActivity::class.java)
        "conv_txt_pdf"      -> Intent(this, TextToPdfActivity::class.java)
        "conv_pdf_combine"  -> Intent(this, PdfCombineActivity::class.java)
        "ess_open_url"      -> Intent(this, OpenUrlActivity::class.java)
        "ess_open_file"     -> Intent(this, OpenFileActivity::class.java)
        "ess_url"           -> Intent(this, UrlShortenerActivity::class.java)
        "ess_whatsapp"      -> Intent(this, WhatsappUnsavedActivity::class.java)
        "ess_gmail"         -> Intent(this, GmailRouterActivity::class.java)
        "ess_compass"       -> Intent(this, CompassActivity::class.java)
        "ess_support"       -> Intent(this, SupportActivity::class.java)
        "color_picker"      -> Intent(this, ColorPickerActivity::class.java)
        "color_from_image"  -> Intent(this, ImageColorPickerActivity::class.java)
        "color_blend"       -> Intent(this, BlendColorsActivity::class.java)
        "calc_2d"           -> Intent(this, Shapes2DActivity::class.java)
        "calc_3d"           -> Intent(this, Shapes3DActivity::class.java)
        else                -> Intent(this, MainActivity::class.java)
    }
}
