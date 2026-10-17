package com.essential.shortcuts

import android.os.Bundle
import android.text.Html
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PrivacyPolicyBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_privacy_policy, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val tvContent = view.findViewById<TextView>(R.id.tv_privacy_policy_content)
        val btnClose = view.findViewById<Button>(R.id.btn_close)

        tvContent.text = getPrivacyPolicySpannable()
        
        // Subtle fade in animation
        tvContent.alpha = 0f
        tvContent.animate().alpha(1f).setDuration(500).start()

        btnClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog ?: return
        val sheet = dialog.findViewById<FrameLayout>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return

        sheet.layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT

        val screenHeight = resources.displayMetrics.heightPixels
        val targetHeight = (screenHeight * 0.85).toInt()

        val behavior = BottomSheetBehavior.from(sheet)
        behavior.peekHeight = targetHeight
        behavior.maxHeight = targetHeight
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun getPrivacyPolicySpannable(): CharSequence {
        val builder = SpannableStringBuilder()

        fun appendSection(title: String, content: String) {
            val start = builder.length
            builder.append(title).append("\n")
            builder.setSpan(StyleSpan(android.graphics.Typeface.BOLD), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.append(content).append("\n\n")
        }

        appendSection("1. Information We Collect", 
            "Essential Shortcuts is committed to protecting your privacy. We collect minimal information:\n" +
            "• Personal Information: We do NOT collect personal information like name, email, or phone number.\n" +
            "• Automatically Collected: Device model, OS version, app version, Usage Data (features used, crash reports via Google Play).")

        appendSection("2. How We Use Information", 
            "• App Functionality: To provide calculator, converter, and tool features.\n" +
            "• Analytics: To improve app performance and fix bugs.")

        appendSection("3. Data Storage", 
            "• All calculations and conversions are performed locally on your device.\n" +
            "• We do NOT store your calculation history on our servers.")

        appendSection("4. Changes to Privacy Policy", 
            "We may update this policy. Changes will be posted in the app and on this page with a new effective date.")

        appendSection("5. Contact Us", 
            "For privacy questions or data deletion requests:\n" +
            "Email: rajassberde@gmail.com\n" +
            "Developer: Rajas Berde")

        return builder
    }

    companion object {
        const val TAG = "PrivacyPolicyBottomSheet"
    }
}
