package io.beyondwords.player.segmentsplayback

import android.content.Context
import android.os.Build
import android.text.SpannableString
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
    private lateinit var segments: List<Segment>
    private lateinit var rvAdapter: SegmentsAdapter

    private val onLoadListener: (text: List<Any>) -> EventListener = { text ->
        object : EventListener {
            @RequiresApi(Build.VERSION_CODES.N)
            override fun onEvent(event: PlayerEvent, settings: PlayerSettings) {
                if(event.type == "ContentAvailable") {
                    player.getMarkers {
                        buildSegments(text, markers = it[settings.contentId] ?: listOf())
                        player.addEventListener(afterLoadListener)
                    }
                }
            }
        }
    }

    private val afterLoadListener = object : EventListener {
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
        val isListOfStrings = text.all { it is String }
        val isListOfSpannables = text.all { it is SpannableString }

        if (!isListOfStrings && !isListOfSpannables) {
            throw Exception("BeyondWordsPlayer:buildSegments: " +
                    "Please provide a list of strings or spannables")
        }

        if (markers.isEmpty()) {
            this.segments = text.mapIndexed { _, value ->
                if (isListOfStrings) Segment(value as String, "") {}
                else Segment(span = value as SpannableString, marker = "") {}
            }
            rvAdapter = SegmentsAdapter(segments)
            this.adapter = rvAdapter
            return
        }

        val segments = text.mapIndexed { idx, value ->
            Segment(
                if (isListOfStrings) value as String else "",
                if (markers.size > idx) markers[idx] else "",
                if (!isListOfStrings) value as SpannableString else null
            ) { updatePlayBack(markers[idx]) }
        }

        this.segments = segments
        rvAdapter = SegmentsAdapter(segments)
        this.adapter = rvAdapter
    }

    private fun updatePlayBack(segmentId: String) {
        val isSameSegmentClicked = try {
            segments.first { it.isActive }.marker == segmentId
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
