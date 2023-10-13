package io.beyondwords.player

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

@RequiresApi(24)
class MediaSession constructor(private val webView: WebView) {
    companion object {
        private const val DEFAULT_NOTIFICATION_CHANNEL_ID = "BeyondWords"
        private val gson: Gson by lazy { GsonBuilder().create() }
        private val currentMediaSessionId = AtomicInteger()

        @JvmStatic
        var notificationChannelId: String? = null

        @JvmStatic
        var notificationProvider = NotificationProvider()

        @JvmStatic
        var verbose: Boolean = false

        private fun ensureNotificationChannel(context: Context) {
            if (notificationChannelId != null) return
            val channelBuilder = NotificationChannelCompat.Builder(
                DEFAULT_NOTIFICATION_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_LOW
            )
            channelBuilder.setName("BeyondWords")
            channelBuilder.setDescription("BeyondWords audio player")
            channelBuilder.setLightsEnabled(false)
            channelBuilder.setShowBadge(false)
            channelBuilder.setVibrationEnabled(false)
            channelBuilder.setSound(null, null)
            val channel = channelBuilder.build()
            NotificationManagerCompat.from(context)
                .createNotificationChannel(channel)
        }

        open class NotificationProvider {
            @SuppressLint("RestrictedApi")
            open fun createNotification(
                context: Context,
                mediaSession: MediaSessionCompat,
                mediaSessionId: Int,
                playbackState: PlaybackStateCompat,
                metadata: MediaMetadataCompat,
                artwork: Bitmap?
            ): NotificationCompat.Builder {
                val notificationBuilder = NotificationCompat.Builder(
                    context,
                    notificationChannelId ?: DEFAULT_NOTIFICATION_CHANNEL_ID
                )
                if (playbackState.actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L) {
                    notificationBuilder.addAction(
                        R.drawable.ic_skip_previous,
                        "Previous",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context,
                            mediaSessionId,
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        )
                    )
                } else if (playbackState.actions and PlaybackStateCompat.ACTION_REWIND != 0L) {
                    notificationBuilder.addAction(
                        R.drawable.ic_rewind,
                        "Rewind",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context,
                            mediaSessionId,
                            PlaybackStateCompat.ACTION_REWIND
                        )
                    )
                }
                var playPauseButtonIndex = -1
                if (playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                    if (playbackState.actions and PlaybackStateCompat.ACTION_PAUSE != 0L) {
                        playPauseButtonIndex = notificationBuilder.mActions.size
                        notificationBuilder.addAction(
                            R.drawable.ic_pause,
                            "Pause",
                            MediaButtonReceiver.buildMediaButtonPendingIntent(
                                context,
                                mediaSessionId,
                                PlaybackStateCompat.ACTION_PAUSE
                            )
                        )
                    }
                } else {
                    if (playbackState.actions and PlaybackStateCompat.ACTION_PLAY != 0L) {
                        playPauseButtonIndex = notificationBuilder.mActions.size
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
                }
                if (playbackState.actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L) {
                    notificationBuilder.addAction(
                        R.drawable.ic_skip_next,
                        "Next",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context,
                            mediaSessionId,
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        )
                    )
                } else if (playbackState.actions and PlaybackStateCompat.ACTION_FAST_FORWARD != 0L) {
                    notificationBuilder.addAction(
                        R.drawable.ic_fast_forward,
                        "Fast forward",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context,
                            mediaSessionId,
                            PlaybackStateCompat.ACTION_FAST_FORWARD
                        )
                    )
                }
                notificationBuilder.setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.sessionToken)
                        .also {
                            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                                if (playPauseButtonIndex != -1 && notificationBuilder.mActions.size > 1) {
                                    it.setShowActionsInCompactView(playPauseButtonIndex)
                                }
                            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                                it.setShowActionsInCompactView(
                                    *List(
                                        notificationBuilder.mActions.size.coerceAtMost(
                                            3
                                        )
                                    ) { index -> index }.toIntArray()
                                )
                            }
                        }
                )
                metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)?.let {
                    notificationBuilder.setContentTitle(it)
                }
                notificationBuilder.setColorized(true)
                notificationBuilder.setSilent(true)
                notificationBuilder.setAutoCancel(false)
                notificationBuilder.setSound(null)
                notificationBuilder.setVibrate(null)
                notificationBuilder.setOngoing(false)
                notificationBuilder.setSmallIcon(R.drawable.ic_volume_up)
                notificationBuilder.setLargeIcon(artwork)
                notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                notificationBuilder.priority = NotificationCompat.PRIORITY_LOW
                return notificationBuilder
            }
        }
    }

    data class SeekToParams(
        val seekTime: Long
    )

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mediaSessionId = currentMediaSessionId.getAndIncrement()
    private val context = webView.context
    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            this@MediaSession.onPlay()
        }

        override fun onPause() {
            this@MediaSession.onPause()
        }

        override fun onFastForward() {
            this@MediaSession.onFastForward()
        }

        override fun onRewind() {
            this@MediaSession.onRewind()
        }

        override fun onSkipToPrevious() {
            this@MediaSession.onSkipToPrevious()
        }

        override fun onSkipToNext() {
            this@MediaSession.onSkipToNext()
        }

        override fun onSeekTo(position: Long) {
            this@MediaSession.onSeekTo(position)
        }
    }
    private val mediaButtonReceiver = object : MediaButtonReceiver(mediaSessionId) {
        override fun onPlay() {
            this@MediaSession.onPlay()
        }

        override fun onPause() {
            this@MediaSession.onPause()
        }

        override fun onFastForward() {
            this@MediaSession.onFastForward()
        }

        override fun onRewind() {
            this@MediaSession.onRewind()
        }

        override fun onSkipToPrevious() {
            this@MediaSession.onSkipToPrevious()
        }

        override fun onSkipToNext() {
            this@MediaSession.onSkipToNext()
        }
    }
    private val bridge = object {
        @JavascriptInterface
        fun onActionHandlersChanged(types: String) {
            if (verbose) println(
                "BeyondWordsMediaSession:onActionHandlersChanged: " +
                        types.lines().joinToString("")
            )
            val parsedTypes: List<String>
            try {
                parsedTypes = gson.fromJson(types, object : TypeToken<List<String>>() {}.type)
            } catch (e: Exception) {
                Log.e("BeyondWordsMediaSession", "onActionHandlersChanged: Failed to parse types $types", e)
                return
            }

            coroutineScope.launch {
                val mediaSession = this@MediaSession.mediaSession ?: return@launch
                val playbackState = mediaSession.controller?.playbackState
                val playbackStateBuilder = playbackState?.let {
                    PlaybackStateCompat.Builder(it)
                } ?: run {
                    PlaybackStateCompat.Builder()
                }
                val actions = parsedTypes.fold(0L) { actions, type ->
                    actions or when (type) {
                        "play" -> PlaybackStateCompat.ACTION_PLAY
                        "pause" -> PlaybackStateCompat.ACTION_PAUSE
                        "seekto" -> PlaybackStateCompat.ACTION_SEEK_TO
                        "seekbackward" -> PlaybackStateCompat.ACTION_REWIND or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS else 0L
                        "seekforward" -> PlaybackStateCompat.ACTION_FAST_FORWARD or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) PlaybackStateCompat.ACTION_SKIP_TO_NEXT else 0L
                        "previoustrack" -> PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        "nexttrack" -> PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        else -> 0L
                    }
                }
                playbackStateBuilder.setActions(actions)
                mediaSession.setPlaybackState(playbackStateBuilder.build())
                updateNotification()
            }
        }

        @JavascriptInterface
        fun onMetadataChanged(
            title: String?,
            artist: String?,
            album: String?,
            artworkUrl: String?
        ) {
            if (verbose) println(
                "BeyondWordsMediaSession:onMetadataChanged: " +
                        "${title?.lines()?.joinToString("")} " +
                        "${artist?.lines()?.joinToString("")} " +
                        "${album?.lines()?.joinToString("")} " +
                        "${artworkUrl?.lines()?.joinToString("")}"
            )
            coroutineScope.launch {
                val mediaSession = this@MediaSession.mediaSession ?: return@launch
                val metadata = mediaSession.controller?.metadata
                if (metadata?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI) != artworkUrl) {
                    artwork = null
                    downloadArtworkJob?.cancel()
                    downloadArtworkJob = null
                    artworkUrl?.let {
                        downloadArtworkJob = coroutineScope.launch {
                            artwork = withContext(Dispatchers.IO) {
                                try {
                                    return@withContext BitmapFactory.decodeStream(URL(it).openStream())
                                } catch (e: Exception) {
                                    return@withContext null
                                }
                            }
                            updateNotification()
                        }
                    }
                }
                val metadataBuilder = metadata?.let {
                    MediaMetadataCompat.Builder(it)
                } ?: run {
                    MediaMetadataCompat.Builder()
                }
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                metadataBuilder.putString(
                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                    artworkUrl
                )
                mediaSession.setMetadata(metadataBuilder.build())
                updateNotification()
            }
        }

        @JavascriptInterface
        fun onPositionStateChanged(position: Float, duration: Float, playbackSpeed: Float) {
            if (verbose) println("BeyondWordsMediaSession:onPositionStateChanged: $position $duration $playbackSpeed")
            coroutineScope.launch {
                val mediaSession = this@MediaSession.mediaSession ?: return@launch
                val metadata = mediaSession.controller?.metadata
                val metadataBuilder = metadata?.let {
                    MediaMetadataCompat.Builder(it)
                } ?: run {
                    MediaMetadataCompat.Builder()
                }
                metadataBuilder.putLong(
                    MediaMetadataCompat.METADATA_KEY_DURATION,
                    (duration * 1000).toLong()
                )
                val playbackState = mediaSession.controller?.playbackState
                val playbackStateBuilder = playbackState?.let {
                    PlaybackStateCompat.Builder(it)
                } ?: run {
                    PlaybackStateCompat.Builder()
                }
                playbackStateBuilder.setState(
                    playbackState?.state ?: PlaybackStateCompat.STATE_NONE,
                    (position * 1000).toLong(),
                    playbackSpeed
                )
                mediaSession.setMetadata(metadataBuilder.build())
                mediaSession.setPlaybackState(playbackStateBuilder.build())
                updateNotification()
            }
        }

        @JavascriptInterface
        fun onPlaybackStateChanged(state: String) {
            if (verbose) println("BeyondWordsMediaSession:onPlaybackStateChanged: $state")
            coroutineScope.launch {
                val mediaSession = this@MediaSession.mediaSession ?: return@launch
                val playbackState = mediaSession.controller?.playbackState
                val playbackStateBuilder = playbackState?.let {
                    PlaybackStateCompat.Builder(it)
                } ?: run {
                    PlaybackStateCompat.Builder()
                }
                playbackStateBuilder.setState(
                    when (state) {
                        "playing" -> PlaybackStateCompat.STATE_PLAYING
                        "paused" -> PlaybackStateCompat.STATE_PAUSED
                        else -> PlaybackStateCompat.STATE_NONE
                    },
                    playbackState?.position ?: 0,
                    playbackState?.playbackSpeed ?: 1f
                )
                playbackStateBuilder.build().let {
                    mediaSession.setPlaybackState(it)
                    mediaSession.isActive = it.state != PlaybackStateCompat.STATE_NONE
                }
                updateNotification()
            }
        }
    }
    private var mediaSession: MediaSessionCompat? = null
    private var artwork: Bitmap? = null
    private var downloadArtworkJob: Job? = null

    init {
        if (verbose) println("BeyondWordsMediaSession:init")
        mediaSession = MediaSessionCompat(context, "BeyondWords").apply {
            setCallback(mediaSessionCallback)
        }
        context.registerReceiver(mediaButtonReceiver, IntentFilter(Intent.ACTION_MEDIA_BUTTON))
        webView.addJavascriptInterface(bridge, "MediaSessionBridge")
    }

    fun release() {
        if (verbose) println("BeyondWordsMediaSession:release")
        context.unregisterReceiver(mediaButtonReceiver)
        webView.removeJavascriptInterface("MediaSessionBridge")
        coroutineScope.cancel()
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        cancelNotification()
    }

    private fun onPlay() {
        if (verbose) println("BeyondWordsMediaSession:onPlay")
        this@MediaSession.mediaSession ?: return
        onAction("play")
    }

    private fun onPause() {
        if (verbose) println("BeyondWordsMediaSession:onPause")
        this@MediaSession.mediaSession ?: return
        onAction("pause")
    }

    private fun onFastForward() {
        if (verbose) println("BeyondWordsMediaSession:onFastForward")
        this@MediaSession.mediaSession ?: return
        onAction("seekforward", listOf(object {}))
    }

    private fun onRewind() {
        if (verbose) println("BeyondWordsMediaSession:onRewind")
        this@MediaSession.mediaSession ?: return
        onAction("seekbackward", listOf(object {}))
    }

    private fun onSkipToPrevious() {
        if (verbose) println("BeyondWordsMediaSession:onSkipToPrevious")
        val mediaSession = this@MediaSession.mediaSession ?: return
        val actions = mediaSession.controller?.playbackState?.actions ?: 0L
        if (actions and PlaybackStateCompat.ACTION_REWIND != 0L) {
            onAction("seekbackward", listOf(object {}))
        } else {
            onAction("previoustrack")
        }
    }

    private fun onSkipToNext() {
        if (verbose) println("BeyondWordsMediaSession:onSkipToNext")
        val mediaSession = this@MediaSession.mediaSession ?: return
        val actions = mediaSession.controller?.playbackState?.actions ?: 0L
        if (actions and PlaybackStateCompat.ACTION_FAST_FORWARD != 0L) {
            onAction("seekforward", listOf(object {}))
        } else {
            onAction("nexttrack")
        }
    }

    private fun onSeekTo(position: Long) {
        if (verbose) println("BeyondWordsMediaSession:onSeekTo")
        this@MediaSession.mediaSession ?: return
        onAction("seekto", listOf(SeekToParams(position / 1000)))
    }

    private fun onAction(action: String, args: List<Any> = listOf()) {
        webView.evaluateJavascript(
            """
            try {
                navigator.mediaSession._actionHandlers["$action"](
                    ${args.map { gson.toJson(it) }.joinToString(",") { it }}
                )
            } catch (e) {
                console.error("BeyondWordsMediaSession:onAction:" + e.message, e)
            }
        """, null
        )
    }

    private fun cancelNotification() {
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.cancel(mediaSessionId)
    }

    private fun updateNotification() {
        val mediaSession = this@MediaSession.mediaSession ?: return
        val metadata = mediaSession.controller?.metadata
        val playbackState = mediaSession.controller?.playbackState
        if (!mediaSession.isActive || metadata == null || playbackState == null || playbackState.state == PlaybackStateCompat.STATE_NONE) {
            cancelNotification()
            return
        }

        ensureNotificationChannel(context)
        val notification = notificationProvider.createNotification(
            context,
            mediaSession,
            mediaSessionId,
            playbackState,
            metadata,
            artwork
        ).build()
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.notify(mediaSessionId, notification)
    }
}
