package com.bag.audioandroid.data

import com.bag.audioandroid.domain.GeneratedAudioInputSourceKind
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.MorseSpeedOption
import com.bag.audioandroid.ui.model.TransportModeOption

// Mirrors the native WBAG v7 metadata layout so file-backed saved WAVs keep mode/style data.
internal fun GeneratedAudioMetadata.toFlipBitsWavMetadataChunk(): ByteArray {
    if (!hasValidFlipBitsWavMetadataChunkHeader()) {
        return byteArrayOf()
    }
    val payloadSize = MetadataFixedPayloadSize + (segmentSampleCounts.size * 4)
    val chunk = ByteArray(ChunkHeaderBytes + payloadSize)
    chunk.writeAsciiAt(0, "WBAG")
    chunk.writeInt32LEAt(4, payloadSize)
    val payloadOffset = ChunkHeaderBytes
    chunk[payloadOffset + MetadataVersionOffset] = version.toByte()
    chunk[payloadOffset + MetadataModeOffset] = mode.metadataWireValue().toByte()
    chunk[payloadOffset + MetadataHasFlashStyleOffset] = if (flashVoicingStyle != null) 1 else 0
    chunk[payloadOffset + MetadataFlashStyleOffset] = (flashVoicingStyle?.metadataWireValue() ?: 0).toByte()
    chunk[payloadOffset + MetadataInputSourceKindOffset] = inputSourceKind.metadataWireValue().toByte()
    chunk[payloadOffset + MetadataHasMiniSpeedStyleOffset] = if (miniSpeedStyle != null) 1 else 0
    chunk[payloadOffset + MetadataMiniSpeedStyleOffset] = (miniSpeedStyle?.metadataWireValue() ?: 0).toByte()
    chunk.writeInt32LEAt(payloadOffset + MetadataDurationOffset, durationMs.coerceAtLeast(0L).coerceAtMost(0xFFFFFFFFL).toInt())
    chunk.writeAsciiAt(payloadOffset + MetadataCreatedAtOffset, createdAtIsoUtc)
    chunk.writeInt32LEAt(payloadOffset + MetadataSampleRateHzOffset, sampleRateHz)
    chunk.writeInt32LEAt(payloadOffset + MetadataFrameSamplesOffset, frameSamples.coerceAtLeast(0))
    chunk.writeInt32LEAt(payloadOffset + MetadataPcmSampleCountOffset, pcmSampleCount.coerceAtLeast(0))
    chunk.writeInt32LEAt(payloadOffset + MetadataPayloadByteCountOffset, payloadByteCount.coerceAtLeast(0))
    chunk.writeAsciiPaddedAt(payloadOffset + MetadataAppVersionOffset, appVersion, MetadataVersionTextByteCount)
    chunk.writeAsciiPaddedAt(payloadOffset + MetadataCoreVersionOffset, coreVersion, MetadataVersionTextByteCount)
    chunk.writeInt32LEAt(payloadOffset + MetadataSegmentCountOffset, segmentCount)
    chunk.writeInt32LEAt(payloadOffset + MetadataSegmentSampleCountCountOffset, segmentSampleCounts.size)
    var sampleCountOffset = payloadOffset + MetadataHeaderSize
    segmentSampleCounts.forEach { sampleCount ->
        chunk.writeInt32LEAt(sampleCountOffset, sampleCount.coerceAtLeast(0))
        sampleCountOffset += 4
    }
    return chunk
}

private fun GeneratedAudioMetadata.hasValidFlipBitsWavMetadataChunkHeader(): Boolean =
    hasValidMetadataText() &&
        sampleRateHz > 0 &&
        segmentCount > 0 &&
        hasValidSegmentSampleCounts()

private fun GeneratedAudioMetadata.hasValidMetadataText(): Boolean =
    createdAtIsoUtc.length == MetadataIsoUtcByteCount &&
        appVersion.isValidMetadataVersionText() &&
        coreVersion.isValidMetadataVersionText()

