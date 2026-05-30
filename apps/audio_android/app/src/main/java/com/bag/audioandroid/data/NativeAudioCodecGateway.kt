package com.bag.audioandroid.data

import android.util.Log
import com.bag.audioandroid.NativeBagBridge
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.AudioDecodePhase
import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.DecodeOperationSnapshot
import com.bag.audioandroid.domain.DecodeOperationState
import com.bag.audioandroid.domain.DecodeOperationWorkPlan
import com.bag.audioandroid.domain.DecodeProgressUpdate
import com.bag.audioandroid.domain.DecodedAudioPayloadResult
import com.bag.audioandroid.domain.EncodeAudioResult
import com.bag.audioandroid.domain.EncodeOperationSnapshot
import com.bag.audioandroid.domain.EncodeOperationState
import com.bag.audioandroid.domain.EncodeOperationWorkPlan
import com.bag.audioandroid.domain.EncodeProgressUpdate
import com.bag.audioandroid.domain.EncodedAudioPayloadResult
import com.bag.audioandroid.domain.FlashSignalInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.yield

class NativeAudioCodecGateway : AudioCodecGateway {
    override fun validateEncodeRequest(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): Int =
        NativeBagBridge.nativeValidateEncodeRequest(
            text,
            sampleRateHz,
            frameSamples,
            mode,
            flashSignalProfile,
            flashVoicingFlavor,
        )

    override suspend fun encodeTextToPcm(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
        onProgress: (EncodeProgressUpdate) -> Unit,
    ): EncodeAudioResult {
        val handle =
            NativeBagBridge.nativeCreateEncodeOperation(
                text,
                sampleRateHz,
                frameSamples,
                mode,
                flashSignalProfile,
                flashVoicingFlavor,
            )
        if (handle == 0L) {
            return EncodeAudioResult.Failed(BagApiCodes.ERROR_INTERNAL)
        }

        try {
            val workPlan =
                NativeBagBridge
                    .nativeGetEncodeOperationWorkPlan(handle)
                    .toEncodeOperationWorkPlan()
            var lastProgressEmitNanos = 0L
            var lastProgressPhase: AudioEncodePhase? = null
            var pumpIterationsSinceYield = 0
            while (true) {
                currentCoroutineContext().ensureActive()
                val pumpCode =
                    NativeBagBridge.nativePumpEncodeOperation(
                        handle = handle,
                        maxWorkUnits = ENCODE_OPERATION_PUMP_MAX_WORK_UNITS,
                        maxWallTimeMs = ENCODE_OPERATION_PUMP_MAX_WALL_TIME_MS,
                    )
                if (pumpCode != BagApiCodes.ERROR_OK && pumpCode != BagApiCodes.ERROR_NOT_READY) {
                    return EncodeAudioResult.Failed(pumpCode)
                }
                val snapshot =
                    NativeBagBridge
                        .nativePollEncodeOperation(handle)
                        .toEncodeOperationSnapshot()
                val nowNanos = System.nanoTime()
                if (
                    shouldEmitEncodeOperationProgress(
                        snapshot = snapshot,
                        nowNanos = nowNanos,
                        lastEmitNanos = lastProgressEmitNanos,
                        lastPhase = lastProgressPhase,
                    )
                ) {
                    onProgress(
                        EncodeProgressUpdate(
                            phase = snapshot.phase,
                            progress0To1 = snapshot.overallProgress0To1,
                            snapshot = snapshot,
                            workPlan = workPlan,
                        ),
                    )
                    lastProgressEmitNanos = nowNanos
                    lastProgressPhase = snapshot.phase
                }
                when (snapshot.state) {
                    EncodeOperationState.Queued,
                    EncodeOperationState.Running,
                    -> {
                        pumpIterationsSinceYield += 1
                        if (pumpCode == BagApiCodes.ERROR_NOT_READY) {
                            delay(ENCODE_OPERATION_IDLE_DELAY_MS)
                            pumpIterationsSinceYield = 0
                        } else if (pumpIterationsSinceYield >= ENCODE_OPERATION_YIELD_EVERY_PUMPS) {
                            pumpIterationsSinceYield = 0
                            yield()
                        }
                    }

                    EncodeOperationState.Succeeded ->
                        return NativeBagBridge
                            .nativeTakeEncodeOperationResult(handle)
                            .toEncodeSuccessOrFailureResult()

                    EncodeOperationState.Failed -> return EncodeAudioResult.Failed(snapshot.terminalCode)
                    EncodeOperationState.Cancelled -> return EncodeAudioResult.Cancelled
                }
            }
        } catch (cancelled: CancellationException) {
            NativeBagBridge.nativeCancelEncodeOperation(handle)
            throw cancelled
        } finally {
            NativeBagBridge.nativeCancelEncodeOperation(handle)
            NativeBagBridge.nativeDestroyEncodeOperation(handle)
        }
    }

