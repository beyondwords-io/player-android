package io.beyondwords.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.webkit.WebView
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.beyondwords.player.MediaSession
import io.beyondwords.player.PlayerView

class MainApplication : Application() {
    companion object {
        private const val AUDIO_PLAYER_NOTIFICATION_CHANNEL_ID = "AudioPlayer"
    }

    override fun onCreate() {
        super.onCreate()
        WebView.setWebContentsDebuggingEnabled(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PlayerView.verbose = true
            MediaSession.verbose = true
            val channelBuilder = NotificationChannelCompat.Builder(
                AUDIO_PLAYER_NOTIFICATION_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_LOW
            )
            channelBuilder.setName("Audio Player")
            channelBuilder.setDescription("Audio player notification channel description")
            val channel = channelBuilder.build()
            NotificationManagerCompat.from(this)
                .createNotificationChannel(channel)
            // Override the default BeyondWords notification channel
            MediaSession.notificationChannelId = AUDIO_PLAYER_NOTIFICATION_CHANNEL_ID
            // Override the default BeyondWords notification provider
            MediaSession.notificationProvider = object :
                MediaSession.Companion.NotificationProvider() {
                override fun createNotification(
                    context: Context,
                    mediaSession: android.support.v4.media.session.MediaSessionCompat,
                    mediaSessionId: Int,
                    playbackState: android.support.v4.media.session.PlaybackStateCompat,
                    metadata: android.support.v4.media.MediaMetadataCompat,
                    artwork: Bitmap?
                ): NotificationCompat.Builder {
                    val notificationBuilder = super.createNotification(
                        context,
                        mediaSession,
                        mediaSessionId,
                        playbackState,
                        metadata,
                        artwork
                    )
                    // You can override all of the notification properties here
                    // notificationBuilder.setSmallIcon(io.beyondwords.player.R.drawable.ic_play)
                    return notificationBuilder
                }
            }
        }
    }
}
