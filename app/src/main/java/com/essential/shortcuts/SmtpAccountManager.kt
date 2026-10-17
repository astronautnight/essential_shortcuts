package com.essential.shortcuts

import android.content.Context

/** Stores the SMTP sending account in SharedPreferences. */
object SmtpAccountManager {
    private const val PREFS = "smtp_account"
    private const val KEY_EMAIL    = "smtp_email"
    private const val KEY_PASSWORD = "smtp_app_password"

    fun getEmail(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_EMAIL, "") ?: ""

    fun getPassword(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PASSWORD, "") ?: ""

    fun isConfigured(ctx: Context) = getEmail(ctx).isNotEmpty() && getPassword(ctx).isNotEmpty()

    fun save(ctx: Context, email: String, password: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_EMAIL, email.trim())
            .putString(KEY_PASSWORD, password.trim())
            .apply()
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
