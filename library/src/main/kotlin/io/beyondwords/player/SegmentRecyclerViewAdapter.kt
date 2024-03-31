package io.beyondwords.player

import android.view.View
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView

@RequiresApi(24)
abstract class SegmentRecyclerViewAdapter<T : SegmentRecyclerViewAdapter.SegmentViewHolder>(private var playerView: PlayerView) :
    RecyclerView.Adapter<T>() {
    companion object {
        const val SEGMENT_VIEW = 0
        const val EXTERNAL_VIEW = 1
    }

    abstract class SegmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var isActive = false
        var onSelect: (() -> Unit)? = null
    }

    private val listener = object : EventListener {
        override fun onEvent(event: PlayerEvent, settings: PlayerSettings) {
            if (event.type == "PlaybackPlaying") isPlaying = true
            if (event.type == "PlaybackPaused") isPlaying = false
            if (event.type == "CurrentSegmentUpdated") {
                currentSegment = settings.currentSegment
                notifyDataSetChanged()
            }
        }
    }

    private var isPlaying: Boolean = false
    private var currentSegment: PlayerSettings.Segment? = null

    init {
        playerView.addEventListener(listener)
    }

    override fun onBindViewHolder(viewHolder: T, position: Int) {
        if (viewHolder.itemViewType == EXTERNAL_VIEW) return
        val segmentMarker = getSegmentMarker(position)
        val segmentXPath = getSegmentXPath(position)
        val segmentMD5 = getSegmentMD5(position)
        viewHolder.isActive = (segmentMarker != null && segmentMarker == currentSegment?.marker) ||
                (segmentXPath != null && segmentXPath == currentSegment?.xpath) ||
                (segmentMD5 != null && segmentMD5 == currentSegment?.md5)
        viewHolder.onSelect = {
            val isSameSegmentClicked = currentSegment?.let {
                it.marker.orEmpty() == segmentMarker || it.xpath.orEmpty() == segmentXPath || it.md5.orEmpty() == segmentMD5
            } ?: false

            if (isPlaying && isSameSegmentClicked) {
                playerView.setPlaybackState("paused")
            } else {
                playerView.setPlaybackState("playing")

                if (!isSameSegmentClicked) playerView.setCurrentSegment(
                    segmentMarker = segmentMarker,
                    segmentXPath = segmentXPath,
                    segmentMD5 = segmentMD5
                )
            }
        }
    }

    open fun getSegmentMarker(position: Int): String? = null

    open fun getSegmentXPath(position: Int): String? = null

    open fun getSegmentMD5(position: Int): String? = null

    fun release() {
        playerView.removeEventListener(listener)
    }
}