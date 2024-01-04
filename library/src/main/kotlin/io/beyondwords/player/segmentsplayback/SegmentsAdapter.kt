package io.beyondwords.player.segmentsplayback

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.beyondwords.player.R
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
            if (segments[position].marker.isNotBlank()) {
                bwSegment.setOnClickListener {
                    segments[position].onClick()
                }
            }

            if (segments[position].isActive) {
                Log.d("SegmentAdapter", "CurrentSegment: ${segments[position]}")
                val spannable = SpannableString(segments[position].text)
                spannable.setSpan(
                    BackgroundColorSpan(ContextCompat.getColor(context, R.color.default_highlight)),
                    0,
                    segments[position].text.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                bwSegment.text = spannable
            } else {
                bwSegment.text = segments[position].text
            }
        }
    }
}
