package io.beyondwords.example

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import io.beyondwords.player.PlayerSettings
import io.beyondwords.player.PlayerView
import io.beyondwords.player.segmentsplayback.SegmentsView

class SegmentsDemo : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        setContentView(layout)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val playerView = PlayerView(this)
            layout.addView(playerView)
            playerView.load(PlayerSettings(
                projectId = 28900,
                contentId = "f3d18cf9-a4fc-4b6e-8c54-5fb7fcb10006"
            ))

            val text = getString(R.string.article)

            val segmentsView = SegmentsView(this)
            segmentsView.layoutManager = LinearLayoutManager(this)
            layout.addView(segmentsView)

            segmentsView.bindPlayer(text, playerView)
        }
    }
}
