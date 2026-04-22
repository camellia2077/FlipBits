package com.bag.audioandroid.domain

data class DecodedPayloadViewData(
    val text: String = "",
    val rawBytesHex: String = "",
    val rawBitsBinary: String = "",
    val textDecodeStatusCode: Int = BagDecodeContentCodes.STATUS_UNAVAILABLE,
    val rawPayloadAvailable: Boolean = false,
) {
    val hasTextResult: Boolean
        get() =
            textDecodeStatusCode == BagDecodeContentCodes.STATUS_OK ||
                textDecodeStatusCode == BagDecodeContentCodes.STATUS_BUFFER_TOO_SMALL

    companion object {
        val Empty = DecodedPayloadViewData()
    }
}
