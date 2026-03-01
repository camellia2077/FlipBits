package com.bag.audioandroid.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

@Suppress("MagicNumber")
fun playPcm(
    pcm: ShortArray,
    sampleRateHz: Int,
    onProgressChanged: (Float) -> Unit = {}
) {
    val minBuffer = AudioTrack.getMinBufferSize(
        sampleRateHz,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    val track = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setSampleRate(sampleRateHz)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
        )
        .setBufferSizeInBytes(maxOf(minBuffer, pcm.size * 2))
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

    try {
        track.write(pcm, 0, pcm.size)
        track.play()
        onProgressChanged(0f)
        while (track.playState == AudioTrack.PLAYSTATE_PLAYING &&
            track.playbackHeadPosition < pcm.size
        ) {
            val progress = track.playbackHeadPosition.toFloat() / pcm.size
            onProgressChanged(progress.coerceIn(0f, 1f))
            Thread.sleep(16)
        }
        onProgressChanged(1f)
    } finally {
        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
            track.stop()
        }
        track.release()
    }
}
