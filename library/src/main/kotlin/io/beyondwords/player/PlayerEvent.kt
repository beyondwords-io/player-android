package io.beyondwords.player

data class PlayerEvent(
    val id: String,
    val type: String,
    val description: String,
    val initiatedBy: String,
    val emittedFrom: String,
    val status: String,
    val createdAt: String,
    val processedAt: String,
    // TODO val changedProps
)
