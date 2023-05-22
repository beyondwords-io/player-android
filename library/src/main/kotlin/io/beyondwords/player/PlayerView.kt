package io.beyondwords.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebSettings.LOAD_NO_CACHE
import android.webkit.WebView
import android.widget.FrameLayout
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.lang.Exception

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
            Log.d("PlayerView:onReady", "pendingCommands: $pendingCommands")
            ready = true
            pendingCommands.forEach {
                webView.evaluateJavascript(it, null)
            }
            pendingCommands.clear()
        }

        @JavascriptInterface
        fun onEvent(event: String) {
            val parsedEvent: PlayerEvent
            try {
                parsedEvent = gson.fromJson(event, PlayerEvent::class.java)
            } catch (e: Exception) {
                Log.e("PlayerView:onEvent", "Failed to parse event $event", e)
                return
            }
            listeners.forEach {
                when (parsedEvent.type) {
                    "PressedPlay" -> it.onPressedPlay(parsedEvent)
                }
                it.onAny(parsedEvent)
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
            setSupportZoom(false)
            setGeolocationEnabled(false)
            setSupportMultipleWindows(false)
        }

        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.addJavascriptInterface(bridge, "AndroidBridge")
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
                console.error("PlayerView:load", e)
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
