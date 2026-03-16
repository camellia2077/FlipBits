package com.bag.audioandroid.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

internal fun createStaticAudioTrack(
    sampleRateHz: Int,
    sampleCount: Int
): AudioTrack {
    val minBuffer = AudioTrack.getMinBufferSize(
        sampleRateHz,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    return AudioTrack.Builder()
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
        .setBufferSizeInBytes(maxOf(minBuffer, sampleCount * 2))
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()
}
