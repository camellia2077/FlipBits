package com.bag.audioandroid.ui.screen

import androidx.annotation.StringRes
import com.bag.audioandroid.R
import java.nio.charset.StandardCharsets.UTF_8

internal data class AudioInputTextMetrics(
    val characterCount: Int,
    val byteCount: Int,
    @param:StringRes val payloadLimitMessageResId: Int?,
)

internal fun measureAudioInputText(inputText: String): AudioInputTextMetrics {
    val utf8ByteCount = inputText.toByteArray(UTF_8).size
    val payloadLimitMessageResId =
        when {
            utf8ByteCount > AUDIO_INPUT_MAX_PAYLOAD_BYTES -> R.string.audio_input_payload_limit_exceeded
            utf8ByteCount >= AUDIO_INPUT_PAYLOAD_WARNING_BYTES -> R.string.audio_input_payload_limit_warning
            else -> null
        }

    // Keep the visible counter aligned with user-perceived characters instead of
    // UTF-16 code units, while the byte budget continues to track transport size.
    return AudioInputTextMetrics(
        characterCount = inputText.codePointCount(0, inputText.length),
        byteCount = utf8ByteCount,
        payloadLimitMessageResId = payloadLimitMessageResId,
    )
}

private const val AUDIO_INPUT_MAX_PAYLOAD_BYTES = 512
private const val AUDIO_INPUT_PAYLOAD_WARNING_BYTES = 448
