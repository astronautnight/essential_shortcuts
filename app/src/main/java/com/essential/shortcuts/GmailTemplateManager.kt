package com.essential.shortcuts

import android.content.Context
import org.json.JSONArray

object GmailTemplateManager {
    private const val PREFS = "gmail_templates"
    private const val KEY   = "templates_json"

    fun getAll(ctx: Context): MutableList<GmailTemplate> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        val list = mutableListOf<GmailTemplate>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                try {
                    val obj = arr.getJSONObject(i)
                    list.add(GmailTemplate.fromJson(obj))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun save(ctx: Context, list: List<GmailTemplate>) {
        val arr = JSONArray().also { a -> list.forEach { a.put(it.toJson()) } }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun add(ctx: Context, t: GmailTemplate) { val l = getAll(ctx); l.add(t); save(ctx, l) }

    fun delete(ctx: Context, id: String) { save(ctx, getAll(ctx).filter { it.id != id }) }

    fun rename(ctx: Context, id: String, newName: String) {
        val l = getAll(ctx); l.find { it.id == id }?.name = newName; save(ctx, l)
    }
}
