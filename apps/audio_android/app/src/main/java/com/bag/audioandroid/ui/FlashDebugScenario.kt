package com.bag.audioandroid.ui

import android.content.Intent
import android.util.Log
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.MorseSpeedOption
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.screen.FlashSignalVisualizationMode

data class FlashDebugScenario(
    val text: String = DefaultText,
    val hasTextOverride: Boolean = false,
    val sampleLength: SampleInputLengthOption? = null,
    val sampleId: String? = null,
    val scenario: FlashDebugScenarioKind = FlashDebugScenarioKind.Ui,
    val style: FlashVoicingStyleOption = FlashVoicingStyleOption.Steady,
    val visualMode: FlashSignalVisualizationMode = FlashSignalVisualizationMode.ToneTracks,
    val encode: Boolean = true,
    val play: Boolean = true,
    val playDurationMs: Long = DefaultPlayDurationMs,
    val requestId: Long = System.nanoTime(),
) {
    companion object {
        const val Action = "com.bag.audioandroid.DEBUG_FLASH_SCENARIO"
        const val ExtraText = "wb.input"
        const val ExtraSampleLength = "wb.sample.length"
        const val ExtraSampleId = "wb.sample.id"
        const val ExtraScenario = "wb.scenario"
        const val ExtraStyle = "wb.flash.style"
        const val ExtraVisual = "wb.visual"
        const val ExtraEncode = "wb.encode"
        const val ExtraPlay = "wb.play"
        const val ExtraPlayDurationMs = "wb.play.ms"
        const val DefaultText = "flash sync test"
        const val DefaultPlayDurationMs = 6_000L
        private const val Tag = "FlashAutomation"

        fun fromIntent(intent: Intent?): FlashDebugScenario? {
            if (!BuildConfig.DEBUG || intent?.action != Action) {
                return null
            }
            val textOverride = intent.getStringExtra(ExtraText)?.takeIf { it.isNotBlank() }
            val scenario =
                FlashDebugScenario(
                    text = textOverride ?: DefaultText,
                    hasTextOverride = textOverride != null,
                    sampleLength = SampleInputLengthOption.fromId(intent.getStringExtra(ExtraSampleLength)),
                    sampleId = intent.getStringExtra(ExtraSampleId)?.takeIf { it.isNotBlank() },
                    scenario = FlashDebugScenarioKind.fromId(intent.getStringExtra(ExtraScenario)),
                    style = FlashVoicingStyleOption.fromId(intent.getStringExtra(ExtraStyle)),
                    visualMode = intent.getStringExtra(ExtraVisual).toFlashVisualizationMode(),
                    encode = intent.getBooleanExtra(ExtraEncode, true),
                    play = intent.getBooleanExtra(ExtraPlay, true),
                    playDurationMs = intent.getLongExtra(ExtraPlayDurationMs, DefaultPlayDurationMs).coerceAtLeast(0L),
                )
            Log.d(
                Tag,
                "received scenario=${scenario.scenario.id} style=${scenario.style.id} visual=${scenario.visualMode.name} " +
                    "encode=${scenario.encode} play=${scenario.play} playMs=${scenario.playDurationMs} " +
                    "requestId=${scenario.requestId} input=${scenario.inputDebugSummary()}",
            )
            return scenario
        }
    }
}

private fun FlashDebugScenario.inputDebugSummary(): String {
    if (hasTextOverride) {
        return "text chars=${text.length}"
    }
    val sampleIdPart = sampleId?.let { " sampleId=$it" }.orEmpty()
    val sampleLengthPart = sampleLength?.let { " sampleLength=${it.id}" }.orEmpty()
    return if (sampleId != null || sampleLength != null) {
        "sample$sampleIdPart$sampleLengthPart fallbackChars=${text.length}"
    } else {
        "text chars=${text.length}"
    }
}

