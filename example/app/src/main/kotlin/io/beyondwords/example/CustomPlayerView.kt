package io.beyondwords.example

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import io.beyondwords.player.PlayerEvent
import io.beyondwords.player.PlayerSettings
import io.beyondwords.player.PlayerView
import kotlin.math.roundToLong

class CustomPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val playerView: PlayerView by lazy { findViewById(R.id.hidden_player_view) }
    private val playPauseButton: MaterialButton by lazy { findViewById(R.id.play_pause_button) }
    private val timeTextView: TextView by lazy { findViewById(R.id.time_text_view) }
    private val seekSlider: Slider by lazy { findViewById(R.id.seek_slider) }
    private val durationTextView: TextView by lazy { findViewById(R.id.duration_text_view) }
    private val playerEventListener = object : io.beyondwords.player.EventListener {
        override fun onAny(event: PlayerEvent, settings: PlayerSettings) {
            playPauseButton.isEnabled = true
            when (settings.playbackState) {
                "playing" -> {
                    playPauseButton.setIconResource(io.beyondwords.player.R.drawable.ic_pause)
                    playPauseButton.setOnClickListener { playerView.setPlaybackState("paused") }
                }

                else -> {
                    playPauseButton.setIconResource(io.beyondwords.player.R.drawable.ic_play)
                    playPauseButton.setOnClickListener { playerView.setPlaybackState("playing") }
                }
            }
            timeTextView.text = (settings.currentTime ?: 0F)
                .roundToLong()
                .toString()
                .padStart(2, '0')
            durationTextView.text = (settings.duration ?: 0F)
                .roundToLong()
                .toString()
                .padStart(2, '0')
            val duration = settings.duration ?: 0F
            if (duration > 0F) {
                if (!seekSlider.isPressed) {
                    seekSlider.value = ((settings.currentTime ?: 0F) / duration)
                        .coerceIn(0F..100F)
                }
                seekSlider.clearOnSliderTouchListeners()
                seekSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                    override fun onStartTrackingTouch(slider: Slider) {}

                    override fun onStopTrackingTouch(slider: Slider) {
                        playerView.setCurrentTime(slider.value * duration)
                    }
                })
                seekSlider.isEnabled = true
            } else {
                seekSlider.value = 0F
                seekSlider.clearOnSliderTouchListeners()
                seekSlider.isEnabled = false
            }
        }
    }

    init {
        inflate(context, R.layout.custom_player, this)
    }

    fun load(settings: PlayerSettings) {
        playerView.load(settings.copy(
            showUserInterface = false
        ))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        playerView.addEventListener(playerEventListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        playerView.removeEventListener(playerEventListener)
    }
}
