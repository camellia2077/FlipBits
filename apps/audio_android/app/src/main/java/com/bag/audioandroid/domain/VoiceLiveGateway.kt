package com.bag.audioandroid.domain

import com.bag.audioandroid.ui.model.VoiceFxPresetOption
import com.bag.audioandroid.ui.model.VoiceFxSubvoiceStyleOption

data class VoiceLiveConfig(
    val sampleRateHz: Int,
    val preset: VoiceFxPresetOption,
    val subvoiceStyle: VoiceFxSubvoiceStyleOption,
)

data class VoiceLiveRouteSnapshot(
    val inputRouteLabel: String,
    val outputRouteLabel: String,
    val speakerOutputRequested: Boolean,
    val speakerOutputActive: Boolean,
)

interface VoiceLiveGateway {
    fun start(
        config: VoiceLiveConfig,
        onRouteChanged: (VoiceLiveRouteSnapshot) -> Unit,
        onStopped: (errorCode: Int) -> Unit,
    ): Boolean

    fun stop()

    fun release()
}
