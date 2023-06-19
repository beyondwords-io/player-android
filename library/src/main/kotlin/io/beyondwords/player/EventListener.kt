package io.beyondwords.player

interface EventListener {
    fun onEvent(event: PlayerEvent, settings: PlayerSettings)
}
