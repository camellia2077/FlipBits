package com.bag.audioandroid.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bag.audioandroid.data.AppSettingsRepository
import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.AudioIoGateway
import com.bag.audioandroid.domain.GeneratedAudioCacheGateway
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.domain.SavedAudioDecodeCacheGateway
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.domain.VoiceAudioFileGateway
import com.bag.audioandroid.domain.VoiceFxGateway
import com.bag.audioandroid.domain.VoiceLiveGateway
import com.bag.audioandroid.domain.VoiceRecordingGateway

class AudioAndroidViewModelFactory(
    private val audioCodecGateway: AudioCodecGateway,
    private val audioIoGateway: AudioIoGateway,
    private val sampleInputTextProvider: SampleInputTextProvider,
    private val appSettingsRepository: AppSettingsRepository,
    private val playbackRuntimeGateway: PlaybackRuntimeGateway,
    private val voiceFxGateway: VoiceFxGateway,
    private val voiceRecordingGateway: VoiceRecordingGateway,
    private val voiceLiveGateway: VoiceLiveGateway,
    private val voiceAudioFileGateway: VoiceAudioFileGateway,
    private val savedAudioRepository: SavedAudioRepository,
    private val generatedAudioCacheGateway: GeneratedAudioCacheGateway,
    private val savedAudioDecodeCacheGateway: SavedAudioDecodeCacheGateway,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AudioAndroidViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AudioAndroidViewModel(
                audioCodecGateway,
                audioIoGateway,
                sampleInputTextProvider,
                appSettingsRepository,
                playbackRuntimeGateway,
                voiceFxGateway,
                voiceRecordingGateway,
                voiceLiveGateway,
                voiceAudioFileGateway,
                savedAudioRepository,
                generatedAudioCacheGateway,
                savedAudioDecodeCacheGateway,
            ) as T
        }
        throw IllegalArgumentException("Unsupported ViewModel class: ${modelClass.name}")
    }
}
