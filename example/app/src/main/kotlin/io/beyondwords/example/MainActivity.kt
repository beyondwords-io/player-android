package io.beyondwords.example

import android.content.Context
import android.os.Bundle
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.widget.addTextChangedListener
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

        playerUIInput.addTextChangedListener { playerUIOnChange() }
        loadButton.setOnClickListener { loadOnClick() }

        loadSettings()
    }

    private fun playerUIOnChange() {
        when (playerUIInput.text.toString()) {
            "default" -> {
                playerStyleInput.isEnabled = true
            }

            "custom" -> {
                playerStyleInput.isEnabled = false
            }
        }
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
        saveSettings()
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

    private fun saveSettings() {
        getPreferences(Context.MODE_PRIVATE)?.let {
            with(it.edit()) {
                putString("playerUIInput", playerUIInput.text.toString())
                putString("projectIdInput", projectIdInput.text.toString())
                putString("contentIdInput", contentIdInput.text.toString())
                putString("playlistIdInput", playlistIdInput.text.toString())
                putString("skipButtonStyleInput", skipButtonStyleInput.text.toString())
                putString("playerStyleInput", playerStyleInput.text.toString())
                apply()
            }
        }
    }

    private fun loadSettings() {
        getPreferences(Context.MODE_PRIVATE)?.let {
            playerUIInput.setText(it.getString("playerUIInput", "default"), false)
            projectIdInput.setText(it.getString("projectIdInput", ""))
            contentIdInput.setText(it.getString("contentIdInput", ""))
            playlistIdInput.setText(it.getString("playlistIdInput", ""))
            skipButtonStyleInput.setText(it.getString("skipButtonStyleInput", "auto"), false)
            playerStyleInput.setText(it.getString("playerStyleInput", "standard"), false)
        }
    }
}
