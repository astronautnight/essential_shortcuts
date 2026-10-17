package com.essential.shortcuts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ScheduledEmail(
    val id: String = UUID.randomUUID().toString(),
    val to: String,
    val cc: String,
    val bcc: String,
    val subject: String,
    val body: String, // serialized HTML blocks
    val isHtml: Boolean,
    val inlineImageUris: List<String>,
    val attachmentUris: List<String>,
    val scheduledTimeMs: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("to", to)
        put("cc", cc)
        put("bcc", bcc)
        put("subject", subject)
        put("body", body)
        put("isHtml", isHtml)
        put("inlineImageUris", JSONArray(inlineImageUris))
        put("attachmentUris", JSONArray(attachmentUris))
        put("scheduledTimeMs", scheduledTimeMs)
    }

    companion object {
        fun fromJson(o: JSONObject): ScheduledEmail {
            val inlineArr = o.optJSONArray("inlineImageUris") ?: JSONArray()
            val inlineUris = (0 until inlineArr.length()).map { inlineArr.getString(it) }
            val attArr = o.optJSONArray("attachmentUris") ?: JSONArray()
            val attUris = (0 until attArr.length()).map { attArr.getString(it) }

            return ScheduledEmail(
                id = o.optString("id", UUID.randomUUID().toString()),
                to = o.optString("to", ""),
                cc = o.optString("cc", ""),
                bcc = o.optString("bcc", ""),
                subject = o.optString("subject", ""),
                body = o.optString("body", ""),
                isHtml = o.optBoolean("isHtml", false),
                inlineImageUris = inlineUris,
                attachmentUris = attUris,
                scheduledTimeMs = o.optLong("scheduledTimeMs", 0L)
            )
        }
    }
}

object ScheduledEmailManager {
    private const val PREFS = "scheduled_emails"
    private const val KEY_LIST = "emails_json"

    fun getAll(ctx: Context): MutableList<ScheduledEmail> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LIST, "[]") ?: "[]"
        val list = mutableListOf<ScheduledEmail>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                try {
                    list.add(ScheduledEmail.fromJson(arr.getJSONObject(i)))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveAll(ctx: Context, list: List<ScheduledEmail>) {
        val arr = JSONArray().also { a -> list.forEach { a.put(it.toJson()) } }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LIST, arr.toString()).apply()
    }

    fun schedule(ctx: Context, email: ScheduledEmail) {
        val list = getAll(ctx)
        list.add(email)
        saveAll(ctx, list)
        setAlarm(ctx, email)
    }

    fun cancel(ctx: Context, id: String) {
        val list = getAll(ctx)
        list.removeAll { it.id == id }
        saveAll(ctx, list)

        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, EmailScheduleReceiver::class.java).apply {
            action = "com.essential.shortcuts.SEND_SCHEDULED_EMAIL"
            putExtra("email_id", id)
        }
        val pi = PendingIntent.getBroadcast(
            ctx,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
        pi.cancel()
    }

    fun setAlarm(ctx: Context, email: ScheduledEmail) {
        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, EmailScheduleReceiver::class.java).apply {
            action = "com.essential.shortcuts.SEND_SCHEDULED_EMAIL"
            putExtra("email_id", email.id)
        }
        val pi = PendingIntent.getBroadcast(
            ctx,
            email.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val info = AlarmManager.AlarmClockInfo(email.scheduledTimeMs, pi)
            alarmManager.setAlarmClock(info, pi)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, email.scheduledTimeMs, pi)
        }
    }
}
