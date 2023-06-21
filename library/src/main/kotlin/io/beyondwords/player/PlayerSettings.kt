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
    var content: List<Content>? = null,
    var contentIndex: Int? = null,
    var introsOutros: List<IntroOutro>? = null,
    var introsOutrosIndex: Int? = null,
    var adverts: List<Advert>? = null,
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

    data class Media(
        var id: Int? = null,
        var url: String,
        var contentType: String? = null,
    )

    data class Segment(
        var marker: String,
        var section: String,
        var startTime: Float,
        var duration: Float? = null
    )

    data class Content(
        var id: String? = null,
        var title: String? = null,
        var imageUrl: String? = null,
        var sourceId: String? = null,
        var sourceUrl: String? = null,
        var adsEnabled: Boolean? = null,
        var duration: Float? = null,
        var media: List<Media>? = null,
        var segments: List<Segment>? = null
    )

    data class IntroOutro(
        val placement: String,
        var media: List<Media>? = null,
    )

    data class Advert(
        val id: Int? = null,
        val type: String? = null,
        val placement: String? = null,
        val clickThroughUrl: String? = null,
        val vastUrl: String? = null,
        var textColor: String? = null,
        var backgroundColor: String? = null,
        var iconColor: String? = null,
        var media: List<Media>? = null,
    )
}
