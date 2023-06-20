package io.beyondwords.player

data class PlayerSettings(
    var playerApiUrl: String? = null,
    var projectId: String? = null,
    var contentId: String? = null,
    var playlistId: Int? = null,
    var sourceId: String? = null,
    var sourceUrl: String? = null,
    var playlist: List<Identifier>? = null,
    var showUserInterface: Boolean? = null,
    var playerStyle: String? = null,
    var playerTitle: String? = null,
    var callToAction: String? = null,
    var skipButtonStyle: String? = null,
    var playlistStyle: String? = null,
    var playlistToggle: String? = null,
    var mediaSession: String? = null,
    var contentIndex: Int? = null,
    var introsOutrosIndex: Int? = null,
    var advertIndex: Int? = null,
    var persistentAdImage: Boolean? = null,
    var persistentIndex: Int? = null,
    var duration: Float? = null,
    var currentTime: Float? = null,
    var playbackState: String? = null,
    var playbackRate: Float? = null,
    var textColor: String? = null,
    var backgroundColor: String? = null,
    var iconColor: String? = null,
    var logoIconEnabled: Boolean? = null,
    var advertConsent: String? = null,
    var analyticsConsent: String? = null,
    var analyticsCustomUrl: String? = null,
    var analyticsTag: String? = null,
    var captureErrors: Boolean? = null
) {
    data class Identifier(
        var contentId: String? = null,
        var playlistId: Int? = null,
        var sourceId: String? = null,
        var sourceUrl: String? = null,
    )
}
