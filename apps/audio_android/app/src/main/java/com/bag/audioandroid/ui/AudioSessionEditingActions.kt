package com.bag.audioandroid.ui

import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.SampleDecorationStyleOption
import com.bag.audioandroid.ui.model.SampleFlavor
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.ModeAudioSessionState
import com.bag.audioandroid.ui.state.SampleEmojiShuffleState
import com.bag.audioandroid.ui.state.SampleInputShuffleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

internal class AudioSessionEditingActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val sessionStateStore: AudioSessionStateStore,
    private val sampleInputTextProvider: SampleInputTextProvider,
    private val stopPlayback: () -> Unit,
    private val refreshSavedAudioItems: () -> Unit,
    private val random: Random = Random.Default,
) {
    private companion object {
        private val SacredMachineEmojiStyle =
            FlavorEmojiStyle(
                rotatingEmojis =
                    listOf(
                        "\uD83D\uDEE0\uFE0F", // 🛠️
                        "\uD83D\uDD29", // 🔩
                        "\u26D3\uFE0F", // ⛓️
                        "\uD83E\uDDEA", // 🧪
                        "\uD83D\uDD6F\uFE0F", // 🕯️
                        "\uD83D\uDCBE", // 💾
                        "\uD83D\uDEF0\uFE0F", // 🛰️
                    ),
            )
        private val AncientDynastyEmojiStyle =
            FlavorEmojiStyle(
                rotatingEmojis =
                    listOf(
                        "\u26B1\uFE0F", // ⚱️
                        "\uD83D\uDD3A", // 🔺
                        "\uD83E\uDDFF", // 🧿
                        "\uD83D\uDCA0", // 💠
                        "\u2600\uFE0F", // ☀️
                    ),
            )
        private val LabyrinthEmojiStyle =
            FlavorEmojiStyle(
                rotatingEmojis =
                    listOf(
                        "\uD83C\uDF00", // 🌀
                        "\uD83D\uDC41\uFE0F", // 👁️
                        "\uD83C\uDFAD", // 🎭
                        "\uD83E\uDDE9", // 🧩
                        "\uD83D\uDD2E", // 🔮
                    ),
            )
        private val ExquisiteEmojiStyle =
            FlavorEmojiStyle(
                rotatingEmojis =
                    listOf(
                        "\uD83D\uDC8E", // 💎
                        "\uD83C\uDF39", // 🌹
                        "\uD83C\uDFBC", // 🎼
                        "\uD83E\uDE9E", // 🪞
                        "\u2728", // ✨
                    ),
            )
        private val ImmortalEmojiStyle =
            FlavorEmojiStyle(
                rotatingEmojis =
                    listOf(
                        "\uD83E\uDEB0", // 🪰
                        "\u2623\uFE0F", // ☣️
                        "\uD83E\uDDA0", // 🦠
                        "\uD83D\uDC80", // 💀
                    ),
            )
        private val ScarletEmojiStyle =
            FlavorEmojiStyle(
                rotatingEmojis =
                    listOf(
                        "\uD83E\uDE78", // 🩸
                        "\uD83D\uDDE1\uFE0F", // 🗡️
                        "\uD83D\uDEE1\uFE0F", // 🛡️
                        "\uD83D\uDD25", // 🔥
                    ),
            )
    }

    fun onInputTextChange(value: String) {
        sessionStateStore.updateCurrentSession {
            it.copy(
                inputText = value,
                sampleInputId = null,
                sampleShuffleState = null,
            )
        }
    }

    fun onRandomizeSampleInput(length: SampleInputLengthOption) {
        val currentState = uiState.value
        if (currentState.currentSession.isCodecBusy) {
            return
        }
        val sampleIds =
            sampleInputTextProvider.sampleIds(
                mode = currentState.transportMode,
                flavor = currentState.currentSampleFlavor,
                length = length,
            )
        if (sampleIds.isEmpty()) {
            return
        }
        val nextSampleSelection =
            nextSampleSelection(
                session = currentState.currentSession,
                flavor = currentState.currentSampleFlavor,
                length = length,
                sampleIds = sampleIds,
            )
        val sample =
            sampleInputTextProvider.sampleById(
                mode = currentState.transportMode,
                language = currentState.selectedLanguage,
                flavor = currentState.currentSampleFlavor,
                sampleId = nextSampleSelection.sampleId,
            ) ?: return
        sessionStateStore.updateCurrentSession {
            val emojiPrefix =
                nextEmojiPrefix(
                    mode = currentState.transportMode,
                    flavor = currentState.currentSampleFlavor,
                    isDecorationEnabled = currentState.isSampleDecorationEnabled,
                    decorationStyle = currentState.sampleDecorationStyle,
                    currentState = it.sampleEmojiShuffleState,
                )
            it.copy(
                inputText = withEmojiPrefix(sample.text, emojiPrefix?.emoji),
                sampleInputId = sample.id,
                sampleShuffleState = nextSampleSelection.shuffleState,
                sampleEmojiShuffleState = emojiPrefix?.state ?: it.sampleEmojiShuffleState,
            )
        }
    }

    fun onTransportModeSelected(mode: TransportModeOption) {
        val currentState = uiState.value
        if (currentState.transportMode == mode &&
            currentState.currentPlaybackSource == AudioPlaybackSource.Generated(mode)
        ) {
            return
        }
        stopPlayback()
        uiState.update {
            it.copy(
                transportMode = mode,
                currentPlaybackSource = AudioPlaybackSource.Generated(mode),
                showSavedAudioSheet = false,
                showPlayerDetailSheet = false,
            )
        }
    }

    fun onOpenSavedAudioSheet() {
        refreshSavedAudioItems()
        uiState.update { it.copy(showSavedAudioSheet = true, showPlayerDetailSheet = false) }
    }

    fun onCloseSavedAudioSheet() {
        uiState.update { it.copy(showSavedAudioSheet = false) }
    }

    private fun nextSampleSelection(
        session: ModeAudioSessionState,
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
        sampleIds: List<String>,
    ): NextSampleSelection {
        // Sample rotation is meant to feel fresh without starving parts of the catalog.
        // We therefore keep a shuffled round in session state and advance through it
        // one item at a time, rather than doing a brand new random pick on every tap.
        val activeShuffleState =
            session.sampleShuffleState
                ?.takeIf { it.matches(flavor, length) && it.shuffledSampleIds.toSet() == sampleIds.toSet() }
        val currentShuffleState =
            activeShuffleState ?: initialShuffleState(session, flavor, length, sampleIds)
        val reshuffledWhenConsumed =
            if (currentShuffleState.nextSampleIndex >= currentShuffleState.shuffledSampleIds.size) {
                reshuffledState(flavor, length, sampleIds, currentShuffleState.lastPresentedSampleId)
            } else {
                currentShuffleState
            }
        val selectedId = reshuffledWhenConsumed.shuffledSampleIds[reshuffledWhenConsumed.nextSampleIndex]
        return NextSampleSelection(
            sampleId = selectedId,
            shuffleState =
                reshuffledWhenConsumed.copy(
                    nextSampleIndex = reshuffledWhenConsumed.nextSampleIndex + 1,
                    lastPresentedSampleId = selectedId,
                ),
        )
    }

    private fun reshuffledState(
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
        sampleIds: List<String>,
        lastPresentedSampleId: String?,
    ): SampleInputShuffleState =
        SampleInputShuffleState(
            flavor = flavor,
            length = length,
            shuffledSampleIds = shuffledSampleIds(sampleIds, lastPresentedSampleId),
            nextSampleIndex = 0,
            lastPresentedSampleId = lastPresentedSampleId,
        )

    private fun initialShuffleState(
        session: ModeAudioSessionState,
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
        sampleIds: List<String>,
    ): SampleInputShuffleState {
        val currentSampleId = session.sampleInputId
        val remainingSampleIds =
            currentSampleId
                ?.takeIf(sampleIds::contains)
                ?.let { sampleIds.filterNot { id -> id == currentSampleId } }
                .orEmpty()
                .ifEmpty { sampleIds }
        return SampleInputShuffleState(
            flavor = flavor,
            length = length,
            shuffledSampleIds = shuffledSampleIds(remainingSampleIds, null),
            nextSampleIndex = 0,
            lastPresentedSampleId = currentSampleId,
        )
    }

    // Once a round has been exhausted, every sample goes back into the pool.
    // We only avoid a hard handoff where the last card of the previous round is
    // immediately repeated as the first card of the next round.
    private fun shuffledSampleIds(
        sampleIds: List<String>,
        leadingAvoidId: String?,
    ): List<String> {
        if (sampleIds.size <= 1) {
            return sampleIds
        }
        val shuffled = sampleIds.shuffled(random).toMutableList()
        if (leadingAvoidId == null || shuffled.first() != leadingAvoidId) {
            return shuffled
        }
        val swapIndex = shuffled.indexOfFirst { it != leadingAvoidId }
        if (swapIndex > 0) {
            val first = shuffled.first()
            shuffled[0] = shuffled[swapIndex]
            shuffled[swapIndex] = first
        }
        return shuffled
    }

    private data class NextSampleSelection(
        val sampleId: String,
        val shuffleState: SampleInputShuffleState,
    )

    private data class NextEmojiPrefix(
        val emoji: String,
        val state: SampleEmojiShuffleState,
    )

    private fun nextEmojiPrefix(
        mode: TransportModeOption,
        flavor: SampleFlavor,
        isDecorationEnabled: Boolean,
        decorationStyle: SampleDecorationStyleOption,
        currentState: SampleEmojiShuffleState?,
    ): NextEmojiPrefix? {
        if (!isDecorationEnabled || decorationStyle != SampleDecorationStyleOption.Emoji) {
            return null
        }
        val style = styleFor(flavor) ?: return null
        if (mode != TransportModeOption.Flash || style.rotatingEmojis.isEmpty()) {
            return null
        }
        val activeState =
            currentState
                ?.takeIf { it.shuffledEmojis.toSet() == style.rotatingEmojis.toSet() }
                ?: SampleEmojiShuffleState(
                    shuffledEmojis = style.rotatingEmojis.shuffled(random),
                    nextEmojiIndex = 0,
                    lastPresentedEmoji = null,
                )
        val reshuffledWhenConsumed =
            if (activeState.nextEmojiIndex >= activeState.shuffledEmojis.size) {
                val nextDeck = activeState.shuffledEmojis.shuffled(random).toMutableList()
                val avoid = activeState.lastPresentedEmoji
                if (avoid != null && nextDeck.size > 1 && nextDeck.first() == avoid) {
                    val swapIndex = nextDeck.indexOfFirst { it != avoid }
                    if (swapIndex > 0) {
                        val first = nextDeck.first()
                        nextDeck[0] = nextDeck[swapIndex]
                        nextDeck[swapIndex] = first
                    }
                }
                SampleEmojiShuffleState(
                    shuffledEmojis = nextDeck,
                    nextEmojiIndex = 0,
                    lastPresentedEmoji = activeState.lastPresentedEmoji,
                )
            } else {
                activeState
            }
        val emoji = reshuffledWhenConsumed.shuffledEmojis[reshuffledWhenConsumed.nextEmojiIndex]
        return NextEmojiPrefix(
            emoji = emoji,
            state =
                reshuffledWhenConsumed.copy(
                    nextEmojiIndex = reshuffledWhenConsumed.nextEmojiIndex + 1,
                    lastPresentedEmoji = emoji,
                ),
        )
    }

    private fun withEmojiPrefix(
        text: String,
        emoji: String?,
    ): String {
        if (emoji.isNullOrBlank()) {
            return text
        }
        val stripped = text.trimStart().replace(Regex("^[\\p{So}\\p{Sk}\\uFE0F]+\\s*"), "")
        return "$emoji $stripped"
    }

    private fun styleFor(flavor: SampleFlavor): FlavorEmojiStyle? =
        when (flavor) {
            SampleFlavor.SacredMachine -> SacredMachineEmojiStyle
            SampleFlavor.AncientDynasty -> AncientDynastyEmojiStyle
            SampleFlavor.LabyrinthOfMutability -> LabyrinthEmojiStyle
            SampleFlavor.ExquisiteFall -> ExquisiteEmojiStyle
            SampleFlavor.ImmortalRot -> ImmortalEmojiStyle
            SampleFlavor.ScarletCarnage -> ScarletEmojiStyle
        }

    private data class FlavorEmojiStyle(
        val rotatingEmojis: List<String>,
    )
}
