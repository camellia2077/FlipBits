package com.bag.audioandroid

object NativePlaybackRuntimeBridge {
    init {
        System.loadLibrary("audio_android_jni")
    }

    external fun nativeClearedState(): IntArray

    external fun nativeLoadState(totalSamples: Int, sampleRateHz: Int): IntArray

    external fun nativePlayStarted(state: IntArray): IntArray

    external fun nativePaused(state: IntArray): IntArray

    external fun nativeResumed(state: IntArray): IntArray

    external fun nativeProgress(state: IntArray, playedSamples: Int): IntArray

    external fun nativeScrubStarted(state: IntArray): IntArray

    external fun nativeScrubChanged(state: IntArray, targetSamples: Int): IntArray

    external fun nativeScrubCommitted(state: IntArray): IntArray

    external fun nativeScrubCanceled(state: IntArray): IntArray

    external fun nativeStopped(state: IntArray): IntArray

    external fun nativeCompleted(state: IntArray): IntArray

    external fun nativeFailed(state: IntArray): IntArray

    external fun nativeProgressFraction(state: IntArray): Float

    external fun nativeClampSamples(totalSamples: Int, sampleIndex: Int): Int

    external fun nativeFractionToSamples(totalSamples: Int, fraction: Float): Int

    external fun nativeElapsedMs(state: IntArray): Long

    external fun nativeTotalMs(state: IntArray): Long
}
