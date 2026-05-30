package com.bag.audioandroid.domain

sealed interface EncodeAudioResult {
    data class Success(
        val pcm: ShortArray,
    ) : EncodeAudioResult

    data object Cancelled : EncodeAudioResult

    data class Failed(
        val errorCode: Int,
    ) : EncodeAudioResult
}

enum class AudioEncodePhase(
    val nativeValue: Int,
) {
    PreparingInput(0),
    RenderingPcm(1),
    Postprocessing(2),
    Finalizing(3),
    ;

    companion object {
        fun fromNative(value: Int): AudioEncodePhase = entries.firstOrNull { it.nativeValue == value } ?: Finalizing
    }
}

enum class EncodeOperationState(
    val nativeValue: Int,
) {
    Queued(0),
    Running(1),
    Succeeded(2),
    Failed(3),
    Cancelled(4),
    ;

    fun isTerminal(): Boolean =
        this == Succeeded ||
            this == Failed ||
            this == Cancelled

    companion object {
        fun fromNative(value: Int): EncodeOperationState = entries.firstOrNull { it.nativeValue == value } ?: Failed
    }
}

data class EncodeOperationWorkPlan(
    val preparingInputWorkUnits: Long,
    val renderingPcmWorkUnits: Long,
    val postprocessingWorkUnits: Long,
    val finalizingWorkUnits: Long,
    val totalWorkUnits: Long,
    val estimatedPcmSampleCount: Long,
    val payloadByteCount: Long,
    val segmentCount: Long,
) {
    companion object {
        val Empty =
            EncodeOperationWorkPlan(
                preparingInputWorkUnits = 0L,
                renderingPcmWorkUnits = 0L,
                postprocessingWorkUnits = 0L,
                finalizingWorkUnits = 0L,
                totalWorkUnits = 0L,
                estimatedPcmSampleCount = 0L,
                payloadByteCount = 0L,
                segmentCount = 0L,
            )
    }
}

data class EncodeOperationSnapshot(
    val state: EncodeOperationState,
    val phase: AudioEncodePhase,
    val overallProgress0To1: Float,
    val phaseProgress0To1: Float,
    val completedWorkUnits: Long,
    val totalWorkUnits: Long,
    val phaseCompletedWorkUnits: Long,
    val phaseTotalWorkUnits: Long,
    val terminalCode: Int,
    val estimatedPcmSampleCount: Long,
    val payloadByteCount: Long,
    val segmentCount: Long,
    val currentSegmentIndex: Long,
) {
    val isTerminal: Boolean
        get() = state.isTerminal()

    companion object {
        val Initial =
            EncodeOperationSnapshot(
                state = EncodeOperationState.Queued,
                phase = AudioEncodePhase.PreparingInput,
                overallProgress0To1 = 0f,
                phaseProgress0To1 = 0f,
                completedWorkUnits = 0L,
                totalWorkUnits = 0L,
                phaseCompletedWorkUnits = 0L,
                phaseTotalWorkUnits = 0L,
                terminalCode = BagApiCodes.ERROR_NOT_READY,
                estimatedPcmSampleCount = 0L,
                payloadByteCount = 0L,
                segmentCount = 0L,
                currentSegmentIndex = 0L,
            )
    }
}

data class EncodeProgressUpdate(
    val phase: AudioEncodePhase,
    val progress0To1: Float,
    val snapshot: EncodeOperationSnapshot =
        EncodeOperationSnapshot(
            state = EncodeOperationState.Running,
            phase = phase,
            overallProgress0To1 = progress0To1.coerceIn(0f, 1f),
            phaseProgress0To1 = progress0To1.coerceIn(0f, 1f),
            completedWorkUnits = 0L,
            totalWorkUnits = 0L,
            phaseCompletedWorkUnits = 0L,
            phaseTotalWorkUnits = 0L,
            terminalCode = BagApiCodes.ERROR_NOT_READY,
            estimatedPcmSampleCount = 0L,
            payloadByteCount = 0L,
            segmentCount = 0L,
            currentSegmentIndex = 0L,
        ),
    val workPlan: EncodeOperationWorkPlan = EncodeOperationWorkPlan.Empty,
)

enum class AudioDecodePhase(
    val nativeValue: Int,
) {
    PreparingInput(0),
    ReadingPcm(1),
    DecodingPayload(2),
    Finalizing(3),
    ;

    companion object {
        fun fromNative(value: Int): AudioDecodePhase = entries.firstOrNull { it.nativeValue == value } ?: Finalizing
    }
}

enum class DecodeOperationState(
    val nativeValue: Int,
) {
    Queued(0),
    Running(1),
    Succeeded(2),
    Failed(3),
    Cancelled(4),
    ;

    fun isTerminal(): Boolean =
        this == Succeeded ||
            this == Failed ||
            this == Cancelled

    companion object {
        fun fromNative(value: Int): DecodeOperationState = entries.firstOrNull { it.nativeValue == value } ?: Failed
    }
}

data class DecodeOperationWorkPlan(
    val preparingInputWorkUnits: Long,
    val readingPcmWorkUnits: Long,
    val decodingPayloadWorkUnits: Long,
    val finalizingWorkUnits: Long,
    val totalWorkUnits: Long,
    val pcmSampleCount: Long,
) {
    companion object {
        val Empty =
            DecodeOperationWorkPlan(
                preparingInputWorkUnits = 0L,
                readingPcmWorkUnits = 0L,
                decodingPayloadWorkUnits = 0L,
                finalizingWorkUnits = 0L,
                totalWorkUnits = 0L,
                pcmSampleCount = 0L,
            )
    }
}

data class DecodeOperationSnapshot(
    val state: DecodeOperationState,
    val phase: AudioDecodePhase,
    val overallProgress0To1: Float,
    val phaseProgress0To1: Float,
    val completedWorkUnits: Long,
    val totalWorkUnits: Long,
    val phaseCompletedWorkUnits: Long,
    val phaseTotalWorkUnits: Long,
    val terminalCode: Int,
    val pcmSampleCount: Long,
    val pushedPcmSampleCount: Long,
) {
    val isTerminal: Boolean
        get() = state.isTerminal()

    companion object {
        val Initial =
            DecodeOperationSnapshot(
                state = DecodeOperationState.Queued,
                phase = AudioDecodePhase.PreparingInput,
                overallProgress0To1 = 0f,
                phaseProgress0To1 = 0f,
                completedWorkUnits = 0L,
                totalWorkUnits = 0L,
                phaseCompletedWorkUnits = 0L,
                phaseTotalWorkUnits = 0L,
                terminalCode = BagApiCodes.ERROR_NOT_READY,
                pcmSampleCount = 0L,
                pushedPcmSampleCount = 0L,
            )
    }
}

data class DecodeProgressUpdate(
    val phase: AudioDecodePhase,
    val progress0To1: Float,
    val snapshot: DecodeOperationSnapshot,
    val workPlan: DecodeOperationWorkPlan = DecodeOperationWorkPlan.Empty,
)