data class MiniDebugScenario(
    val text: String = DefaultText,
    val scenario: FlashDebugScenarioKind = FlashDebugScenarioKind.Ui,
    val speed: MorseSpeedOption = MorseSpeedOption.default,
    val encode: Boolean = true,
    val play: Boolean = true,
    val playDurationMs: Long = DefaultPlayDurationMs,
    val requestId: Long = System.nanoTime(),
) {
    companion object {
        const val Action = "com.bag.audioandroid.DEBUG_MINI_SCENARIO"
        const val ExtraText = FlashDebugScenario.ExtraText
        const val ExtraScenario = FlashDebugScenario.ExtraScenario
        const val ExtraSpeed = "wb.mini.speed"
        const val ExtraEncode = FlashDebugScenario.ExtraEncode
        const val ExtraPlay = FlashDebugScenario.ExtraPlay
        const val ExtraPlayDurationMs = FlashDebugScenario.ExtraPlayDurationMs
        const val DefaultText = "mini sync test"
        const val DefaultPlayDurationMs = 6_000L
        private const val Tag = "MiniAutomation"

        fun fromIntent(intent: Intent?): MiniDebugScenario? {
            if (!BuildConfig.DEBUG || intent?.action != Action) {
                return null
            }
            val scenario =
                MiniDebugScenario(
                    text = intent.getStringExtra(ExtraText)?.takeIf { it.isNotBlank() } ?: DefaultText,
                    scenario = FlashDebugScenarioKind.fromId(intent.getStringExtra(ExtraScenario)),
                    speed = intent.getStringExtra(ExtraSpeed).toMorseSpeedOption(),
                    encode = intent.getBooleanExtra(ExtraEncode, true),
                    play = intent.getBooleanExtra(ExtraPlay, true),
                    playDurationMs = intent.getLongExtra(ExtraPlayDurationMs, DefaultPlayDurationMs).coerceAtLeast(0L),
                )
            Log.d(
                Tag,
                "received scenario=${scenario.scenario.id} speed=${scenario.speed.id} " +
                    "encode=${scenario.encode} play=${scenario.play} playMs=${scenario.playDurationMs} " +
                    "requestId=${scenario.requestId} text=${scenario.text}",
            )
            return scenario
        }
    }
}

