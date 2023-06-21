package io.beyondwords.example

import android.app.Application
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import io.beyondwords.player.MediaSession

class MainApplication : Application() {
    companion object {
        private const val AUDIO_PLAYER_NOTIFICATION_CHANNEL_ID = "AudioPlayer"
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val channelBuilder = NotificationChannelCompat.Builder(
                AUDIO_PLAYER_NOTIFICATION_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_LOW
            )
            channelBuilder.setName("Audio Player")
            channelBuilder.setDescription("Audio player notification channel description")
            val channel = channelBuilder.build()
            NotificationManagerCompat.from(this)
                .createNotificationChannel(channel)
            MediaSession.notificationChannelId = AUDIO_PLAYER_NOTIFICATION_CHANNEL_ID
        }
    }
}
