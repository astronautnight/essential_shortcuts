package com.essential.shortcuts

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SavedUrl(
    val id: String,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

object OpenUrlManager {
    private const val PREFS_NAME = "OpenUrlPrefs"
    private const val KEY_SAVED_URLS = "saved_urls_list"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSavedUrls(context: Context): List<SavedUrl> {
        val jsonString = getPrefs(context).getString(KEY_SAVED_URLS, null) ?: return emptyList()
        val list = mutableListOf<SavedUrl>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SavedUrl(
                        id = obj.getString("id"),
                        title = obj.optString("title", ""),
                        url = obj.getString("url"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveUrl(context: Context, title: String, url: String): SavedUrl {
        val list = getSavedUrls(context).toMutableList()
        val cleanTitle = if (title.isBlank()) extractDomainOrTitle(url) else title.trim()
        val formattedUrl = formatUrl(url)
        val newEntry = SavedUrl(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            url = formattedUrl
        )
        list.add(0, newEntry) // Most recent first
        saveList(context, list)
        return newEntry
    }

    fun deleteUrl(context: Context, id: String) {
        val list = getSavedUrls(context).toMutableList()
        list.removeAll { it.id == id }
        saveList(context, list)
    }

    private fun saveList(context: Context, list: List<SavedUrl>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("url", item.url)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_SAVED_URLS, array.toString()).apply()
    }

    fun formatUrl(url: String): String {
        val trimmed = url.trim()
        return if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            "https://$trimmed"
        } else {
            trimmed
        }
    }

    private fun extractDomainOrTitle(url: String): String {
        return try {
            val uri = android.net.Uri.parse(formatUrl(url))
            uri.host?.removePrefix("www.") ?: url
        } catch (_: Exception) {
            url
        }
    }
}
