package com.bag.audioandroid.ui.screen

import android.util.Log

internal object MiniVisualPerfTrace {
    private const val Tag = "MiniVisualPerf"
    private const val ReportIntervalNanos = 1_000_000_000L

    private var enabled = false
    private var windowStartNanos = 0L
    private var composeCount = 0
    private var drawCount = 0
    private var drawDurationTotalNanos = 0L
    private var drawDurationMaxNanos = 0L
    private var rawUpdateCount = 0
    private var rawStepTotalSamples = 0f
    private var rawStepMaxSamples = 0f
    private var smoothStepTotalSamples = 0f
    private var smoothStepMaxSamples = 0f
    private var visualPxStepTotal = 0f
    private var visualPxStepMax = 0f
    private var windowStartStepMaxSamples = 0f
    private var previousRawSample: Int? = null
    private var previousSmoothSample: Int? = null
    private var previousPlayheadX: Float? = null
    private var previousWindowStart: Int? = null
    private var lastSampleRateHz = 0
    private var latestSnapshot = MiniVisualPerfSnapshot()

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) {
            reset()
            latestSnapshot = MiniVisualPerfSnapshot()
        }
    }

    fun recordCompose(
        rawSample: Int,
        smoothSample: Int,
        sampleRateHz: Int,
        windowStartSample: Int,
        windowEndSample: Int,
        visibleSamples: Int,
        totalSamples: Int,
        visibleToneEntries: Int,
    ) {
        if (!enabled) {
            return
        }
        composeCount += 1
        lastSampleRateHz = sampleRateHz
        capturePlaybackStep(rawSample, smoothSample, sampleRateHz, windowStartSample)
        latestSnapshot =
            latestSnapshot.copy(
                rawSample = rawSample,
                currentSample = smoothSample,
                sampleRateHz = sampleRateHz,
                windowStartSample = windowStartSample,
                windowEndSample = windowEndSample,
                visibleSamples = visibleSamples,
                totalSamples = totalSamples,
                visibleToneEntries = visibleToneEntries,
            )
        maybeReport()
    }

    fun recordDraw(
        drawDurationNanos: Long,
        playheadXPx: Float,
        rawSample: Int,
        smoothSample: Int,
        sampleRateHz: Int,
        windowStartSample: Int,
        windowEndSample: Int,
        visibleSamples: Int,
        totalSamples: Int,
        visibleToneEntries: Int,
    ) {
        if (!enabled) {
            return
        }
        drawCount += 1
        drawDurationTotalNanos += drawDurationNanos
        drawDurationMaxNanos = maxOf(drawDurationMaxNanos, drawDurationNanos)
        lastSampleRateHz = sampleRateHz
        previousPlayheadX?.let { previousX ->
            val pxStep = kotlin.math.abs(playheadXPx - previousX)
            visualPxStepTotal += pxStep
            visualPxStepMax = maxOf(visualPxStepMax, pxStep)
        }
        previousPlayheadX = playheadXPx
        latestSnapshot =
            latestSnapshot.copy(
                rawSample = rawSample,
                currentSample = smoothSample,
                sampleRateHz = sampleRateHz,
                windowStartSample = windowStartSample,
                windowEndSample = windowEndSample,
                visibleSamples = visibleSamples,
                totalSamples = totalSamples,
                visibleToneEntries = visibleToneEntries,
            )
        maybeReport()
    }

    fun forceReport(reason: String) {
        if (!enabled) {
            return
        }
        report(reason)
    }

    fun snapshot(): MiniVisualPerfSnapshot = latestSnapshot

    private fun capturePlaybackStep(
        rawSample: Int,
        smoothSample: Int,
        sampleRateHz: Int,
        windowStartSample: Int,
    ) {
        previousRawSample?.let { previous ->
            val sampleStep = kotlin.math.abs(rawSample - previous).toFloat()
            if (sampleStep > 0.5f) {
                rawUpdateCount += 1
                rawStepTotalSamples += sampleStep
                rawStepMaxSamples = maxOf(rawStepMaxSamples, sampleStep)
            }
        }
        previousRawSample = rawSample
        previousSmoothSample?.let { previous ->
            val sampleStep = kotlin.math.abs(smoothSample - previous).toFloat()
            if (sampleStep > 0.5f) {
                smoothStepTotalSamples += sampleStep
                smoothStepMaxSamples = maxOf(smoothStepMaxSamples, sampleStep)
            }
        }
        previousSmoothSample = smoothSample
        previousWindowStart?.let { previous ->
            val step = kotlin.math.abs(windowStartSample - previous).toFloat()
            windowStartStepMaxSamples = maxOf(windowStartStepMaxSamples, step)
        }
        previousWindowStart = windowStartSample
        lastSampleRateHz = sampleRateHz
        latestSnapshot =
            latestSnapshot.copy(
                rawSample = rawSample,
                currentSample = smoothSample,
                visualErrorMs = samplesToMs(kotlin.math.abs(rawSample - smoothSample).toFloat(), sampleRateHz),
            )
    }

    private fun maybeReport() {
        val now = System.nanoTime()
        if (windowStartNanos == 0L) {
            windowStartNanos = now
            return
        }
        if (now - windowStartNanos < ReportIntervalNanos) {
            return
        }
        report("interval")
    }

    private fun report(reason: String) {
        val now = System.nanoTime()
        if (windowStartNanos == 0L) {
            windowStartNanos = now
            return
        }
        val elapsedSeconds = ((now - windowStartNanos).coerceAtLeast(1L)).toDouble() / 1_000_000_000.0
        val sampleRateHz = lastSampleRateHz.coerceAtLeast(1)
        val drawFps = drawCount / elapsedSeconds
        val composeFps = composeCount / elapsedSeconds
        val drawAvgMs =
            if (drawCount > 0) {
                drawDurationTotalNanos.toDouble() / drawCount.toDouble() / 1_000_000.0
            } else {
                0.0
            }
        val drawMaxMs = drawDurationMaxNanos.toDouble() / 1_000_000.0
        val rawUpdatesPerSecond = rawUpdateCount / elapsedSeconds
        val rawStepAvgMs =
            if (rawUpdateCount > 0) {
                samplesToMs(rawStepTotalSamples / rawUpdateCount.toFloat(), sampleRateHz)
            } else {
                0f
            }
        val rawStepMaxMs = samplesToMs(rawStepMaxSamples, sampleRateHz)
        val smoothStepCount = (composeCount - 1).coerceAtLeast(0)
        val smoothStepAvgMs =
            if (smoothStepCount > 0) {
                samplesToMs(smoothStepTotalSamples / smoothStepCount.toFloat(), sampleRateHz)
            } else {
                0f
            }
        val smoothStepMaxMs = samplesToMs(smoothStepMaxSamples, sampleRateHz)
        val pxStepAvg =
            if (drawCount > 1) {
                visualPxStepTotal / (drawCount - 1).toFloat()
            } else {
                0f
            }
        val pxStepMax = visualPxStepMax
        val windowStartStepMaxMs = samplesToMs(this.windowStartStepMaxSamples, sampleRateHz)
        val visualErrorMs =
            samplesToMs(
                kotlin.math.abs(latestSnapshot.rawSample - latestSnapshot.currentSample).toFloat(),
                sampleRateHz,
            )
        latestSnapshot =
            latestSnapshot.copy(
                drawFps = drawFps.toFloat(),
                composeFps = composeFps.toFloat(),
                drawAvgMs = drawAvgMs.toFloat(),
                drawMaxMs = drawMaxMs.toFloat(),
                rawUpdatesPerSecond = rawUpdatesPerSecond.toFloat(),
                rawStepAvgMs = rawStepAvgMs,
                rawStepMaxMs = rawStepMaxMs,
                smoothStepAvgMs = smoothStepAvgMs,
                smoothStepMaxMs = smoothStepMaxMs,
                visualErrorMs = visualErrorMs,
                pxStepAvg = pxStepAvg,
                pxStepMax = pxStepMax,
                windowStartStepMaxMs = windowStartStepMaxMs,
            )
        try {
            Log.d(
                Tag,
                "reason=$reason " +
                    "drawAvgMs=${"%.2f".format(drawAvgMs)} " +
                    "rawUpdate/s=${"%.1f".format(rawUpdatesPerSecond)} " +
                    "rawStepMaxMs=${"%.1f".format(rawStepMaxMs)} " +
                    "smoothStepMaxMs=${"%.1f".format(smoothStepMaxMs)} " +
                    "visualErrorMs=${"%.1f".format(visualErrorMs)} " +
                    "windowStepMaxMs=${"%.1f".format(windowStartStepMaxMs)} " +
                    "draw/s=${"%.1f".format(drawFps)} compose/s=${"%.1f".format(composeFps)} " +
                    "drawMaxMs=${"%.2f".format(drawMaxMs)} rawStepAvgMs=${"%.1f".format(rawStepAvgMs)} " +
                    "smoothStepAvgMs=${"%.1f".format(smoothStepAvgMs)} " +
                    "pxStepAvg=${"%.2f".format(pxStepAvg)} pxStepMax=${"%.2f".format(pxStepMax)} " +
                    "sample=${latestSnapshot.rawSample}/${latestSnapshot.currentSample} sr=${latestSnapshot.sampleRateHz} " +
                    "window=[${latestSnapshot.windowStartSample},${latestSnapshot.windowEndSample}) visible=${latestSnapshot.visibleToneEntries}/${latestSnapshot.visibleSamples} total=${latestSnapshot.totalSamples}",
            )
        } catch (_: Throwable) {
        }
        reset()
        windowStartNanos = now
    }

    private fun reset() {
        windowStartNanos = 0L
        composeCount = 0
        drawCount = 0
        drawDurationTotalNanos = 0L
        drawDurationMaxNanos = 0L
        rawUpdateCount = 0
        rawStepTotalSamples = 0f
        rawStepMaxSamples = 0f
        smoothStepTotalSamples = 0f
        smoothStepMaxSamples = 0f
        visualPxStepTotal = 0f
        visualPxStepMax = 0f
        windowStartStepMaxSamples = 0f
        previousPlayheadX = null
        previousRawSample = null
        previousSmoothSample = null
        previousWindowStart = null
    }

    private fun samplesToMs(
        samples: Float,
        sampleRateHz: Int,
    ): Float =
        if (sampleRateHz <= 0) {
            0f
        } else {
            samples * 1000f / sampleRateHz.toFloat()
        }
}

internal data class MiniVisualPerfSnapshot(
    val drawFps: Float = 0f,
    val composeFps: Float = 0f,
    val drawAvgMs: Float = 0f,
    val drawMaxMs: Float = 0f,
    val rawUpdatesPerSecond: Float = 0f,
    val rawStepAvgMs: Float = 0f,
    val rawStepMaxMs: Float = 0f,
    val smoothStepAvgMs: Float = 0f,
    val smoothStepMaxMs: Float = 0f,
    val visualErrorMs: Float = 0f,
    val pxStepAvg: Float = 0f,
    val pxStepMax: Float = 0f,
    val windowStartStepMaxMs: Float = 0f,
    val rawSample: Int = 0,
    val currentSample: Int = 0,
    val sampleRateHz: Int = 0,
    val windowStartSample: Int = 0,
    val windowEndSample: Int = 0,
    val visibleSamples: Int = 0,
    val totalSamples: Int = 0,
    val visibleToneEntries: Int = 0,
)
