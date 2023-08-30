package io.beyondwords.player.segmentsplayback

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import io.beyondwords.player.PlayerView
import java.util.UUID

class SegmentsView(
    context: Context,
    attrs: AttributeSet? = null
): RecyclerView(context, attrs) {
    private lateinit var player: PlayerView

    fun bindPlayer(player: PlayerView) {
        this.player = player
    }

    fun buildSegments(
        text: String,
        delimiter: String = "\n\n",
        markers: Array<String> = arrayOf()
    ) {
        val segments = text.split(delimiter).mapIndexed { idx, marker ->
            Segment(
                marker,
                if (markers.size > idx) markers[idx] else UUID.randomUUID().toString()
            ) {
                if (::player.isInitialized && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    player.setPlaybackState("paused")
                }
            }
        }.toTypedArray()

        this.adapter = SegmentsAdapter(segments)
    }

}