package com.bag.audioandroid.ui

import com.bag.audioandroid.domain.BagDecodeContentCodes
import com.bag.audioandroid.domain.DecodedAudioPayloadResult
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowByteTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowLineRawSegmentViewData
import com.bag.audioandroid.domain.TextFollowLineTokenRangeViewData
import com.bag.audioandroid.domain.TextFollowLyricLineTimelineEntry
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import com.bag.audioandroid.domain.TextFollowRawSegmentViewData
import com.bag.audioandroid.domain.TextFollowTimelineEntry
import kotlin.text.Charsets.UTF_8

internal data class SegmentedInputPlan(
    val segments: List<String>,
) {
    val segmentCount: Int
        get() = segments.size
}

internal fun splitInputIntoPayloadSegments(
    text: String,
    maxPayloadBytes: Int,
): SegmentedInputPlan {
    require(maxPayloadBytes > 0) { "maxPayloadBytes must be positive." }
    if (text.isEmpty()) {
        return SegmentedInputPlan(listOf(""))
    }

    val segments = ArrayList<String>()
    var segmentStart = 0
    var segmentByteCount = 0
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        val codePointCharCount = Character.charCount(codePoint)
        val nextIndex = index + codePointCharCount
        val codePointByteCount = text.substring(index, nextIndex).toByteArray(UTF_8).size
        if (segmentByteCount > 0 && segmentByteCount + codePointByteCount > maxPayloadBytes) {
            segments += text.substring(segmentStart, index)
            segmentStart = index
            segmentByteCount = 0
        }
        segmentByteCount += codePointByteCount
        index = nextIndex
    }
    if (segmentStart <= text.length) {
        segments += text.substring(segmentStart, text.length)
    }

    return SegmentedInputPlan(segments = segments.filter { it.isNotEmpty() }.ifEmpty { listOf("") })
}

internal fun mergeSegmentedFollowData(segments: List<PayloadFollowViewData>): PayloadFollowViewData {
    if (segments.isEmpty()) {
        return PayloadFollowViewData.Empty
    }
    if (segments.size == 1) {
        return segments.first()
    }

    val textTokens = ArrayList<String>()
    val textTokenTimeline = ArrayList<TextFollowTimelineEntry>()
    val textRawSegments = ArrayList<TextFollowRawSegmentViewData>()
    val textRawDisplayUnits = ArrayList<TextFollowRawDisplayUnitViewData>()
    val lyricLines = ArrayList<String>()
    val lyricLineTimeline = ArrayList<TextFollowLyricLineTimelineEntry>()
    val lineTokenRanges = ArrayList<TextFollowLineTokenRangeViewData>()
    val lineRawSegments = ArrayList<TextFollowLineRawSegmentViewData>()
    val hexTokens = ArrayList<String>()
    val binaryTokens = ArrayList<String>()
    val byteTimeline = ArrayList<PayloadFollowByteTimelineEntry>()
    val binaryGroupTimeline = ArrayList<PayloadFollowBinaryGroupTimelineEntry>()

    var sampleOffset = 0
    var tokenOffset = 0
    var lineOffset = 0
    var byteOffset = 0
    var groupOffset = 0
    var payloadBeginSample = 0
    var payloadSampleCount = 0
    var sawPayloadBegin = false

    segments.forEach { segment ->
        if (!segment.followAvailable) {
            return PayloadFollowViewData.Empty
        }
        if (!sawPayloadBegin && segment.payloadSampleCount > 0) {
            payloadBeginSample = sampleOffset + segment.payloadBeginSample
            sawPayloadBegin = true
        }
        payloadSampleCount += segment.payloadSampleCount

        textTokens += segment.textTokens
        textTokenTimeline +=
            segment.textTokenTimeline.map { entry ->
                entry.copy(
                    startSample = sampleOffset + entry.startSample,
                    tokenIndex = tokenOffset + entry.tokenIndex,
                )
            }
        textRawSegments +=
            segment.textRawSegments.map { entry ->
                entry.copy(
                    tokenIndex = tokenOffset + entry.tokenIndex,
                    startSample = sampleOffset + entry.startSample,
                    byteOffset = byteOffset + entry.byteOffset,
                )
            }
        textRawDisplayUnits +=
            segment.textRawDisplayUnits.map { entry ->
                entry.copy(
                    tokenIndex = tokenOffset + entry.tokenIndex,
                    startSample = sampleOffset + entry.startSample,
                    byteOffset = byteOffset + entry.byteOffset,
                )
            }
        lyricLines += segment.lyricLines
        lyricLineTimeline +=
            segment.lyricLineTimeline.map { entry ->
                entry.copy(
                    startSample = sampleOffset + entry.startSample,
                    lineIndex = lineOffset + entry.lineIndex,
                )
            }
        lineTokenRanges +=
            segment.lineTokenRanges.map { entry ->
                entry.copy(
                    lineIndex = lineOffset + entry.lineIndex,
                    tokenBeginIndex = tokenOffset + entry.tokenBeginIndex,
                )
            }
        lineRawSegments +=
            segment.lineRawSegments.map { entry ->
                entry.copy(
                    lineIndex = lineOffset + entry.lineIndex,
                    startSample = sampleOffset + entry.startSample,
                    byteOffset = byteOffset + entry.byteOffset,
                )
            }
        hexTokens += segment.hexTokens
        binaryTokens += segment.binaryTokens
        byteTimeline +=
            segment.byteTimeline.map { entry ->
                entry.copy(
                    startSample = sampleOffset + entry.startSample,
                    byteIndex = byteOffset + entry.byteIndex,
                )
            }
        binaryGroupTimeline +=
            segment.binaryGroupTimeline.map { entry ->
                entry.copy(
                    startSample = sampleOffset + entry.startSample,
                    groupIndex = groupOffset + entry.groupIndex,
                )
            }

        sampleOffset += segment.totalPcmSampleCount
        tokenOffset += segment.textTokens.size
        lineOffset += segment.lyricLines.size
        byteOffset += segment.hexTokens.size
        groupOffset += segment.binaryGroupTimeline.size
    }

    return PayloadFollowViewData(
        textTokens = textTokens,
        textTokenTimeline = textTokenTimeline,
        textRawSegments = textRawSegments,
        textRawDisplayUnits = textRawDisplayUnits,
        textFollowAvailable = segments.all(PayloadFollowViewData::textFollowAvailable),
        lyricLines = lyricLines,
        lyricLineTimeline = lyricLineTimeline,
        lineTokenRanges = lineTokenRanges,
        lineRawSegments = lineRawSegments,
        lyricLineFollowAvailable = segments.all(PayloadFollowViewData::lyricLineFollowAvailable),
        hexTokens = hexTokens,
        binaryTokens = binaryTokens,
        byteTimeline = byteTimeline,
        binaryGroupTimeline = binaryGroupTimeline,
        payloadBeginSample = payloadBeginSample,
        payloadSampleCount = payloadSampleCount,
        totalPcmSampleCount = sampleOffset,
        followAvailable = true,
    )
}

