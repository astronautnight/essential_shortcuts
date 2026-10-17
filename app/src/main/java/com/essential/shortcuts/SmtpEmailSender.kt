package com.essential.shortcuts

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.util.Properties
import javax.mail.*
import javax.mail.internet.*

/**
 * Sends email via Gmail SMTP in a background thread.
 * Requires a Gmail App Password (not your regular password).
 * Generate one at: myaccount.google.com → Security → App Passwords
 */
object SmtpEmailSender {

    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = "587"

    /**
     * @param onSuccess called on the main thread when mail is sent
     * @param onError   called on the main thread with an error message
     */
    fun sendAsync(
        context: Context,
        fromEmail: String,
        appPassword: String,
        to: String,
        cc: String,
        bcc: String,
        subject: String,
        body: String,
        isHtml: Boolean = false,
        inlineImages: List<Uri> = emptyList(),
        attachments: List<Uri> = emptyList(),
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val props = Properties().apply {
                    put("mail.smtp.auth",            "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host",            SMTP_HOST)
                    put("mail.smtp.port",            SMTP_PORT)
                    put("mail.smtp.ssl.trust",       SMTP_HOST)
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication() =
                        PasswordAuthentication(fromEmail, appPassword)
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(fromEmail))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                    if (cc.isNotEmpty())  setRecipients(Message.RecipientType.CC,  InternetAddress.parse(cc))
                    if (bcc.isNotEmpty()) setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc))
                    setSubject(subject, "UTF-8")

                    if (!isHtml && inlineImages.isEmpty() && attachments.isEmpty()) {
                        setText(body, "UTF-8")
                    } else {
                        val mpMixed = MimeMultipart("mixed")

                        // 1. Related part for HTML body and inline images
                        val mpRelated = MimeMultipart("related")

                        // HTML body part
                        val htmlBodyPart = MimeBodyPart().apply {
                            if (isHtml) {
                                setContent(body, "text/html; charset=utf-8")
                            } else {
                                setText(body, "UTF-8")
                            }
                        }
                        mpRelated.addBodyPart(htmlBodyPart)

                        // Inline images
                        inlineImages.forEachIndexed { index, uri ->
                            try {
                                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                                if (bytes != null) {
                                    val imgPart = MimeBodyPart().apply {
                                        setContent(bytes, context.contentResolver.getType(uri) ?: "image/png")
                                        setHeader("Content-ID", "<inline_img_$index>")
                                        disposition = Part.INLINE
                                    }
                                    mpRelated.addBodyPart(imgPart)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        // Add related container part to mixed multipart
                        val relatedContainer = MimeBodyPart().apply {
                            setContent(mpRelated)
                        }
                        mpMixed.addBodyPart(relatedContainer)

                        // 2. Standard attachments
                        attachments.forEach { uri ->
                            try {
                                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                                if (bytes != null) {
                                    val attPart = MimeBodyPart().apply {
                                        val name = getFileName(context, uri)
                                        setContent(bytes, "application/octet-stream")
                                        fileName = name
                                        setHeader("Content-Transfer-Encoding", "base64")
                                    }
                                    mpMixed.addBodyPart(attPart)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        setContent(mpMixed)
                    }
                }

                Transport.send(message)
                android.os.Handler(android.os.Looper.getMainLooper()).post { onSuccess() }

            } catch (e: AuthenticationFailedException) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onError("Authentication failed. Check your Gmail address and App Password.")
                }
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onError(e.message ?: "Failed to send email.")
                }
            }
        }.start()
    }

    private fun getFileName(ctx: Context, uri: Uri): String {
        var name = "attachment"
        ctx.contentResolver.query(uri, null, null, null, null)?.use {
            val col = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && col >= 0) name = it.getString(col)
        }
        return name
    }
}