    override suspend fun buildEncodeFollowData(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): EncodedAudioPayloadResult =
        NativeBagBridge.nativeBuildEncodeFollowData(
            text,
            sampleRateHz,
            frameSamples,
            mode,
            flashSignalProfile,
            flashVoicingFlavor,
        )

    override fun describeFlashSignal(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): FlashSignalInfo =
        NativeBagBridge.nativeDescribeFlashSignal(
            text,
            sampleRateHz,
            frameSamples,
            flashSignalProfile,
            flashVoicingFlavor,
        )

    override fun validateDecodeConfig(
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): Int =
        NativeBagBridge.nativeValidateDecodeConfig(
            sampleRateHz,
            frameSamples,
            mode,
            flashSignalProfile,
            flashVoicingFlavor,
        )

    override suspend fun decodeGeneratedPcm(
        pcm: ShortArray,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
        onProgress: (DecodeProgressUpdate) -> Unit,
    ): DecodedAudioPayloadResult {
        val createStartedNanos = System.nanoTime()
        val handle =
            NativeBagBridge.nativeCreateDecodeOperation(
                pcm,
                sampleRateHz,
                frameSamples,
                mode,
                flashSignalProfile,
                flashVoicingFlavor,
            )
        safeLogE(
            SAVED_DECODE_TIMING_LOG_TAG,
            "nativeCreateDecodeOperation elapsedMs=${elapsedMsSince(createStartedNanos)} " +
                "handleCreated=${handle != 0L} samples=${pcm.size} sampleRate=$sampleRateHz frameSamples=$frameSamples " +
                "mode=$mode signalProfile=$flashSignalProfile voicingFlavor=$flashVoicingFlavor",
        )
        if (handle == 0L) {
            val fallbackStartedNanos = System.nanoTime()
            val result =
                NativeBagBridge.nativeDecodeGeneratedPcm(
                    pcm,
                    sampleRateHz,
                    frameSamples,
                    mode,
                    flashSignalProfile,
                    flashVoicingFlavor,
                )
            safeLogE(
                SAVED_DECODE_TIMING_LOG_TAG,
                "nativeDecodeGeneratedPcmFallback elapsedMs=${elapsedMsSince(fallbackStartedNanos)} samples=${pcm.size}",
            )
            return result
        }

        try {
            val workPlanStartedNanos = System.nanoTime()
            val workPlan =
                NativeBagBridge
                    .nativeGetDecodeOperationWorkPlan(handle)
                    .toDecodeOperationWorkPlan()
            safeLogE(
                SAVED_DECODE_TIMING_LOG_TAG,
                "nativeGetDecodeOperationWorkPlan elapsedMs=${elapsedMsSince(workPlanStartedNanos)} " +
                    "total=${workPlan.totalWorkUnits} reading=${workPlan.readingPcmWorkUnits} " +
                    "payload=${workPlan.decodingPayloadWorkUnits} pcmSamples=${workPlan.pcmSampleCount}",
            )
            var lastProgressEmitNanos = 0L
            var lastProgressPhase: AudioDecodePhase? = null
            var pumpIterationsSinceYield = 0
            var pumpCount = 0
            while (true) {
                currentCoroutineContext().ensureActive()
                pumpCount += 1
                val pumpStartedNanos = System.nanoTime()
                val pumpCode =
                    NativeBagBridge.nativePumpDecodeOperation(
                        handle = handle,
                        maxWorkUnits = DECODE_OPERATION_PUMP_MAX_WORK_UNITS,
                        maxWallTimeMs = DECODE_OPERATION_PUMP_MAX_WALL_TIME_MS,
                    )
                val pumpElapsedMs = elapsedMsSince(pumpStartedNanos)
                if (pumpElapsedMs >= DECODE_OPERATION_SLOW_STEP_LOG_MS) {
                    safeLogE(
                        SAVED_DECODE_TIMING_LOG_TAG,
                        "nativePumpDecodeOperation slow elapsedMs=$pumpElapsedMs pump=$pumpCount code=$pumpCode",
                    )
                }
                if (pumpCode != BagApiCodes.ERROR_OK && pumpCode != BagApiCodes.ERROR_NOT_READY) {
                    return takeDecodeOperationResultWithTiming(handle, "pumpCode=$pumpCode")
                }
                val pollStartedNanos = System.nanoTime()
                val snapshot =
                    NativeBagBridge
                        .nativePollDecodeOperation(handle)
                        .toDecodeOperationSnapshot()
                val pollElapsedMs = elapsedMsSince(pollStartedNanos)
                if (pollElapsedMs >= DECODE_OPERATION_SLOW_STEP_LOG_MS) {
                    safeLogE(
                        SAVED_DECODE_TIMING_LOG_TAG,
                        "nativePollDecodeOperation slow elapsedMs=$pollElapsedMs pump=$pumpCount " +
                            "state=${snapshot.state.name} phase=${snapshot.phase.name} percent=${snapshot.overallProgress0To1}",
                    )
                }
                val nowNanos = System.nanoTime()
                if (
                    shouldEmitDecodeOperationProgress(
                        snapshot = snapshot,
                        nowNanos = nowNanos,
                        lastEmitNanos = lastProgressEmitNanos,
                        lastPhase = lastProgressPhase,
                    )
                ) {
                    onProgress(
                        DecodeProgressUpdate(
                            phase = snapshot.phase,
                            progress0To1 = snapshot.overallProgress0To1,
                            snapshot = snapshot,
                            workPlan = workPlan,
                        ),
                    )
                    lastProgressEmitNanos = nowNanos
                    lastProgressPhase = snapshot.phase
                }
                when (snapshot.state) {
                    DecodeOperationState.Queued,
                    DecodeOperationState.Running,
                    -> {
                        pumpIterationsSinceYield += 1
                        if (pumpCode == BagApiCodes.ERROR_NOT_READY) {
                            delay(DECODE_OPERATION_IDLE_DELAY_MS)
                            pumpIterationsSinceYield = 0
                        } else if (pumpIterationsSinceYield >= DECODE_OPERATION_YIELD_EVERY_PUMPS) {
                            pumpIterationsSinceYield = 0
                            yield()
                        }
                    }

                    DecodeOperationState.Succeeded -> return takeDecodeOperationResultWithTiming(handle, "succeeded")
                    DecodeOperationState.Failed,
                    DecodeOperationState.Cancelled,
                    -> return takeDecodeOperationResultWithTiming(handle, snapshot.state.name.lowercase())
                }
            }
        } catch (cancelled: CancellationException) {
            NativeBagBridge.nativeCancelDecodeOperation(handle)
            throw cancelled
        } finally {
            NativeBagBridge.nativeCancelDecodeOperation(handle)
            NativeBagBridge.nativeDestroyDecodeOperation(handle)
        }
    }

