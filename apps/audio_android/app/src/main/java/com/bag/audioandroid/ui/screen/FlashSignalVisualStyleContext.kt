package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption

internal data class FlashSignalVisualStyleContext(
    val activeWindowBucketCount: Int,
    val toneFrequencyScale: ToneFrequencyScale,
    val toneCarrierLayout: ToneCarrierLayout,
)

@Composable
internal fun rememberFlashSignalVisualStyleContext(flashVoicingStyle: FlashVoicingStyleOption?): FlashSignalVisualStyleContext {
    val activeWindowBucketCount =
        remember(flashVoicingStyle) {
            flashSignalActiveWindowBucketCount(flashVoicingStyle)
        }
    val toneFrequencyScale =
        remember(flashVoicingStyle) {
            toneFrequencyScaleForStyle(flashVoicingStyle)
        }
    val toneCarrierLayout =
        remember(flashVoicingStyle) {
            toneCarrierLayoutForStyle(flashVoicingStyle)
        }
    return remember(
        activeWindowBucketCount,
        toneFrequencyScale,
        toneCarrierLayout,
    ) {
        FlashSignalVisualStyleContext(
            activeWindowBucketCount = activeWindowBucketCount,
            toneFrequencyScale = toneFrequencyScale,
            toneCarrierLayout = toneCarrierLayout,
        )
    }
}
