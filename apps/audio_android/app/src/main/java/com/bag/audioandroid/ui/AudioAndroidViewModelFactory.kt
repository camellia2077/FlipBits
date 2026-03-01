package com.bag.audioandroid.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bag.audioandroid.domain.AudioCodecGateway

class AudioAndroidViewModelFactory(
    private val audioCodecGateway: AudioCodecGateway
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AudioAndroidViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AudioAndroidViewModel(audioCodecGateway) as T
        }
        throw IllegalArgumentException("Unsupported ViewModel class: ${modelClass.name}")
    }
}
