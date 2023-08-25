package io.beyondwords.example

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import io.beyondwords.player.BWSegment
import io.beyondwords.player.PlayerSettings
import io.beyondwords.player.PlayerView

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
        }

        val texts = getString(R.string.article).split("\n\n")
        texts.forEachIndexed { idx, text ->
            val view = BWSegment(this)
            view.text = text
            view.setMarker(idx.toString())
            Log.d(  "SegmentsDemo $idx", view.getMarker())
            view.setOnClickListener {
                Log.d("SegmentsDemo $idx", view.getMarker())
            }
            layout.addView(view)
        }
    }
}
