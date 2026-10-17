package com.essential.shortcuts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SupportBottomSheet : BottomSheetDialogFragment() {

    // ── Replace with your real links ──
    private val kofiUrl    = "https://ko-fi.com/rbstudioz"
    private val patreonUrl = "https://www.patreon.com/"
    private val upiId      = "rajassberde@oksbi"
    // ─────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_support, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons(view)
        animateCards(view)
    }

    override fun onStart() {
        super.onStart()

        // Make the sheet 75% of the screen height, full width
        val dialog = dialog as? BottomSheetDialog ?: return
        val sheet = dialog.findViewById<FrameLayout>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return

        // Full width — remove any side padding the dialog adds
        sheet.layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT

        val screenHeight = resources.displayMetrics.heightPixels
        val targetHeight = (screenHeight * 0.75).toInt()

        val behavior = BottomSheetBehavior.from(sheet)
        behavior.peekHeight    = targetHeight
        behavior.maxHeight     = targetHeight
        behavior.state         = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = false
        behavior.isDraggable   = true
    }

    private fun setupButtons(view: View) {
        // Ko-fi
        view.findViewById<LinearLayout>(R.id.card_kofi).setOnClickListener {
            openUrl(kofiUrl)
        }

        // Patreon
        view.findViewById<LinearLayout>(R.id.card_patreon).setOnClickListener {
            openUrl(patreonUrl)
        }

        // UPI — copy to clipboard & try to open payment app
        val cardUpi = view.findViewById<LinearLayout>(R.id.card_upi)
        view.findViewById<TextView>(R.id.tv_upi_id).text = upiId

        cardUpi.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("UPI ID", upiId))
            Toast.makeText(requireContext(), "UPI ID copied! \uD83D\uDCCB", Toast.LENGTH_SHORT).show()

            try {
                val uri = Uri.parse("upi://pay?pa=$upiId&pn=Essential%20Shortcuts&cu=INR")
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (_: Exception) { /* no UPI app — clipboard is enough */ }
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun animateCards(view: View) {
        val slideIn = AnimationUtils.loadAnimation(requireContext(), R.anim.bottom_sheet_item_enter)
        listOf(R.id.card_kofi, R.id.card_patreon, R.id.card_upi)
            .forEachIndexed { index, id ->
                val card = view.findViewById<LinearLayout>(id)
                card.alpha = 0f
                card.postDelayed({
                    card.alpha = 1f
                    card.startAnimation(slideIn)
                }, index * 90L)
            }
    }

    companion object {
        const val TAG = "SupportBottomSheet"
    }
}
