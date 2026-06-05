package com.bag.audioandroid.audio

import com.bag.audioandroid.util.safeDebugLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class AudioPlaybackCoordinator(
    private val audioPlayer: AudioPlayer = AudioPlayer(),
) {
    private val playbackRequestIds = AtomicLong(0L)

    @Volatile
    private var activePlaybackRequestId: Long? = null

    @Volatile
    private var activePlaybackKey: String? = null

    @Volatile
    private var activePlaybackHandle: AudioPlayer.PlaybackHandle? = null

    @Volatile
    private var handoffPreviousPlaybackHandle: AudioPlayer.PlaybackHandle? = null

    @Volatile
    private var playbackPaused = false

    fun hasActivePlaybackForOtherSource(sourceKey: String): Boolean = activePlaybackKey != null && activePlaybackKey != sourceKey

    fun isPlaybackActiveForSource(sourceKey: String): Boolean = activePlaybackRequestId != null && activePlaybackKey == sourceKey

    fun hasActivePlaybackForSource(sourceKey: String): Boolean = isPlaybackActiveForSource(sourceKey)

    fun isPlaybackPausedForSource(sourceKey: String): Boolean = isPlaybackActiveForSource(sourceKey) && playbackPaused

    fun startPlayback(
        scope: CoroutineScope,
        sourceKey: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        totalSamples: Int,
        sampleRateHz: Int,
        playbackSpeed: Float,
        startSampleIndex: Int,
        renderContext: PlaybackRenderContext = PlaybackRenderContext.Empty,
        handoffFromActivePlayback: Boolean = false,
        onStarted: () -> Unit,
        onProgressChanged: (Int, Int) -> Unit,
        onFinished: (String, PlaybackResult) -> Unit,
        onFailed: (String) -> Unit,
    ) {
        val activeHandleBeforeStart = activePlaybackHandle
        val handoffHandleBeforeStart = handoffPreviousPlaybackHandle
        val previousPlaybackHandle =
            if (handoffFromActivePlayback && activePlaybackKey == sourceKey) {
                handoffHandleBeforeStart ?: activeHandleBeforeStart
            } else {
                null
            }
        val supersededPendingPlaybackHandle =
            activeHandleBeforeStart
                ?.takeIf { previousPlaybackHandle != null && it !== previousPlaybackHandle }
        if (activePlaybackRequestId != null) {
            if (previousPlaybackHandle == null) {
                safeDebugLog(
                    PlaybackCoordinatorDiagTag,
                    "startPlaybackPreempt source=$sourceKey activeSource=${activePlaybackKey.orEmpty()} " +
                        "activeRequestId=${activePlaybackRequestId ?: -1L}",
                )
                stopPlayback()
            } else {
                safeDebugLog(
                    PlaybackCoordinatorDiagTag,
                    "startPlaybackHandoff source=$sourceKey activeRequestId=${activePlaybackRequestId ?: -1L} " +
                        "previousHandle=${previousPlaybackHandle.debugHandleId()}",
                )
                handoffPreviousPlaybackHandle = previousPlaybackHandle
                supersededPendingPlaybackHandle?.let { supersededHandle ->
                    safeDebugLog(
                        PlaybackCoordinatorDiagTag,
                        "handoffStopSupersededPending source=$sourceKey handle=${supersededHandle.debugHandleId()}",
                    )
                    scope.launch(Dispatchers.IO) {
                        audioPlayer.stop(supersededHandle)
                    }
                }
            }
        }
        val playbackHandle = audioPlayer.prepareForNewPlayback()
        val requestId = beginPlayback(sourceKey, playbackHandle)
        safeDebugLog(
            PlaybackCoordinatorDiagTag,
            "startPlayback requestId=$requestId source=$sourceKey handle=${playbackHandle.debugHandleId()} " +
                "total=$totalSamples fileBacked=${!pcmFilePath.isNullOrBlank()} sampleRate=$sampleRateHz " +
                "speed=$playbackSpeed start=$startSampleIndex",
        )
        onStarted()
        scope.launch(Dispatchers.IO) {
            try {
                val result =
                    if (pcmFilePath.isNullOrBlank()) {
                        audioPlayer.playPcm(
                            playback = playbackHandle,
                            pcm = pcm.copyOf(),
                            sampleRateHz = sampleRateHz,
                            playbackSpeed = playbackSpeed,
                            startSampleIndex = startSampleIndex,
                            renderContext = renderContext,
                            preferStreamingSpeedAdjustedPcm = handoffFromActivePlayback,
                            onPlaybackStarted = {
                                stopHandoffPreviousPlayback(
                                    scope = scope,
                                    requestId = requestId,
                                    sourceKey = sourceKey,
                                    previousPlaybackHandle = previousPlaybackHandle,
                                )
                            },
                        ) { playedSamples, reportedTotalSamples ->
                            if (isPlaybackActive(requestId, sourceKey)) {
                                onProgressChanged(playedSamples, reportedTotalSamples)
                            }
                        }
                    } else {
                        audioPlayer.playPcmFile(
                            playback = playbackHandle,
                            pcmFilePath = pcmFilePath,
                            sampleRateHz = sampleRateHz,
                            totalSamples = totalSamples,
                            playbackSpeed = playbackSpeed,
                            startSampleIndex = startSampleIndex,
                            renderContext = renderContext,
                            preferStreamingSpeedAdjustedPcm = handoffFromActivePlayback,
                            onPlaybackStarted = {
                                stopHandoffPreviousPlayback(
                                    scope = scope,
                                    requestId = requestId,
                                    sourceKey = sourceKey,
                                    previousPlaybackHandle = previousPlaybackHandle,
                                )
                            },
                        ) { playedSamples, reportedTotalSamples ->
                            if (isPlaybackActive(requestId, sourceKey)) {
                                onProgressChanged(playedSamples, reportedTotalSamples)
                            }
                        }
                    }
                if (!isPlaybackActive(requestId, sourceKey)) {
                    safeDebugLog(
                        PlaybackCoordinatorDiagTag,
                        "finishIgnored requestId=$requestId source=$sourceKey handle=${playbackHandle.debugHandleId()} result=$result",
                    )
                    return@launch
                }
                clearActivePlayback(requestId)
                safeDebugLog(
                    PlaybackCoordinatorDiagTag,
                    "finish requestId=$requestId source=$sourceKey handle=${playbackHandle.debugHandleId()} result=$result",
                )
                onFinished(sourceKey, result)
            } catch (error: Exception) {
                if (!isPlaybackActive(requestId, sourceKey)) {
                    safeDebugLog(
                        PlaybackCoordinatorDiagTag,
                        "failureIgnored requestId=$requestId source=$sourceKey handle=${playbackHandle.debugHandleId()} " +
                            "error=${error::class.java.simpleName}",
                    )
                    return@launch
                }
                clearActivePlayback(requestId)
                safeDebugLog(
                    PlaybackCoordinatorDiagTag,
                    "failure requestId=$requestId source=$sourceKey handle=${playbackHandle.debugHandleId()} " +
                        "error=${error::class.java.simpleName} message=${error.message.orEmpty()}",
                )
                onFailed(sourceKey)
            }
        }
    }

    fun pausePlayback(sourceKey: String): Boolean {
        if (!isPlaybackActiveForSource(sourceKey) || playbackPaused) {
            safeDebugLog(
                PlaybackCoordinatorDiagTag,
                "pauseRejected source=$sourceKey active=$activePlaybackKey paused=$playbackPaused requestId=${activePlaybackRequestId ?: -1L}",
            )
            return false
        }
        playbackPaused = true
        safeDebugLog(
            PlaybackCoordinatorDiagTag,
            "pause source=$sourceKey requestId=${activePlaybackRequestId ?: -1L}",
        )
        audioPlayer.pause()
        return true
    }

    fun resumePlayback(sourceKey: String): Boolean {
        if (!isPlaybackActiveForSource(sourceKey) || !playbackPaused) {
            safeDebugLog(
                PlaybackCoordinatorDiagTag,
                "resumeRejected source=$sourceKey active=$activePlaybackKey paused=$playbackPaused requestId=${activePlaybackRequestId ?: -1L}",
            )
            return false
        }
        playbackPaused = false
        safeDebugLog(
            PlaybackCoordinatorDiagTag,
            "resume source=$sourceKey requestId=${activePlaybackRequestId ?: -1L}",
        )
        audioPlayer.resume()
        return true
    }

    fun setPlaybackSpeed(
        sourceKey: String,
        playbackSpeed: Float,
    ): Boolean {
        if (activePlaybackKey != sourceKey) {
            safeDebugLog(
                PlaybackCoordinatorDiagTag,
                "setSpeedRejected source=$sourceKey active=$activePlaybackKey requestId=${activePlaybackRequestId ?: -1L} speed=$playbackSpeed",
            )
            return false
        }
        safeDebugLog(
            PlaybackCoordinatorDiagTag,
            "setSpeed source=$sourceKey requestId=${activePlaybackRequestId ?: -1L} speed=$playbackSpeed",
        )
        return audioPlayer.setPlaybackSpeed(playbackSpeed)
    }

    fun beginScrub(sourceKey: String): Boolean =
        if (isPlaybackActiveForSource(sourceKey)) {
            pausePlayback(sourceKey)
        } else {
            false
        }

    fun updateScrub(
        sourceKey: String,
        targetSamples: Int,
    ): Boolean = isPlaybackActiveForSource(sourceKey) && targetSamples >= 0

    fun commitScrub(
        sourceKey: String,
        targetSamples: Int,
        resumeAfterCommit: Boolean,
    ): Boolean {
        if (!isPlaybackActiveForSource(sourceKey)) {
            return false
        }
        val appliedPosition = audioPlayer.seekTo(targetSamples) ?: return false
        playbackPaused = !resumeAfterCommit
        if (resumeAfterCommit) {
            audioPlayer.resume()
        } else {
            audioPlayer.pause()
        }
        return appliedPosition >= 0
    }

    fun cancelScrub(
        sourceKey: String,
        resumeAfterCancel: Boolean,
    ): Boolean {
        if (!isPlaybackActiveForSource(sourceKey)) {
            return false
        }
        playbackPaused = !resumeAfterCancel
        if (resumeAfterCancel) {
            audioPlayer.resume()
        } else {
            audioPlayer.pause()
        }
        return true
    }

    fun stopPlayback(onStopped: (String) -> Unit = {}) {
        val playbackKey = activePlaybackKey ?: return
        val previousPlaybackHandle = handoffPreviousPlaybackHandle
        safeDebugLog(
            PlaybackCoordinatorDiagTag,
            "stopPlayback source=$playbackKey requestId=${activePlaybackRequestId ?: -1L} paused=$playbackPaused",
        )
        clearActivePlayback()
        audioPlayer.stop()
        previousPlaybackHandle?.let(audioPlayer::stop)
        onStopped(playbackKey)
    }

    fun release() {
        safeDebugLog(
            PlaybackCoordinatorDiagTag,
            "release source=${activePlaybackKey.orEmpty()} requestId=${activePlaybackRequestId ?: -1L}",
        )
        val previousPlaybackHandle = handoffPreviousPlaybackHandle
        clearActivePlayback()
        audioPlayer.stop()
        previousPlaybackHandle?.let(audioPlayer::stop)
    }

    private fun beginPlayback(
        sourceKey: String,
        playbackHandle: AudioPlayer.PlaybackHandle,
    ): Long {
        val requestId = playbackRequestIds.incrementAndGet()
        safeDebugLog(
            PlaybackCoordinatorDiagTag,
            "beginPlayback source=$sourceKey requestId=$requestId previousSource=${activePlaybackKey.orEmpty()} " +
                "previousRequestId=${activePlaybackRequestId ?: -1L}",
        )
        activePlaybackRequestId = requestId
        activePlaybackKey = sourceKey
        activePlaybackHandle = playbackHandle
        playbackPaused = false
        return requestId
    }

    private fun clearActivePlayback(requestId: Long? = null) {
        if (requestId == null || activePlaybackRequestId == requestId) {
            safeDebugLog(
                PlaybackCoordinatorDiagTag,
                "clearActivePlayback requestId=${requestId ?: -1L} activeSource=${activePlaybackKey.orEmpty()} " +
                    "activeRequestId=${activePlaybackRequestId ?: -1L} paused=$playbackPaused",
            )
            activePlaybackRequestId = null
            activePlaybackKey = null
            activePlaybackHandle = null
            handoffPreviousPlaybackHandle = null
            playbackPaused = false
        }
    }

    private fun isPlaybackActive(
        requestId: Long,
        sourceKey: String,
    ): Boolean = activePlaybackRequestId == requestId && activePlaybackKey == sourceKey

    private fun stopHandoffPreviousPlayback(
        scope: CoroutineScope,
        requestId: Long,
        sourceKey: String,
        previousPlaybackHandle: AudioPlayer.PlaybackHandle?,
    ) {
        if (previousPlaybackHandle == null || !isPlaybackActive(requestId, sourceKey)) {
            return
        }
        if (handoffPreviousPlaybackHandle !== previousPlaybackHandle) {
            return
        }
        handoffPreviousPlaybackHandle = null
        safeDebugLog(
            PlaybackCoordinatorDiagTag,
            "handoffStopPrevious requestId=$requestId source=$sourceKey handle=${previousPlaybackHandle.debugHandleId()}",
        )
        scope.launch(Dispatchers.IO) {
            audioPlayer.stop(previousPlaybackHandle)
        }
    }
}

private const val PlaybackCoordinatorDiagTag = "PlaybackCoordDiag"

private fun AudioPlayer.PlaybackHandle.debugHandleId(): String = Integer.toHexString(System.identityHashCode(this))
