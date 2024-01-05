package io.beyondwords.player.segmentsplayback

import android.text.SpannableString

data class Segment(
    val text: String = "",
    val marker: String = "",
    var span: SpannableString? = null,
    var isActive: Boolean = false,
    val onClick: () -> Unit
)
