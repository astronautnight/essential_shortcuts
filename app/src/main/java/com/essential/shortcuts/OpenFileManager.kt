package com.essential.shortcuts

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SavedFile(
    val id: String,
    val name: String,
    val uriString: String,
    val mimeType: String,
    val timestamp: Long = System.currentTimeMillis()
)

object OpenFileManager {
    private const val PREFS_NAME = "OpenFilePrefs"
    private const val KEY_SAVED_FILES = "saved_files_list"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSavedFiles(context: Context): List<SavedFile> {
        val jsonString = getPrefs(context).getString(KEY_SAVED_FILES, null) ?: return emptyList()
        val list = mutableListOf<SavedFile>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SavedFile(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        uriString = obj.getString("uriString"),
                        mimeType = obj.optString("mimeType", "*/*"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveFile(context: Context, name: String, uriString: String, mimeType: String): SavedFile {
        val list = getSavedFiles(context).toMutableList()
        // If file with same URI already exists, remove it first to bump to top
        list.removeAll { it.uriString == uriString }
        val newEntry = SavedFile(
            id = UUID.randomUUID().toString(),
            name = name,
            uriString = uriString,
            mimeType = mimeType
        )
        list.add(0, newEntry) // Most recent first
        saveList(context, list)
        return newEntry
    }

    fun deleteFile(context: Context, id: String) {
        val list = getSavedFiles(context).toMutableList()
        list.removeAll { it.id == id }
        saveList(context, list)
    }

    private fun saveList(context: Context, list: List<SavedFile>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("uriString", item.uriString)
                put("mimeType", item.mimeType)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_SAVED_FILES, array.toString()).apply()
    }
}
