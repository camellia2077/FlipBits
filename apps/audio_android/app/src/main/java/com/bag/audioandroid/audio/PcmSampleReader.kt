package com.bag.audioandroid.audio

import java.io.BufferedInputStream
import java.io.FileInputStream

internal const val PcmShortBytes = 2
internal const val PcmStreamingBufferBytes = 32 * 1024

internal fun skipPcmBytesFully(
    input: BufferedInputStream,
    bytesToSkip: Long,
) {
    var remaining = bytesToSkip
    while (remaining > 0) {
        val skipped = input.skip(remaining)
        if (skipped > 0) {
            remaining -= skipped
            continue
        }
        if (input.read() == -1) {
            break
        }
        remaining -= 1
    }
}

internal fun readPcmFileRange(
    pcmFilePath: String,
    startSampleIndex: Int,
    totalSamples: Int,
): ShortArray {
    val sampleCount = (totalSamples - startSampleIndex).coerceAtLeast(0)
    if (sampleCount <= 0) {
        return ShortArray(0)
    }
    val output = ShortArray(sampleCount)
    BufferedInputStream(FileInputStream(pcmFilePath), PcmStreamingBufferBytes).use { input ->
        skipPcmBytesFully(input, startSampleIndex.toLong() * PcmShortBytes.toLong())
        val byteBuffer = ByteArray(PcmStreamingBufferBytes)
        var outputOffset = 0
        while (outputOffset < output.size) {
            val bytesRead = input.read(byteBuffer)
            if (bytesRead <= 0) {
                break
            }
            val samplesRead = bytesRead / PcmShortBytes
            var byteIndex = 0
            repeat(samplesRead.coerceAtMost(output.size - outputOffset)) {
                val low = byteBuffer[byteIndex].toInt() and 0xFF
                val high = byteBuffer[byteIndex + 1].toInt() shl 8
                output[outputOffset] = (high or low).toShort()
                outputOffset += 1
                byteIndex += PcmShortBytes
            }
        }
    }
    return output
}

internal fun readShortSamples(
    input: BufferedInputStream,
    buffer: ShortArray,
): Int {
    val byteBuffer = ByteArray(buffer.size * PcmShortBytes)
    val bytesRead = input.read(byteBuffer)
    if (bytesRead <= 0) {
        return 0
    }
    val sampleCount = bytesRead / PcmShortBytes
    var byteIndex = 0
    repeat(sampleCount) { sampleIndex ->
        val low = byteBuffer[byteIndex].toInt() and 0xFF
        val high = byteBuffer[byteIndex + 1].toInt() shl 8
        buffer[sampleIndex] = (high or low).toShort()
        byteIndex += PcmShortBytes
    }
    return sampleCount
}
