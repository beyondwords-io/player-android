package io.beyondwords.player.segmentsplayback

import android.content.Context
import android.os.Build
import android.text.SpannableString
import android.util.AttributeSet
import android.util.Log
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
    private lateinit var segments: List<Any>
    private lateinit var rvAdapter: SegmentsAdapter

    private val onLoadListener: (text: List<Any>) -> EventListener = { text ->
        object : EventListener {
            @RequiresApi(Build.VERSION_CODES.N)
            override fun onEvent(event: PlayerEvent, settings: PlayerSettings) {
                if(event.type == "ContentAvailable") {
//                    player.getMarkers {
//                        buildSegments(text, markers = it[settings.contentId] ?: listOf())
//                        player.addEventListener(afterLoadListener)
//                    }
                }
            }
        }
    }

    private val afterLoadListener = object : EventListener {
        @RequiresApi(Build.VERSION_CODES.N)
        override fun onEvent(event: PlayerEvent, settings: PlayerSettings) {
            if (event.type == "CurrentSegmentUpdated" && settings.currentSegment != null) {
                segments.forEach { if (it is Segment) it.isActive = false }

                val newSegmentIdx = segments.indexOfFirst {
                    it is Segment && it.marker == settings.currentSegment?.marker
                }

                if (newSegmentIdx != -1 && newSegmentIdx < segments.size)
                    (segments[newSegmentIdx] as Segment).isActive = true

                rvAdapter.notifyDataSetChanged()
            }

            if (event.type == "PlaybackEnded") {
                segments.forEach { if (it is Segment) it.isActive = false }
                rvAdapter.notifyDataSetChanged()
            }

            if (event.type == "PlaybackPlaying") isPlaying = true
            if (event.type == "PlaybackPaused") isPlaying = false
        }
    }

    fun bindPlayer(player: PlayerView, text: List<Any>) {
        this.player = player
        buildSegments(text) // draw initial UI without segment binding

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        player.addEventListener(onLoadListener(text))
    }

    private fun buildSegments(
        text: List<Any>,
        markers: List<String> = listOf()
    ) {
        if (markers.isEmpty()) {
            this.segments = text.map { value ->
                when (value) {
                    is String -> Segment(value, "") {}
                    is SpannableString -> Segment(span = value, marker = "") {}
                    else -> value
                }
            }
            rvAdapter = SegmentsAdapter(segments)
            this.adapter = rvAdapter
            return
        }

        val segments = text.filter { it is String || it is SpannableString }.mapIndexed { idx, value ->
            when (value) {
                is String -> {
                    Segment(value, if (markers.size > idx) markers[idx] else "") {
                        updatePlayBack(markers[idx])
                    }
                }

                is SpannableString -> {
                    Segment(span = value, marker = if (markers.size > idx) markers[idx] else "") {
                        updatePlayBack(markers[idx])
                    }
                }

                else -> {}
            }
        }.toMutableList()

        text.forEachIndexed { idx, it ->
            if (it !is String && it !is SpannableString) segments.add(idx, it)
        }

        this.segments = segments
        rvAdapter = SegmentsAdapter(segments)
        this.adapter = rvAdapter
    }

    private fun updatePlayBack(segmentId: String) {
        val isSameSegmentClicked = try {
            (segments.first { it is Segment && it.isActive } as Segment).marker == segmentId
        } catch (e: Exception) {
            false
        }

        @RequiresApi(Build.VERSION_CODES.N)
        if (isPlaying) {
            if (isSameSegmentClicked) player.setPlaybackState("paused")
            else player.playSegment(segmentId)
        } else {
            player.setPlaybackState("playing")
            if (!isSameSegmentClicked) player.playSegment(segmentId)
        }
    }
}
