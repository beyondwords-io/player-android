package io.beyondwords.example

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import io.beyondwords.player.EventListener
import io.beyondwords.player.PlayerEvent
import io.beyondwords.player.PlayerSettings
import io.beyondwords.player.PlayerView
import io.beyondwords.player.SegmentRecyclerViewAdapter

@RequiresApi(24)
class PlayFromParagraphActivity : AppCompatActivity() {
    private data class Segment(val text: SpannableString, var marker: String? = null)

    private class MySegmentViewHolder(itemView: View) :
        SegmentRecyclerViewAdapter.SegmentViewHolder(itemView)

    private class MySegmentAdapter(private val paragraphs: List<Any>, playerView: PlayerView) :
        SegmentRecyclerViewAdapter<MySegmentViewHolder>(playerView) {
        override fun getItemCount() = paragraphs.size

        override fun getItemViewType(position: Int): Int {
            return if (paragraphs[position] is Segment) SEGMENT_VIEW else EXTERNAL_VIEW
        }

        override fun getSegmentMarker(position: Int): String? =
            (paragraphs[position] as? Segment)?.marker

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MySegmentViewHolder {
            if (viewType == EXTERNAL_VIEW) {
                val layout = LinearLayout(parent.context)
                layout.orientation = LinearLayout.VERTICAL
                return MySegmentViewHolder(layout)
            }

            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.segment_view, parent, false)

            return MySegmentViewHolder(itemView)
        }

        override fun onBindViewHolder(viewHolder: MySegmentViewHolder, position: Int) {
            super.onBindViewHolder(viewHolder, position)

            if (viewHolder.itemViewType == EXTERNAL_VIEW) {
                viewHolder.itemView.apply {
                    val externalView = paragraphs[position] as View

                    if (externalView.parent != null) {
                        (externalView.parent as ViewGroup).removeView(externalView)
                    }

                    (viewHolder.itemView as LinearLayout).addView(externalView)
                }
                return
            }

            val segment = paragraphs[position] as Segment
            val textView = viewHolder.itemView.findViewById<TextView>(R.id.text_view)

            if (viewHolder.isActive) {
                segment.text.setSpan(
                    BackgroundColorSpan(Color.LTGRAY), 0, segment.text.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else {
                segment.text.apply {
                    getSpans(0, length, BackgroundColorSpan::class.java).forEach { removeSpan(it) }
                }
            }

            textView.text = segment.text

            if (!segment.marker.isNullOrBlank()) {
                viewHolder.itemView.setOnClickListener {
                    viewHolder.onSelect?.invoke()
                }
            }
        }
    }

    private lateinit var playerView: PlayerView
    private lateinit var articleView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_from_paragraph)

        playerView = findViewById(R.id.player_view)
        articleView = findViewById(R.id.article_view)

        val paragraphs: MutableList<Any> = getString(R.string.article).split("\n\n").map {
            // If you have the marker mapping already, supply them here itself:
            // Segment(SpannableString(it), marker = "marker")
            // else, fetch them from the player after the audio loads (see the event listener below)

            if (it.contains("#")) {
                // make the text a heading
                Segment(SpannableString(it.removePrefix("#")).apply {
                    setSpan(
                        AbsoluteSizeSpan(26, true), 0, length,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    setSpan(
                        android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, length,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                })
            } else {
                Segment(SpannableString(it))
            }
        }.toMutableList()

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 30, 0, 30)

        paragraphs.add(2, MaterialButton(this).apply {
            text = "Click me!"
            layoutParams = params
            setOnClickListener {
                Toast.makeText(this@PlayFromParagraphActivity, "Button clicked!", Toast.LENGTH_SHORT)
                    .show()
            }
        })

        paragraphs.add(4, ImageView(this).apply {
            layoutParams = params
            setImageResource(R.drawable.beyondwords_logo)
        })

        articleView.layoutManager = LinearLayoutManager(this)
        articleView.adapter = MySegmentAdapter(paragraphs, playerView)

        playerView.load(
            PlayerSettings(
                projectId = 40510,
                contentId = "7ab9f4c7-70ba-4135-82f3-a38a836568de"
            )
        )

        // If you supplied the markers already, you can skip this
        playerView.addEventListener(object : EventListener {
            override fun onEvent(event: PlayerEvent, settings: PlayerSettings) {
                if (event.type == "ContentAvailable") {
                    settings.content?.first { it.id == settings.contentId }?.segments?.let {
                        paragraphs.filter { it is Segment }.forEachIndexed { idx, item ->
                            (item as Segment).marker = it.getOrNull(idx)?.marker
                        }

                        (articleView.adapter as MySegmentAdapter).notifyDataSetChanged()
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()

        (articleView.adapter as MySegmentAdapter).release()
        if (::playerView.isInitialized) playerView.release()
    }
}
