package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.PayloadFollowViewData

internal data class FlashSignalVisualizerRenderState(
    val buckets: List<FskEnergyBucket>,
    val toneSpectrumBuckets: List<ToneSpectrumBucket>,
    val toneFrequencyScale: ToneFrequencyScale,
    val toneCarrierLayout: ToneCarrierLayout,
    val bucketFrame: FlashSignalBucketFrame,
    val fixedTimelineFrame: FlashSignalFixedTimelineFrame?,
    val visualSegments: List<FlashSignalToneSegment>,
    val playbackSampleState: FlashVisualPlaybackSampleState,
    val followData: PayloadFollowViewData?,
    val bitReadoutSource: FlashBitReadoutSource?,
    val bitReadoutFrame: FlashBitReadoutFrame?,
    val bitReadoutSample: Float,
    val activeWindowBucketCount: Int,
    val primitiveEstimate: Int,
    val usesFallbackTimeline: Boolean,
    val enableViewportEdgeFade: Boolean,
    val traceWindowSamples: Int,
    val traceWindowStartSample: Int,
    val traceWindowEndSample: Int,
    val totalSamples: Int,
)
