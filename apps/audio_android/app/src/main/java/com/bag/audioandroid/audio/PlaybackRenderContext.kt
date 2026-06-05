package com.bag.audioandroid.audio

import com.bag.audioandroid.domain.PayloadFollowViewData

data class PlaybackRenderContext(
    val isMiniMode: Boolean = false,
    val isFlashMode: Boolean = false,
    val frameSamples: Int = 0,
    val followData: PayloadFollowViewData = PayloadFollowViewData.Empty,
    val totalSamples: Int = 0,
) {
    companion object {
        val Empty = PlaybackRenderContext()
    }
}

internal fun PlaybackRenderContext.diagPlaybackMode(): String =
    when {
        isMiniMode -> "mini"
        isFlashMode -> "flash"
        else -> "generic"
    }
