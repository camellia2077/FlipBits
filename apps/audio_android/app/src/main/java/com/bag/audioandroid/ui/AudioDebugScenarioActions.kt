package com.bag.audioandroid.ui

import android.util.Log
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.ui.model.MorseSpeedOption
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.flow.StateFlow
import java.nio.charset.StandardCharsets.UTF_8

internal class AudioDebugScenarioActions(
    private val uiState: StateFlow<AudioAppUiState>,
    private val sampleInputTextProvider: SampleInputTextProvider,
    private val sessionStateStore: AudioSessionStateStore,
    private val onTransportModeSelected: (TransportModeOption) -> Unit,
    private val onFlashVoicingStyleSelected: (com.bag.audioandroid.ui.model.FlashVoicingStyleOption) -> Unit,
    private val onMorseSpeedSelected: (MorseSpeedOption) -> Unit,
    private val onInputTextChange: (String) -> Unit,
    private val onEncode: () -> Unit,
) {
    fun startFlashDebugScenario(scenario: FlashDebugScenario) {
        if (!BuildConfig.DEBUG) {
            return
        }
        val input =
            resolveDebugInput(
                mode = TransportModeOption.Flash,
                text = scenario.text,
                hasTextOverride = scenario.hasTextOverride,
                sampleLength = scenario.sampleLength,
                sampleId = scenario.sampleId,
                requestId = scenario.requestId,
                logTag = FLASH_AUTOMATION_TAG,
            )
        onTransportModeSelected(TransportModeOption.Flash)
        onFlashVoicingStyleSelected(scenario.style)
        applyInput(mode = TransportModeOption.Flash, input = input)
        safeLogD(
            FLASH_AUTOMATION_TAG,
            "inputResolved requestId=${scenario.requestId} source=${input.source} " +
                "sampleId=${input.sampleId.orEmpty()} chars=${input.text.length} " +
                "payloadBytes=${input.text.toByteArray(UTF_8).size} style=${scenario.style.id}",
        )
        if (scenario.encode) {
            onEncode()
        }
    }

    fun startMiniDebugScenario(scenario: MiniDebugScenario) {
        if (!BuildConfig.DEBUG) {
            return
        }
        onTransportModeSelected(TransportModeOption.Mini)
        onMorseSpeedSelected(scenario.speed)
        onInputTextChange(scenario.text)
        if (scenario.encode) {
            onEncode()
        }
    }

    fun startEncodeProgressDebugScenario(scenario: EncodeProgressDebugScenario) {
        if (!BuildConfig.DEBUG) {
            return
        }
        val input = resolveEncodeProgressDebugInput(scenario)
        onTransportModeSelected(scenario.mode)
        if (scenario.mode == TransportModeOption.Mini) {
            onMorseSpeedSelected(scenario.speed)
        }
        applyInput(mode = scenario.mode, input = input)
        safeLogD(
            ENCODE_PROGRESS_AUTOMATION_TAG,
            "inputResolved requestId=${scenario.requestId} mode=${scenario.mode.wireName} " +
                "source=${input.source} sampleId=${input.sampleId.orEmpty()} " +
                "chars=${input.text.length} payloadBytes=${input.text.toByteArray(UTF_8).size} " +
                "repeat=${scenario.repeatCount} speed=${scenario.speed.id}",
        )
        if (scenario.encode) {
            onEncode()
        }
    }

    private fun applyInput(
        mode: TransportModeOption,
        input: DebugResolvedInput,
    ) {
        onInputTextChange(input.text)
        if (input.sampleId != null) {
            sessionStateStore.updateSession(mode) {
                it.copy(
                    sampleInputId = input.sampleId,
                    sampleShuffleState = null,
                    appliedSampleEmojiPrefix = null,
                )
            }
        }
    }

    private fun resolveEncodeProgressDebugInput(scenario: EncodeProgressDebugScenario): DebugResolvedInput {
        val input =
            resolveDebugInput(
                mode = scenario.mode,
                text = scenario.text,
                hasTextOverride = scenario.hasTextOverride,
                sampleLength = scenario.sampleLength,
                sampleId = scenario.sampleId,
                requestId = scenario.requestId,
                logTag = ENCODE_PROGRESS_AUTOMATION_TAG,
            )
        val repeatedText =
            if (scenario.repeatCount <= 1) {
                input.text
            } else {
                buildString(input.text.length * scenario.repeatCount) {
                    repeat(scenario.repeatCount) {
                        append(input.text)
                    }
                }
            }
        return input.copy(
            text = repeatedText,
            source =
                if (scenario.repeatCount <= 1) {
                    input.source
                } else {
                    "${input.source}:repeat${scenario.repeatCount}"
                },
        )
    }

    private fun resolveDebugInput(
        mode: TransportModeOption,
        text: String,
        hasTextOverride: Boolean,
        sampleLength: SampleInputLengthOption?,
        sampleId: String?,
        requestId: Long,
        logTag: String,
    ): DebugResolvedInput {
        if (hasTextOverride) {
            return DebugResolvedInput(text = text, sampleId = null, source = "text")
        }

        val state = uiState.value
        if (sampleId != null) {
            val sample =
                sampleInputTextProvider.sampleById(
                    mode = mode,
                    language = state.selectedLanguage,
                    flavor = state.currentSampleFlavor,
                    sampleId = sampleId,
                )
            if (sample != null) {
                return DebugResolvedInput(text = sample.text, sampleId = sample.id, source = "sample:id")
            }
            safeLogD(
                logTag,
                "inputSampleMissing requestId=$requestId mode=${mode.wireName} sampleId=$sampleId " +
                    "flavor=${state.currentSampleFlavor}",
            )
        }

        if (sampleLength != null) {
            val resolvedSampleId =
                sampleInputTextProvider
                    .sampleIds(mode = mode, flavor = state.currentSampleFlavor, length = sampleLength)
                    .firstOrNull()
            val sample =
                resolvedSampleId?.let {
                    sampleInputTextProvider.sampleById(
                        mode = mode,
                        language = state.selectedLanguage,
                        flavor = state.currentSampleFlavor,
                        sampleId = it,
                    )
                }
            if (sample != null) {
                return DebugResolvedInput(text = sample.text, sampleId = sample.id, source = "sample:${sampleLength.id}")
            }
            safeLogD(
                logTag,
                "inputSampleMissing requestId=$requestId mode=${mode.wireName} sampleLength=${sampleLength.id} " +
                    "flavor=${state.currentSampleFlavor}",
            )
        }

        return DebugResolvedInput(text = text, sampleId = null, source = "text")
    }
}

private const val FLASH_AUTOMATION_TAG = "FlashAutomation"
private const val ENCODE_PROGRESS_AUTOMATION_TAG = "EncodeProgressAutomation"

private data class DebugResolvedInput(
    val text: String,
    val sampleId: String?,
    val source: String,
)

private fun safeLogD(
    tag: String,
    message: String,
) {
    try {
        Log.d(tag, message)
    } catch (_: RuntimeException) {
        // Plain JVM unit tests use the Android stub jar, where Log.d is not implemented.
    }
}
