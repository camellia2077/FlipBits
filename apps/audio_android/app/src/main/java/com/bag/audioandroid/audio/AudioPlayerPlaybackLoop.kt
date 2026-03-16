package com.bag.audioandroid.audio

import android.media.AudioTrack

internal fun runStaticPlaybackLoop(
    playback: AudioPlayer.PlaybackHandle,
    track: AudioTrack,
    pcm: ShortArray,
    playbackStartOffsetSamples: Int = 0,
    reportedTotalSamples: Int = pcm.size,
    onProgressChanged: (Int, Int) -> Unit = { _, _ -> }
): PlaybackResult {
    val bufferedSamples = pcm.size
    track.write(pcm, 0, pcm.size)
    val initialPosition = playbackStartOffsetSamples.coerceIn(0, reportedTotalSamples)
    onProgressChanged(initialPosition, reportedTotalSamples)
    track.play()
    while (!playback.stopRequested && track.playbackHeadPosition < bufferedSamples) {
        if (playback.pauseRequested) {
            safelyPauseTrack(track)
            while (playback.pauseRequested && !playback.stopRequested) {
                Thread.sleep(PlaybackProgressUpdateIntervalMs)
            }
            if (playback.stopRequested) {
                return PlaybackResult.Stopped
            }
            safelyPlayTrack(track)
        }
        if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
            Thread.sleep(PlaybackProgressUpdateIntervalMs)
            continue
        }
        val reportedProgress = (playbackStartOffsetSamples + track.playbackHeadPosition)
            .coerceIn(0, reportedTotalSamples)
        onProgressChanged(reportedProgress, reportedTotalSamples)
        Thread.sleep(PlaybackProgressUpdateIntervalMs)
    }
    if (playback.stopRequested) {
        return PlaybackResult.Stopped
    }
    onProgressChanged(reportedTotalSamples, reportedTotalSamples)
    return PlaybackResult.Completed
}

private const val PlaybackProgressUpdateIntervalMs = 50L
