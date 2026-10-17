package com.essential.shortcuts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat

class EmailScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.essential.shortcuts.SEND_SCHEDULED_EMAIL") {
            val id = intent.getStringExtra("email_id") ?: return

            val emails = ScheduledEmailManager.getAll(context)
            val email = emails.find { it.id == id } ?: return

            sendEmail(context, email)
        }
    }

    private fun sendEmail(context: Context, email: ScheduledEmail) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "scheduled_emails_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Scheduled Emails",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentTitle("Sending scheduled email...")
            .setContentText(email.subject)
            .setProgress(0, 0, true)
            .setOngoing(true)
        notificationManager.notify(email.id.hashCode(), builder.build())

        val from = SmtpAccountManager.getEmail(context)
        val pass = SmtpAccountManager.getPassword(context)

        if (from.isEmpty() || pass.isEmpty()) {
            updateNotificationFailed(context, email, "Account not configured. Open app to set App Password.")
            return
        }

        val inlineImages = email.inlineImageUris.map { Uri.parse(it) }
        val attachments = email.attachmentUris.map { Uri.parse(it) }

        SmtpEmailSender.sendAsync(
            context = context,
            fromEmail = from,
            appPassword = pass,
            to = email.to,
            cc = email.cc,
            bcc = email.bcc,
            subject = email.subject,
            body = email.body,
            isHtml = email.isHtml,
            inlineImages = inlineImages,
            attachments = attachments,
            onSuccess = {
                ScheduledEmailManager.cancel(context, email.id)

                val successBuilder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Scheduled email sent")
                    .setContentText(email.subject)
                    .setAutoCancel(true)
                notificationManager.notify(email.id.hashCode(), successBuilder.build())
            },
            onError = { msg ->
                updateNotificationFailed(context, email, msg)
            }
        )
    }

    private fun updateNotificationFailed(context: Context, email: ScheduledEmail, errorMsg: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "scheduled_emails_channel"
        val failedBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Scheduled email failed")
            .setContentText("Subject: ${email.subject}\nError: $errorMsg")
            .setAutoCancel(true)
        notificationManager.notify(email.id.hashCode(), failedBuilder.build())
    }
}
