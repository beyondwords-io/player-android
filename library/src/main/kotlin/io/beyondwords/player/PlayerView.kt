package io.beyondwords.player

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.webkit.DownloadListener
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings.LOAD_DEFAULT
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@RequiresApi(24)
@SuppressLint("SetJavaScriptEnabled")
class PlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
    companion object {
        private val gson: Gson by lazy { GsonBuilder().create() }
        @JvmStatic
        var verbose: Boolean = false
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val webViewContainer = FrameLayout(context)
    private val listeners = mutableSetOf<EventListener>()
    private val pendingCommands = mutableListOf<String>()
    private val bridge = object {
        @JavascriptInterface
        fun onReady() {
            if (verbose) println("BeyondWordsPlayer:onReady")
            coroutineScope.launch {
                val webView = this@PlayerView.webView ?: return@launch
                ready = true
                pendingCommands.forEach {
                    webView.evaluateJavascript(it, null)
                }
                pendingCommands.clear()
            }
        }

        @Suppress("UNUSED_PARAMETER")
        @JavascriptInterface
        fun onResize(width: Float, height: Float) {
            if (verbose) println("BeyondWordsPlayer:onResize: $width $height")
            coroutineScope.launch {
                this@PlayerView.webView ?: return@launch
                webViewContainer.updateLayoutParams {
                    this.height = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        height,
                        resources.displayMetrics
                    ).toInt()
                }
            }
        }

        @JavascriptInterface
        fun onEvent(event: String, settings: String) {
            if (verbose) {
                println("BeyondWordsPlayer:onEvent: ${event.lines().joinToString("")}")
                println("BeyondWordsPlayer:onEvent: ${settings.lines().joinToString("")}")
            }
            val parsedEvent: PlayerEvent
            try {
                parsedEvent = gson.fromJson(event, object : TypeToken<PlayerEvent>() {}.type)
            } catch (e: Exception) {
                Log.e("BeyondWordsPlayer", "onEvent: Failed to parse event $event", e)
                return
            }

            val parsedSettings: PlayerSettings
            try {
                parsedSettings =
                    gson.fromJson(settings, object : TypeToken<PlayerSettings>() {}.type)
            } catch (e: Exception) {
                Log.e("BeyondWordsPlayer", "onEvent: Failed to parse settings $settings", e)
                return
            }

            coroutineScope.launch {
                this@PlayerView.webView ?: return@launch
                listeners.forEach { it.onEvent(parsedEvent, parsedSettings) }
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
    private var ready: Boolean = false
    var webView: WebView? = null
    var mediaSession: MediaSession? = null

    init {
        if (verbose) println("BeyondWordsPlayer:init")
        addView(webViewContainer, LayoutParams(LayoutParams.MATCH_PARENT, 0))
        webView = WebView(context).also {
            webViewContainer.addView(
                it,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            )
            it.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                databaseEnabled = false
                cacheMode = LOAD_DEFAULT
                mediaPlaybackRequiresUserGesture = false
                builtInZoomControls = false
                displayZoomControls = false
                loadWithOverviewMode = false
                setSupportZoom(false)
                setGeolocationEnabled(false)
                setSupportMultipleWindows(false)
            }
            it.webViewClient = webViewClient
            it.setDownloadListener(downloadListener)
            it.setBackgroundColor(Color.TRANSPARENT)
            it.addJavascriptInterface(bridge, "PlayerViewBridge")
            mediaSession = MediaSession(it)
            it.loadDataWithBaseURL(
                "https://beyondwords.io",
                resources.openRawResource(R.raw.player)
                    .bufferedReader()
                    .use { it.readText() },
                "text/html",
                "UTF-8",
                null
            )
        }
    }

    fun release() {
        if (verbose) println("BeyondWordsPlayer:release")
        ready = false
        listeners.clear()
        pendingCommands.clear()
        coroutineScope.cancel()
        mediaSession?.release()
        mediaSession = null
        webViewContainer.removeAllViews()
        webViewContainer.updateLayoutParams {
            this.height = 0
        }
        webView?.removeJavascriptInterface("PlayerViewBridge")
        webView?.destroy()
        webView = null
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

    fun setPlayerApiUrl(playerApiUrl: String) {
        setProp("player.playerApiUrl", playerApiUrl)
    }

    fun setProjectId(projectId: Int) {
        setProp("player.projectId", projectId)
    }

    fun setContentId(contentId: String) {
        setProp("player.contentId", contentId)
    }

    fun setPlaylistId(playlistId: Int) {
        setProp("player.playlistId", playlistId)
    }

    fun setSourceId(sourceId: String) {
        setProp("player.sourceId", sourceId)
    }

    fun setSourceUrl(sourceUrl: String) {
        setProp("player.sourceUrl", sourceUrl)
    }

    fun setPlaylist(playlist: List<PlayerSettings.Identifier>) {
        setProp("player.playlist", playlist)
    }

    fun setShowUserInterface(showUserInterface: Boolean) {
        setProp("player.showUserInterface", showUserInterface)
    }

    fun setPlayerStyle(playerStyle: String) {
        callFunction("setPlayerStyle", listOf(playerStyle))
    }

    fun setPlayerTitle(playerTitle: String) {
        setProp("player.playerTitle", playerTitle)
    }

    fun setCallToAction(callToAction: String) {
        setProp("player.callToAction", callToAction)
    }

    fun setSkipButtonStyle(skipButtonStyle: String) {
        setProp("player.skipButtonStyle", skipButtonStyle)
    }

    fun setPlaylistStyle(playlistStyle: String) {
        setProp("player.playlistStyle", playlistStyle)
    }

    fun setPlaylistToggle(playlistToggle: String) {
        setProp("player.playlistToggle", playlistToggle)
    }

    fun setMediaSession(mediaSession: String) {
        setProp("player.mediaSession", mediaSession)
    }

    fun setContent(content: List<PlayerSettings.Content>) {
        setProp("player.content", content)
    }

    fun setContentIndex(contentIndex: Int) {
        setProp("player.contentIndex", contentIndex)
    }

    fun setIntrosOutros(introsOutros: List<PlayerSettings.IntroOutro>) {
        setProp("player.introsOutros", introsOutros)
    }

    fun setIntrosOutrosIndex(introsOutrosIndex: Int) {
        setProp("player.introsOutrosIndex", introsOutrosIndex)
    }

    fun setAdverts(adverts: List<PlayerSettings.Advert>) {
        setProp("player.adverts", adverts)
    }

    fun setAdvertIndex(advertIndex: Int) {
        setProp("player.advertIndex", advertIndex)
    }

    fun setMinDurationForMidroll(minDurationForMidroll: Float) {
        setProp("player.minDurationForMidroll", minDurationForMidroll)
    }

    fun setMinTimeUntilEndForMidroll(minTimeUntilEndForMidroll: Float) {
        setProp("player.minTimeUntilEndForMidroll", minTimeUntilEndForMidroll)
    }

    fun setPersistentAdImage(persistentAdImage: Boolean) {
        setProp("player.persistentAdImage", persistentAdImage)
    }

    fun setPersistentIndex(persistentIndex: Int) {
        setProp("player.persistentIndex", persistentIndex)
    }

    fun setCurrentTime(currentTime: Float) {
        setProp("player.currentTime", currentTime)
    }

    fun setPlaybackState(playbackState: String) {
        setProp("player.playbackState", playbackState)
    }

    fun setPlaybackRate(playbackRate: Float) {
        setProp("player.playbackRate", playbackRate)
    }

    fun setTextColor(textColor: String) {
        setProp("player.textColor", textColor)
    }

    fun setBackgroundColor(backgroundColor: String) {
        setProp("player.backgroundColor", backgroundColor)
    }

    fun setIconColor(iconColor: String) {
        setProp("player.iconColor", iconColor)
    }

    fun setLogoIconEnabled(logoIconEnabled: Boolean) {
        setProp("player.logoIconEnabled", logoIconEnabled)
    }

    fun setAdvertConsent(advertConsent: String) {
        setProp("player.advertConsent", advertConsent)
    }

    fun setAnalyticsConsent(analyticsConsent: String) {
        setProp("player.analyticsConsent", analyticsConsent)
    }

    fun setAnalyticsCustomUrl(analyticsCustomUrl: String) {
        setProp("player.analyticsCustomUrl", analyticsCustomUrl)
    }

    fun setAnalyticsTag(analyticsTag: String) {
        setProp("player.analyticsTag", analyticsTag)
    }

    fun setCaptureErrors(captureErrors: Boolean) {
        setProp("player.captureErrors", captureErrors)
    }

    private fun callFunction(name: String, args: List<Any>) {
        exec("""
            try {
                $name(${args.map { gson.toJson(it) }.joinToString(",") { it }})
            } catch (e) {
                console.error("PlayerView:callFunction:" + e.message, e)
            }
        """
        )
    }

    private fun setProp(name: String, value: Any) {
        exec(
            """
            try {
                $name = ${gson.toJson(value)}
            } catch (e) {
                console.error("PlayerView:setProp:" + e.message, e)
            }
        """
        )
    }

    private fun exec(command: String) {
        if (verbose) println("BeyondWordsPlayer:exec: ${command.lines().joinToString("")}")
        val webView = this.webView ?: return
        if (!ready) {
            pendingCommands.add(command)
        } else {
            webView.evaluateJavascript(command, null)
        }
    }
}