    override suspend fun decodePcmFileSegment(
        pcmFilePath: String,
        startSample: Long,
        sampleCount: Int,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): DecodedAudioPayloadResult =
        NativeBagBridge.nativeDecodePcmFileSegment(
            pcmFilePath = pcmFilePath,
            startSample = startSample,
            sampleCount = sampleCount,
            sampleRateHz = sampleRateHz,
            frameSamples = frameSamples,
            mode = mode,
            flashSignalProfile = flashSignalProfile,
            flashVoicingFlavor = flashVoicingFlavor,
        )

    override fun getCoreVersion(): String = NativeBagBridge.nativeGetCoreVersion()
}

private fun takeDecodeOperationResultWithTiming(
    handle: Long,
    reason: String,
): DecodedAudioPayloadResult {
    val startedNanos = System.nanoTime()
    val result = NativeBagBridge.nativeTakeDecodeOperationResult(handle)
    safeLogE(
        SAVED_DECODE_TIMING_LOG_TAG,
        "nativeTakeDecodeOperationResult elapsedMs=${elapsedMsSince(startedNanos)} reason=$reason " +
            "status=${result.decodedPayload.textDecodeStatusCode} textChars=${result.decodedPayload.text.length}",
    )
    return result
}

private fun elapsedMsSince(startedNanos: Long): Long = (System.nanoTime() - startedNanos).coerceAtLeast(0L) / 1_000_000L

