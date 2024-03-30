package io.beyondwords.example

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
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
            // Please provide a list of strings
            // val text = getString(R.string.article).split("\n\n")
            // segmentsView.bindPlayer(playerView, text)

//            val splitText = getString(R.string.articleWithHeadings).split("\n\n")
            val splitText = "".split("\n\n")
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

            val list = mutableListOf<Any>()
            list.addAll(headingSpannables)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(30, 30, 30, 30)

            val button = MaterialButton(this)
            button.text = "Click me!"
            button.layoutParams = params
            button.setOnClickListener {
                Toast.makeText(this, "Button clicked!", Toast.LENGTH_SHORT).show()
            }

            list.add(3, button)

            val imageView = ImageView(this)
            imageView.setImageResource(R.drawable.beyondwords_logo)
            imageView.layoutParams = params

            list.add(7, imageView)
            segmentsView.bindPlayer(playerView, list)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && ::playerView.isInitialized) {
            playerView.release()
        }
    }
}