private fun GeneratedAudioMetadata.hasValidSegmentSampleCounts(): Boolean =
    segmentSampleCounts.isEmpty() || segmentSampleCounts.size == segmentCount

private fun String.isValidMetadataVersionText(): Boolean = isNotBlank() && length <= MetadataVersionTextByteCount

private fun TransportModeOption.metadataWireValue(): Int =
    when (this) {
        TransportModeOption.Mini -> 1
        TransportModeOption.Flash -> 2
        TransportModeOption.Pro -> 3
        TransportModeOption.Ultra -> 4
    }

private fun FlashVoicingStyleOption.metadataWireValue(): Int =
    when (this) {
        FlashVoicingStyleOption.Standard -> 1
        FlashVoicingStyleOption.Litany -> 2
        FlashVoicingStyleOption.Hostility -> 4
        FlashVoicingStyleOption.Collapse -> 5
        FlashVoicingStyleOption.Zeal -> 6
        FlashVoicingStyleOption.Void -> 7
    }

private fun MorseSpeedOption.metadataWireValue(): Int =
    when (this) {
        MorseSpeedOption.Wpm10 -> 1
        MorseSpeedOption.Wpm15 -> 2
        MorseSpeedOption.Wpm20 -> 3
    }

private fun GeneratedAudioInputSourceKind.metadataWireValue(): Int =
    when (this) {
        GeneratedAudioInputSourceKind.Manual -> 1
        GeneratedAudioInputSourceKind.Sample -> 2
    }

private fun ByteArray.writeAsciiAt(
    offset: Int,
    value: String,
) {
    val bytes = value.toByteArray(Charsets.US_ASCII)
    bytes.copyInto(this, offset)
}

private fun ByteArray.writeAsciiPaddedAt(
    offset: Int,
    value: String,
    byteCount: Int,
) {
    val bytes = value.toByteArray(Charsets.US_ASCII)
    bytes.copyInto(this, offset, 0, minOf(bytes.size, byteCount))
}

private fun ByteArray.writeInt32LEAt(
    offset: Int,
    value: Int,
) {
    this[offset] = (value and 0xFF).toByte()
    this[offset + 1] = ((value ushr 8) and 0xFF).toByte()
    this[offset + 2] = ((value ushr 16) and 0xFF).toByte()
    this[offset + 3] = ((value ushr 24) and 0xFF).toByte()
}

private const val ChunkHeaderBytes = 8
private const val MetadataIsoUtcByteCount = 20
private const val MetadataVersionTextByteCount = 32
private const val MetadataVersionOffset = 0
private const val MetadataModeOffset = 1
private const val MetadataHasFlashStyleOffset = 2
private const val MetadataFlashStyleOffset = 3
private const val MetadataInputSourceKindOffset = 4
private const val MetadataHasMiniSpeedStyleOffset = 5
private const val MetadataMiniSpeedStyleOffset = 6
private const val MetadataDurationOffset = 8
private const val MetadataCreatedAtOffset = 12
private const val MetadataSampleRateHzOffset = MetadataCreatedAtOffset + MetadataIsoUtcByteCount
private const val MetadataFrameSamplesOffset = MetadataSampleRateHzOffset + 4
private const val MetadataPcmSampleCountOffset = MetadataFrameSamplesOffset + 4
private const val MetadataPayloadByteCountOffset = MetadataPcmSampleCountOffset + 4
private const val MetadataAppVersionOffset = MetadataPayloadByteCountOffset + 4
private const val MetadataCoreVersionOffset = MetadataAppVersionOffset + MetadataVersionTextByteCount
private const val MetadataSegmentCountOffset = MetadataCoreVersionOffset + MetadataVersionTextByteCount
private const val MetadataSegmentSampleCountCountOffset = MetadataSegmentCountOffset + 4
private const val MetadataHeaderSize = MetadataSegmentSampleCountCountOffset + 4
private const val MetadataFixedPayloadSize = MetadataHeaderSize + (2 * MetadataVersionTextByteCount)
