package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.domain.EncodeOperationSnapshot
import kotlin.math.roundToInt

data class EncodeProgressDisplayModel(
    val progress0To1: Float,
    val phase: AudioEncodePhase,
) {
    val percent: Int
        get() = (progress0To1.coerceIn(0f, 1f) * 100f).roundToInt()
}

fun EncodeOperationSnapshot?.toEncodeProgressDisplayModel(): EncodeProgressDisplayModel? =
    this?.let { snapshot ->
        EncodeProgressDisplayModel(
            progress0To1 = snapshot.overallProgress0To1.coerceIn(0f, 1f),
            phase = snapshot.phase,
        )
    }
