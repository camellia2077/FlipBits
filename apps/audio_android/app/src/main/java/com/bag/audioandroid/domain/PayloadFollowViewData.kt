package com.bag.audioandroid.domain

import androidx.annotation.Keep

@Keep
data class PayloadFollowByteTimelineEntry(
    val startSample: Int,
    val sampleCount: Int,
    val byteIndex: Int,
)

@Keep
data class PayloadFollowBinaryGroupTimelineEntry(
    val startSample: Int,
    val sampleCount: Int,
    val groupIndex: Int,
    val bitOffset: Int,
    val bitCount: Int,
    val carrierFrequencyHz: Float = 0f,
)

@Keep
enum class UltraFrameSection(
    val wireValue: Int,
) {
    Preamble(0),
    Sync(1),
    Version(2),
    Flags(3),
    PayloadLength(4),
    Payload(5),
    Crc16(6),
    ;

    companion object {
        fun fromWireValue(value: Int): UltraFrameSection = entries.firstOrNull { it.wireValue == value } ?: Payload
    }
}

@Keep
data class UltraFrameSymbolTimelineEntry(
    val startSample: Int,
    val sampleCount: Int,
    val frameByteIndex: Int,
    val nibbleIndexInByte: Int,
    val nibbleValue: Int,
    val carrierFrequencyHz: Float,
    val sectionCode: Int,
    val isPayload: Boolean,
    val payloadByteIndex: Int,
) {
    val section: UltraFrameSection
        get() = UltraFrameSection.fromWireValue(sectionCode)
}

@Keep
enum class TextFollowCharacterKind(
    val wireValue: Int,
) {
    Visible(0),
    Space(1),
    Newline(2),
    SeparatorOther(3),
    ;

    companion object {
        fun fromWireValue(value: Int): TextFollowCharacterKind = entries.firstOrNull { it.wireValue == value } ?: SeparatorOther
    }
}

@Keep
data class TextFollowTimelineEntry(
    val startSample: Int,
    val sampleCount: Int,
    val tokenIndex: Int,
)

@Keep
data class TextFollowCharacterViewData(
    val tokenIndex: Int,
    val characterIndexWithinToken: Int,
    val byteIndexWithinToken: Int,
    val byteCount: Int,
    val startSample: Int,
    val sampleCount: Int,
    val kindCode: Int = TextFollowCharacterKind.Visible.wireValue,
    val text: String = "",
) {
    val kind: TextFollowCharacterKind
        get() = TextFollowCharacterKind.fromWireValue(kindCode)
}

@Keep
data class TextFollowRawSegmentViewData(
    val tokenIndex: Int,
    val startSample: Int,
    val sampleCount: Int,
    val byteOffset: Int,
    val byteCount: Int,
    val hexText: String,
    val binaryText: String,
)

@Keep
data class TextFollowRawDisplayUnitViewData(
    val tokenIndex: Int,
    val startSample: Int,
    val sampleCount: Int,
    val byteIndexWithinToken: Int,
    val byteOffset: Int,
    val byteCount: Int,
    val characterIndexWithinToken: Int = 0,
    val byteIndexWithinCharacter: Int = 0,
    val characterByteCount: Int = 0,
    val isCharacterStart: Boolean = false,
    val isCharacterEnd: Boolean = false,
    val hexText: String,
    val binaryText: String,
)

@Keep
data class TextFollowLyricLineTimelineEntry(
    val startSample: Int,
    val sampleCount: Int,
    val lineIndex: Int,
)

@Keep
data class TextFollowLineTokenRangeViewData(
    val lineIndex: Int,
    val tokenBeginIndex: Int,
    val tokenCount: Int,
)

@Keep
data class TextFollowLineRawSegmentViewData(
    val lineIndex: Int,
    val startSample: Int,
    val sampleCount: Int,
    val byteOffset: Int,
    val byteCount: Int,
    val hexText: String,
    val binaryText: String,
)

@Keep
data class PayloadFollowViewData(
    val textTokens: List<String> = emptyList(),
    val textTokenTimeline: List<TextFollowTimelineEntry> = emptyList(),
    val textCharacters: List<TextFollowCharacterViewData> = emptyList(),
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
    val ultraFrameTimeline: List<UltraFrameSymbolTimelineEntry> = emptyList(),
) {
    companion object {
        val Empty = PayloadFollowViewData()
    }
}
