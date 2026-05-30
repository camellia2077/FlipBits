package com.bag.audioandroid.ui

import com.bag.audioandroid.domain.AudioDecodePhase
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.DecodeOperationSnapshot
import com.bag.audioandroid.domain.DecodeOperationState
import com.bag.audioandroid.domain.DecodeOperationWorkPlan
import com.bag.audioandroid.domain.DecodeProgressUpdate

internal enum class SavedDecodeProgressStage(
    val phase: AudioDecodePhase,
    val progressStart0To1: Float,
    val progressEnd0To1: Float,
) {
    PreparingInput(AudioDecodePhase.PreparingInput, 0.00f, 0.05f),
    LoadingAudioData(AudioDecodePhase.ReadingPcm, 0.05f, 0.20f),
    AnalyzingSignal(AudioDecodePhase.DecodingPayload, 0.20f, 0.90f),
    BuildingPlaybackData(AudioDecodePhase.Finalizing, 0.90f, 1.00f),
}

internal fun mapSavedDecodeProgress0To1(
    stage: SavedDecodeProgressStage,
    stageProgress0To1: Float,
): Float {
    val clampedStageProgress = stageProgress0To1.coerceIn(0f, 1f)
    return stage.progressStart0To1 +
        (stage.progressEnd0To1 - stage.progressStart0To1) * clampedStageProgress
}

internal class SavedDecodeProgressReporter(
    private val onProgress: (DecodeProgressUpdate) -> Unit,
) {
    fun reportPreparing() {
        reportStage(SavedDecodeProgressStage.PreparingInput, 0f)
    }

    fun reportAudioDataLoading(
        completedSamples: Int,
        totalSamples: Int,
    ) {
        val progress =
            if (totalSamples <= 0) {
                1f
            } else {
                completedSamples.toFloat() / totalSamples.toFloat()
            }
        reportStage(SavedDecodeProgressStage.LoadingAudioData, progress)
    }

    fun reportNativeDecode(update: DecodeProgressUpdate) {
        val mappedProgress =
            mapSavedDecodeProgress0To1(
                SavedDecodeProgressStage.AnalyzingSignal,
                update.snapshot.overallProgress0To1,
            )
        onProgress(
            update.copy(
                progress0To1 = mappedProgress,
                snapshot =
                    update.snapshot.copy(
                        overallProgress0To1 = mappedProgress,
                        completedWorkUnits = savedDecodeCompletedWorkUnits(mappedProgress),
                        totalWorkUnits = SAVED_DECODE_TOTAL_WORK_UNITS,
                    ),
                workPlan = SAVED_DECODE_WORK_PLAN,
            ),
        )
    }

    fun reportPlaybackDataBuilding(progress0To1: Float) {
        reportStage(SavedDecodeProgressStage.BuildingPlaybackData, progress0To1)
    }

    private fun reportStage(
        stage: SavedDecodeProgressStage,
        stageProgress0To1: Float,
    ) {
        val clampedStageProgress = stageProgress0To1.coerceIn(0f, 1f)
        val overallProgress = mapSavedDecodeProgress0To1(stage, clampedStageProgress)
        val phaseTotalWorkUnits = savedDecodeStageWorkUnits(stage)
        onProgress(
            DecodeProgressUpdate(
                phase = stage.phase,
                progress0To1 = overallProgress,
                snapshot =
                    DecodeOperationSnapshot(
                        state = DecodeOperationState.Running,
                        phase = stage.phase,
                        overallProgress0To1 = overallProgress,
                        phaseProgress0To1 = clampedStageProgress,
                        completedWorkUnits = savedDecodeCompletedWorkUnits(overallProgress),
                        totalWorkUnits = SAVED_DECODE_TOTAL_WORK_UNITS,
                        phaseCompletedWorkUnits = (phaseTotalWorkUnits * clampedStageProgress).toLong(),
                        phaseTotalWorkUnits = phaseTotalWorkUnits,
                        terminalCode = BagApiCodes.ERROR_NOT_READY,
                        pcmSampleCount = 0L,
                        pushedPcmSampleCount = 0L,
                    ),
                workPlan = SAVED_DECODE_WORK_PLAN,
            ),
        )
    }
}

private const val SAVED_DECODE_TOTAL_WORK_UNITS = 1_000_000L

private val SAVED_DECODE_WORK_PLAN =
    DecodeOperationWorkPlan(
        preparingInputWorkUnits = savedDecodeStageWorkUnits(SavedDecodeProgressStage.PreparingInput),
        readingPcmWorkUnits = savedDecodeStageWorkUnits(SavedDecodeProgressStage.LoadingAudioData),
        decodingPayloadWorkUnits = savedDecodeStageWorkUnits(SavedDecodeProgressStage.AnalyzingSignal),
        finalizingWorkUnits = savedDecodeStageWorkUnits(SavedDecodeProgressStage.BuildingPlaybackData),
        totalWorkUnits = SAVED_DECODE_TOTAL_WORK_UNITS,
        pcmSampleCount = 0L,
    )

private fun savedDecodeCompletedWorkUnits(progress0To1: Float): Long =
    (progress0To1.coerceIn(0f, 1f) * SAVED_DECODE_TOTAL_WORK_UNITS).toLong()

private fun savedDecodeStageWorkUnits(stage: SavedDecodeProgressStage): Long =
    ((stage.progressEnd0To1 - stage.progressStart0To1) * SAVED_DECODE_TOTAL_WORK_UNITS).toLong()
