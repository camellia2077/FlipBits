package com.bag.audioandroid.audio

internal data class StreamingSpeedAdjustedRenderPlan(
    val timeline: RenderedPlaybackTimeline,
    val rendererType: SpeedAdjustedPlaybackRendererType,
    val renderChunk: (outputStartSamples: Int, outputSampleCount: Int) -> ShortArray,
) {
    val renderedTotalSamples: Int
        get() = timeline.renderedTotalSamples
}

internal data class PreRenderedSpeedAdjustedRenderPlan(
    val renderedPlayback: RenderedSpeedAdjustedPcm,
    val rendererType: SpeedAdjustedPlaybackRendererType,
)

internal enum class SpeedAdjustedPlaybackRendererType(
    val diagName: String,
) {
    MiniCw("mini_cw"),
    Flash("flash"),
    Generic("generic"),
}

internal fun shouldStreamSpeedAdjustedPlayback(
    sourceSampleCount: Int,
    playbackSpeed: Float,
    preferStreaming: Boolean,
): Boolean = preferStreaming || shouldStreamSpeedAdjustedPcm(sourceSampleCount, playbackSpeed)

internal fun buildPreRenderedSpeedAdjustedRenderPlan(
    sourcePcm: ShortArray,
    sourceStartSamples: Int,
    sourceTotalSamples: Int,
    playbackSpeed: Float,
    sampleRateHz: Int,
    context: PlaybackRenderContext,
): PreRenderedSpeedAdjustedRenderPlan {
    val miniCw =
        renderMiniCwSpeedAdjustedPcm(
            sourceStartSamples = sourceStartSamples,
            sourceTotalSamples = sourceTotalSamples,
            playbackSpeed = playbackSpeed,
            sampleRateHz = sampleRateHz,
            context = context,
        )
    if (miniCw != null) {
        return PreRenderedSpeedAdjustedRenderPlan(
            renderedPlayback = miniCw,
            rendererType = SpeedAdjustedPlaybackRendererType.MiniCw,
        )
    }

    val flash =
        renderFlashSpeedAdjustedPcm(
            sourcePcm = sourcePcm,
            sourceStartSamples = sourceStartSamples,
            sourceTotalSamples = sourceTotalSamples,
            playbackSpeed = playbackSpeed,
            sampleRateHz = sampleRateHz,
            context = context,
        )
    if (flash != null) {
        return PreRenderedSpeedAdjustedRenderPlan(
            renderedPlayback = flash,
            rendererType = SpeedAdjustedPlaybackRendererType.Flash,
        )
    }

    return PreRenderedSpeedAdjustedRenderPlan(
        renderedPlayback =
            renderSpeedAdjustedPcm(
                sourcePcm = sourcePcm,
                sourceStartSamples = sourceStartSamples,
                sourceTotalSamples = sourceTotalSamples,
                playbackSpeed = playbackSpeed,
                sampleRateHz = sampleRateHz,
            ),
        rendererType = SpeedAdjustedPlaybackRendererType.Generic,
    )
}

internal fun renderPreRenderedSpeedAdjustedPlayback(
    sourcePcm: ShortArray,
    sourceStartSamples: Int,
    sourceTotalSamples: Int,
    playbackSpeed: Float,
    sampleRateHz: Int,
    context: PlaybackRenderContext,
): RenderedSpeedAdjustedPcm =
    buildPreRenderedSpeedAdjustedRenderPlan(
        sourcePcm = sourcePcm,
        sourceStartSamples = sourceStartSamples,
        sourceTotalSamples = sourceTotalSamples,
        playbackSpeed = playbackSpeed,
        sampleRateHz = sampleRateHz,
        context = context,
    ).renderedPlayback

internal fun buildStreamingSpeedAdjustedRenderPlan(
    sourcePcm: ShortArray,
    sourceStartSamples: Int,
    sourceTotalSamples: Int,
    playbackSpeed: Float,
    sampleRateHz: Int,
    context: PlaybackRenderContext,
): StreamingSpeedAdjustedRenderPlan {
    val miniCwTimeline =
        buildMiniCwSpeedAdjustedTimeline(
            sourceStartSamples = sourceStartSamples,
            sourceTotalSamples = sourceTotalSamples,
            playbackSpeed = playbackSpeed,
            context = context,
        )
    if (miniCwTimeline != null) {
        return StreamingSpeedAdjustedRenderPlan(
            timeline = miniCwTimeline,
            rendererType = SpeedAdjustedPlaybackRendererType.MiniCw,
            renderChunk = { outputStartSamples, outputSampleCount ->
                renderMiniCwSpeedAdjustedPcmChunk(
                    sourceStartSamples = sourceStartSamples,
                    sourceTotalSamples = sourceTotalSamples,
                    outputStartSamples = outputStartSamples,
                    outputSampleCount = outputSampleCount,
                    playbackSpeed = playbackSpeed,
                    sampleRateHz = sampleRateHz,
                    context = context,
                )
            },
        )
    }

    val flashTimeline =
        buildFlashSpeedAdjustedTimeline(
            sourceStartSamples = sourceStartSamples,
            sourceTotalSamples = sourceTotalSamples,
            playbackSpeed = playbackSpeed,
            context = context,
        )
    if (flashTimeline != null) {
        return StreamingSpeedAdjustedRenderPlan(
            timeline = flashTimeline,
            rendererType = SpeedAdjustedPlaybackRendererType.Flash,
            renderChunk = { outputStartSamples, outputSampleCount ->
                renderFlashSpeedAdjustedPcmChunk(
                    sourcePcm = sourcePcm,
                    sourceStartSamples = sourceStartSamples,
                    sourceTotalSamples = sourceTotalSamples,
                    outputStartSamples = outputStartSamples,
                    outputSampleCount = outputSampleCount,
                    playbackSpeed = playbackSpeed,
                    sampleRateHz = sampleRateHz,
                    context = context,
                )
            },
        )
    }

    val renderedTotalSamples = speedAdjustedRenderedSampleCount(sourcePcm.size, playbackSpeed)
    return StreamingSpeedAdjustedRenderPlan(
        timeline =
            RenderedPlaybackTimeline(
                sourceStartSamples = sourceStartSamples,
                sourceTotalSamples = sourceTotalSamples,
                renderedTotalSamples = renderedTotalSamples,
                sourceSamplesPerRenderedSample =
                    if (renderedTotalSamples > 0) {
                        (sourceTotalSamples - sourceStartSamples).coerceAtLeast(0).toDouble() / renderedTotalSamples.toDouble()
                    } else {
                        1.0
                    },
            ),
        rendererType = SpeedAdjustedPlaybackRendererType.Generic,
        renderChunk = { outputStartSamples, outputSampleCount ->
            renderSpeedAdjustedPcmChunk(
                sourcePcm = sourcePcm,
                outputStartSamples = outputStartSamples,
                outputSampleCount = outputSampleCount,
                playbackSpeed = playbackSpeed,
                sampleRateHz = sampleRateHz,
            )
        },
    )
}
