package io.beyondwords.example

import android.os.Bundle
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.android.material.textfield.TextInputEditText
import io.beyondwords.player.PlayerSettings
import io.beyondwords.player.PlayerView

class MainActivity : AppCompatActivity() {
    private lateinit var playerContainerLayout: FrameLayout
    private lateinit var playerUIInput: AutoCompleteTextView
    private lateinit var projectIdInput: TextInputEditText
    private lateinit var contentIdInput: TextInputEditText
    private lateinit var playlistIdInput: TextInputEditText
    private lateinit var skipButtonStyleInput: AutoCompleteTextView
    private lateinit var playerStyleInput: AutoCompleteTextView
    private lateinit var loadButton: AppCompatButton
    private var defaultPlayerView: PlayerView? = null
    private var customPlayerView: CustomPlayerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerContainerLayout = findViewById(R.id.player_container_layout)
        playerUIInput = findViewById(R.id.player_ui_input)
        projectIdInput = findViewById(R.id.project_id_input)
        contentIdInput = findViewById(R.id.content_id_input)
        playlistIdInput = findViewById(R.id.playlist_id_input)
        skipButtonStyleInput = findViewById(R.id.skip_button_style_input)
        playerStyleInput = findViewById(R.id.player_style_input)
        loadButton = findViewById(R.id.load_button)

        loadButton.setOnClickListener { loadOnClick() }
    }

    private fun loadOnClick() {
        playerContainerLayout.removeAllViews()
        defaultPlayerView = null
        customPlayerView = null
        val settings = PlayerSettings(
            projectId = projectIdInput.text.toString(),
            contentId = contentIdInput.text.toString(),
            playlistId = playlistIdInput.text.toString(),
            skipButtonStyle = skipButtonStyleInput.text.toString(),
            playerStyle = playerStyleInput.text.toString()
        )
        when (playerUIInput.text.toString()) {
            "default" -> {
                defaultPlayerView = PlayerView(this).also {
                    it.load(settings)
                    playerContainerLayout.addView(it)
                }
            }

            "custom" -> {
                customPlayerView = CustomPlayerView(this).also {
                    it.load(settings)
                    playerContainerLayout.addView(it)
                }
            }
        }
    }
}
