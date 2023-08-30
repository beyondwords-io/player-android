package io.beyondwords.player.segmentsplayback

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.beyondwords.player.databinding.SegmentViewBinding

class SegmentsAdapter(private val segments: Array<Segment>) :
    RecyclerView.Adapter<SegmentsAdapter.RvViewHolder>() {
    private lateinit var context: Context

    inner class RvViewHolder(val binding: SegmentViewBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RvViewHolder {
        context = parent.context

        val binding = SegmentViewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false,
        )

        return RvViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return segments.size
    }

    override fun onBindViewHolder(holder: RvViewHolder, position: Int) {
        holder.binding.apply {
            bwSegment.text = segments[position].text

            bwSegment.setOnClickListener {
                segments[position].onClick()
            }
        }
    }
}
