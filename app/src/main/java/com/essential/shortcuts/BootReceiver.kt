package com.essential.shortcuts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            val now = System.currentTimeMillis()
            val emails = ScheduledEmailManager.getAll(context)
            emails.forEach { email ->
                if (email.scheduledTimeMs > now) {
                    ScheduledEmailManager.setAlarm(context, email)
                } else {
                    // Trigger missed scheduled send immediately (plus 5s padding)
                    ScheduledEmailManager.setAlarm(context, email.copy(scheduledTimeMs = now + 5000L))
                }
            }
        }
    }
}
