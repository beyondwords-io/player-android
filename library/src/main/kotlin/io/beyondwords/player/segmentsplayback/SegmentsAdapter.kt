package io.beyondwords.player.segmentsplayback

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.beyondwords.player.R
import io.beyondwords.player.databinding.SegmentViewBinding

class SegmentsAdapter(private val segments: List<Any>) :
    RecyclerView.Adapter<SegmentsAdapter.RvViewHolder>() {
    companion object {
        private const val SEGMENTVIEW = 0
        private const val EXTRENALVIEW = 1
    }
    private lateinit var context: Context

    inner class RvViewHolder(view: View? = null, binding: SegmentViewBinding? = null) :
        RecyclerView.ViewHolder(binding?.root ?: view!!)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RvViewHolder {
        context = parent.context

        if(viewType == EXTRENALVIEW) {
            val layout = LinearLayout(parent.context)
            layout.orientation = LinearLayout.VERTICAL
            return RvViewHolder(layout, null)
        }

        val binding = SegmentViewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false,
        )

        return RvViewHolder(null, binding)
    }

    override fun getItemCount(): Int {
        return segments.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (segments[position] is Segment) SEGMENTVIEW
        else EXTRENALVIEW
    }

    override fun onBindViewHolder(holder: RvViewHolder, position: Int) {
        if (holder.itemViewType == EXTRENALVIEW) {
            holder.itemView.apply {
                val externalView = segments[position] as View

                if (externalView.parent != null) {
                    (externalView.parent as ViewGroup).removeView(externalView)
                }

                (holder.itemView as LinearLayout).addView(externalView)
            }
            return
        }

        holder.itemView.apply {
            val bwSegment = findViewById<TextView>(R.id.bw_segment)

            if (segments[position] is Segment) {
                val segment = segments[position] as Segment

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
}
