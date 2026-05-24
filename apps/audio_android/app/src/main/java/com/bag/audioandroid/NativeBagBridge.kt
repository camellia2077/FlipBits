package com.bag.audioandroid

import com.bag.audioandroid.domain.DecodedAudioPayloadResult
import com.bag.audioandroid.domain.EncodedAudioPayloadResult
import com.bag.audioandroid.domain.FlashSignalInfo

object NativeBagBridge {
    init {
        System.loadLibrary("audio_android_jni")
    }

    external fun nativeValidateEncodeRequest(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): Int

    external fun nativeCreateEncodeOperation(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): Long

    external fun nativePumpEncodeOperation(
        handle: Long,
        maxWorkUnits: Int,
        maxWallTimeMs: Int,
    ): Int

    external fun nativeGetEncodeOperationWorkPlan(handle: Long): DoubleArray

    external fun nativePollEncodeOperation(handle: Long): DoubleArray

    external fun nativeTakeEncodeOperationResult(handle: Long): EncodedAudioPayloadResult

    external fun nativeBuildEncodeFollowData(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): EncodedAudioPayloadResult

    external fun nativeDescribeFlashSignal(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): FlashSignalInfo

    external fun nativeCancelEncodeOperation(handle: Long): Int

    external fun nativeDestroyEncodeOperation(handle: Long)

    external fun nativeDecodeGeneratedPcm(
        pcm: ShortArray,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): DecodedAudioPayloadResult

    external fun nativeValidateDecodeConfig(
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): Int

    external fun nativeGetCoreVersion(): String
}
