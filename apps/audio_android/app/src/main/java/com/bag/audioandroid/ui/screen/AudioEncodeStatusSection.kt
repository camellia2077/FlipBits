package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.ui.theme.AudioEncodeGlyphColors

@Composable
internal fun AudioEncodeStatusSection(
    glyphProgressDisplay: GlyphProgressDisplayModel?,
    encodeProgressDisplay: EncodeProgressDisplayModel?,
    glyphBaseSize: Dp = 108.dp,
    showGlyphCropGuide: Boolean = false,
    glyphColorsOverride: AudioEncodeGlyphColors? = null,
    modifier: Modifier = Modifier,
) {
    val progress = glyphProgressDisplay?.progress0To1 ?: 0f
    val isGlyphActive = glyphProgressDisplay?.isActive == true
    val showIdleCoreRing = glyphProgressDisplay?.showIdleCoreRing ?: true
    val isEncodingBusy = encodeProgressDisplay != null && isGlyphActive
    val glyphCropGuideBackgroundColor = glyphColorsOverride?.secondarySplit ?: MaterialTheme.colorScheme.surface
    val glyphCropGuideColor =
        if (showGlyphCropGuide) {
            cropGuideContrastColor(glyphCropGuideBackgroundColor)
        } else {
            Color.Unspecified
        }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            AudioEncodeGlyph(
                encodeProgress = progress,
                isEncodingBusy = isGlyphActive,
                baseSize = glyphBaseSize,
                showCropGuide = showGlyphCropGuide,
                cropGuideColor = glyphCropGuideColor,
                cropGuideBackgroundColor = glyphCropGuideBackgroundColor,
                showIdleCoreRing = showIdleCoreRing,
                glyphColorsOverride = glyphColorsOverride,
            )
        }
        if (isEncodingBusy) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                encodeProgressDisplay.phase.let { phase ->
                    Text(
                        text = stringResource(phase.labelResId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun cropGuideContrastColor(backgroundColor: Color): Color =
    if (backgroundColor.luminance() < 0.42f) {
        Color.White
    } else {
        Color.Black
    }

private val AudioEncodePhase.labelResId: Int
    get() =
        when (this) {
            AudioEncodePhase.PreparingInput -> R.string.audio_encode_phase_preparing_input_label
            AudioEncodePhase.RenderingPcm -> R.string.audio_encode_phase_rendering_pcm_label
            AudioEncodePhase.Postprocessing -> R.string.audio_encode_phase_postprocessing_label
            AudioEncodePhase.Finalizing -> R.string.audio_encode_phase_finalizing_label
        }
