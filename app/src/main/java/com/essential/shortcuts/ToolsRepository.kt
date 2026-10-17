package com.essential.shortcuts  // Change this to YOUR package name

import android.content.Context

object ToolsRepository {

    fun getAllCategories(context: Context): List<Category> {
        return listOf(
            // Calculator Category
            Category(
                id = "calculator",
                name = "Calculator",
                colorRes = R.color.category_calculator,
                tools = listOf(
                    Tool("calc_basic", "Basic Calculator", R.drawable.ic_calculator, "calculator"),
                    Tool("calc_scientific", "Scientific Calculator", R.drawable.ic_scientific, "calculator"),
                    Tool("calc_age", "Calculate Age", R.drawable.ic_calendar, "calculator"),
                    Tool("calc_days_until", "Days Until", R.drawable.ic_calendar, "calculator"),
                    Tool("calc_percentage", "Percentage", R.drawable.ic_percentage, "calculator"),
                    Tool("calc_average", "Average", R.drawable.ic_average, "calculator"),
                    Tool("calc_fraction", "Fraction Simplifier", R.drawable.ic_fraction, "calculator"),
                    Tool("calc_factorial", "Factorial", R.drawable.ic_factorial, "calculator"),
                    Tool("calc_2d", "2D Shapes", R.drawable.ic_2d_shapes, "calculator"),
                    Tool("calc_3d", "3D Geometry", R.drawable.ic_3d_geometry, "calculator")
                )
            ),

            // Essential Tools Category
            Category(
                id = "essential",
                name = "Essential Shortcuts",
                colorRes = R.color.category_essential,
                tools = listOf(
                    Tool("ess_url", "Shorten URL", R.drawable.ic_link, "essential"),
                    Tool("ess_whatsapp", "WhatsApp without saving", R.drawable.ic_chat, "essential"),
                    Tool("ess_compass", "Compass", R.drawable.ic_compass, "essential"),
                    Tool("ess_open_url", "Open URL", R.drawable.ic_link, "essential"),
                    Tool("ess_open_file", "Open File", R.drawable.ic_open_file, "essential"),
                    Tool("ess_gmail", "Send Email", R.drawable.ic_gmail, "essential"),
                    Tool("img_meme", "Meme Generator", R.drawable.ic_edit, "essential"),
                    Tool("ess_support", "Support Development", R.drawable.ic_heart_support, "essential")
                )
            ),

            // Generator Tools Category
            Category(
                id = "generator",
                name = "Generators",
                colorRes = R.color.category_generator,
                tools = listOf(
                    Tool("gen_qr", "Make QR Code", R.drawable.ic_qr_code, "generator"),
                    Tool("gen_barcode", "Make Barcode", R.drawable.ic_barcode, "generator"),
                    Tool("gen_password", "Make Password", android.R.drawable.ic_lock_lock, "generator"),
                    Tool("gen_scanner", "Scan QR/Barcode", R.drawable.ic_scanner, "generator"),
                    Tool("gen_number", "Generate Number", R.drawable.ic_number, "generator")
                )
            ),

            // Image Tools Category
            Category(
                id = "image_tools",
                name = "Image Shortcuts",
                colorRes = R.color.category_image,
                tools = listOf(
                    Tool("img_resize", "Resize Image", android.R.drawable.ic_menu_crop, "image_tools"),
                    Tool("img_rounded", "Rounded Image", R.drawable.ic_rounded_image, "image_tools"),
                    Tool("img_compress", "Compress Image", R.drawable.ic_compress, "image_tools"),
                    Tool("img_pixel_frame", "Pixel Frames", R.drawable.ic_pixel_frame, "image_tools"),
                    Tool("img_laptop_frame", "Laptop Frames", R.drawable.ic_laptop_frame, "image_tools")
                )
            ),

            // Converter Tools Category
            Category(
                id = "converter",
                name = "Convert",
                colorRes = R.color.category_converter,
                tools = listOf(
                    Tool("conv_image", "Convert Image", R.drawable.ic_jpg_png, "converter"),
                    Tool("conv_word_pdf", "PDF ⇄ Word", R.drawable.ic_word_pdf, "converter"),
                    Tool("conv_txt_pdf", "Text to PDF", R.drawable.ic_txt_pdf, "converter"),
                    Tool("conv_pdf_combine", "Combine PDFs", R.drawable.ic_pdf_combine, "converter")
                )
            ),

            // Color Tools Category
            Category(
                id = "color_tools",
                name = "Color Shortcuts",
                colorRes = R.color.category_color_tools,
                tools = listOf(
                    Tool("color_picker", "Pick Color", R.drawable.ic_color_picker, "color_tools"),
                    Tool("color_from_image", "Pick Color from Image", android.R.drawable.ic_menu_gallery, "color_tools"),
                    Tool("color_blend", "Blend Colors", R.drawable.ic_color_blend, "color_tools")
                )
            ),

            // Text Tools Category
            Category(
                id = "text_tools",
                name = "Text Shortcuts",
                colorRes = R.color.category_text_tools,
                tools = listOf(
                    Tool("text_case", "Case Converter", R.drawable.ic_text_case, "text_tools"),
                    Tool("text_morse", "Morse Code", R.drawable.ic_text_case, "text_tools"),
                    Tool("text_repeat", "Repeat Text", R.drawable.ic_repeat, "text_tools")
                )
            ),

            // Playground Category
            Category(
                id = "playground",
                name = "Playground",
                colorRes = R.color.category_playground,
                tools = listOf(
                    Tool("play_lucky_wheel", "Lucky Wheel", R.drawable.ic_lucky_wheel, "playground"),
                    Tool("play_multi_number", "Multi Number Gen", R.drawable.ic_multi_number, "playground"),
                    Tool("play_flip_coin", "Flip Coin", R.drawable.ic_flip_coin, "playground"),
                    Tool("play_rps", "Rock Paper Scissors", R.drawable.ic_rps, "playground"),
                    Tool("play_spin_bottle", "Spin Bottle", R.drawable.ic_spin_bottle, "playground"),
                    Tool("play_roll_dice", "Roll Dice", R.drawable.ic_roll_dice, "playground")
                )
            )
        )
    }

    fun getAllTools(context: Context): List<Tool> {
        return getAllCategories(context).flatMap { it.tools }
    }

    fun getCategoryForTool(context: Context, toolId: String): String {
        val categories = getAllCategories(context)
        for (category in categories) {
            if (category.tools.any { it.id == toolId }) {
                return category.id
            }
        }
        return "essential" // Fallback category
    }
}
