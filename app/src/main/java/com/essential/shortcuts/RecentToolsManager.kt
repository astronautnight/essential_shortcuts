package com.essential.shortcuts

import android.content.Context
import android.content.SharedPreferences

object RecentToolsManager {
    private const val PREFS_NAME = "RecentToolsPrefs"
    private const val KEY_RECENT_TOOLS = "recent_tools_list"
    private const val MAX_RECENT_TOOLS = 12

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveRecentTool(context: Context, toolId: String) {
        val currentToolsMutable = getRecentTools(context).toMutableList()
        
        // Remove if it already exists so we can move it to the top
        currentToolsMutable.remove(toolId)
        
        // Add to the top of the list
        currentToolsMutable.add(0, toolId)
        
        // Truncate to max allowed tools
        if (currentToolsMutable.size > MAX_RECENT_TOOLS) {
            currentToolsMutable.removeAt(currentToolsMutable.size - 1)
        }
        
        // Save back as a comma-separated string
        val joinedString = currentToolsMutable.joinToString(",")
        getPrefs(context).edit().putString(KEY_RECENT_TOOLS, joinedString).apply()
    }

    fun getRecentTools(context: Context): List<String> {
        val savedString = getPrefs(context).getString(KEY_RECENT_TOOLS, "") ?: ""
        if (savedString.isBlank()) return emptyList()
        return savedString.split(",").filter { it.isNotBlank() }
    }
}
