package com.bag.audioandroid.data

import com.bag.audioandroid.NativeBagBridge
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.VoiceFxGateway
import com.bag.audioandroid.domain.VoiceFxNativeResult
import com.bag.audioandroid.domain.VoiceFxProcessResult
import com.bag.audioandroid.domain.VoiceFxProcessorSession
import com.bag.audioandroid.ui.model.VoiceFxPresetOption
import com.bag.audioandroid.ui.model.VoiceFxSubvoiceStyleOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NativeVoiceFxGateway : VoiceFxGateway {
    override suspend fun applyVoiceFx(
        preset: VoiceFxPresetOption,
        subvoiceStyle: VoiceFxSubvoiceStyleOption,
        pcm: ShortArray,
        sampleRateHz: Int,
    ): VoiceFxProcessResult =
        withContext(Dispatchers.Default) {
            NativeBagBridge
                .nativeApplyVoiceFx(
                    pcm = pcm,
                    sampleRateHz = sampleRateHz,
                    preset = preset.nativeValue,
                    enableDiagnostics = 1,
                    subvoiceStyle = subvoiceStyle.nativeValue,
                ).toProcessResult()
        }

    override fun createProcessor(
        preset: VoiceFxPresetOption,
        subvoiceStyle: VoiceFxSubvoiceStyleOption,
        sampleRateHz: Int,
        enableDiagnostics: Boolean,
    ): VoiceFxProcessorSession? {
        val handle =
            NativeBagBridge.nativeCreateVoiceFxProcessor(
                sampleRateHz = sampleRateHz,
                preset = preset.nativeValue,
                enableDiagnostics = if (enableDiagnostics) 1 else 0,
                subvoiceStyle = subvoiceStyle.nativeValue,
            )
        if (handle == 0L) {
            return null
        }
        return NativeVoiceFxProcessorSession(handle)
    }
}

private class NativeVoiceFxProcessorSession(
    private var handle: Long,
) : VoiceFxProcessorSession {
    override fun processBlock(pcm: ShortArray): VoiceFxProcessResult {
        if (handle == 0L) {
            return VoiceFxProcessResult.Failed(BagApiCodes.ERROR_INTERNAL)
        }
        return NativeBagBridge.nativeProcessVoiceFxBlock(handle, pcm).toProcessResult()
    }

    override fun flush(): VoiceFxProcessResult {
        if (handle == 0L) {
            return VoiceFxProcessResult.Failed(BagApiCodes.ERROR_INTERNAL)
        }
        return NativeBagBridge.nativeFlushVoiceFxProcessor(handle).toProcessResult()
    }

    override fun release() {
        if (handle == 0L) {
            return
        }
        NativeBagBridge.nativeDestroyVoiceFxProcessor(handle)
        handle = 0L
    }
}

private fun VoiceFxNativeResult.toProcessResult(): VoiceFxProcessResult =
    if (errorCode == BagApiCodes.ERROR_OK) {
        VoiceFxProcessResult.Success(
            finalMix = finalMix,
            mainVoice = mainVoice,
            subvoice = subvoice,
            signalOverlay = signalOverlay,
        )
    } else {
        VoiceFxProcessResult.Failed(errorCode = errorCode)
    }
