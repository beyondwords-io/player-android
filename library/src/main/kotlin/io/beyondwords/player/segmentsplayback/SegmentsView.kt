package io.beyondwords.player.segmentsplayback

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.RequiresApi
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

    fun bindPlayer(player: PlayerView, text: String, breakpoint: String = "\n\n") {
        this.player = player
        buildSegments(text) // draw initial UI without segment binding

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        val afterLoadListener = object : EventListener {
            @RequiresApi(Build.VERSION_CODES.N)
            override fun onEvent(event: PlayerEvent, settings: PlayerSettings) {
                if (event.type == "CurrentSegmentUpdated") {
                    player.getCurrentSegment {
                        segments.forEach { segment ->
                            segment.isActive = false
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
        }

        player.addEventListener(object : EventListener {
            @RequiresApi(Build.VERSION_CODES.N)
            override fun onEvent(event: PlayerEvent, settings: PlayerSettings) {
                if(event.type == "ContentAvailable") {
                    player.getMarkers {
                        buildSegments(text, breakpoint, markers = it[settings.contentId] ?: listOf())
                        player.addEventListener(afterLoadListener)
                    }
                }
            }
        })
    }

    private fun buildSegments(
        text: String,
        delimiter: String = "\n\n",
        markers: List<String> = listOf()
    ) {
        if (markers.isEmpty()) {
            this.segments = text.split(delimiter).mapIndexed { _, paragraph ->
                Segment(paragraph, "") {}
            }.toTypedArray()
            rvAdapter = SegmentsAdapter(segments)
            this.adapter = rvAdapter
            return
        }

        val segments = text.split(delimiter).mapIndexed { idx, paragraph ->
            Segment(
                paragraph,
                if (markers.size > idx) markers[idx] else ""
            ) {
                val isSameSegmentClicked = try {
                    segments.first { it.isActive }.marker == markers[idx]
                } catch (e: Exception) {
                    false
                }


                @RequiresApi(Build.VERSION_CODES.N)
                if (isPlaying) {
                    if (isSameSegmentClicked) player.setPlaybackState("paused")
                    else player.playSegment(markers[idx])
                } else {
                    player.setPlaybackState("playing")
                    if (!isSameSegmentClicked) player.playSegment(markers[idx])
                }
            }
        }.toTypedArray()

        this.segments = segments
        rvAdapter = SegmentsAdapter(segments)
        this.adapter = rvAdapter
    }
}
