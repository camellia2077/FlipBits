package com.bag.audioandroid.ui

import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.domain.FlashSignalInfo
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.SavedAudioContent
import com.bag.audioandroid.domain.SavedAudioDecodedCacheEntry
import com.bag.audioandroid.domain.needsSavedDecodeRefresh
import com.bag.audioandroid.ui.state.PlaybackDetailsSource
import com.bag.audioandroid.ui.state.PlaybackUiState
import com.bag.audioandroid.ui.state.SavedAudioPlaybackSelection

internal fun SavedAudioContent.toPlaybackSelection(
    playbackSpeed: Float,
    cachedDecode: SavedAudioDecodedCacheEntry?,
    playback: PlaybackUiState,
): SavedAudioPlaybackSelection {
    val usableCachedDecode = cachedDecode.takeUnless { it?.needsSavedDecodeRefresh(item, metadata) == true }
    return SavedAudioPlaybackSelection(
        item = item,
        pcm = pcm,
        waveformPcm = waveformPcm,
        pcmFilePath = pcmFilePath,
        sampleRateHz = sampleRateHz,
        metadata = metadata,
        wavAudioInfo = wavAudioInfo,
        playback = playback,
        playbackSpeed = playbackSpeed,
        decodedPayload = usableCachedDecode?.decodedPayload ?: DecodedPayloadViewData.Empty,
        followData = usableCachedDecode?.followData ?: PayloadFollowViewData.Empty,
        playbackDetailsSource =
            if (usableCachedDecode != null) {
                PlaybackDetailsSource.SavedCache
            } else {
                PlaybackDetailsSource.DecodePending
            },
        flashSignalInfo = usableCachedDecode?.flashSignalInfo ?: FlashSignalInfo.Empty,
        isLoadingContent = false,
        needsDecodedContent = usableCachedDecode == null,
        isDecodingContent = false,
    )
}
