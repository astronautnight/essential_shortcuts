package com.essential.shortcuts

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class SavedPassword(
    val username: String,
    val password: String,
    val timestamp: Long
)

object PasswordManager {
    private const val PREFS_NAME = "PasswordManagerPrefs"
    private const val KEY_PASSWORDS = "saved_passwords"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun savePassword(context: Context, username: String, password: String) {
        val passwords = getSavedPasswords(context).toMutableList()
        passwords.add(0, SavedPassword(username, password, System.currentTimeMillis()))
        persistPasswords(context, passwords)
    }

    fun getSavedPasswords(context: Context): List<SavedPassword> {
        val json = getPrefs(context).getString(KEY_PASSWORDS, "[]") ?: "[]"
        val result = mutableListOf<SavedPassword>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    SavedPassword(
                        username = obj.getString("username"),
                        password = obj.getString("password"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
        } catch (_: Exception) { }
        return result
    }

    fun deletePassword(context: Context, timestamp: Long) {
        val passwords = getSavedPasswords(context).toMutableList()
        passwords.removeAll { it.timestamp == timestamp }
        persistPasswords(context, passwords)
    }

    fun persistPasswords(context: Context, passwords: List<SavedPassword>) {
        val arr = JSONArray()
        for (p in passwords) {
            val obj = JSONObject()
            obj.put("username", p.username)
            obj.put("password", p.password)
            obj.put("timestamp", p.timestamp)
            arr.put(obj)
        }
        getPrefs(context).edit().putString(KEY_PASSWORDS, arr.toString()).apply()
    }
}
