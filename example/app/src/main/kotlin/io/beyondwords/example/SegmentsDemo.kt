package io.beyondwords.example

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import io.beyondwords.player.PlayerSettings
import io.beyondwords.player.PlayerView
import io.beyondwords.player.segmentsplayback.SegmentsView

class SegmentsDemo : AppCompatActivity() {
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        setContentView(layout)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            playerView = PlayerView(this)
            layout.addView(playerView)
            playerView.load(PlayerSettings(
                projectId = 28900,
                contentId = "496b5abf-e1c0-47cb-941e-53893bb065d3"
                // Without headings:
                // contentId = "f3d18cf9-a4fc-4b6e-8c54-5fb7fcb10006"
            ))

            val segmentsView = SegmentsView(this)
            segmentsView.layoutManager = LinearLayoutManager(this)
            layout.addView(segmentsView)

            // This builds playback from paragraph with unformatted text
            // '\n\n' is the default breakpoint to build a paragraph
            // val text = getString(R.string.article)
            // segmentsView.bindPlayer(playerView, text)

            val splitText = getString(R.string.articleWithHeadings).split("\n\n")
            val headingSpannables = splitText.map { paragraph ->
                val spannable = SpannableString(paragraph)
                if (paragraph.lowercase().contains("heading"))
                    spannable.setSpan(
                        AbsoluteSizeSpan(24, true),
                        0,
                        paragraph.length,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                spannable
            }

            segmentsView.bindPlayer(playerView, headingSpannables)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && ::playerView.isInitialized) {
            playerView.release()
        }
    }
}
