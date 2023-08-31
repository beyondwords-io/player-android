package io.beyondwords.player.segmentsplayback

data class Segment(
    val text: String,
    val marker: String,
    var isActive: Boolean = false,
    val onClick: () -> Unit
)
