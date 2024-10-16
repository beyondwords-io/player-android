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
    interface Segment { var marker: String? }
    data class ImageSegment(val resId: Int, override var marker: String? = null) : Segment
    data class TextSegment(val text: SpannableString, override var marker: String? = null) : Segment
    
    private class MySegmentViewHolder(itemView: View) :
        SegmentRecyclerViewAdapter.SegmentViewHolder(itemView)

    private class MySegmentAdapter(private val paragraphs: List<Any>, playerView: PlayerView) :
        SegmentRecyclerViewAdapter<MySegmentViewHolder>(playerView) {
        companion object {
            private const val TEXT_VIEW = 0
            private const val IMAGE_VIEW = 1
        }
        override fun getItemCount() = paragraphs.size

        override fun getItemViewType(position: Int): Int {
            return if (paragraphs[position] is TextSegment) TEXT_VIEW else IMAGE_VIEW
        }

        override fun getSegmentMarker(position: Int): String? =
            (paragraphs[position] as? Segment)?.marker

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MySegmentViewHolder {
            if (viewType == IMAGE_VIEW) {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.image_view, parent, false)

                return MySegmentViewHolder(itemView)
            }

            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.segment_view, parent, false)

            return MySegmentViewHolder(itemView)
        }

        override fun onBindViewHolder(viewHolder: MySegmentViewHolder, position: Int) {
            super.onBindViewHolder(viewHolder, position)

            if (viewHolder.itemViewType == IMAGE_VIEW) {
                val imageView = paragraphs[position] as ImageSegment

                viewHolder.itemView.findViewById<ImageView>(R.id.image_view)
                    .setImageResource(imageView.resId)

                return
            }

            val segment = paragraphs[position] as TextSegment
            val textView = viewHolder.itemView.findViewById<TextView>(R.id.text_view)

            // Full view width segment highlighting:
            // textView.setBackgroundColor(if (viewHolder.isActive) Color.YELLOW else Color.WHITE)

            // Text width segment highlighting:
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
            if (segment.marker.isNullOrBlank()) return

            viewHolder.itemView.setOnClickListener {
                viewHolder.onSelect?.invoke()
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

        val paragraphs = mutableListOf(
            ImageSegment(R.drawable.article_thumb),
            // If you have the markers already, supply them with the text here itself:
            // TextSegment(SpannableString("text"), marker = "marker")
            // else, fetch them from the player after the audio loads (see the event listener below)
            TextSegment(
                SpannableString("Lisbon team meetup").apply {
                    setSpan(
                        AbsoluteSizeSpan(26, true), 0, length,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    setSpan(
                        android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, length,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            ),
            TextSegment(
                SpannableString("Last week, the BeyondWords team, less one or two, gathered in sunny Lisbon for our annual team meetup.")
            ),
            TextSegment(
                SpannableString("Flying in from all corners of Europe (and some from further afar) we enjoyed what has become an essential part of life at BeyondWords.")
            ),
            TextSegment(
                SpannableString("Our meetups are designed in a way to share ideas and foster connection and collaboration amongst our team - making the most out of the precious time we have all-together.")
            ),
            TextSegment(
                SpannableString("This year was a great balance of roadmap planning, team building, a little sightseeing, and lots of productivity breaks for pastéis de nata!")
            ),
            TextSegment(
                SpannableString("Below we've highlighted a few themes from the meetup.")
            ),
            TextSegment(
                SpannableString("Our exceptional team").apply {
                    setSpan(
                        AbsoluteSizeSpan(20, true), 0, length,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    setSpan(
                        android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, length,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            ),
            TextSegment(
                SpannableString("Over the past few years, we've developed into a lean product-focused and customer-obsessed team. Through a number of pivots, we've fine-tuned our strategy, creating and delivering best-in-class AI voice products for news publishers.")
            ),
            TextSegment(
                SpannableString("From product development to support, every team member - linguists, engineers, founders - are focused on helping our customers succeed.")
            ),
            TextSegment(
                SpannableString("Our customer-first approach has been key to our growth, and our time in Lisbon reinforced that the passion and commitment of our team will continue to drive BeyondWords forward.")
            ),
            TextSegment(
                SpannableString("Fun fact: Alongside our customer-obsession, we're also enthusiastic about burritos…next week will mark our 100th trip to Chipotle since we returned to the office!")
            ),
            TextSegment(
                SpannableString("Operational focus").apply {
                    setSpan(
                        AbsoluteSizeSpan(20, true), 0, length,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    setSpan(
                        android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, length,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            ),
            TextSegment(
                SpannableString("As we moved from remote to hybrid working - adding an office in central London - Lisbon also gave us an opportunity to strengthen our operational model.")
            ),
            TextSegment(
                SpannableString("As a company, we've matured around 3 departments - Growth, Engineering and Ops - with cross-functional teams within each department.")
            ),
            TextSegment(
                SpannableString("We think of ourselves as an arrow, with customer-led product development at the tip.")
            ),
            TextSegment(
                SpannableString("Simplifying the org chart into this structure provides us with clarity over responsibilities, improved accountability and ownership, better communication and team cohesion.")
            ),
            ImageSegment(R.drawable.in_article_img),
            TextSegment(
                SpannableString("And not to miss the crucial element to any long term strategy…having fun!"),
                marker = "segment-16"
            ),
            TextSegment(
                SpannableString("On this meetup, we took on the famous hills of Lisbon (on electric bikes!) while receiving a fun and informative introduction to Portuguese history from our guide, Avi, and we sampled delicious local food at the TimeOut market."),
                marker = "segment-17"
            ),
            TextSegment(
                SpannableString("New customer partnerships").apply {
                    setSpan(
                        AbsoluteSizeSpan(20, true), 0, length,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    setSpan(
                        android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, length,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                },
                marker = "segment-18"
            ),
            TextSegment(
                SpannableString("Lastly, we celebrated new customer partnerships with Thomson Reuters, Nation Media, Stavanger Aftenblad, Die Presse, Kleine Zeitung, Charities Aid Foundation and many more."),
                marker = "segment-19"
            ),
            TextSegment(
                SpannableString("We're also excited to have recently helped News Australia successfully train and launch a number of custom AI voices, based on their journalists, across their titles."),
                marker = "segment-20"
            ),
            TextSegment(
                SpannableString("As a team we hope to continue to grow and collaborate more effectively, to be creative, open minded, bold, and, in doing-so, delight our customers."),
                marker = "segment-21"
            ),
            TextSegment(
                SpannableString("If you're interested in working with us, in our team, as a partner or as a customer, feel free to book a meeting or reach out to us at hello@beyondwords.io."),
                marker = "segment-22"
            ),
        )

        articleView.layoutManager = LinearLayoutManager(this)
        articleView.adapter = MySegmentAdapter(paragraphs, playerView)

        playerView.load(
            PlayerSettings(
                projectId = 9504,
                contentId = "0d0cec6c-4bba-4c26-bf14-adacb6b801bb"
            )
        )

        // If you manually supplied the markers in the paragraphs data above, you can skip this
        playerView.addEventListener(object : EventListener {
            override fun onEvent(event: PlayerEvent, settings: PlayerSettings) {
                if (event.type == "ContentAvailable") {
                    settings.content?.first { it.id == settings.contentId }?.segments?.let {
                        paragraphs.filterIsInstance<TextSegment>().forEachIndexed { idx, item ->
                            if (item.marker == null) item.marker = it.getOrNull(idx)?.marker
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
