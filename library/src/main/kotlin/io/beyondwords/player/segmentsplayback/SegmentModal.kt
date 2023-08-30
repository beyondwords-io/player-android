package io.beyondwords.player.segmentsplayback

data class Segment(
    val text: String,
    val marker: String,
    val onClick: () -> Unit
)
