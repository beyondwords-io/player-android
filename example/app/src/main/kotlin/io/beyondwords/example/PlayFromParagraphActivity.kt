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
import io.beyondwords.player.PlayerSettings
import io.beyondwords.player.PlayerView
import io.beyondwords.player.SegmentRecyclerViewAdapter

@RequiresApi(24)
class PlayFromParagraphActivity : AppCompatActivity() {
    private data class Segment(val text: String, val marker: String)

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
            viewHolder.textView.setBackgroundColor(if (viewHolder.current) Color.YELLOW else Color.WHITE)
            viewHolder.itemView.setOnClickListener {
                viewHolder.onSelect?.invoke()
            }
        }
    }

    private lateinit var playerView: PlayerView
    private lateinit var articleView: RecyclerView

    private val paragraphs = listOf(
        Segment("Artificial intelligence", "37ac0e52-3513-409a-97f5-a48bd2b70261"),
        Segment(
            "Artificial intelligence (AI) is the intelligence of machines or software, as opposed to the intelligence of living beings, primarily of humans. It is a field of study in computer science that develops and studies intelligent machines. Such machines may be called AIs.",
            "9192d1cc-58c4-4c46-9c05-9c3163c7373d"
        ),
        Segment(
            "AI technology is widely used throughout industry, government, and science. Some high-profile applications are: advanced web search engines (e.g., Google Search), recommendation systems (used by YouTube, Amazon, and Netflix), interacting via human speech (e.g., Google Assistant, Siri, and Alexa), self-driving cars (e.g., Waymo), generative and creative tools (e.g., ChatGPT and AI art), and superhuman play and analysis in strategy games (e.g., chess and Go).[1]",
            "01f8f368-59ee-419a-a911-88374b7a6cc4"
        ),
        Segment(
            "Alan Turing was the first person to conduct substantial research in the field that he called machine intelligence.[2] Artificial intelligence was founded as an academic discipline in 1956.[3] The field went through multiple cycles of optimism,[4][5] followed by periods of disappointment and loss of funding, known as AI winter.[6][7] Funding and interest vastly increased after 2012 when deep learning surpassed all previous AI techniques,[8] and after 2017 with the transformer architecture.[9] This led to the AI spring of the early 2020s, with companies, universities, and laboratories overwhelmingly based in the United States pioneering significant advances in artificial intelligence.[10]",
            "76d14c82-f1c8-4ecc-a78e-ee8bd40034f3"
        ),
        Segment(
            "The growing use of artificial intelligence in the 21st century is influencing a societal and economic shift towards increased automation, data-driven decision-making, and the integration of AI systems into various economic sectors and areas of life, impacting job markets, healthcare, government, industry, and education. This raises questions about the ethical implications and risks of AI, prompting discussions about regulatory policies to ensure the safety and benefits of the technology.",
            "78539209-d817-4a2d-938f-c487e80f0255"
        ),
        Segment(
            "The various sub-fields of AI research are centered around particular goals and the use of particular tools. The traditional goals of AI research include reasoning, knowledge representation, planning, learning, natural language processing, perception, and support for robotics.[a] General intelligence (the ability to complete any task performable by a human on an at least equal level) is among the field's long-term goals.[11]",
            "a254e95f-b132-4bfd-9396-bc57c26e8bef"
        ),
        Segment(
            "To solve these problems, AI researchers have adapted and integrated a wide range of problem-solving techniques, including search and mathematical optimization, formal logic, artificial neural networks, and methods based on statistics, operations research, and economics.[b] AI also draws upon psychology, linguistics, philosophy, neuroscience and other fields.",
            "65c0c8be-babe-4014-8280-042f0a253709"
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_from_paragraph)

        playerView = findViewById(R.id.player_view)
        articleView = findViewById(R.id.article_view)

        articleView.layoutManager = LinearLayoutManager(this)
        articleView.adapter = MySegmentAdapter(paragraphs, playerView)

        playerView.load(
            PlayerSettings(
                projectId = 40510,
                contentId = "7ab9f4c7-70ba-4135-82f3-a38a836568de"
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        (articleView.adapter as MySegmentAdapter).release()
        if (::playerView.isInitialized) playerView.release()
    }
}
