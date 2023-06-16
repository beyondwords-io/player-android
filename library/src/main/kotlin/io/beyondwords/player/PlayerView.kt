package io.beyondwords.player

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
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
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import com.google.gson.Gson
import com.google.gson.GsonBuilder

@SuppressLint("SetJavaScriptEnabled", "ObsoleteSdkInt")
class PlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
    companion object {
        private val gson: Gson by lazy { GsonBuilder().create() }
    }

    private val webViewContainer = FrameLayout(context)
    private val webView = WebView(context)
    private val listeners = mutableSetOf<EventListener>()
    private val bridge = object {
        @Suppress("UNUSED_PARAMETER")
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
        fun onEvent(event: String, settings: String) {
            val parsedEvent: PlayerEvent
            try {
                parsedEvent = gson.fromJson(event, PlayerEvent::class.java)
            } catch (e: Exception) {
                Log.e("PlayerView:onEvent", "Failed to parse event $event", e)
                return
            }

            val parsedSettings: PlayerSettings
            try {
                parsedSettings = gson.fromJson(settings, PlayerSettings::class.java)
            } catch (e: Exception) {
                Log.e("PlayerView:onEvent", "Failed to parse settings $settings", e)
                return
            }

            post {
                listeners.forEach {
                    when (parsedEvent.type) {
                        "PressedPlay" -> it.onPressedPlay(parsedEvent)
                    }
                    it.onAny(parsedEvent, parsedSettings)
                }
            }
        }
    }
    private val webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            request?.let {
                val intent = Intent(Intent.ACTION_VIEW, it.url)
                ContextCompat.startActivity(context, intent, null)
            }
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
            ContextCompat.getSystemService(context, DownloadManager::class.java)
                ?.let { downloadManager ->
                    val uri = Uri.parse(url)
                    val request = DownloadManager.Request(uri)
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    downloadManager.enqueue(request)
                }
        }
    }
    private var mediaSession: MediaSession? = null

    init {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            throw Exception("Beyondwords Player SDK is supported on Android 7+")
        }

        addView(webViewContainer, LayoutParams(LayoutParams.MATCH_PARENT, 0))
        webViewContainer.addView(
            webView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            databaseEnabled = false
            cacheMode = LOAD_NO_CACHE
            mediaPlaybackRequiresUserGesture = false
            builtInZoomControls = false
            displayZoomControls = false
            loadWithOverviewMode = false
            setSupportZoom(false)
            setGeolocationEnabled(false)
            setSupportMultipleWindows(false)
        }

        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.webViewClient = webViewClient
        webView.setDownloadListener(downloadListener)
        webView.addJavascriptInterface(bridge, "PlayerViewBridge")
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mediaSession = MediaSession(webView)
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mediaSession?.release()
        mediaSession = null
        webView.loadUrl("about:blank")
        webViewContainer.updateLayoutParams {
            this.height = 0
        }
    }

    fun addEventListener(listener: EventListener) {
        listeners.add(listener)
    }

    fun removeEventListener(listener: EventListener) {
        listeners.remove(listener)
    }

    fun load(settings: PlayerSettings) {
        callFunction("load", listOf(settings))
    }

    fun setSkipButtonStyle(skipButtonStyle: String) {
        setProp("player.skipButtonStyle", skipButtonStyle)
    }

    fun setPlayerStyle(playerStyle: String) {
        setProp("player.playerStyle", playerStyle)
    }

    fun setPlaybackState(playbackState: String) {
        setProp("player.playbackState", playbackState)
    }

    private fun callFunction(name: String, args: List<Any>) {
        webView.evaluateJavascript(
            """
                try {
                    $name(${args.map { gson.toJson(it) }.joinToString(",") { it }})
                } catch (e) {
                    console.error("PlayerView:callFunction:" + e.message, e)
                }
            """, null
        )
    }

    private fun setProp(name: String, value: Any) {
        webView.evaluateJavascript(
            """
                try {
                    $name = ${gson.toJson(value)}
                } catch (e) {
                    console.error("PlayerView:setProp:" + e.message, e)
                }
            """, null
        )
    }
}
