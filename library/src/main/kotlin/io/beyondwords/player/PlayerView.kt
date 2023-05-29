package io.beyondwords.player

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
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
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.util.concurrent.atomic.AtomicInteger

//https://developer.android.com/about/versions/oreo/android-8.0-changes.html
//https://developer.android.com/about/versions/13/behavior-changes-13#playback-controls
@SuppressLint("SetJavaScriptEnabled")
class PlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
    companion object {
        const val DEFAULT_NOTIFICATION_CHANNEL_ID = "BeyondWords"

        private val currentMediaSessionId = AtomicInteger(0)
        private val gson: Gson by lazy { GsonBuilder().create() }

        var notificationChannelId: String? = null

        private fun registerNotificationChannel(context: Context) {
            if (notificationChannelId != null) return
            val channel = NotificationChannelCompat.Builder(
                DEFAULT_NOTIFICATION_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_LOW
            )
                .setName("BeyondWords")
                .setDescription("BeyondWords audio player")
                .setLightsEnabled(false)
                .setShowBadge(false)
                .setVibrationEnabled(false)
                .setSound(null, null)
                .build()
            NotificationManagerCompat.from(context)
                .createNotificationChannel(channel)
        }
    }

    private var ready = false
    private val mediaSessionId = currentMediaSessionId.getAndIncrement()
    private var mediaMetadata: MediaMetadataCompat? = null
    private var playbackState: PlaybackStateCompat? = null
    private var mediaSession: MediaSessionCompat? = null
    private var mediaButtonReceiver: MediaButtonReceiver? = null
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
        fun onEvent(event: String) {
            val parsedEvent: PlayerEvent
            try {
                parsedEvent = gson.fromJson(event, PlayerEvent::class.java)
            } catch (e: Exception) {
                Log.e("PlayerView:onEvent", "Failed to parse event $event", e)
                return
            }

            post {
                listeners.forEach {
                    when (parsedEvent.type) {
                        "PressedPlay" -> it.onPressedPlay(parsedEvent)
                    }
                    it.onAny(parsedEvent)
                }
            }
        }

        @JavascriptInterface
        fun onMetadataChanged(title: String?, artist: String?, album: String?, artwork: String?) {
            post {
                val mediaMetadataBuilder = mediaMetadata?.let {
                    MediaMetadataCompat.Builder(it)
                } ?: run {
                    MediaMetadataCompat.Builder()
                }
                mediaMetadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                mediaMetadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                mediaMetadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                mediaMetadataBuilder.putString(
                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                    artwork
                )
                mediaMetadata = mediaMetadataBuilder.build()
                updateMediaSession()
            }
        }

        @JavascriptInterface
        fun onPlaybackStateChanged(
            state: String,
            position: Float,
            duration: Float,
            playbackSpeed: Float
        ) {
            post {
                val mediaMetadataBuilder = mediaMetadata?.let {
                    MediaMetadataCompat.Builder(it)
                } ?: run {
                    MediaMetadataCompat.Builder()
                }
                mediaMetadataBuilder.putLong(
                    MediaMetadataCompat.METADATA_KEY_DURATION,
                    (duration * 1000).toLong()
                )
                val playbackStateBuilder = PlaybackStateCompat.Builder()
                    .setState(
                        when (state) {
                            "playing" -> PlaybackStateCompat.STATE_PLAYING
                            "paused" -> PlaybackStateCompat.STATE_PAUSED
                            else -> PlaybackStateCompat.STATE_NONE
                        },
                        (position * 1000).toLong(),
                        playbackSpeed
                    )
                    .setActions(
                        PlaybackStateCompat.ACTION_PLAY or
                                PlaybackStateCompat.ACTION_PAUSE or
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                                PlaybackStateCompat.ACTION_SEEK_TO
                    )
                playbackState = playbackStateBuilder.build()
                mediaMetadata = mediaMetadataBuilder.build()
                updateMediaSession()
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

    init {
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateMediaSession()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mediaButtonReceiver?.let {
            context.unregisterReceiver(it)
        }
        mediaButtonReceiver = null
        mediaSession?.release()
        mediaSession = null
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.cancel(mediaSessionId)
    }

    fun addEventListener(listener: EventListener) {
        listeners.add(listener)
    }

    fun removeEventListener(listener: EventListener) {
        listeners.remove(listener)
    }

    fun createPlayer(playerSettings: PlayerSettings) {
        callFunction("createPlayer", listOf(playerSettings))
    }

    fun setPlayerStyle(playerStyle: String) {
        setProp("player.playerStyle", playerStyle)
    }

    private fun callFunction(name: String, args: List<Any>) {
        exec(
            """
                try {
                    $name(${args.map { gson.toJson(it) }.joinToString(",") { it }})
                } catch (e) {
                    console.error("PlayerView:callFunction", e.message, e)
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
                    console.error("PlayerView:setProp", e.message, e)
                }
            """
        )
    }

    private fun exec(command: String) {
        if (!ready) {
            pendingCommands.add(command)
        } else {
            webView.evaluateJavascript(command, null)
        }
    }

    private fun updateMediaSession() {
        if (!isAttachedToWindow || mediaMetadata == null || playbackState == null) return
        if (mediaButtonReceiver == null) {
            mediaButtonReceiver = object : MediaButtonReceiver(mediaSessionId) {
                override fun onPlay() {
                    callFunction("navigator.mediaSession._actionHandlers.play", listOf())
                }

                override fun onPause() {
                    callFunction("navigator.mediaSession._actionHandlers.pause", listOf())
                }

                override fun onSkipToNext() {
                    Log.d("MediaSession", "onSkipToNext")
                }

                override fun onSkipToPrevious() {
                    Log.d("MediaSession", "onSkipToPrevious")
                }
            }
            context.registerReceiver(mediaButtonReceiver, IntentFilter(Intent.ACTION_MEDIA_BUTTON))
        }
        if (mediaSession == null) {
            mediaSession = MediaSessionCompat(context, "BeyondWords")
            mediaSession!!.setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    callFunction("navigator.mediaSession._actionHandlers.play", listOf())
                }

                override fun onPause() {
                    callFunction("navigator.mediaSession._actionHandlers.pause", listOf())
                }

                override fun onSkipToNext() {
                    Log.d("MediaSession", "onSkipToNext")
                }

                override fun onSkipToPrevious() {
                    Log.d("MediaSession", "onSkipToPrevious")
                }

                override fun onSeekTo(position: Long) {
                    callFunction("navigator.mediaSession._actionHandlers.seekto", listOf(object {
                        val seekTime = position / 1000
                    }))
                }
            })
        }
        mediaSession!!.setMetadata(mediaMetadata)
        mediaSession!!.setPlaybackState(playbackState)
        mediaSession!!.isActive = true
        registerNotificationChannel(context)
        updateNotification()
    }

    private fun updateNotification() {
        val metadata = mediaSession?.controller?.metadata ?: return
        val playbackState = mediaSession?.controller?.playbackState ?: return

        if (playbackState.state != PlaybackStateCompat.STATE_PLAYING &&
            playbackState.state != PlaybackStateCompat.STATE_PAUSED
        ) return

        val notificationBuilder = NotificationCompat.Builder(
            context,
            notificationChannelId ?: DEFAULT_NOTIFICATION_CHANNEL_ID
        )
        notificationBuilder.setSmallIcon(R.drawable.ic_volume_up)
        metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)?.let {
            notificationBuilder.setContentTitle(it)
        }
        notificationBuilder.addAction(
            R.drawable.ic_rewind,
            "Rewind",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context,
                mediaSessionId,
                PlaybackStateCompat.ACTION_REWIND
            )
        )
        if (playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
            notificationBuilder.addAction(
                R.drawable.ic_pause,
                "Pause",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    mediaSessionId,
                    PlaybackStateCompat.ACTION_PAUSE
                )
            )
        } else {
            notificationBuilder.addAction(
                R.drawable.ic_play,
                "Play",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    mediaSessionId,
                    PlaybackStateCompat.ACTION_PLAY
                )
            )
        }
        notificationBuilder.addAction(
            R.drawable.ic_fast_forward,
            "Fast forward",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context,
                mediaSessionId,
                PlaybackStateCompat.ACTION_FAST_FORWARD
            )
        )
        notificationBuilder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1)
                .setMediaSession(mediaSession?.sessionToken)
        )
        notificationBuilder.setColorized(true)
        notificationBuilder.setSilent(true)
        notificationBuilder.setAutoCancel(false)
        notificationBuilder.setSound(null)
        notificationBuilder.setVibrate(null)
        notificationBuilder.setOngoing(true)
        notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        notificationBuilder.priority = NotificationCompat.PRIORITY_LOW
        val notification = notificationBuilder.build()
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.notify(mediaSessionId, notification)
    }
}