private fun safeLogE(
    tag: String,
    message: String,
) {
    try {
        Log.e(tag, message)
    } catch (_: RuntimeException) {
        // Plain JVM unit tests use the Android stub jar, where Log.e is not implemented.
    }
}

private const val SAVED_DECODE_TIMING_LOG_TAG = "SavedAudioDecodeProgress"
private const val DECODE_OPERATION_SLOW_STEP_LOG_MS = 50L

private const val ENCODE_OPERATION_PROGRESS_EMIT_INTERVAL_NANOS = 33_000_000L
private const val ENCODE_OPERATION_IDLE_DELAY_MS = 1L
private const val ENCODE_OPERATION_PUMP_MAX_WORK_UNITS = 32_768
private const val ENCODE_OPERATION_PUMP_MAX_WALL_TIME_MS = 8
private const val ENCODE_OPERATION_YIELD_EVERY_PUMPS = 4
private const val DECODE_OPERATION_PROGRESS_EMIT_INTERVAL_NANOS = 33_000_000L
private const val DECODE_OPERATION_IDLE_DELAY_MS = 1L
private const val DECODE_OPERATION_PUMP_MAX_WORK_UNITS = 32_768
private const val DECODE_OPERATION_PUMP_MAX_WALL_TIME_MS = 8
private const val DECODE_OPERATION_YIELD_EVERY_PUMPS = 4

private fun shouldEmitEncodeOperationProgress(
    snapshot: EncodeOperationSnapshot,
    nowNanos: Long,
    lastEmitNanos: Long,
    lastPhase: AudioEncodePhase?,
): Boolean =
    snapshot.isTerminal ||
        lastEmitNanos == 0L ||
        snapshot.phase != lastPhase ||
        nowNanos - lastEmitNanos >= ENCODE_OPERATION_PROGRESS_EMIT_INTERVAL_NANOS

private fun shouldEmitDecodeOperationProgress(
    snapshot: DecodeOperationSnapshot,
    nowNanos: Long,
    lastEmitNanos: Long,
    lastPhase: AudioDecodePhase?,
): Boolean =
    snapshot.isTerminal ||
        lastEmitNanos == 0L ||
        snapshot.phase != lastPhase ||
        nowNanos - lastEmitNanos >= DECODE_OPERATION_PROGRESS_EMIT_INTERVAL_NANOS

private const val ENCODE_OPERATION_WORK_PLAN_FIELD_COUNT = 8
private const val DECODE_OPERATION_WORK_PLAN_FIELD_COUNT = 6

internal fun DoubleArray.toEncodeOperationSnapshot(): EncodeOperationSnapshot {
    val stateValue = getOrNull(0)?.toInt() ?: EncodeOperationState.Failed.nativeValue
    val phaseValue = getOrNull(1)?.toInt() ?: AudioEncodePhase.Finalizing.nativeValue
    val overallProgress = getOrNull(2)?.coerceIn(0.0, 1.0)?.toFloat() ?: 0f
    val phaseProgress = getOrNull(3)?.coerceIn(0.0, 1.0)?.toFloat() ?: 0f
    val completedWorkUnits = getOrNull(4).toNonNegativeLong()
    val totalWorkUnits = getOrNull(5).toNonNegativeLong()
    val phaseCompletedWorkUnits = getOrNull(6).toNonNegativeLong()
    val phaseTotalWorkUnits = getOrNull(7).toNonNegativeLong()
    val terminalCode = getOrNull(8)?.toInt() ?: BagApiCodes.ERROR_INTERNAL
    val estimatedPcmSampleCount = getOrNull(9).toNonNegativeLong()
    val payloadByteCount = getOrNull(10).toNonNegativeLong()
    val segmentCount = getOrNull(11).toNonNegativeLong()
    val currentSegmentIndex = getOrNull(12).toNonNegativeLong()
    return EncodeOperationSnapshot(
        state = EncodeOperationState.fromNative(stateValue),
        phase = AudioEncodePhase.fromNative(phaseValue),
        overallProgress0To1 = overallProgress,
        phaseProgress0To1 = phaseProgress,
        completedWorkUnits = completedWorkUnits,
        totalWorkUnits = totalWorkUnits,
        phaseCompletedWorkUnits = phaseCompletedWorkUnits,
        phaseTotalWorkUnits = phaseTotalWorkUnits,
        terminalCode = terminalCode,
        estimatedPcmSampleCount = estimatedPcmSampleCount,
        payloadByteCount = payloadByteCount,
        segmentCount = segmentCount,
        currentSegmentIndex = currentSegmentIndex,
    )
}

