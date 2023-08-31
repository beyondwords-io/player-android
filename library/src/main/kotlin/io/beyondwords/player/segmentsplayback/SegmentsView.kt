package io.beyondwords.player.segmentsplayback

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import io.beyondwords.player.EventListener
import io.beyondwords.player.PlayerEvent
import io.beyondwords.player.PlayerSettings
import io.beyondwords.player.PlayerView

class SegmentsView(
    context: Context,
    attrs: AttributeSet? = null
): RecyclerView(context, attrs) {
    private var isPlaying: Boolean = false
    private lateinit var player: PlayerView
    private lateinit var segments: Array<Segment>
    private lateinit var rvAdapter: SegmentsAdapter

    fun bindPlayer(player: PlayerView) {
        this.player = player

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            player.addEventListener(object : EventListener {
                override fun onEvent(event: PlayerEvent, settings: PlayerSettings) {
                    if (event.type == "CurrentSegmentUpdated") {
                        player.getCurrentSegment {
                            segments.forEach {
                                it.isActive = false
                            }

                            val newSegmentIdx = segments.indexOfFirst { segment ->
                                segment.marker == it
                            }

                            if (newSegmentIdx != -1 && newSegmentIdx < segments.size)
                                segments[newSegmentIdx].isActive = true

                            rvAdapter.notifyDataSetChanged()
                        }
                    }

                    if (event.type == "PlaybackEnded") {
                        segments.forEach { it.isActive = false }
                        rvAdapter.notifyDataSetChanged()
                    }

                    if (event.type == "PlaybackPlaying") isPlaying = true
                    if (event.type == "PlaybackPaused") isPlaying = false
                }
            })
        }
    }

    fun buildSegments(
        text: String,
        delimiter: String = "\n\n",
        markers: Array<String> = arrayOf()
    ) {
        val segments = text.split(delimiter).mapIndexed { idx, marker ->
            Segment(
                marker,
                if (markers.size > idx) markers[idx] else ""
            ) {
                if (::player.isInitialized && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val isSameSegmentClicked = try {
                        segments.first { it.isActive }.marker == markers[idx]
                    } catch (e: Exception) {
                        false
                    }

                    if (isPlaying) {
                        if (isSameSegmentClicked) player.setPlaybackState("paused")
                        else player.playSegment(markers[idx])
                    } else {
                        player.setPlaybackState("playing")
                        if (!isSameSegmentClicked) player.playSegment(markers[idx])
                    }
                }
            }
        }.toTypedArray()

        this.segments = segments
        rvAdapter = SegmentsAdapter(segments)
        this.adapter = rvAdapter
    }

}