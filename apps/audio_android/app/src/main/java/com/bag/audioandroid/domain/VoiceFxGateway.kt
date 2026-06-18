package com.bag.audioandroid.domain

import com.bag.audioandroid.ui.model.VoiceFxPresetOption
import com.bag.audioandroid.ui.model.VoiceFxSubvoiceStyleOption

sealed interface VoiceFxProcessResult {
    data class Success(
        val finalMix: ShortArray,
        val mainVoice: ShortArray,
        val subvoice: ShortArray,
        val signalOverlay: ShortArray,
    ) : VoiceFxProcessResult

    data class Failed(
        val errorCode: Int,
    ) : VoiceFxProcessResult
}

interface VoiceFxProcessorSession {
    fun processBlock(pcm: ShortArray): VoiceFxProcessResult

    fun flush(): VoiceFxProcessResult

    fun release()
}

interface VoiceFxGateway {
    /**
     * Offline/canonical path for imported files and after-recording processing.
     */
    suspend fun applyVoiceFx(
        preset: VoiceFxPresetOption,
        subvoiceStyle: VoiceFxSubvoiceStyleOption,
        pcm: ShortArray,
        sampleRateHz: Int,
    ): VoiceFxProcessResult

    /**
     * Streaming path for live preview and while-recording processing only.
     */
    fun createProcessor(
        preset: VoiceFxPresetOption,
        subvoiceStyle: VoiceFxSubvoiceStyleOption,
        sampleRateHz: Int,
        enableDiagnostics: Boolean = false,
    ): VoiceFxProcessorSession?
}
