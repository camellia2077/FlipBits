package com.bag.audioandroid.domain

internal fun SavedAudioDecodedCacheEntry.needsSavedDecodeRefresh(
    item: SavedAudioItem,
    metadata: GeneratedAudioMetadata?,
): Boolean = followData.needsSavedDecodeRefresh(item, metadata)

internal fun PayloadFollowViewData.needsSavedDecodeRefresh(
    item: SavedAudioItem,
    metadata: GeneratedAudioMetadata?,
): Boolean {
    if (hasStaleSavedDecodeFollowData()) {
        return true
    }
    val expectedPayloadBytes = metadata.expectedPayloadByteCount(item) ?: return false
    if (!followAvailable) {
        return true
    }
    return observedPayloadByteCount() < expectedPayloadBytes
}

private fun PayloadFollowViewData.hasStaleSavedDecodeFollowData(): Boolean =
    followAvailable &&
        !textFollowAvailable &&
        hasNoSavedDecodeTimelineData()

private fun PayloadFollowViewData.hasNoSavedDecodeTimelineData(): Boolean =
    textTokens.isEmpty() &&
        textTokenTimeline.isEmpty() &&
        binaryGroupTimeline.isEmpty() &&
        ultraFrameTimeline.isEmpty()

private fun GeneratedAudioMetadata?.expectedPayloadByteCount(item: SavedAudioItem): Int? =
    this?.payloadByteCount?.takeIf { it > 0 }
        ?: item.payloadByteCount?.takeIf { it > 0 }

private fun PayloadFollowViewData.observedPayloadByteCount(): Int {
    val byteCountFromGroups =
        binaryGroupTimeline
            .maxOfOrNull { it.bitOffset + it.bitCount }
            ?.let { (it + 7) / 8 }
            ?: 0
    val byteCountFromRawUnits = textRawDisplayUnits.sumOf { it.byteCount.coerceAtLeast(1) }
    return listOf(
        byteTimeline.size,
        hexTokens.size,
        byteCountFromRawUnits,
        byteCountFromGroups,
    ).maxOrNull() ?: 0
}
