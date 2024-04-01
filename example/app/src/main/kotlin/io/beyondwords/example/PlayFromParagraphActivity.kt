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
        companion object {
            private const val SEGMENT_VIEW = 0
            private const val EXTERNAL_VIEW = 1
        }
        override fun getItemCount() = paragraphs.size

        override fun getItemViewType(position: Int): Int {
            return if (paragraphs[position] is Segment) SEGMENT_VIEW else EXTERNAL_VIEW
        }

        override fun getSegmentMarker(position: Int): String? =
            (paragraphs[position] as? Segment)?.marker

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MySegmentViewHolder {
            if (viewType == EXTERNAL_VIEW) {
                val layout = LinearLayout(parent.context).apply { orientation = LinearLayout.VERTICAL }
                return MySegmentViewHolder(layout)
            }

            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.segment_view, parent, false)

            return MySegmentViewHolder(itemView)
        }

        override fun onBindViewHolder(viewHolder: MySegmentViewHolder, position: Int) {
            super.onBindViewHolder(viewHolder, position)

            if (viewHolder.itemViewType == EXTERNAL_VIEW) {
                val externalView = paragraphs[position] as View

                externalView.parent?.let { (it as ViewGroup).removeView(externalView) }
                (viewHolder.itemView as LinearLayout).addView(externalView)

                return
            }

            val segment = paragraphs[position] as Segment
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

        val paragraphs = mutableListOf<Any>(
            // If you have the markers already, supply them with the text here itself:
            // Segment(SpannableString("text"), marker = "marker")
            // else, fetch them from the player after the audio loads (see the event listener below)
            Segment(
                SpannableString("Artificial intelligence").apply {
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
            Segment(
                SpannableString("Artificial intelligence (AI) is the intelligence of machines or software, as opposed to the intelligence of living beings, primarily of humans. It is a field of study in computer science that develops and studies intelligent machines. Such machines may be called AIs.")
            ),
            Segment(
                SpannableString("AI technology is widely used throughout industry, government, and science. Some high-profile applications are: advanced web search engines (e.g., Google Search), recommendation systems (used by YouTube, Amazon, and Netflix), interacting via human speech (e.g., Google Assistant, Siri, and Alexa), self-driving cars (e.g., Waymo), generative and creative tools (e.g., ChatGPT and AI art), and superhuman play and analysis in strategy games (e.g., chess and Go).[1]")
            ),
            Segment(
                SpannableString("Alan Turing was the first person to conduct substantial research in the field that he called machine intelligence.[2] Artificial intelligence was founded as an academic discipline in 1956.[3] The field went through multiple cycles of optimism,[4][5] followed by periods of disappointment and loss of funding, known as AI winter.[6][7] Funding and interest vastly increased after 2012 when deep learning surpassed all previous AI techniques,[8] and after 2017 with the transformer architecture.[9] This led to the AI spring of the early 2020s, with companies, universities, and laboratories overwhelmingly based in the United States pioneering significant advances in artificial intelligence.[10]")
            ),
            Segment(
                SpannableString("The growing use of artificial intelligence in the 21st century is influencing a societal and economic shift towards increased automation, data-driven decision-making, and the integration of AI systems into various economic sectors and areas of life, impacting job markets, healthcare, government, industry, and education. This raises questions about the ethical implications and risks of AI, prompting discussions about regulatory policies to ensure the safety and benefits of the technology.")
            ),
            Segment(
                SpannableString("The various sub-fields of AI research are centered around particular goals and the use of particular tools. The traditional goals of AI research include reasoning, knowledge representation, planning, learning, natural language processing, perception, and support for robotics.[a] General intelligence (the ability to complete any task performable by a human on an at least equal level) is among the field\\'s long-term goals.[11]")
            ),
            Segment(
                SpannableString("To solve these problems, AI researchers have adapted and integrated a wide range of problem-solving techniques, including search and mathematical optimization, formal logic, artificial neural networks, and methods based on statistics, operations research, and economics.[b] AI also draws upon psychology, linguistics, philosophy, neuroscience and other fields.")
            )
        )

        // Add a native button and an image to the article
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 30, 0, 30) }

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

        // If you manually supplied the markers in the paragraphs data above, you can skip this
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