internal fun DoubleArray.toEncodeOperationWorkPlan(): EncodeOperationWorkPlan {
    if (size < ENCODE_OPERATION_WORK_PLAN_FIELD_COUNT) {
        return EncodeOperationWorkPlan.Empty
    }
    return EncodeOperationWorkPlan(
        preparingInputWorkUnits = getOrNull(0).toNonNegativeLong(),
        renderingPcmWorkUnits = getOrNull(1).toNonNegativeLong(),
        postprocessingWorkUnits = getOrNull(2).toNonNegativeLong(),
        finalizingWorkUnits = getOrNull(3).toNonNegativeLong(),
        totalWorkUnits = getOrNull(4).toNonNegativeLong(),
        estimatedPcmSampleCount = getOrNull(5).toNonNegativeLong(),
        payloadByteCount = getOrNull(6).toNonNegativeLong(),
        segmentCount = getOrNull(7).toNonNegativeLong(),
    )
}

internal fun DoubleArray.toDecodeOperationSnapshot(): DecodeOperationSnapshot {
    val stateValue = getOrNull(0)?.toInt() ?: DecodeOperationState.Failed.nativeValue
    val phaseValue = getOrNull(1)?.toInt() ?: AudioDecodePhase.Finalizing.nativeValue
    val overallProgress = getOrNull(2)?.coerceIn(0.0, 1.0)?.toFloat() ?: 0f
    val phaseProgress = getOrNull(3)?.coerceIn(0.0, 1.0)?.toFloat() ?: 0f
    return DecodeOperationSnapshot(
        state = DecodeOperationState.fromNative(stateValue),
        phase = AudioDecodePhase.fromNative(phaseValue),
        overallProgress0To1 = overallProgress,
        phaseProgress0To1 = phaseProgress,
        completedWorkUnits = getOrNull(4).toNonNegativeLong(),
        totalWorkUnits = getOrNull(5).toNonNegativeLong(),
        phaseCompletedWorkUnits = getOrNull(6).toNonNegativeLong(),
        phaseTotalWorkUnits = getOrNull(7).toNonNegativeLong(),
        terminalCode = getOrNull(8)?.toInt() ?: BagApiCodes.ERROR_INTERNAL,
        pcmSampleCount = getOrNull(9).toNonNegativeLong(),
        pushedPcmSampleCount = getOrNull(10).toNonNegativeLong(),
    )
}

internal fun DoubleArray.toDecodeOperationWorkPlan(): DecodeOperationWorkPlan {
    if (size < DECODE_OPERATION_WORK_PLAN_FIELD_COUNT) {
        return DecodeOperationWorkPlan.Empty
    }
    return DecodeOperationWorkPlan(
        preparingInputWorkUnits = getOrNull(0).toNonNegativeLong(),
        readingPcmWorkUnits = getOrNull(1).toNonNegativeLong(),
        decodingPayloadWorkUnits = getOrNull(2).toNonNegativeLong(),
        finalizingWorkUnits = getOrNull(3).toNonNegativeLong(),
        totalWorkUnits = getOrNull(4).toNonNegativeLong(),
        pcmSampleCount = getOrNull(5).toNonNegativeLong(),
    )
}

private fun Double?.toNonNegativeLong(): Long =
    if (this == null || !this.isFinite() || this <= 0.0) {
        0L
    } else {
        this.toLong()
    }

internal fun EncodedAudioPayloadResult.toEncodeSuccessOrFailureResult(): EncodeAudioResult =
    if (terminalCode != BagApiCodes.ERROR_OK) {
        EncodeAudioResult.Failed(terminalCode)
    } else if (pcm.isEmpty()) {
        EncodeAudioResult.Failed(BagApiCodes.ERROR_INTERNAL)
    } else {
        EncodeAudioResult.Success(
            pcm = pcm,
        )
    }
