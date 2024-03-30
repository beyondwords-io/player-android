package io.beyondwords.example

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.beyondwords.player.EventListener
import io.beyondwords.player.PlayerEvent
import io.beyondwords.player.PlayerSettings
import io.beyondwords.player.PlayerView
import io.beyondwords.player.SegmentRecyclerViewAdapter

@RequiresApi(24)
class PlayFromParagraphActivity : AppCompatActivity() {
    private data class Segment(val text: String, var marker: String? = null)

    private class MySegmentViewHolder(itemView: View) :
        SegmentRecyclerViewAdapter.SegmentViewHolder(itemView) {
        val textView: TextView

        init {
            textView = itemView.findViewById(R.id.text_view)
        }
    }

    private class MySegmentAdapter(private val paragraphs: List<Segment>, playerView: PlayerView) :
        SegmentRecyclerViewAdapter<MySegmentViewHolder>(playerView) {
        override fun getItemCount() = paragraphs.size

        override fun getSegmentMarker(position: Int): String? =
            paragraphs.getOrNull(position)?.marker

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MySegmentViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.segment_view, parent, false)
            return MySegmentViewHolder(itemView)
        }

        override fun onBindViewHolder(viewHolder: MySegmentViewHolder, position: Int) {
            super.onBindViewHolder(viewHolder, position)

            viewHolder.textView.text = paragraphs[position].text
            viewHolder.textView.setBackgroundColor(if (viewHolder.isActive) Color.YELLOW else Color.WHITE)

            if (!paragraphs[position].marker.isNullOrBlank()) {
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


        val paragraphs = getString(R.string.article).split("\n\n").map {
            Segment(it)

            // If you have the marker mapping already, supply them here:
            // Segment(it, marker = "marker")
            // else, fetch them from the player after the audio loads (see below)
        }

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
                        paragraphs.forEachIndexed { idx, segment ->
                            segment.marker = it.getOrNull(idx)?.marker
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
