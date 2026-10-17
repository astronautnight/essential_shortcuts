package com.essential.shortcuts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class UrlShortenerActivity : AppCompatActivity() {

    private lateinit var etUrl: TextInputEditText
    private lateinit var btnShorten: Button
    private lateinit var btnClose: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var cardResult: MaterialCardView
    private lateinit var cardTlyAffiliate: MaterialCardView
    private lateinit var tvShortUrl: TextView
    private lateinit var btnCopy: Button
    private lateinit var btnShare: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_url_shortener)

        // Make window background transparent for popup
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        etUrl = findViewById(R.id.et_url)
        btnShorten = findViewById(R.id.btn_shorten)
        btnClose = findViewById(R.id.btn_close)
        progressBar = findViewById(R.id.progress_bar)
        cardResult = findViewById(R.id.card_result)
        cardTlyAffiliate = findViewById(R.id.card_tly_affiliate)
        tvShortUrl = findViewById(R.id.tv_short_url)
        btnCopy = findViewById(R.id.btn_copy)
        btnShare = findViewById(R.id.btn_share)

        cardTlyAffiliate.setOnClickListener {
            animateTap(it) {
                openTlyLink()
            }
        }

        btnShorten.setOnClickListener {
            val url = etUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                shortenUrl(url)
            } else {
                etUrl.error = "Please enter a URL"
            }
        }

        btnClose.setOnClickListener {
            finish()
        }

        btnCopy.setOnClickListener {
            val shortUrl = tvShortUrl.text.toString()
            if (shortUrl.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Shortened URL", shortUrl)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        btnShare.setOnClickListener {
            val shortUrl = tvShortUrl.text.toString()
            if (shortUrl.isNotEmpty()) {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shortUrl)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)
            }
        }
    }

    private fun shortenUrl(longUrl: String) {
        var validUrl = longUrl
        if (!validUrl.startsWith("http://") && !validUrl.startsWith("https://")) {
            validUrl = "https://$validUrl"
        }

        // Hide keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etUrl.windowToken, 0)

        // Show UI change
        cardResult.visibility = View.GONE
        btnShorten.isEnabled = false
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiUrl = URL("https://tinyurl.com/api-create.php?url=$validUrl")
                val connection = apiUrl.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val shortLink = reader.readLine()
                    reader.close()

                    withContext(Dispatchers.Main) {
                        tvShortUrl.text = shortLink
                        cardResult.visibility = View.VISIBLE
                        setResult(RESULT_OK)   // ← mark as completed
                        progressBar.visibility = View.GONE
                        btnShorten.isEnabled = true
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showError()
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showError()
                }
            }
        }
    }

    private fun showError() {
        progressBar.visibility = View.GONE
        btnShorten.isEnabled = true
        Toast.makeText(this, "Failed to shorten URL. Check internet connection or valid URL.", Toast.LENGTH_LONG).show()
    }

    private fun openTlyLink() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TLY_AFFILIATE_URL))
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun animateTap(view: View, action: () -> Unit) {
        val anim = android.view.animation.ScaleAnimation(
            1f, 0.96f, 1f, 0.96f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 80
            repeatCount = 1
            repeatMode = android.view.animation.Animation.REVERSE
            setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(a: android.view.animation.Animation?) {}
                override fun onAnimationRepeat(a: android.view.animation.Animation?) {}
                override fun onAnimationEnd(a: android.view.animation.Animation?) { action() }
            })
        }
        view.startAnimation(anim)
    }

    companion object {
        private const val TLY_AFFILIATE_URL = "https://t.ly/register?via=rajas"
    }
}
