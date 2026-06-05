package com.bag.audioandroid.audio

import android.os.Debug
import com.bag.audioandroid.util.measureElapsedMs
import com.bag.audioandroid.util.safeDebugLog

internal data class PlaybackSpeedMemorySnapshot(
    val heapUsedBytes: Long,
    val heapTotalBytes: Long,
    val heapFreeBytes: Long,
    val nativeHeapAllocatedBytes: Long,
)

internal fun capturePlaybackSpeedMemorySnapshot(): PlaybackSpeedMemorySnapshot {
    val runtime = Runtime.getRuntime()
    val total = runtime.totalMemory()
    val free = runtime.freeMemory()
    return PlaybackSpeedMemorySnapshot(
        heapUsedBytes = total - free,
        heapTotalBytes = total,
        heapFreeBytes = free,
        nativeHeapAllocatedBytes = Debug.getNativeHeapAllocatedSize(),
    )
}

internal class PlaybackSpeedMemoryRenderTrace(
    private val handleId: String,
    private val playbackMode: String,
    private val playbackSpeed: Float,
    private val streaming: Boolean,
    private val fileBacked: Boolean,
    private val sourceSampleCount: Int,
) {
    // Keep speed-render allocation diagnostics outside AudioPlayer so playback control stays separate from measurement.
    private val before = capturePlaybackSpeedMemorySnapshot()
    private var renderTimeMs = 0L

    fun <T> measureRender(block: () -> T): T {
        val (result, elapsedMs) = measureElapsedMs(block)
        renderTimeMs += elapsedMs
        return result
    }

    fun log(
        event: String,
        rendererType: SpeedAdjustedPlaybackRendererType,
        renderedSampleCount: Int,
    ) {
        logPlaybackSpeedMemory(
            event = event,
            handleId = handleId,
            rendererType = rendererType,
            playbackMode = playbackMode,
            playbackSpeed = playbackSpeed,
            streaming = streaming,
            fileBacked = fileBacked,
            sourceSampleCount = sourceSampleCount,
            renderedSampleCount = renderedSampleCount,
            before = before,
            after = capturePlaybackSpeedMemorySnapshot(),
            renderTimeMs = renderTimeMs,
        )
    }
}

internal fun loadPcmFileRangeWithPlaybackSpeedMemoryTrace(
    handleId: String,
    playbackMode: String,
    playbackSpeed: Float,
    sourceSampleCount: Int,
    load: () -> ShortArray,
): ShortArray {
    val before = capturePlaybackSpeedMemorySnapshot()
    val (playbackPcm, loadTimeMs) = measureElapsedMs(load)
    logPlaybackSpeedSourceLoadMemory(
        handleId = handleId,
        playbackMode = playbackMode,
        playbackSpeed = playbackSpeed,
        fileBacked = true,
        sourceSampleCount = sourceSampleCount,
        before = before,
        after = capturePlaybackSpeedMemorySnapshot(),
        loadTimeMs = loadTimeMs,
    )
    return playbackPcm
}

internal fun logPlaybackSpeedMemory(
    event: String,
    handleId: String,
    rendererType: SpeedAdjustedPlaybackRendererType,
    playbackMode: String,
    playbackSpeed: Float,
    streaming: Boolean,
    fileBacked: Boolean,
    sourceSampleCount: Int,
    renderedSampleCount: Int,
    before: PlaybackSpeedMemorySnapshot,
    after: PlaybackSpeedMemorySnapshot,
    renderTimeMs: Long,
) {
    val sourcePcmBytes = pcmBytes(sourceSampleCount)
    val renderedPcmBytes = if (streaming) 0L else pcmBytes(renderedSampleCount)
    safeDebugLog(
        PlaybackSpeedMemoryDiagTag,
        "$event handle=$handleId renderer=${rendererType.diagName} mode=$playbackMode speed=$playbackSpeed " +
            "streaming=$streaming fileBacked=$fileBacked sourceSamples=$sourceSampleCount renderedSamples=$renderedSampleCount " +
            "sourcePcmBytes=$sourcePcmBytes renderedPcmBytes=$renderedPcmBytes heapBefore=${before.heapUsedBytes} " +
            "heapAfter=${after.heapUsedBytes} heapDelta=${after.heapUsedBytes - before.heapUsedBytes} " +
            "heapTotalBefore=${before.heapTotalBytes} heapTotalAfter=${after.heapTotalBytes} " +
            "nativeHeapBefore=${before.nativeHeapAllocatedBytes} nativeHeapAfter=${after.nativeHeapAllocatedBytes} " +
            "nativeHeapDelta=${after.nativeHeapAllocatedBytes - before.nativeHeapAllocatedBytes} renderTimeMs=$renderTimeMs",
    )
}

internal fun logPlaybackSpeedSourceLoadMemory(
    handleId: String,
    playbackMode: String,
    playbackSpeed: Float,
    fileBacked: Boolean,
    sourceSampleCount: Int,
    before: PlaybackSpeedMemorySnapshot,
    after: PlaybackSpeedMemorySnapshot,
    loadTimeMs: Long,
) {
    safeDebugLog(
        PlaybackSpeedMemoryDiagTag,
        "sourceLoad handle=$handleId mode=$playbackMode speed=$playbackSpeed fileBacked=$fileBacked " +
            "sourceSamples=$sourceSampleCount sourcePcmBytes=${pcmBytes(sourceSampleCount)} " +
            "heapBefore=${before.heapUsedBytes} heapAfter=${after.heapUsedBytes} heapDelta=${after.heapUsedBytes - before.heapUsedBytes} " +
            "nativeHeapBefore=${before.nativeHeapAllocatedBytes} nativeHeapAfter=${after.nativeHeapAllocatedBytes} " +
            "nativeHeapDelta=${after.nativeHeapAllocatedBytes - before.nativeHeapAllocatedBytes} loadTimeMs=$loadTimeMs",
    )
}

private fun pcmBytes(sampleCount: Int): Long = sampleCount.coerceAtLeast(0).toLong() * PcmShortBytes.toLong()

private const val PlaybackSpeedMemoryDiagTag = "PlaybackSpeedMemory"
