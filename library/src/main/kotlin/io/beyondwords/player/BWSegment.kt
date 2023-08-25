package io.beyondwords.player

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView

class BWSegment(
    context: Context,
    attrs: AttributeSet? = null
) : TextView(context, attrs) {
    private lateinit var marker: String

    fun setMarker(marker: String) {
        this.marker = marker
    }

    fun getMarker(): String {
        return if (::marker.isInitialized) this.marker else ""
    }
}
