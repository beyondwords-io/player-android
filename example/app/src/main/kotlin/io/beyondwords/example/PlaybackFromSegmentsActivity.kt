package io.beyondwords.example

import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import io.beyondwords.player.EventListener
import io.beyondwords.player.PlayerEvent
import io.beyondwords.player.PlayerSettings
import io.beyondwords.player.PlayerView

@RequiresApi(24)
class PlaybackFromSegmentsActivity : AppCompatActivity() {

    private lateinit var contentView: LinearLayout
    private lateinit var playerView: PlayerView
    private val segments: Map<String, String> = mapOf(
        "ce3811c3-46e0-4007-88ae-231e2a019564" to "Twenty-five years ago, concerns over the millennium bug led to fears of widespread digital failures, but the actual impact was minimal; today, however, a new threat looms with the rise of quantum computing, which could easily crack existing encryption algorithms.",
        "ed331186-cd55-41c3-a524-96789ad99b39" to "Quantum computers operate using qubits, allowing them to perform complex calculations much faster than classical computers, potentially compromising the security of sensitive data across various sectors, including finance and personal information.",
        "693e0c4e-ca13-4274-8dd0-c957950a91cf" to "While quantum computers capable of breaking current encryption are still years away, the technology industry is proactively developing post-quantum encryption standards to secure digital information, emphasizing the need for a comprehensive upgrade of existing systems to mitigate future risks."
    )
    private val playerEventListener: EventListener = object : EventListener {
        override fun onEvent(event: PlayerEvent, settings: PlayerSettings) {
            if (event.type == "CurrentSegmentUpdated") {
                val text = segments[settings.currentSegment?.marker]
                for (i in 0 until contentView.childCount) {
                    val view = contentView.getChildAt(i)
                    if (view !is TextView) continue
                    if (view.text == text) {
                        view.setBackgroundColor(android.graphics.Color.LTGRAY)
                    } else {
                        view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playback_from_segments)

        contentView = findViewById(R.id.content_view)

        val titleText = TextView(this)
        titleText.text = "Will quantum computers disrupt critical infrastructure?"
        titleText.setTypeface(null, Typeface.BOLD)
        titleText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24f)
        titleText.layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )
        titleText.gravity = android.view.Gravity.CENTER
        contentView.addView(titleText)

        playerView = PlayerView(this)
        playerView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        contentView.addView(playerView)
        playerView.addEventListener(playerEventListener)
        playerView.load(
            PlayerSettings(
                projectId = 48772,
                contentId = "2a1038c4-dafe-40aa-9b9a-442a537794f2",
                summary = true
            )
        )

        for ((_, value) in segments) {
            val segmentText = TextView(this)
            segmentText.text = value
            segmentText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18f)
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.topMargin = (4 * resources.displayMetrics.density).toInt()
            segmentText.layoutParams = layoutParams
            segmentText.setOnClickListener { onSegmentClick(segmentText) }
            contentView.addView(segmentText)
        }
    }

    private fun onSegmentClick(segmentText: TextView) {
        val marker = segments.entries.firstOrNull { it.value == segmentText.text }?.key ?: return
        playerView.setCurrentSegment(segmentMarker = marker)
    }
}