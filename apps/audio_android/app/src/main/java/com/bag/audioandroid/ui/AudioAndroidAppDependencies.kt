package com.bag.audioandroid.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.bag.audioandroid.data.AndroidIntentAudioShareGateway
import com.bag.audioandroid.data.AndroidSampleInputTextProvider
import com.bag.audioandroid.data.AndroidVoiceAudioFileGateway
import com.bag.audioandroid.data.AndroidVoiceLiveGateway
import com.bag.audioandroid.data.AndroidVoiceRecordingGateway
import com.bag.audioandroid.data.AppGeneratedAudioCacheGateway
import com.bag.audioandroid.data.AppSavedAudioDecodeCacheGateway
import com.bag.audioandroid.data.AppSettingsRepository
import com.bag.audioandroid.data.DefaultSavedAudioRepository
import com.bag.audioandroid.data.GeneratedAudioTemporaryShareGateway
import com.bag.audioandroid.data.MediaStoreAudioExportGateway
import com.bag.audioandroid.data.MediaStoreSavedAudioLibraryGateway
import com.bag.audioandroid.data.NativeAudioCodecGateway
import com.bag.audioandroid.data.NativeAudioIoGateway
import com.bag.audioandroid.data.NativePlaybackRuntimeGateway
import com.bag.audioandroid.data.NativeVoiceFxGateway
import com.bag.audioandroid.data.SavedAudioLibraryMetadataStore

@Composable
internal fun rememberAudioAndroidViewModelFactory(appContext: Context): AudioAndroidViewModelFactory {
    val audioCodecGateway = remember { NativeAudioCodecGateway() }
    val audioIoGateway = remember { NativeAudioIoGateway() }
    val sampleInputTextProvider =
        remember(appContext) {
            AndroidSampleInputTextProvider(appContext)
        }
    val playbackRuntimeGateway = remember { NativePlaybackRuntimeGateway() }
    val voiceFxGateway = remember { NativeVoiceFxGateway() }
    val voiceRecordingGateway = remember { AndroidVoiceRecordingGateway() }
    val voiceLiveGateway = remember(appContext, voiceFxGateway) { AndroidVoiceLiveGateway(appContext, voiceFxGateway) }
    val voiceAudioFileGateway =
        remember(appContext, audioIoGateway) {
            AndroidVoiceAudioFileGateway(appContext, audioIoGateway)
        }
    val generatedAudioCacheGateway =
        remember(appContext) {
            AppGeneratedAudioCacheGateway(appContext)
        }
    val savedAudioDecodeCacheGateway =
        remember(appContext) {
            AppSavedAudioDecodeCacheGateway(appContext)
        }
    val audioExportGateway =
        remember(appContext, audioIoGateway) {
            MediaStoreAudioExportGateway(appContext, audioIoGateway)
        }
    val savedAudioLibraryGateway =
        remember(appContext, audioIoGateway) {
            MediaStoreSavedAudioLibraryGateway(appContext, audioIoGateway, generatedAudioCacheGateway)
        }
    val audioShareGateway =
        remember(appContext, audioIoGateway) {
            AndroidIntentAudioShareGateway(
                appContext = appContext,
                generatedAudioTemporaryShareGateway =
                    GeneratedAudioTemporaryShareGateway(
                        appContext = appContext,
                        audioIoGateway = audioIoGateway,
                    ),
            )
        }
    val libraryMetadataStore =
        remember(appContext) {
            SavedAudioLibraryMetadataStore(appContext)
        }
    val savedAudioRepository =
        remember(audioExportGateway, savedAudioLibraryGateway, audioShareGateway, libraryMetadataStore) {
            DefaultSavedAudioRepository(
                audioExportGateway = audioExportGateway,
                savedAudioLibraryGateway = savedAudioLibraryGateway,
                audioShareGateway = audioShareGateway,
                libraryMetadataStore = libraryMetadataStore,
            )
        }
    val appSettingsRepository =
        remember(appContext) {
            AppSettingsRepository(appContext)
        }

    return remember(
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
    ) {
        AudioAndroidViewModelFactory(
            audioCodecGateway = audioCodecGateway,
            audioIoGateway = audioIoGateway,
            sampleInputTextProvider = sampleInputTextProvider,
            appSettingsRepository = appSettingsRepository,
            playbackRuntimeGateway = playbackRuntimeGateway,
            voiceFxGateway = voiceFxGateway,
            voiceRecordingGateway = voiceRecordingGateway,
            voiceLiveGateway = voiceLiveGateway,
            voiceAudioFileGateway = voiceAudioFileGateway,
            savedAudioRepository = savedAudioRepository,
            generatedAudioCacheGateway = generatedAudioCacheGateway,
            savedAudioDecodeCacheGateway = savedAudioDecodeCacheGateway,
        )
    }
}
