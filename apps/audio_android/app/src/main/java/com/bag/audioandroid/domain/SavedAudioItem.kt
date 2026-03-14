package com.bag.audioandroid.domain

data class SavedAudioItem(
    val itemId: String,
    val displayName: String,
    val uriString: String,
    val modeWireName: String,
    val durationMs: Long,
    val savedAtEpochSeconds: Long
)
