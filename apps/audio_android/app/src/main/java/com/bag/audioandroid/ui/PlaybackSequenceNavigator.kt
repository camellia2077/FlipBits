package com.bag.audioandroid.ui

import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlin.random.Random

internal class PlaybackSequenceNavigator(
    private val random: Random = Random.Default
) {
    fun previousSavedSource(
        savedAudioItems: List<SavedAudioItem>,
        currentSource: AudioPlaybackSource,
        wrap: Boolean = false
    ): AudioPlaybackSource? = adjacentSavedSource(
        savedAudioItems = savedAudioItems,
        currentSource = currentSource,
        step = -1,
        wrap = wrap
    )

    fun nextSavedSource(
        savedAudioItems: List<SavedAudioItem>,
        currentSource: AudioPlaybackSource,
        wrap: Boolean = false
    ): AudioPlaybackSource? = adjacentSavedSource(
        savedAudioItems = savedAudioItems,
        currentSource = currentSource,
        step = 1,
        wrap = wrap
    )

    fun nextSourceForCompletion(
        state: AudioAppUiState,
        completedSource: AudioPlaybackSource
    ): AudioPlaybackSource? =
        when (state.playbackSequenceMode) {
            PlaybackSequenceMode.Normal -> null
            PlaybackSequenceMode.RepeatOne -> completedSource
            PlaybackSequenceMode.RepeatList -> nextSavedSource(
                savedAudioItems = state.savedAudioItems,
                currentSource = completedSource,
                wrap = true
            )
            PlaybackSequenceMode.Shuffle -> randomSavedSource(state.savedAudioItems, completedSource)
        }

    private fun adjacentSavedSource(
        savedAudioItems: List<SavedAudioItem>,
        currentSource: AudioPlaybackSource,
        step: Int,
        wrap: Boolean
    ): AudioPlaybackSource? {
        if (currentSource !is AudioPlaybackSource.Saved || savedAudioItems.isEmpty()) {
            return null
        }
        val currentIndex = savedAudioItems.indexOfFirst { it.itemId == currentSource.itemId }
        if (currentIndex < 0) {
            return null
        }
        val targetIndex = currentIndex + step
        val resolvedIndex = when {
            targetIndex in savedAudioItems.indices -> targetIndex
            !wrap -> return null
            targetIndex < 0 -> savedAudioItems.lastIndex
            else -> 0
        }
        return AudioPlaybackSource.Saved(savedAudioItems[resolvedIndex].itemId)
    }

    private fun randomSavedSource(
        savedAudioItems: List<SavedAudioItem>,
        currentSource: AudioPlaybackSource
    ): AudioPlaybackSource? {
        if (currentSource !is AudioPlaybackSource.Saved || savedAudioItems.isEmpty()) {
            return null
        }
        val candidates = if (savedAudioItems.size > 1) {
            savedAudioItems.filterNot { it.itemId == currentSource.itemId }
        } else {
            savedAudioItems
        }
        val nextItem = candidates.randomOrNull(random) ?: return null
        return AudioPlaybackSource.Saved(nextItem.itemId)
    }
}
