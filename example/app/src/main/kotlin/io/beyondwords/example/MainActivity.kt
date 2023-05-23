package io.beyondwords.example

import android.os.Bundle
import android.util.Log
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import io.beyondwords.player.EventListener
import io.beyondwords.player.PlayerEvent
import io.beyondwords.player.PlayerSettings
import io.beyondwords.player.PlayerView

class MainActivity : AppCompatActivity() {
    private lateinit var playerView: PlayerView
    private lateinit var projectIdInput: TextInputEditText
    private lateinit var contentIdInput: TextInputEditText
    private lateinit var playerStyleInput: AutoCompleteTextView
    private lateinit var loadButton: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.player_view)
        projectIdInput = findViewById(R.id.project_id_input)
        contentIdInput = findViewById(R.id.content_id_input)
        playerStyleInput = findViewById(R.id.player_style_input)
        loadButton = findViewById(R.id.load_button)

        playerView.addEventListener(object : EventListener {
            override fun onPressedPlay(event: PlayerEvent) {
                Log.d("PlayerView", "onPressedPlay($event)")
            }

            override fun onAny(event: PlayerEvent) {
                Log.d("PlayerView", "onAny($event)")
            }
        })

        loadButton.setOnClickListener { load() }

        playerStyleInput.addTextChangedListener {
            playerView.setPlayerStyle(playerStyleInput.text.toString())
        }
    }

    private fun load() {
        playerView.load(
            PlayerSettings(
                projectId = projectIdInput.text.toString(),
                contentId = contentIdInput.text.toString(),
                playerStyle = playerStyleInput.text.toString()
            )
        )
    }
}
