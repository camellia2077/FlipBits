package com.bag.audioandroid.ui

import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.FollowDataWindowSource
import com.bag.audioandroid.ui.state.FollowDataWindowState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class FollowDataWindowActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val scope: CoroutineScope,
    private val audioCodecGateway: AudioCodecGateway,
    private val sessionStateStore: AudioSessionStateStore,
    private val sampleRateHz: Int,
    private val frameSamples: Int,
    private val workerDispatcher: CoroutineDispatcher,
) {
    private val jobs = mutableMapOf<TransportModeOption, Job>()

    fun ensureCurrentWindow(
        mode: TransportModeOption,
        displayedSamples: Int,
    ) {
        val session = uiState.value.sessions[mode] ?: return
        val source = session.followWindowSource ?: return
        val sample = displayedSamples.coerceIn(0, source.totalPcmSampleCount)
        if (session.followData.followAvailable && session.followWindow.isComfortablyInside(sample)) {
            return
        }
        val revision = session.generatedContentRevision
        val window = source.followWindowAround(sample)
        val activeJob = jobs[mode]
        if (activeJob?.isActive == true) {
            return
        }
        jobs[mode] =
            scope.launch {
                val followData = buildWindow(source, window, mode) ?: PayloadFollowViewData.Empty
                sessionStateStore.updateSession(mode) { current ->
                    if (current.generatedContentRevision != revision) {
                        current
                    } else {
                        current.copy(
                            followData = followData,
                            followWindow = window,
                        )
                    }
                }
            }
    }

    private suspend fun buildWindow(
        source: FollowDataWindowSource,
        window: FollowDataWindowState,
        mode: TransportModeOption,
    ): PayloadFollowViewData? =
        withContext(workerDispatcher) {
            val selectedSegments = ArrayList<PayloadFollowViewData>()
            var selectedStartSample = 0
            var segmentStartSample = 0
            source.segmentTexts.forEachIndexed { index, segmentText ->
                val segmentSampleCount = source.segmentSampleCounts[index]
                val segmentEndSample = segmentStartSample + segmentSampleCount
                if (segmentEndSample > window.startSample && segmentStartSample < window.endSampleExclusive) {
                    if (selectedSegments.isEmpty()) {
                        selectedStartSample = segmentStartSample
                    }
                    val result =
                        audioCodecGateway.buildEncodeFollowData(
                            segmentText,
                            sampleRateHz,
                            frameSamples,
                            mode.nativeValue,
                            source.flashSignalProfile,
                            source.flashVoicingFlavor,
                        )
                    selectedSegments += result.followData.takeIf { it.followAvailable } ?: return@withContext null
                }
                segmentStartSample = segmentEndSample
            }
            mergeSegmentedFollowDataWindow(
                segments = selectedSegments,
                firstSampleOffset = selectedStartSample,
                totalPcmSampleCount = source.totalPcmSampleCount,
            )
        }
}

internal fun FollowDataWindowSource.followWindowAround(sample: Int): FollowDataWindowState {
    val halfWindowSamples = FollowWindowRadiusSamples
    val start = (sample - halfWindowSamples).coerceAtLeast(0)
    val end = (sample + halfWindowSamples).coerceAtMost(totalPcmSampleCount)
    val alignedStart = segmentStartAtOrBefore(start)
    val alignedEnd = segmentEndAtOrAfter(end.coerceAtLeast(start + 1))
    return FollowDataWindowState(startSample = alignedStart, endSampleExclusive = alignedEnd)
}

private fun FollowDataWindowSource.segmentStartAtOrBefore(sample: Int): Int {
    var segmentStart = 0
    segmentSampleCounts.forEach { count ->
        val segmentEnd = segmentStart + count
        if (sample < segmentEnd) {
            return segmentStart
        }
        segmentStart = segmentEnd
    }
    return 0
}

private fun FollowDataWindowSource.segmentEndAtOrAfter(sample: Int): Int {
    var segmentEnd = 0
    segmentSampleCounts.forEach { count ->
        segmentEnd += count
        if (sample <= segmentEnd) {
            return segmentEnd
        }
    }
    return totalPcmSampleCount.coerceAtLeast(1)
}

private fun FollowDataWindowState.isComfortablyInside(sample: Int): Boolean =
    covers(sample) &&
        sample - startSample > FollowWindowRefreshMarginSamples &&
        endSampleExclusive - sample > FollowWindowRefreshMarginSamples

private const val FollowWindowRadiusSamples = 44_100 * 30
private const val FollowWindowRefreshMarginSamples = 44_100 * 5
