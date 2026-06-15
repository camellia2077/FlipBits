package com.bag.audioandroid.domain

import com.bag.audioandroid.ui.model.VoiceFxPresetOption
import com.bag.audioandroid.ui.model.VoiceFxSubvoiceStyleOption
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

data class VoiceFxStreamCollectedResult(
    val finalMix: ShortArray,
    val errorCode: Int,
)

class VoiceFxStreamCollector private constructor(
    private val processor: VoiceFxStreamProcessor,
    private val preset: VoiceFxPresetOption,
    private val subvoiceStyle: VoiceFxSubvoiceStyleOption,
    private val sampleRateHz: Int,
) {
    private val lock = Any()
    private val pendingChunks = LinkedBlockingQueue<ShortArray>()
    private val processedChunks = mutableListOf<ShortArray>()
    private var inputSampleCount = 0
    private var outputSampleCount = 0
    private var blockCount = 0
    private var errorCode = BagApiCodes.ERROR_OK
    private var acceptingBlocks = true
    private var released = false
    private val workerThread =
        thread(
            name = "VoiceFxRecordProcessor",
            isDaemon = true,
            block = ::runWorker,
        )

    fun processBlock(pcm: ShortArray) {
        synchronized(lock) {
            if (released || !acceptingBlocks || errorCode != BagApiCodes.ERROR_OK) {
                return
            }
        }
        pendingChunks.put(pcm.copyOf())
    }

    private fun runWorker() {
        while (true) {
            val chunk = pendingChunks.take()
            if (chunk.isEmpty()) {
                return
            }
            val result = processor.processBlock(chunk)
            synchronized(lock) {
                if (released) {
                    return
                }
                recordProcessedBlockLocked(chunk, result)
            }
        }
    }

    fun finish(): VoiceFxStreamCollectedResult = finishAfterQueuedBlocks()

    private fun finishAfterQueuedBlocks(): VoiceFxStreamCollectedResult {
        synchronized(lock) {
            if (released) {
                return VoiceFxStreamCollectedResult(
                    finalMix = shortArrayOf(),
                    errorCode = BagApiCodes.ERROR_INTERNAL,
                )
            }
            acceptingBlocks = false
        }
        pendingChunks.put(EndOfStream)
        workerThread.join()
        synchronized(lock) {
            if (errorCode != BagApiCodes.ERROR_OK) {
                val failure = errorCode
                VoiceFxRecordDiag.log(
                    "finish failed-before-flush preset=${preset.id} style=${subvoiceStyle.id} " +
                        "sr=$sampleRateHz blocks=$blockCount inTotal=$inputSampleCount " +
                        "outTotal=$outputSampleCount error=$failure",
                )
                releaseLocked()
                return VoiceFxStreamCollectedResult(shortArrayOf(), failure)
            }
        }
        return when (val flushResult = processor.flush()) {
            is VoiceFxProcessResult.Success -> {
                synchronized(lock) {
                    if (flushResult.finalMix.isNotEmpty()) {
                        processedChunks += flushResult.finalMix
                    }
                    val finalMix = flattenPcmChunks(processedChunks)
                    val totalOutputWithFlush = outputSampleCount + flushResult.finalMix.size
                    VoiceFxRecordDiag.log(
                        "finish ok preset=${preset.id} style=${subvoiceStyle.id} " +
                            "sr=$sampleRateHz blocks=$blockCount inTotal=$inputSampleCount " +
                            "blockOutTotal=$outputSampleCount flushFinal=${flushResult.finalMix.size} " +
                            "flushMain=${flushResult.mainVoice.size} flushSub=${flushResult.subvoice.size} " +
                            "flushSignal=${flushResult.signalOverlay.size} finalTotal=${finalMix.size} " +
                            "ratio=${formatRatio(totalOutputWithFlush, inputSampleCount)}",
                    )
                    releaseLocked()
                    VoiceFxStreamCollectedResult(finalMix, BagApiCodes.ERROR_OK)
                }
            }
            is VoiceFxProcessResult.Failed -> {
                synchronized(lock) {
                    val failure = flushResult.errorCode
                    VoiceFxRecordDiag.log(
                        "flush failed preset=${preset.id} style=${subvoiceStyle.id} " +
                            "sr=$sampleRateHz blocks=$blockCount inTotal=$inputSampleCount " +
                            "outTotal=$outputSampleCount error=$failure",
                    )
                    releaseLocked()
                    VoiceFxStreamCollectedResult(shortArrayOf(), failure)
                }
            }
        }
    }

    fun release() {
        synchronized(lock) {
            acceptingBlocks = false
            pendingChunks.clear()
        }
        pendingChunks.offer(EndOfStream)
        workerThread.join(500L)
        synchronized(lock) {
            releaseLocked()
        }
    }

    private fun recordProcessedBlockLocked(
        pcm: ShortArray,
        result: VoiceFxProcessResult,
    ) {
        when (result) {
            is VoiceFxProcessResult.Success -> {
                blockCount += 1
                inputSampleCount += pcm.size
                if (result.finalMix.isNotEmpty()) {
                    processedChunks += result.finalMix
                    outputSampleCount += result.finalMix.size
                }
                if (VoiceFxRecordDiag.shouldLogBlock(blockCount)) {
                    VoiceFxRecordDiag.log(
                        "block index=$blockCount preset=${preset.id} style=${subvoiceStyle.id} " +
                            "sr=$sampleRateHz in=${pcm.size} inTotal=$inputSampleCount " +
                            "outFinal=${result.finalMix.size} outMain=${result.mainVoice.size} " +
                            "outSub=${result.subvoice.size} outSignal=${result.signalOverlay.size} " +
                            "outTotal=$outputSampleCount queue=${pendingChunks.size}",
                    )
                }
            }
            is VoiceFxProcessResult.Failed -> {
                errorCode = result.errorCode
                acceptingBlocks = false
                pendingChunks.clear()
                pendingChunks.offer(EndOfStream)
                VoiceFxRecordDiag.log(
                    "block failed preset=${preset.id} style=${subvoiceStyle.id} " +
                        "sr=$sampleRateHz in=${pcm.size} error=${result.errorCode}",
                )
            }
        }
    }

    private fun releaseLocked() {
        if (released) {
            return
        }
        released = true
        acceptingBlocks = false
        pendingChunks.clear()
        processor.release()
        processedChunks.clear()
        errorCode = BagApiCodes.ERROR_OK
    }

    private fun flattenPcmChunks(chunks: List<ShortArray>): ShortArray {
        val totalSamples = chunks.sumOf { it.size }
        if (totalSamples <= 0) {
            return shortArrayOf()
        }
        val output = ShortArray(totalSamples)
        var offset = 0
        chunks.forEach { chunk ->
            chunk.copyInto(output, destinationOffset = offset)
            offset += chunk.size
        }
        return output
    }

    companion object {
        private val EndOfStream = shortArrayOf()

        fun create(
            voiceFxGateway: VoiceFxGateway,
            preset: VoiceFxPresetOption,
            subvoiceStyle: VoiceFxSubvoiceStyleOption,
            sampleRateHz: Int,
        ): VoiceFxStreamCollector? =
            VoiceFxStreamProcessor
                .create(
                    voiceFxGateway = voiceFxGateway,
                    preset = preset,
                    subvoiceStyle = subvoiceStyle,
                    sampleRateHz = sampleRateHz,
                )?.let { processor ->
                    VoiceFxStreamCollector(
                        processor = processor,
                        preset = preset,
                        subvoiceStyle = subvoiceStyle,
                        sampleRateHz = sampleRateHz,
                    )
                }

        private fun formatRatio(
            outputSamples: Int,
            inputSamples: Int,
        ): String =
            if (inputSamples > 0) {
                "%.3f".format(outputSamples.toDouble() / inputSamples.toDouble())
            } else {
                "n/a"
            }
    }
}
