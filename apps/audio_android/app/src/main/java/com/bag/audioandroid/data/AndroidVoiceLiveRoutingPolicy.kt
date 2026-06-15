package com.bag.audioandroid.data

import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack

internal class AndroidVoiceLiveRoutingPolicy(
    private val audioManager: AudioManager,
) {
    private var activeCommunicationDevice: AudioDeviceInfo? = null
    private var previousAudioMode: Int? = null

    fun apply(
        record: AudioRecord,
        track: AudioTrack,
        preferredInputDevice: AudioDeviceInfo?,
        preferredOutputDevice: AudioDeviceInfo?,
    ) {
        previousAudioMode = audioManager.mode
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        record.preferredDevice = preferredInputDevice
        track.preferredDevice = preferredOutputDevice
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (preferredOutputDevice != null &&
                audioManager.availableCommunicationDevices.any { it.id == preferredOutputDevice.id }
            ) {
                if (audioManager.setCommunicationDevice(preferredOutputDevice)) {
                    activeCommunicationDevice = preferredOutputDevice
                }
            }
        } else {
            @Suppress("DEPRECATION")
            run {
                audioManager.isSpeakerphoneOn = preferredOutputDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }
        }
    }

    fun release() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (activeCommunicationDevice != null) {
                audioManager.clearCommunicationDevice()
                activeCommunicationDevice = null
            }
        } else {
            @Suppress("DEPRECATION")
            run {
                audioManager.isSpeakerphoneOn = false
            }
        }
        previousAudioMode?.let { mode ->
            audioManager.mode = mode
            previousAudioMode = null
        }
    }
}
