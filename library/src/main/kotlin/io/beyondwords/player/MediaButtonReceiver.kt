package io.beyondwords.player

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import androidx.annotation.RequiresApi

open class MediaButtonReceiver @RequiresApi(24) constructor(private val mediaSessionId: Int) :
    BroadcastReceiver() {
    companion object {
        const val EXTRA_MEDIA_SESSION_ID = "io.beyondwords.player.MEDIA_SESSION_ID"

        @JvmStatic
        fun buildMediaButtonPendingIntent(
            context: Context,
            mediaSessionId: Int,
            action: Long
        ): PendingIntent? {
            val keyCode = PlaybackStateCompat.toKeyCode(action)
            if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
                return null
            }

            val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
            intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            intent.putExtra(EXTRA_MEDIA_SESSION_ID, mediaSessionId)
            val requestCode = mediaSessionId * 1000 + keyCode // keyCode is a 3 digit number
            val flags = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            return PendingIntent.getBroadcast(context, requestCode, intent, flags)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent === null
            || intent.action != Intent.ACTION_MEDIA_BUTTON
            || !intent.hasExtra(Intent.EXTRA_KEY_EVENT)
            || intent.getIntExtra(EXTRA_MEDIA_SESSION_ID, 0) != mediaSessionId
        ) return
        val event: KeyEvent? = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT) as KeyEvent?
        when (event?.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY -> onPlay()
            KeyEvent.KEYCODE_MEDIA_PAUSE -> onPause()
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> onSkipToPrevious()
            KeyEvent.KEYCODE_MEDIA_NEXT -> onSkipToNext()
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> onFastForward()
            KeyEvent.KEYCODE_MEDIA_REWIND -> onRewind()
        }
    }

    open fun onPlay() {}

    open fun onPause() {}

    open fun onSkipToNext() {}

    open fun onSkipToPrevious() {}

    open fun onFastForward() {}

    open fun onRewind() {}
}
