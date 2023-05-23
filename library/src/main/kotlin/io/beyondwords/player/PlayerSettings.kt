package io.beyondwords.player

data class PlayerSettings(
    var playerApiUrl: String? = null,
    var projectId: String? = null,
    var contentId: String? = null,
    var sourceId: String? = null,
    var playerStyle: String? = null
)
