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

            val texts = getString(R.string.article)

            val markers = arrayOf(
                "95eec57a-754c-408f-9006-55f54257eb3b",
                "f329fe2f-bf0a-4808-bea9-3194a9b6d9fe",
                "eae5f714-8d7e-46bd-a9f8-fbd87b8d51ae",
                "bb21b087-1c4f-475e-a764-df3e8ca265b6",
                "9d910ae4-deaa-4ffc-971d-a987e2c53323",
                "f7470c90-5341-4cf9-b581-8d870727e23c",
                "c4424e29-814c-4bd3-91c4-567586fb153d",
                "ce14acd8-64d1-4987-88a4-17472ac51090",
                "b38931cd-23f1-4d44-a846-9d627be3d13b",
                "1c7cda13-348b-4274-bfd5-bf164b87e8bd",
                "76e1d996-c702-4a5e-8845-3bbc899a8c00",
                "cf92449c-1e01-4460-b11c-907a5d95a20a"
            )

            val segmentsView = SegmentsView(this)
            segmentsView.layoutManager = LinearLayoutManager(this)
            segmentsView.buildSegments(texts, markers = markers)
            layout.addView(segmentsView)

            segmentsView.bindPlayer(playerView)
        }
    }
}
