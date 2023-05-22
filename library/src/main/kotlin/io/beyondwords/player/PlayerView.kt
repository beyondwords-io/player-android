package io.beyondwords.player

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.webkit.DownloadListener
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings.LOAD_NO_CACHE
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.updateLayoutParams
import com.google.gson.Gson
import com.google.gson.GsonBuilder

@SuppressLint("SetJavaScriptEnabled")
class PlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
    companion object {
        private val gson: Gson by lazy {
            GsonBuilder().create()
        }
    }

    private var ready = false
    private val webView = WebView(context)
    private val listeners = mutableSetOf<EventListener>()
    private val pendingCommands = mutableListOf<String>()
    private val bridge = object {
        @JavascriptInterface
        fun onReady() {
            post {
                ready = true
                pendingCommands.forEach {
                    webView.evaluateJavascript(it, null)
                }
                pendingCommands.clear()
            }
        }

        @JavascriptInterface
        fun onResize(width: Int, height: Int) {
            post {
                updateLayoutParams {
                    this.width = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        width.toFloat(),
                        resources.displayMetrics
                    ).toInt()
                    this.height = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        height.toFloat(),
                        resources.displayMetrics
                    ).toInt()
                }
            }
        }

        @JavascriptInterface
        fun onEvent(event: String) {
            post {
                val parsedEvent: PlayerEvent
                try {
                    parsedEvent = gson.fromJson(event, PlayerEvent::class.java)
                } catch (e: Exception) {
                    Log.e("PlayerView:onEvent", "Failed to parse event $event", e)
                    return@post
                }
                listeners.forEach {
                    when (parsedEvent.type) {
                        "PressedPlay" -> it.onPressedPlay(parsedEvent)
                    }
                    it.onAny(parsedEvent)
                }
            }
        }
    }
    private val webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            return false
        }
    }
    private val downloadListener = object : DownloadListener {
        override fun onDownloadStart(
            url: String?,
            userAgent: String?,
            contentDisposition: String?,
            mimetype: String?,
            contentLength: Long
        ) {
            getSystemService(context, DownloadManager::class.java)?.let { downloadManager ->
                val uri = Uri.parse(url)
                val request = DownloadManager.Request(uri)
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                downloadManager.enqueue(request)
            }
        }
    }

    init {
        addView(webView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            databaseEnabled = false
            cacheMode = LOAD_NO_CACHE
            mediaPlaybackRequiresUserGesture = false
            builtInZoomControls = false
            displayZoomControls = false
            loadWithOverviewMode = true
            setSupportZoom(false)
            setGeolocationEnabled(false)
            setSupportMultipleWindows(false)
        }

        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.addJavascriptInterface(bridge, "AndroidBridge")
        webView.webViewClient = webViewClient
        webView.setDownloadListener(downloadListener)
        webView.loadDataWithBaseURL(
            "https://beyondwords.io",
            resources.openRawResource(R.raw.player)
                .bufferedReader()
                .use { it.readText() },
            "text/html",
            "UTF-8",
            null
        )
    }

    fun load(playerSettings: PlayerSettings) {
        val loadCommand = """
            try {
                load(${gson.toJson(playerSettings)})
            } catch (e) {
                console.error("PlayerView:load", e.message, e)
            }
        """
        if (!ready) {
            pendingCommands.add(loadCommand)
        } else {
            webView.evaluateJavascript(loadCommand, null)
        }
    }

    fun addEventListener(listener: EventListener) {
        listeners.add(listener)
    }

    fun removeEventListener(listener: EventListener) {
        listeners.remove(listener)
    }
}
