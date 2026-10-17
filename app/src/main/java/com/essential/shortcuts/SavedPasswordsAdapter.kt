package com.essential.shortcuts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class SavedPasswordsAdapter(
    private var passwords: MutableList<SavedPassword>,
    private val onDelete: (SavedPassword) -> Unit
) : RecyclerView.Adapter<SavedPasswordsAdapter.ViewHolder>() {

    // Track which positions have the password revealed
    private val revealedPositions = mutableSetOf<Int>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        val tvPassword: TextView = itemView.findViewById(R.id.tv_password)
        val btnToggle: ImageButton = itemView.findViewById(R.id.btn_toggle_visibility)
        val btnCopy: ImageButton = itemView.findViewById(R.id.btn_copy)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_password, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = passwords[position]
        holder.tvUsername.text = entry.username

        val isRevealed = revealedPositions.contains(position)
        holder.tvPassword.text = if (isRevealed) entry.password else maskPassword(entry.password)

        holder.btnToggle.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            if (revealedPositions.contains(pos)) {
                revealedPositions.remove(pos)
            } else {
                revealedPositions.add(pos)
            }
            notifyItemChanged(pos)
        }

        holder.btnCopy.setOnClickListener {
            val clipboard = holder.itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("password", entry.password))
            Toast.makeText(holder.itemView.context, "Password copied!", Toast.LENGTH_SHORT).show()
        }

        holder.btnDelete.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            onDelete(passwords[pos])
        }
    }

    override fun getItemCount() = passwords.size

    fun updatePasswords(newPasswords: List<SavedPassword>) {
        passwords = newPasswords.toMutableList()
        revealedPositions.clear()
        notifyDataSetChanged()
    }

    private fun maskPassword(password: String): String {
        return "•".repeat(password.length.coerceAtMost(20))
    }
}
