package com.bag.audioandroid.domain

import com.bag.audioandroid.ui.model.VoiceFxPresetOption
import com.bag.audioandroid.ui.model.VoiceFxSubvoiceStyleOption

class VoiceFxStreamProcessor private constructor(
    private val processorSession: VoiceFxProcessorSession,
) {
    private var streamErrorCode: Int = BagApiCodes.ERROR_OK
    private var released = false

    fun processBlock(pcm: ShortArray): VoiceFxProcessResult {
        if (released) {
            return VoiceFxProcessResult.Failed(BagApiCodes.ERROR_INTERNAL)
        }
        if (streamErrorCode != BagApiCodes.ERROR_OK) {
            return VoiceFxProcessResult.Failed(streamErrorCode)
        }
        if (pcm.isEmpty()) {
            return EmptySuccess
        }
        return processorSession.processBlock(pcm).rememberFailure()
    }

    fun flush(): VoiceFxProcessResult {
        if (released) {
            return VoiceFxProcessResult.Failed(BagApiCodes.ERROR_INTERNAL)
        }
        if (streamErrorCode != BagApiCodes.ERROR_OK) {
            return VoiceFxProcessResult.Failed(streamErrorCode)
        }
        return processorSession.flush().rememberFailure()
    }

    fun release() {
        if (released) {
            return
        }
        released = true
        processorSession.release()
    }

    private fun VoiceFxProcessResult.rememberFailure(): VoiceFxProcessResult {
        if (this is VoiceFxProcessResult.Failed) {
            streamErrorCode = this.errorCode
        }
        return this
    }

    companion object {
        fun create(
            voiceFxGateway: VoiceFxGateway,
            preset: VoiceFxPresetOption,
            subvoiceStyle: VoiceFxSubvoiceStyleOption,
            sampleRateHz: Int,
            enableDiagnostics: Boolean = false,
        ): VoiceFxStreamProcessor? =
            voiceFxGateway
                .createProcessor(
                    preset = preset,
                    subvoiceStyle = subvoiceStyle,
                    sampleRateHz = sampleRateHz,
                    enableDiagnostics = enableDiagnostics,
                )?.let(::VoiceFxStreamProcessor)

        private val EmptySuccess =
            VoiceFxProcessResult.Success(
                finalMix = shortArrayOf(),
                mainVoice = shortArrayOf(),
                subvoice = shortArrayOf(),
                signalOverlay = shortArrayOf(),
            )
    }
}