internal fun splitPcmIntoSegments(
    pcm: ShortArray,
    segmentSampleCounts: List<Int>,
): List<ShortArray>? {
    if (segmentSampleCounts.isEmpty() || segmentSampleCounts.any { it <= 0 }) {
        return null
    }
    if (segmentSampleCounts.sum() != pcm.size) {
        return null
    }

    val segments = ArrayList<ShortArray>(segmentSampleCounts.size)
    var offset = 0
    segmentSampleCounts.forEach { sampleCount ->
        val nextOffset = offset + sampleCount
        segments += pcm.copyOfRange(offset, nextOffset)
        offset = nextOffset
    }
    return segments
}

internal fun mergeSegmentedDecodedPayloadResults(segments: List<DecodedAudioPayloadResult>): DecodedAudioPayloadResult {
    if (segments.isEmpty()) {
        return DecodedAudioPayloadResult()
    }
    if (segments.size == 1) {
        return segments.first()
    }

    val decodedPayloads = segments.map(DecodedAudioPayloadResult::decodedPayload)
    val mergedTextStatus = mergeSegmentedTextStatus(decodedPayloads)
    val mergedPayload =
        DecodedPayloadViewData(
            text =
                if (
                    mergedTextStatus == BagDecodeContentCodes.STATUS_OK ||
                    mergedTextStatus == BagDecodeContentCodes.STATUS_BUFFER_TOO_SMALL
                ) {
                    decodedPayloads.joinToString(separator = "") { it.text }
                } else {
                    ""
                },
            rawBytesHex = joinNonBlank(decodedPayloads.map(DecodedPayloadViewData::rawBytesHex)),
            rawBitsBinary = joinNonBlank(decodedPayloads.map(DecodedPayloadViewData::rawBitsBinary)),
            textDecodeStatusCode = mergedTextStatus,
            rawPayloadAvailable = decodedPayloads.all(DecodedPayloadViewData::rawPayloadAvailable),
        )

    val mergedFollowData = mergeSegmentedFollowData(segments.map(DecodedAudioPayloadResult::followData))
    return DecodedAudioPayloadResult(
        decodedPayload = mergedPayload,
        followData = mergedFollowData,
    )
}

private fun mergeSegmentedTextStatus(decodedPayloads: List<DecodedPayloadViewData>): Int =
    when {
        decodedPayloads.any { it.textDecodeStatusCode == BagDecodeContentCodes.STATUS_INTERNAL_ERROR } ->
            BagDecodeContentCodes.STATUS_INTERNAL_ERROR
        decodedPayloads.any { it.textDecodeStatusCode == BagDecodeContentCodes.STATUS_INVALID_TEXT_PAYLOAD } ->
            BagDecodeContentCodes.STATUS_INVALID_TEXT_PAYLOAD
        decodedPayloads.any { it.textDecodeStatusCode == BagDecodeContentCodes.STATUS_BUFFER_TOO_SMALL } ->
            BagDecodeContentCodes.STATUS_BUFFER_TOO_SMALL
        decodedPayloads.all { it.textDecodeStatusCode == BagDecodeContentCodes.STATUS_OK } ->
            BagDecodeContentCodes.STATUS_OK
        else -> BagDecodeContentCodes.STATUS_UNAVAILABLE
    }

private fun joinNonBlank(parts: List<String>): String = parts.filter(String::isNotBlank).joinToString(separator = " ")
