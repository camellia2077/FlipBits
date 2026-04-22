package com.bag.audioandroid.domain

data class PayloadFollowByteTimelineEntry(
    val startSample: Int,
    val sampleCount: Int,
    val byteIndex: Int,
)

data class PayloadFollowBinaryGroupTimelineEntry(
    val startSample: Int,
    val sampleCount: Int,
    val groupIndex: Int,
    val bitOffset: Int,
    val bitCount: Int,
)

data class TextFollowTimelineEntry(
    val startSample: Int,
    val sampleCount: Int,
    val tokenIndex: Int,
)

data class TextFollowRawSegmentViewData(
    val tokenIndex: Int,
    val startSample: Int,
    val sampleCount: Int,
    val byteOffset: Int,
    val byteCount: Int,
    val hexText: String,
    val binaryText: String,
)

data class TextFollowRawDisplayUnitViewData(
    val tokenIndex: Int,
    val startSample: Int,
    val sampleCount: Int,
    val byteIndexWithinToken: Int,
    val byteOffset: Int,
    val byteCount: Int,
    val hexText: String,
    val binaryText: String,
)

data class TextFollowLyricLineTimelineEntry(
    val startSample: Int,
    val sampleCount: Int,
    val lineIndex: Int,
)

data class TextFollowLineTokenRangeViewData(
    val lineIndex: Int,
    val tokenBeginIndex: Int,
    val tokenCount: Int,
)

data class TextFollowLineRawSegmentViewData(
    val lineIndex: Int,
    val startSample: Int,
    val sampleCount: Int,
    val byteOffset: Int,
    val byteCount: Int,
    val hexText: String,
    val binaryText: String,
)

data class PayloadFollowViewData(
    val textTokens: List<String> = emptyList(),
    val textTokenTimeline: List<TextFollowTimelineEntry> = emptyList(),
    val textRawSegments: List<TextFollowRawSegmentViewData> = emptyList(),
    val textRawDisplayUnits: List<TextFollowRawDisplayUnitViewData> = emptyList(),
    val textFollowAvailable: Boolean = false,
    val lyricLines: List<String> = emptyList(),
    val lyricLineTimeline: List<TextFollowLyricLineTimelineEntry> = emptyList(),
    val lineTokenRanges: List<TextFollowLineTokenRangeViewData> = emptyList(),
    val lineRawSegments: List<TextFollowLineRawSegmentViewData> = emptyList(),
    val lyricLineFollowAvailable: Boolean = false,
    val hexTokens: List<String> = emptyList(),
    val binaryTokens: List<String> = emptyList(),
    val byteTimeline: List<PayloadFollowByteTimelineEntry> = emptyList(),
    val binaryGroupTimeline: List<PayloadFollowBinaryGroupTimelineEntry> = emptyList(),
    val payloadBeginSample: Int = 0,
    val payloadSampleCount: Int = 0,
    val totalPcmSampleCount: Int = 0,
    val followAvailable: Boolean = false,
) {
    companion object {
        val Empty = PayloadFollowViewData()
    }
}
