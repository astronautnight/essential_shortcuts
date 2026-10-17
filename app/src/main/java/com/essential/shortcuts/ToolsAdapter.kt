package com.essential.shortcuts

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

/**
 * ToolsAdapter — renders tool cards with a progress tick animation.
 *
 * The animation flow:
 *  1. User taps → scale bounce → tick + 1/3-circle arc animates in
 *  2. The hosting fragment launches the tool Activity via [LaunchRequest]
 *  3. When the Activity returns:
 *     - RESULT_OK  → arc completes full circle clockwise, fades out
 *     - else       → arc retracts counter-clockwise, fades out
 *
 * Callers must supply a [launchRequestHandler] that actually launches the
 * Intent and reports results. See [AllToolsFragment] / [RecentFragment] for usage.
 */
class ToolsAdapter(
    private var tools: List<Tool>,
    private val categoryId: String,
    private val isHorizontal: Boolean = false,
    private val launchRequestHandler: ((LaunchRequest) -> Unit)? = null,
    private val onToolClick: (Tool) -> Unit
) : RecyclerView.Adapter<ToolsAdapter.ToolViewHolder>() {

    /**
     * Passed to [launchRequestHandler] when the user taps a card.
     * The handler launches [intent] and calls [onResult] with the Activity result code.
     */
    data class LaunchRequest(
        val intent: Intent?,
        val onResult: (resultCode: Int) -> Unit
    )

    inner class ToolViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.card_tool)
        val tvToolName: TextView = itemView.findViewById(R.id.tv_tool_name)
        val ivToolIcon: ImageView = itemView.findViewById(R.id.iv_tool_icon)
        val progressTick: ProgressTickView = itemView.findViewById(R.id.progress_tick)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        val layoutId = if (isHorizontal) R.layout.item_tool_card_horizontal else R.layout.item_tool_card
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ToolViewHolder(view)
    }

    override fun onViewRecycled(holder: ToolViewHolder) {
        super.onViewRecycled(holder)
        holder.progressTick.reset()
    }

    override fun onBindViewHolder(holder: ToolViewHolder, position: Int) {
        val tool = tools[position]
        holder.tvToolName.text = tool.name
        holder.ivToolIcon.setImageResource(tool.icon)
        holder.ivToolIcon.setColorFilter(
            ContextCompat.getColor(holder.itemView.context, android.R.color.white),
            android.graphics.PorterDuff.Mode.SRC_IN
        )

        // Reset tick on every bind (handles recycled views)
        holder.progressTick.reset()

        // Determine true category for styling
        val effectiveCategoryId = if (categoryId == "recent") {
            ToolsRepository.getCategoryForTool(holder.itemView.context, tool.id)
        } else {
            categoryId
        }

        val gradientRes = when (effectiveCategoryId) {
            "calculator"  -> R.drawable.gradient_calculator
            "text_tools"  -> R.drawable.gradient_text_tools
            "generator"   -> R.drawable.gradient_generator
            "image_tools" -> R.drawable.gradient_image
            "converter"   -> R.drawable.gradient_converter
            "color_tools" -> R.drawable.gradient_color_tools
            "essential"   -> if (tool.id == "ess_support") R.drawable.gradient_support else R.drawable.gradient_essential
            "playground"  -> R.drawable.gradient_playground
            else          -> R.drawable.gradient_essential
        }

        holder.card.setCardBackgroundColor(
            ContextCompat.getColor(holder.itemView.context, android.R.color.transparent)
        )
        holder.card.background = ContextCompat.getDrawable(holder.itemView.context, gradientRes)
        try { holder.card.setCardForegroundColor(null) } catch (_: Exception) {}
        holder.card.strokeWidth = 0

        // ── Click handler ────────────────────────────────────────────────────
        holder.card.setOnClickListener {
            val scaleAnim = android.view.animation.ScaleAnimation(
                1f, 0.96f, 1f, 0.96f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 80
                repeatCount = 1
                repeatMode = android.view.animation.Animation.REVERSE
                setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(a: android.view.animation.Animation?) {}
                    override fun onAnimationRepeat(a: android.view.animation.Animation?) {}
                    override fun onAnimationEnd(a: android.view.animation.Animation?) {
                        startTickThenLaunch(tool, holder)
                    }
                })
            }
            holder.card.startAnimation(scaleAnim)
        }
    }

    private fun startTickThenLaunch(tool: Tool, holder: ToolViewHolder) {
        val tick = holder.progressTick
        val ctx  = holder.itemView.context

        tick.animateStart {
            // Tick + 1/3 arc done → now open the tool
            val intent = buildIntent(tool, ctx)

            if (launchRequestHandler != null && intent != null) {
                launchRequestHandler.invoke(LaunchRequest(intent) { resultCode ->
                    if (resultCode == Activity.RESULT_OK) {
                        tick.animateComplete()
                    } else {
                        tick.animateRevert()
                    }
                })
            } else if (intent != null) {
                // No launcher registered (e.g. widget usage) — open and revert
                tick.animateRevert()
                ctx.startActivity(intent)
            } else {
                // Special-case (BottomSheet / Toast) — handle inline then revert
                handleSpecialCase(tool, ctx)
                tick.animateRevert()
            }

            // Also fire the original click callback (for recents tracking etc.)
            onToolClick(tool)
        }
    }

    override fun getItemCount() = tools.size

    fun updateTools(newTools: List<Tool>) {
        tools = newTools
        notifyDataSetChanged()
    }

    // ── Intent builder ───────────────────────────────────────────────────────

    private fun buildIntent(tool: Tool, ctx: android.content.Context): Intent? = when (tool.id) {
        "calc_percentage"  -> Intent(ctx, PercentageCalculatorActivity::class.java)
        "calc_average"     -> Intent(ctx, AverageCalculatorActivity::class.java)
        "calc_days_until"  -> Intent(ctx, DaysUntilActivity::class.java)
        "calc_age"         -> Intent(ctx, AgeCalculatorActivity::class.java)
        "calc_fraction"    -> Intent(ctx, FractionSimplifierActivity::class.java)
        "calc_factorial"   -> Intent(ctx, FactorialCalculatorActivity::class.java)
        "calc_basic"       -> Intent(ctx, BasicCalculatorActivity::class.java)
        "calc_scientific"  -> Intent(ctx, ScientificCalculatorActivity::class.java)
        "gen_qr"           -> Intent(ctx, QrCodeGeneratorActivity::class.java)
        "gen_barcode"      -> Intent(ctx, BarcodeGeneratorActivity::class.java)
        "gen_password"     -> Intent(ctx, PasswordGeneratorActivity::class.java)
        "text_case"        -> Intent(ctx, CaseConverterActivity::class.java)
        "text_morse"       -> Intent(ctx, MorseCodeActivity::class.java)
        "text_repeat"      -> Intent(ctx, RepeatTextActivity::class.java)
        "gen_number"       -> Intent(ctx, NumberGeneratorActivity::class.java)
        "img_resize"       -> Intent(ctx, ImageResizerActivity::class.java)
        "img_rounded"      -> Intent(ctx, RoundedImageActivity::class.java)
        "img_compress"     -> Intent(ctx, ImageCompressorActivity::class.java)
        "img_pixel_frame"  -> Intent(ctx, PixelFramesActivity::class.java)
        "img_laptop_frame" -> Intent(ctx, LaptopFramesActivity::class.java)
        "img_meme"         -> Intent(ctx, MemeGeneratorActivity::class.java)
        "conv_image"       -> Intent(ctx, ImageConverterActivity::class.java)
        "conv_word_pdf",
        "conv_pdf_word"    -> Intent(ctx, WordPdfConverterActivity::class.java)
        "conv_txt_pdf"     -> Intent(ctx, TextToPdfActivity::class.java)
        "conv_pdf_combine" -> Intent(ctx, PdfCombineActivity::class.java)
        "play_lucky_wheel" -> Intent(ctx, LuckyWheelActivity::class.java)
        "play_multi_number"-> Intent(ctx, MultiNumberGeneratorActivity::class.java)
        "play_flip_coin"   -> Intent(ctx, FlipCoinActivity::class.java)
        "play_rps"         -> Intent(ctx, RockPaperScissorActivity::class.java)
        "play_spin_bottle" -> Intent(ctx, SpinBottleActivity::class.java)
        "play_roll_dice"   -> Intent(ctx, RollDiceActivity::class.java)
        "color_from_image" -> Intent(ctx, ImageColorPickerActivity::class.java)
        "color_picker"     -> Intent(ctx, ColorPickerActivity::class.java)
        "color_blend"      -> Intent(ctx, BlendColorsActivity::class.java)
        "calc_2d"          -> Intent(ctx, Shapes2DActivity::class.java)
        "calc_3d"          -> Intent(ctx, Shapes3DActivity::class.java)
        "ess_open_url"     -> Intent(ctx, OpenUrlActivity::class.java)
        "ess_open_file"    -> Intent(ctx, OpenFileActivity::class.java)
        "ess_url"          -> Intent(ctx, UrlShortenerActivity::class.java)
        "ess_whatsapp"     -> Intent(ctx, WhatsappUnsavedActivity::class.java)
        "ess_gmail"        -> Intent(ctx, GmailRouterActivity::class.java)
        "ess_compass"      -> Intent(ctx, CompassActivity::class.java)

        "gen_scanner"      -> Intent(ctx, ScannerActivity::class.java)
        else               -> null   // special-case tools handled separately
    }

    private fun handleSpecialCase(tool: Tool, ctx: android.content.Context) {
        when (tool.id) {
            "ess_support" -> {
                val activity = ctx as? androidx.fragment.app.FragmentActivity
                if (activity != null) {
                    SupportBottomSheet().show(activity.supportFragmentManager, SupportBottomSheet.TAG)
                }
            }
            else -> android.widget.Toast.makeText(ctx, "Coming soon: ${tool.name}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // ── Static helper kept for backward compat (widget / non-fragment callers) ──
    companion object {
        fun handleToolNavigation(tool: Tool, context: android.content.Context) {
            when (tool.id) {
                "calc_percentage"  -> context.startActivity(Intent(context, PercentageCalculatorActivity::class.java))
                "calc_average"     -> context.startActivity(Intent(context, AverageCalculatorActivity::class.java))
                "calc_days_until"  -> context.startActivity(Intent(context, DaysUntilActivity::class.java))
                "calc_age"         -> context.startActivity(Intent(context, AgeCalculatorActivity::class.java))
                "calc_fraction"    -> context.startActivity(Intent(context, FractionSimplifierActivity::class.java))
                "calc_factorial"   -> context.startActivity(Intent(context, FactorialCalculatorActivity::class.java))
                "calc_basic"       -> context.startActivity(Intent(context, BasicCalculatorActivity::class.java))
                "calc_scientific"  -> context.startActivity(Intent(context, ScientificCalculatorActivity::class.java))
                "gen_qr"           -> context.startActivity(Intent(context, QrCodeGeneratorActivity::class.java))
                "gen_barcode"      -> context.startActivity(Intent(context, BarcodeGeneratorActivity::class.java))
                "gen_password"     -> context.startActivity(Intent(context, PasswordGeneratorActivity::class.java))
                "text_case"        -> context.startActivity(Intent(context, CaseConverterActivity::class.java))
                "text_morse"       -> context.startActivity(Intent(context, MorseCodeActivity::class.java))
                "text_repeat"      -> context.startActivity(Intent(context, RepeatTextActivity::class.java))
                "gen_number"       -> context.startActivity(Intent(context, NumberGeneratorActivity::class.java))
                "img_resize"       -> context.startActivity(Intent(context, ImageResizerActivity::class.java))
                "img_rounded"      -> context.startActivity(Intent(context, RoundedImageActivity::class.java))
                "img_compress"     -> context.startActivity(Intent(context, ImageCompressorActivity::class.java))
                "img_pixel_frame"  -> context.startActivity(Intent(context, PixelFramesActivity::class.java))
                "img_laptop_frame" -> context.startActivity(Intent(context, LaptopFramesActivity::class.java))
                "img_meme"         -> context.startActivity(Intent(context, MemeGeneratorActivity::class.java))
                "conv_jpg_png"     -> context.startActivity(Intent(context, JpgPngConverterActivity::class.java))
                "conv_img_pdf"     -> context.startActivity(Intent(context, ImageToPdfActivity::class.java))
                "conv_img_webp"    -> context.startActivity(Intent(context, ImageToWebpActivity::class.java))
                "conv_word_pdf",
                "conv_pdf_word"    -> context.startActivity(Intent(context, WordPdfConverterActivity::class.java))
                "conv_txt_pdf"     -> context.startActivity(Intent(context, TextToPdfActivity::class.java))
                "conv_pdf_combine" -> context.startActivity(Intent(context, PdfCombineActivity::class.java))
                "play_lucky_wheel" -> context.startActivity(Intent(context, LuckyWheelActivity::class.java))
                "play_multi_number"-> context.startActivity(Intent(context, MultiNumberGeneratorActivity::class.java))
                "play_flip_coin"   -> context.startActivity(Intent(context, FlipCoinActivity::class.java))
                "play_rps"         -> context.startActivity(Intent(context, RockPaperScissorActivity::class.java))
                "play_spin_bottle" -> context.startActivity(Intent(context, SpinBottleActivity::class.java))
                "play_roll_dice"   -> context.startActivity(Intent(context, RollDiceActivity::class.java))
                "color_from_image" -> context.startActivity(Intent(context, ImageColorPickerActivity::class.java))
                "color_picker"     -> context.startActivity(Intent(context, ColorPickerActivity::class.java))
                "color_blend"      -> context.startActivity(Intent(context, BlendColorsActivity::class.java))
                "calc_2d"          -> context.startActivity(Intent(context, Shapes2DActivity::class.java))
                "calc_3d"          -> context.startActivity(Intent(context, Shapes3DActivity::class.java))
                "ess_open_url"     -> context.startActivity(Intent(context, OpenUrlActivity::class.java))
                "ess_open_file"    -> context.startActivity(Intent(context, OpenFileActivity::class.java))
                "ess_url"          -> context.startActivity(Intent(context, UrlShortenerActivity::class.java))
                "ess_whatsapp"     -> context.startActivity(Intent(context, WhatsappUnsavedActivity::class.java))
                "ess_gmail"        -> context.startActivity(Intent(context, GmailRouterActivity::class.java))
                "ess_compass"      -> context.startActivity(Intent(context, CompassActivity::class.java))

                "gen_scanner"      -> context.startActivity(Intent(context, ScannerActivity::class.java))
                "ess_support" -> {
                    val activity = context as? androidx.fragment.app.FragmentActivity
                    if (activity != null) {
                        SupportBottomSheet().show(activity.supportFragmentManager, SupportBottomSheet.TAG)
                    }
                }
                else -> android.widget.Toast.makeText(context, "Coming soon: ${tool.name}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}
