package io.beyondwords.player.segmentsplayback

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.beyondwords.player.R
import io.beyondwords.player.databinding.SegmentViewBinding

class SegmentsAdapter(private val segments: List<Segment>) :
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
            val segment = segments[position]

            if (segment.marker.isNotBlank()) {
                bwSegment.setOnClickListener {
                    segment.onClick()
                }
            }

            if (segment.isActive) {
                val spannable = segment.span ?: SpannableString(segment.text)

                spannable.setSpan(
                    BackgroundColorSpan(ContextCompat.getColor(context, R.color.default_highlight)),
                    0,
                    segment.span?.length ?: segment.text.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                bwSegment.text = spannable
            } else {
                bwSegment.text = segment.span?.apply {
                    getSpans(0, length, BackgroundColorSpan::class.java).forEach {
                        removeSpan(it)
                    }
                } ?: segment.text
            }
        }
    }
}
