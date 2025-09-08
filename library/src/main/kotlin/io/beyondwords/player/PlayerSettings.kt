package io.beyondwords.player

data class PlayerSettings(
    var playerApiUrl: String? = null,
    var projectId: Int? = null,
    var contentId: String? = null,
    var playlistId: Int? = null,
    var sourceId: String? = null,
    var sourceUrl: String? = null,
    var playlist: List<Identifier>? = null,
    var summary: Boolean? = null,
    var loadContentAs: List<String>? = null,
    var contentVariant: String? = null,
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
    var minDurationForMidroll: Float? = null,
    var minTimeUntilEndForMidroll: Float? = null,
    var persistentAdImage: Boolean? = null,
    var persistentIndex: Int? = null,
    var duration: Float? = null,
    var currentTime: Float? = null,
    var playbackState: String? = null,
    var playbackRate: Float? = null,
    var playbackRates: List<Float>? = null,
    var textColor: String? = null,
    var backgroundColor: String? = null,
    var iconColor: String? = null,
    var logoIconEnabled: Boolean? = null,
    var currentSegment: Segment? = null,
    var loadedMedia: Media? = null,
    var advertConsent: String? = null,
    var analyticsConsent: String? = null,
    var analyticsCustomUrl: String? = null,
    var analyticsTag: String? = null,
    var bundleIdentifier: String? = null,
    var vendorIdentifier: String? = null,
) {
    data class Identifier(
        var contentId: String? = null,
        var playlistId: Int? = null,
        var sourceId: String? = null,
        var sourceUrl: String? = null,
    )

    data class Media(
        var id: Int? = null,
        var url: String? = null,
        var contentType: String? = null,
        var duration: Float? = null,
        var format: String? = null,
        var variant: String? = null,
    )

    data class Segment(
        var segmentIndex: Int? = null,
        var contentIndex: Int? = null,
        var marker: String? = null,
        var md5: String? = null,
        var xpath: String? = null,
        var section: String? = null,
        var startTime: Float? = null,
        var duration: Float? = null,
    )

    data class Content(
        var id: String? = null,
        var title: String? = null,
        var imageUrl: String? = null,
        var sourceId: String? = null,
        var sourceUrl: String? = null,
        var adsEnabled: Boolean? = null,
        var duration: Float? = null,
        var audio: List<Media>? = null,
        var video: List<Media>? = null,
        var segments: List<Segment>? = null,
        var summarization: Summarization? = null
    )

    data class IntroOutro(
        var placement: String? = null,
        var audio: List<Media>? = null,
        var video: List<Media>? = null,
    )

    data class Advert(
        var id: Int? = null,
        var type: String? = null,
        var placement: String? = null,
        var clickThroughUrl: String? = null,
        var vastUrl: String? = null,
        var textColor: String? = null,
        var backgroundColor: String? = null,
        var iconColor: String? = null,
        var audio: List<Media>? = null,
        var video: List<Media>? = null,
    )

    data class Summarization(
        var audio: List<Media>? = null,
        var video: List<Media>? = null,
    )
}
