package com.bag.audioandroid.data

import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioRouting

internal class AndroidVoiceLiveDeviceSelector(
    private val audioManager: AudioManager,
) {
    fun choosePreferredInputDevice(): AudioDeviceInfo? {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        return devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC } ?: devices.firstOrNull()
    }

    fun choosePreferredOutputDevice(): AudioDeviceInfo? {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.firstOrNull(::isBluetoothOutputDevice)
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
    }

    fun preferredDevice(routing: AudioRouting): AudioDeviceInfo? = routing.preferredDevice

    fun isPreferredOutputDeviceActive(
        preferredOutputDevice: AudioDeviceInfo?,
        activeOutputDevice: AudioDeviceInfo?,
    ): Boolean {
        if (preferredOutputDevice == null) {
            return activeOutputDevice == null
        }
        return activeOutputDevice?.id == preferredOutputDevice.id
    }

    fun routedDeviceLabel(device: AudioDeviceInfo?): String =
        when (device?.type) {
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Phone mic"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone speaker"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired headset mic"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headphones"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB headset"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth speaker"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth headset"
            AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLE headset"
            AudioDeviceInfo.TYPE_BLE_SPEAKER -> "BLE speaker"
            AudioDeviceInfo.TYPE_BLE_BROADCAST -> "BLE broadcast"
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Earpiece"
            else -> device?.productName?.toString()?.ifBlank { null } ?: "System default"
        }

    private fun isBluetoothOutputDevice(device: AudioDeviceInfo): Boolean =
        device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
            device.type == AudioDeviceInfo.TYPE_BLE_BROADCAST ||
            device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
}
