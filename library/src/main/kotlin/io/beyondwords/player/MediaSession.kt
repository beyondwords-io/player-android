package io.beyondwords.player

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

class MediaSession(private val webView: WebView) {
    companion object {
        private const val DEFAULT_NOTIFICATION_CHANNEL_ID = "BeyondWords"
        private val currentMediaSessionId = AtomicInteger()

        @JvmStatic
        var notificationChannelId: String? = null

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
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mediaSessionId = currentMediaSessionId.getAndIncrement()
    private val context = webView.context
    private val mediaSession = MediaSessionCompat(context, "BeyondWords").apply {
        setCallback(object : MediaSessionCompat.Callback() {
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

            override fun onSeekTo(position: Long) {
                this@MediaSession.onSeekTo(position)
            }
        })
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
    }
    private val bridge = object {
        @JavascriptInterface
        fun onActionHandlerChanged(type: String, attached: Boolean) {
            coroutineScope.launch {
                val playbackState = mediaSession.controller?.playbackState
                val playbackStateBuilder = playbackState?.let {
                    PlaybackStateCompat.Builder(it)
                } ?: run {
                    PlaybackStateCompat.Builder()
                }
                val action = when (type) {
                    "play" -> PlaybackStateCompat.ACTION_PLAY
                    "pause" -> PlaybackStateCompat.ACTION_PAUSE
                    "seekto" -> PlaybackStateCompat.ACTION_SEEK_TO
                    "seekbackward" -> PlaybackStateCompat.ACTION_REWIND
                    "seekforward" -> PlaybackStateCompat.ACTION_FAST_FORWARD
                    else -> return@launch
                }
                if (attached) {
                    playbackStateBuilder.setActions((playbackState?.actions ?: 0L) or action)
                } else {
                    playbackStateBuilder.setActions((playbackState?.actions ?: 0L) and action.inv())
                }
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
            coroutineScope.launch {
                val metadata = mediaSession.controller?.metadata
                if (metadata?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI) != artworkUrl) {
                    artwork = null
                    downloadArtworkJob?.cancel()
                    downloadArtworkJob = null
                    artworkUrl?.let {
                        downloadArtworkJob = coroutineScope.launch {
                            artwork = withContext(Dispatchers.IO) {
                                BitmapFactory.decodeStream(URL(it).openStream())
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
            coroutineScope.launch {
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
            coroutineScope.launch {
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
    private var artwork: Bitmap? = null
    private var downloadArtworkJob: Job? = null

    init {
        context.registerReceiver(mediaButtonReceiver, IntentFilter(Intent.ACTION_MEDIA_BUTTON))
        webView.addJavascriptInterface(bridge, "MediaSessionBridge")
    }

    fun release() {
        context.unregisterReceiver(mediaButtonReceiver)
        webView.removeJavascriptInterface("MediaSessionBridge")
        coroutineScope.cancel()
        mediaSession.release()
        updateNotification()
    }

    private fun onPlay() {
        onAction("play")
    }

    private fun onPause() {
        onAction("pause")
    }

    private fun onFastForward() {
        onAction("seekforward")
    }

    private fun onRewind() {
        onAction("seekbackward")
    }

    private fun onSeekTo(position: Long) {
        onAction("seekto", "{ seekTime: ${position / 1000} }")
    }

    private fun onAction(action: String, args: String = "{}") {
        webView.evaluateJavascript(
            """
            try {
                navigator.mediaSession._actionHandlers["$action"]($args)
            } catch (e) {
                console.error("MediaSession:onAction:" + e.message, e)
            }
        """, null
        )
    }

    @SuppressLint("RestrictedApi")
    private fun updateNotification() {
        val metadata = mediaSession.controller?.metadata
        val playbackState = mediaSession.controller?.playbackState
        if (metadata == null || playbackState == null || playbackState.state == PlaybackStateCompat.STATE_NONE) {
            ContextCompat.getSystemService(context, NotificationManager::class.java)
                ?.cancel(mediaSessionId)
            return
        }

        ensureNotificationChannel(context)

        val notificationBuilder = NotificationCompat.Builder(
            context,
            notificationChannelId ?: DEFAULT_NOTIFICATION_CHANNEL_ID
        )
        notificationBuilder.setSmallIcon(R.drawable.ic_volume_up)
        metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)?.let {
            notificationBuilder.setContentTitle(it)
        }
        if (playbackState.actions and PlaybackStateCompat.ACTION_REWIND != 0L) {
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
        if (playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
            if (playbackState.actions and PlaybackStateCompat.ACTION_PAUSE != 0L) {
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
        if (playbackState.actions and PlaybackStateCompat.ACTION_FAST_FORWARD != 0L) {
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
                .setShowActionsInCompactView(*List(notificationBuilder.mActions.size) { index -> index }
                    .toIntArray())
                .setMediaSession(mediaSession.sessionToken)
        )
        notificationBuilder.setColorized(true)
        notificationBuilder.setSilent(true)
        notificationBuilder.setAutoCancel(false)
        notificationBuilder.setSound(null)
        notificationBuilder.setVibrate(null)
        notificationBuilder.setOngoing(true)
        notificationBuilder.setLargeIcon(artwork)
        notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        notificationBuilder.priority = NotificationCompat.PRIORITY_LOW
        val notification = notificationBuilder.build()
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.notify(mediaSessionId, notification)
    }
}
