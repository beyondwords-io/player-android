package io.beyondwords.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings.LOAD_NO_CACHE
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.core.view.updateLayoutParams
import com.google.gson.Gson
import com.google.gson.GsonBuilder

@SuppressLint("SetJavaScriptEnabled")
class WidgetView @JvmOverloads constructor(
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
    private var playerView: PlayerView? = null
    private val webViewContainer = FrameLayout(context)
    private val webView = WebView(context)
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
                    Log.e("WidgetView:onEvent", "Failed to parse event $event", e)
                    return@post
                }
                playerView?.onWidgetEvent(parsedEvent)
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
        webView.loadDataWithBaseURL(
            "https://beyondwords.io",
            resources.openRawResource(R.raw.widget)
                .bufferedReader()
                .use { it.readText() },
            "text/html",
            "UTF-8",
            null
        )
    }

    fun setPlayerView(playerView: PlayerView) {
        this.playerView = playerView
    }

    fun onPlayerEvent(event: PlayerEvent) {
        callFunction("onPlayerEvent", gson.toJson(event))
    }

    private fun callFunction(name: String, args: String) {
        execCommand(
            """
                try {
                    $name($args)
                } catch (e) {
                    console.error("WidgetView:callFunction", e.message, e)
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
                    console.error("WidgetView:setProp", e.message, e)
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
