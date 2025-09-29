## Segments Playback

The player supports a feature called 'Playback from Segments'. This lets you to click on a segment in your app (i.e. a paragraph) to begin playback from that segment. If the segment is already playing then it will be paused instead.

The segments playback in the Android Player is based on the [Segments Playback in the Web Player](https://github.com/beyondwords-io/player/blob/main/doc/segments-playback.md). The Android SDK cannot automatically identify segments, instead you should be able to manually link each text segment with its beyondwords marker.

You can find an example of how segment playback can be integrated in your app in [PlaybackFromSegmentsActivity.kt](../example/app/src/main/kotlin/io/beyondwords/example/PlaybackFromSegmentsActivity.kt) and [PlaybackFromSegmentsRecyclerActivity.kt](../example/app/src/main/kotlin/io/beyondwords/example/PlaybackFromSegmentsRecyclerActivity.kt)

## How it works

To highlight the current segment you have to listen for the `CurrentSegmentUpdated` event, then find the correspondig UI element to the `currentSegment` and apply the desired styling to it.

```kotlin
object : EventListener {
    override fun onEvent(event: PlayerEvent, settings: PlayerSettings) {
        if (event.type == "CurrentSegmentUpdated") {
            val text = segments[settings.currentSegment?.marker]
            for (i in 0 until contentView.childCount) {
                val view = contentView.getChildAt(i)
                if (view !is TextView) continue
                if (view.text == text) {
                    view.setBackgroundColor(android.graphics.Color.LTGRAY)
                } else {
                    view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            }
        }
    }
}
```

To change the current time of the player when a segment is clicked you have to set `View.OnClickListener` and call `setCurrentSegment` with the correspondig marker.

```kotlin
fun onSegmentClick(segmentText: TextView) {
    val marker = segments.entries.firstOrNull { it.value == segmentText.text }?.key ?: return
    playerView.setCurrentSegment(segmentMarker = marker)
}
```
