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
    private val webViewContainer = FrameLayout(context)
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
            webViewContainer.post {
                webViewContainer.updateLayoutParams {
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
            return true
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
        addView(webViewContainer, LayoutParams(LayoutParams.MATCH_PARENT, 0))
        webViewContainer.addView(webView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

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

    fun addEventListener(listener: EventListener) {
        listeners.add(listener)
    }

    fun removeEventListener(listener: EventListener) {
        listeners.remove(listener)
    }

    fun load(playerSettings: PlayerSettings) {
        callFunction("load", gson.toJson(playerSettings))
    }

    fun setPlayerStyle(playerStyle: String) {
        setProp("player.playerStyle", "\"$playerStyle\"")
    }

    fun onWidgetEvent(event: PlayerEvent) {
        callFunction("onWidgetEvent", gson.toJson(event))
    }

    private fun callFunction(name: String, args: String) {
        execCommand(
            """
                try {
                    $name($args)
                } catch (e) {
                    console.error("PlayerView:callFunction", e.message, e)
                }
            """
        )
    }

    private fun setProp(name: String, value: String) {
        execCommand(
            """
                try {
                    $name = $value
                } catch (e) {
                    console.error("PlayerView:setProp", e.message, e)
                }
            """
        )
    }

    private fun execCommand(command: String) {
        if (!ready) {
            pendingCommands.add(command)
        } else {
            webView.evaluateJavascript(command, null)
        }
    }
}
