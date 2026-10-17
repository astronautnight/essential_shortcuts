package com.essential.shortcuts

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class GmailTemplate(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    val from: String,
    val subject: String,
    val body: String,
    /** URIs of attachments to restore when the template is applied. */
    val attachmentUris: List<Uri> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("from", from)
        put("subject", subject); put("body", body)
        val uriArr = JSONArray().also { a -> attachmentUris.forEach { a.put(it.toString()) } }
        put("attachmentUris", uriArr)
    }

    companion object {
        fun fromJson(o: JSONObject): GmailTemplate {
            val uriArr = o.optJSONArray("attachmentUris") ?: JSONArray()
            val uris = (0 until uriArr.length()).map { Uri.parse(uriArr.getString(it)) }
            return GmailTemplate(
                id             = o.optString("id", UUID.randomUUID().toString()),
                name           = o.optString("name", "Untitled"),
                from           = o.optString("from", ""),
                subject        = o.optString("subject", ""),
                body           = o.optString("body", ""),
                attachmentUris = uris
            )
        }
    }
}
