package io.beyondwords.player

data class PlayerSettings(
    var playerApiUrl: String? = null,
    var projectId: String? = null,
    var contentId: String? = null,
    var playlistId: String? = null,
    var sourceId: String? = null,
    var sourceUrl: String? = null,
    var showUserInterface: Boolean? = null,
    var skipButtonStyle: String? = null,
    var playerStyle: String? = null
)
