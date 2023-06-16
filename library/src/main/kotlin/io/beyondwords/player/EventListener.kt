package io.beyondwords.player

interface EventListener {
    fun onPressedPlay(event: PlayerEvent) {}
    fun onAny(event: PlayerEvent, settings: PlayerSettings) {}
}
