package com.bag.audioandroid.ui.screen

data class GlyphProgressDisplayModel(
    val progress0To1: Float,
    val isActive: Boolean,
    val showIdleCoreRing: Boolean = true,
)

fun EncodeProgressDisplayModel?.toGlyphProgressDisplayModel(isEncodingBusy: Boolean): GlyphProgressDisplayModel? =
    this?.let { display ->
        GlyphProgressDisplayModel(
            progress0To1 = display.progress0To1.coerceIn(0f, 1f),
            isActive = isEncodingBusy,
            showIdleCoreRing = true,
        )
    }
