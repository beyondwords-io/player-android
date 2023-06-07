package io.beyondwords.player

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
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
        private const val SUPPORTED_ACTIONS = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO
        private val currentMediaSessionId = AtomicInteger()
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

            override fun onSkipToNext() {
                // TODO
            }

            override fun onSkipToPrevious() {
                // TODO
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

        override fun onSkipToNext() {
            Log.d("MediaSession", "onSkipToNext")
        }

        override fun onSkipToPrevious() {
            Log.d("MediaSession", "onSkipToPrevious")
        }
    }
    private val bridge = object {
        @JavascriptInterface
        fun onMetadataChanged(
            title: String?,
            artist: String?,
            album: String?,
            artworkUrl: String?
        ) {
            coroutineScope.launch {
                if (mediaMetadata?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI) != artworkUrl) {
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
                    artworkUrl
                )
                mediaMetadata = mediaMetadataBuilder.build()
                updateMediaSession()
            }
        }

        @JavascriptInterface
        fun onPositionStateChanged(position: Float, duration: Float, playbackSpeed: Float) {
            coroutineScope.launch {
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
                        playbackState?.state ?: PlaybackStateCompat.STATE_NONE,
                        (position * 1000).toLong(),
                        playbackSpeed
                    )
                    .setActions(SUPPORTED_ACTIONS)
                playbackState = playbackStateBuilder.build()
                mediaMetadata = mediaMetadataBuilder.build()
                updateMediaSession()
            }
        }

        @JavascriptInterface
        fun onPlaybackStateChanged(state: String) {
            coroutineScope.launch {
                val playbackStateBuilder = PlaybackStateCompat.Builder()
                    .setState(
                        when (state) {
                            "playing" -> PlaybackStateCompat.STATE_PLAYING
                            "paused" -> PlaybackStateCompat.STATE_PAUSED
                            else -> PlaybackStateCompat.STATE_NONE
                        },
                        playbackState?.position ?: 0,
                        playbackState?.playbackSpeed ?: 1f
                    )
                    .setActions(SUPPORTED_ACTIONS)
                playbackState = playbackStateBuilder.build()
                updateMediaSession()
            }
        }
    }
    private var mediaMetadata: MediaMetadataCompat? = null
    private var playbackState: PlaybackStateCompat? = null
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
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.cancel(mediaSessionId)
    }

    private fun onPlay() {
        webView.evaluateJavascript(
            """
            navigator.mediaSession._actionHandlers.play()
        """, null
        )
    }

    private fun onPause() {
        webView.evaluateJavascript("""
            navigator.mediaSession._actionHandlers.pause()
        """, null)
    }

    private fun onSeekTo(position: Long) {
        webView.evaluateJavascript("""
            navigator.mediaSession._actionHandlers.seekto({ 
                seekTime: ${position / 1000}
            })
        """, null)
    }

    private fun updateMediaSession() {
        if (mediaMetadata == null || playbackState == null) return
        mediaSession.setMetadata(mediaMetadata)
        mediaSession.setPlaybackState(playbackState)
        mediaSession.isActive = true
        updateNotification()
    }

    private fun updateNotification() {
        val metadata = mediaSession.controller?.metadata
        val playbackState = mediaSession.controller?.playbackState
        if (metadata == null || playbackState == null || playbackState.state == PlaybackStateCompat.STATE_NONE) {
            ContextCompat.getSystemService(context, NotificationManager::class.java)
                ?.cancel(mediaSessionId)
            return
        }

        registerNotificationChannel(context)

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
