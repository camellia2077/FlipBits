package com.bag.audioandroid.ui.state

enum class PlaybackPhase(
    val nativeValue: Int,
) {
    Idle(0),
    Playing(1),
    Paused(2),
    Stopped(3),
    Completed(4),
    Failed(5),
    ;

    companion object {
        fun fromNativeValue(value: Int): PlaybackPhase = entries.firstOrNull { it.nativeValue == value } ?: Idle
    }
}