data class EncodeProgressDebugScenario(
    val mode: TransportModeOption = TransportModeOption.Mini,
    val text: String = DefaultText,
    val hasTextOverride: Boolean = false,
    val sampleLength: SampleInputLengthOption? = null,
    val sampleId: String? = null,
    val repeatCount: Int = 1,
    val speed: MorseSpeedOption = MorseSpeedOption.default,
    val encode: Boolean = true,
    val captureDurationMs: Long = DefaultCaptureDurationMs,
    val pollIntervalMs: Long = DefaultPollIntervalMs,
    val requestId: Long = System.nanoTime(),
) {
    companion object {
        const val Action = "com.bag.audioandroid.DEBUG_ENCODE_PROGRESS_SCENARIO"
        const val ExtraMode = "wb.mode"
        const val ExtraText = FlashDebugScenario.ExtraText
        const val ExtraSampleLength = FlashDebugScenario.ExtraSampleLength
        const val ExtraSampleId = FlashDebugScenario.ExtraSampleId
        const val ExtraRepeat = "wb.repeat"
        const val ExtraSpeed = MiniDebugScenario.ExtraSpeed
        const val ExtraEncode = FlashDebugScenario.ExtraEncode
        const val DefaultText = "encode progress test"
        const val ExtraCaptureDurationMs = "wb.capture.ms"
        const val ExtraPollIntervalMs = "wb.poll.ms"
        const val DefaultCaptureDurationMs = 120_000L
        const val DefaultPollIntervalMs = 33L
        private const val Tag = "EncodeProgressAutomation"

        fun fromIntent(intent: Intent?): EncodeProgressDebugScenario? {
            if (!BuildConfig.DEBUG || intent?.action != Action) {
                return null
            }
            val textOverride = intent.getStringExtra(ExtraText)?.takeIf { it.isNotBlank() }
            val scenario =
                EncodeProgressDebugScenario(
                    mode = intent.getStringExtra(ExtraMode).toProgressScenarioMode(),
                    text = textOverride ?: DefaultText,
                    hasTextOverride = textOverride != null,
                    sampleLength = SampleInputLengthOption.fromId(intent.getStringExtra(ExtraSampleLength)),
                    sampleId = intent.getStringExtra(ExtraSampleId)?.takeIf { it.isNotBlank() },
                    repeatCount = intent.getIntExtra(ExtraRepeat, 1).coerceIn(1, 50),
                    speed = intent.getStringExtra(ExtraSpeed).toMorseSpeedOption(),
                    encode = intent.getBooleanExtra(ExtraEncode, true),
                    captureDurationMs =
                        intent
                            .getLongExtra(ExtraCaptureDurationMs, DefaultCaptureDurationMs)
                            .coerceIn(1_000L, 600_000L),
                    pollIntervalMs =
                        intent
                            .getLongExtra(ExtraPollIntervalMs, DefaultPollIntervalMs)
                            .coerceIn(16L, 1_000L),
                )
            Log.d(
                Tag,
                "received mode=${scenario.mode.wireName} speed=${scenario.speed.id} " +
                    "encode=${scenario.encode} repeat=${scenario.repeatCount} " +
                    "captureMs=${scenario.captureDurationMs} pollMs=${scenario.pollIntervalMs} " +
                    "requestId=${scenario.requestId} input=${scenario.inputDebugSummary()}",
            )
            return scenario
        }
    }
}

private fun EncodeProgressDebugScenario.inputDebugSummary(): String {
    if (hasTextOverride) {
        return "text chars=${text.length}"
    }
    val sampleIdPart = sampleId?.let { " sampleId=$it" }.orEmpty()
    val sampleLengthPart = sampleLength?.let { " sampleLength=${it.id}" }.orEmpty()
    return if (sampleId != null || sampleLength != null) {
        "sample$sampleIdPart$sampleLengthPart fallbackChars=${text.length}"
    } else {
        "text chars=${text.length}"
    }
}

enum class FlashDebugScenarioKind(
    val id: String,
) {
    Ui("ui"),
    Headless("headless"),
    ;

    companion object {
        fun fromId(id: String?): FlashDebugScenarioKind = entries.firstOrNull { it.id == id?.lowercase() } ?: Ui
    }
}

private fun String?.toFlashVisualizationMode(): FlashSignalVisualizationMode =
    when (this?.lowercase()) {
        "lanes", "tracks", "tonetracks", "tone_tracks" -> FlashSignalVisualizationMode.ToneTracks
        "energy", "toneenergy", "tone_energy" -> FlashSignalVisualizationMode.ToneEnergy
        "pitch", "pitchladder", "pitch_ladder" -> FlashSignalVisualizationMode.PitchLadder
        else -> FlashSignalVisualizationMode.ToneTracks
    }

internal val MorseSpeedOption.id: String
    get() =
        when (this) {
            MorseSpeedOption.Slow -> "slow"
            MorseSpeedOption.Standard -> "standard"
            MorseSpeedOption.Fast -> "fast"
        }

private fun String?.toMorseSpeedOption(): MorseSpeedOption =
    when (this?.lowercase()) {
        "slow" -> MorseSpeedOption.Slow
        "fast" -> MorseSpeedOption.Fast
        else -> MorseSpeedOption.Standard
    }

private fun String?.toProgressScenarioMode(): TransportModeOption =
    when (this?.lowercase()) {
        "pro" -> TransportModeOption.Pro
        "ultra" -> TransportModeOption.Ultra
        "mini" -> TransportModeOption.Mini
        else -> TransportModeOption.Mini
    }
