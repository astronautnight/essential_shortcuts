package com.essential.shortcuts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class CategoriesAdapter(
    private var categories: List<Category>,
    var onSearchQueryChanged: ((String) -> Unit)? = null,
    private val launchRequestHandler: ((ToolsAdapter.LaunchRequest) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_CATEGORY = 1
        private const val TYPE_EMPTY = 2
    }

    private var currentQuery: String = ""
    private var isShowingEmpty: Boolean = false



    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoryName: TextView = itemView.findViewById(R.id.tv_category_name)
        val rvTools: RecyclerView = itemView.findViewById(R.id.rv_category_tools)
    }

    inner class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemViewType(position: Int): Int {
        return if (isShowingEmpty && position == 0) TYPE_EMPTY else TYPE_CATEGORY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_EMPTY -> {
                val tv = TextView(parent.context).apply {
                    text = "No tools found"
                    textSize = 16f
                    setTextColor(androidx.core.content.ContextCompat.getColor(parent.context, R.color.text_secondary))
                    setPadding(48, 64, 48, 64)
                    gravity = android.view.Gravity.CENTER
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                EmptyViewHolder(tv)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_category, parent, false)
                CategoryViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CategoryViewHolder -> {
                if (position in categories.indices) {
                    val category = categories[position]
                    holder.tvCategoryName.visibility = View.VISIBLE
                    holder.rvTools.visibility = View.VISIBLE
                    holder.tvCategoryName.text = category.name

                    // Setup nested RecyclerView for tools
                    holder.rvTools.layoutManager = LinearLayoutManager(
                        holder.itemView.context, LinearLayoutManager.HORIZONTAL, false
                    )

                    val toolsAdapter = ToolsAdapter(
                        tools = category.tools,
                        categoryId = category.id,
                        isHorizontal = true,
                        launchRequestHandler = launchRequestHandler
                    ) { tool ->
                        handleToolClick(tool, holder.itemView.context)
                    }
                    holder.rvTools.adapter = toolsAdapter
                }
            }
            is EmptyViewHolder -> {
                // Nothing to bind — static "No tools found" text
            }
        }
    }

    override fun getItemCount(): Int {
        // Categories count, or 1 (empty state) if no results
        return if (isShowingEmpty) 1 else categories.size
    }

    fun updateCategories(newCategories: List<Category>, query: String = "") {
        categories = newCategories
        currentQuery = query
        isShowingEmpty = newCategories.isEmpty() && currentQuery.isNotEmpty()
        notifyDataSetChanged()
    }

    private fun handleToolClick(tool: Tool, context: android.content.Context) {
        // Navigation is handled by ToolsAdapter (via launchRequestHandler + tick animation).
        // This callback is only for side-effects like saving to recents.
        RecentToolsManager.saveRecentTool(context, tool.id)
    }
}
