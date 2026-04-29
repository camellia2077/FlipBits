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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioEncodePhase

@Composable
internal fun AudioEncodeStatusSection(
    encodeProgress: Float?,
    encodePhase: AudioEncodePhase?,
    isEncodingBusy: Boolean,
    modifier: Modifier = Modifier,
) {
    val clampedProgress = (encodeProgress ?: 0f).coerceIn(0f, 1f)

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
                encodeProgress = clampedProgress,
                isEncodingBusy = isEncodingBusy,
            )
        }
        if (isEncodingBusy) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                encodePhase?.let { phase ->
                    Text(
                        text = stringResource(phase.labelResId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { clampedProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private val AudioEncodePhase.labelResId: Int
    get() =
        when (this) {
            AudioEncodePhase.PreparingInput -> R.string.audio_encode_phase_preparing_input_label
            AudioEncodePhase.RenderingPcm -> R.string.audio_encode_phase_rendering_pcm_label
            AudioEncodePhase.Postprocessing -> R.string.audio_encode_phase_postprocessing_label
            AudioEncodePhase.Finalizing -> R.string.audio_encode_phase_finalizing_label
        }
