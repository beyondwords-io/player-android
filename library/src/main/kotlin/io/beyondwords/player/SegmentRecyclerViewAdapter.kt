package io.beyondwords.player

import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView

@RequiresApi(Build.VERSION_CODES.N)
abstract class SegmentRecyclerViewAdapter<T: SegmentRecyclerViewAdapter.SegmentViewHolder>(var playerView: PlayerView): RecyclerView.Adapter<T>() {
    abstract class SegmentViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var current = false
        var onSelect: (() -> Unit)? = null
    }

    private val listener = object: EventListener {
        override fun onEvent(event: PlayerEvent, settings: PlayerSettings) {
            if (event.type == "CurrentSegmentUpdated") {
                notifyDataSetChanged()
            }
        }
    }

    init {
        playerView.addEventListener(listener)
    }

    override fun onBindViewHolder(viewHolder: T, position: Int) {
        val segmentMarker = getSegmentMarker(position)
        viewHolder.current = segmentMarker == playerView.currentSegment?.marker
        viewHolder.onSelect = {
            playerView.setCurrentSegment(segmentMarker = segmentMarker)
        }
    }

    abstract fun getSegmentMarker(position: Int): String?

    fun release() {
        playerView.removeEventListener(listener)
    }
}