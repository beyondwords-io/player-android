package io.beyondwords.player

import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView

@RequiresApi(24)
abstract class SegmentRecyclerViewAdapter<T : SegmentRecyclerViewAdapter.SegmentViewHolder>(private var playerView: PlayerView) :
    RecyclerView.Adapter<T>() {
    abstract class SegmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var current = false
        var onSelect: (() -> Unit)? = null
    }

    private val listener = object : EventListener {
        override fun onEvent(event: PlayerEvent, settings: PlayerSettings) {
            if (event.type == "CurrentSegmentUpdated") {
                currentSegment = settings.currentSegment
                notifyDataSetChanged()
            }
        }
    }
    private var currentSegment: PlayerSettings.Segment? = null

    init {
        playerView.addEventListener(listener)
    }

    override fun onBindViewHolder(viewHolder: T, position: Int) {
        val segmentMarker = getSegmentMarker(position)
        val segmentXPath = getSegmentXPath(position)
        val segmentMD5 = getSegmentMD5(position)
        viewHolder.current = (segmentMarker != null && segmentMarker == currentSegment?.marker) ||
                (segmentXPath != null && segmentXPath == currentSegment?.xpath) ||
                (segmentMD5 != null && segmentMD5 == currentSegment?.md5)
        viewHolder.onSelect = {
            playerView.setCurrentSegment(
                segmentMarker = segmentMarker,
                segmentMD5 = segmentMD5,
                segmentXPath = segmentXPath
            )
        }
    }

    open fun getSegmentMarker(position: Int): String? = null

    open fun getSegmentXPath(position: Int): String? = null

    open fun getSegmentMD5(position: Int): String? = null

    fun release() {
        playerView.removeEventListener(listener)
    }
}