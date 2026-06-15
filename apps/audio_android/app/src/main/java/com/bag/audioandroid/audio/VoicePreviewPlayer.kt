package com.bag.audioandroid.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

internal class VoicePreviewPlayer(
    private val audioPlayer: AudioPlayer = AudioPlayer(),
) {
    private val playbackIds = AtomicLong(0L)
    private var playbackJob: Job? = null
    private var playbackHandle: AudioPlayer.PlaybackHandle? = null

    fun play(
        scope: CoroutineScope,
        pcm: ShortArray,
        sampleRateHz: Int,
        startSampleIndex: Int = 0,
        onPlaybackStarted: () -> Unit,
        onProgressChanged: (Int, Int) -> Unit = { _, _ -> },
        onPlaybackFinished: () -> Unit,
    ) {
        stop()
        val playbackId = playbackIds.incrementAndGet()
        val handle = audioPlayer.prepareForNewPlayback()
        playbackHandle = handle
        playbackJob =
            scope.launch(Dispatchers.IO) {
                try {
                    audioPlayer.playPcm(
                        playback = handle,
                        pcm = pcm,
                        sampleRateHz = sampleRateHz,
                        startSampleIndex = startSampleIndex,
                        onPlaybackStarted = {
                            if (playbackIds.get() == playbackId) {
                                onPlaybackStarted()
                            }
                        },
                        onProgressChanged = { playedSamples, totalSamples ->
                            if (playbackIds.get() == playbackId) {
                                onProgressChanged(playedSamples, totalSamples)
                            }
                        },
                    )
                } finally {
                    if (playbackIds.get() == playbackId) {
                        playbackHandle = null
                        onPlaybackFinished()
                    }
                }
            }
    }

    fun seekTo(sampleIndex: Int): Int? = audioPlayer.seekTo(sampleIndex)

    fun stop() {
        playbackIds.incrementAndGet()
        playbackHandle?.let(audioPlayer::stop)
        playbackHandle = null
        playbackJob?.cancel()
        playbackJob = null
    }
}
