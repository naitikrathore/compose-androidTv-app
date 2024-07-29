package com.iwedia.cltv.components.welcome_screen

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.text.Html
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.ScrollingMovementMethod
import android.text.style.BulletSpan
import android.text.style.LeadingMarginSpan
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import com.iwedia.cltv.R
import com.iwedia.cltv.platform.model.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Matcher
import java.util.regex.Pattern

@RequiresApi(Build.VERSION_CODES.P)
@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class CustomWelcomeScreenWebView(context: Context, url: String, val listener: Listener) : ConstraintLayout(context) {

    val TAG = "CustomWelcomeScreenWebView"
    private var textView: TextView? = null
    private var connection: HttpURLConnection? = null

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.layout_web_view, this, true)
        textView = findViewById(R.id.textView)
        textView!!.movementMethod = ScrollingMovementMethod()
        textView!!.setBackgroundColor(Color.parseColor("#293241"))
        textView!!.setTextColor(Color.parseColor("#eeeeee"))
        textView!!.setOnKeyListener { _, keyCode, keyEvent ->
            when (keyEvent.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        listener.onBackClicked()
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "init: url === $url")
        getHtml(url) {
            var stringHtml = "<br/><br/><br/>"
                .plus(it)
                .plus("<br/><br/><br/>")

            // removing unwanted title & styles
            stringHtml = removeMatchingContent(Pattern.compile("<title>(.*?)</title>"),stringHtml)
            stringHtml = removeMatchingContent(Pattern.compile("<style>(.*?)</style>"),stringHtml)

            val spanned = Html.fromHtml(stringHtml, Html.FROM_HTML_MODE_LEGACY)
            val spannableBuilder = SpannableStringBuilder(spanned)
            val adjustedSpannable = adjustBulletSpans(spannableBuilder)

            CoroutineScope(Dispatchers.Main).launch {
                textView!!.text = adjustedSpannable
            }
        }
    }

    /**
     * Adjusts the spacing and margin for BulletSpans within a Spannable.
     *
     * This method iterates over the BulletSpans in the Spannable text and adjusts the spacing
     * before the bullet and the left margin of the text to create a formatted bullet list.
     *
     * @param spannable The Spannable text containing BulletSpans.
     * @return The adjusted Spannable text with modified BulletSpans and spacing.
     */
    private fun adjustBulletSpans(spannable: Spannable): Spannable {
        val spannableBuilder = SpannableStringBuilder(spannable)

        // Find BulletSpans and adjust their margins
        val bulletSpans = spannable.getSpans(0, spannable.length, BulletSpan::class.java)
        for (bulletSpan in bulletSpans) {
            val start = spannable.getSpanStart(bulletSpan)
            val end = spannable.getSpanEnd(bulletSpan)

            // Remove the existing BulletSpan
            spannableBuilder.removeSpan(bulletSpan)

            // Apply the space before bullet
            spannableBuilder.setSpan(
                MarginSpan( ),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Apply the BulletSpan again
            spannableBuilder.setSpan(
                bulletSpan,
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            // Apply LeadingMarginSpan to add the left margin
            spannableBuilder.setSpan(
                LeadingMarginSpan.Standard(10),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return spannableBuilder
    }

    /**
     * A custom span that adds a fixed leading margin to the text.
     *
     * This span adds a fixed amount of space (50 pixels) before the text, effectively creating an indentation.
     * It can be used to add padding or indentation to paragraphs or lines of text.
     */
    class MarginSpan : LeadingMarginSpan {
        override fun getLeadingMargin(first: Boolean): Int {
            return 50
        }

        override fun drawLeadingMargin(
            c: Canvas, p: Paint, x: Int, dir: Int, top: Int,
            baseline: Int, bottom: Int, text: CharSequence,
            start: Int, end: Int, first: Boolean, layout: Layout
        ) {
            // No drawing needed for the margin span
        }
    }

    /**
     * Removes all occurrences of a pattern from the given content string.
     *
     * @param pattern The Pattern to be matched and removed from the content.
     * @param content The input string from which the pattern should be removed.
     * @return A new string with all occurrences of the pattern removed.
     */
    private fun removeMatchingContent(pattern : Pattern, content : String) : String{
        val matcher: Matcher = pattern.matcher(content)
        val result = StringBuffer()
        while (matcher.find()) {
            matcher.appendReplacement(result, "")
        }
        matcher.appendTail(result)
        return result.toString()
    }

    private fun getHtml(url: String, callback: (html: String)->Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                connection = URL(url).openConnection() as HttpURLConnection
                connection?.connectTimeout = 0
                connection?.readTimeout = 0
                connection?.connect()

                val inputStream  = connection?.getInputStream()
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                val stringHtml = StringBuilder()
                var line:String ?= null
                do {
                    line = bufferedReader.readLine()
                    if (line != null)
                        stringHtml.append(line)
                } while (line != null)
                callback(stringHtml.toString())
            } catch (e: IOException) {
                Log.e(Constants.LogTag.CLTV_TAG + TAG, "exception: ", e)
            }
            connection?.disconnect()
        }
    }

    fun destroy() {
        connection?.disconnect()
    }

    interface Listener {
        fun onBackClicked()
    }
}