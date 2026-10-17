package com.essential.shortcuts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class GmailTemplateAdapter(
    private var items: MutableList<GmailTemplate>,
    private val onUse:    (GmailTemplate) -> Unit,
    private val onRename: (GmailTemplate) -> Unit,
    private val onDelete: (GmailTemplate) -> Unit
) : RecyclerView.Adapter<GmailTemplateAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name:    TextView    = v.findViewById(R.id.tv_template_name)
        val subject: TextView    = v.findViewById(R.id.tv_template_subject)
        val use:     MaterialButton = v.findViewById(R.id.btn_template_use)
        val rename:  ImageView   = v.findViewById(R.id.btn_template_rename)
        val delete:  ImageView   = v.findViewById(R.id.btn_template_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gmail_template, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val t = items[pos]
        h.name.text    = t.name
        h.subject.text = if (t.subject.isNotEmpty()) "Subject: ${t.subject}" else "From: ${t.from}"
        h.use.setOnClickListener    { onUse(t) }
        h.rename.setOnClickListener { onRename(t) }
        h.delete.setOnClickListener { onDelete(t) }
    }

    fun reload(newItems: MutableList<GmailTemplate>) {
        items = newItems
        notifyDataSetChanged()
    }
}
