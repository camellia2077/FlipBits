package com.bag.audioandroid.ui.screen

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun formatSavedAudioTime(savedAtEpochSeconds: Long): String {
    if (savedAtEpochSeconds <= 0L) {
        return ""
    }
    return Instant.ofEpochSecond(savedAtEpochSeconds)
        .atZone(ZoneId.systemDefault())
        .format(SAVED_AUDIO_TIME_FORMATTER)
}

internal fun samplesToMillis(samples: Int, sampleRateHz: Int): Long {
    if (samples <= 0 || sampleRateHz <= 0) {
        return 0L
    }
    return (samples.toLong() * 1000L) / sampleRateHz.toLong()
}

internal fun formatDurationMillis(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private val SAVED_AUDIO_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
