package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.VoicePreviewTrackOption
import com.bag.audioandroid.ui.state.VoiceSessionState
import com.bag.audioandroid.ui.theme.appThemeVisualTokens
import com.bag.audioandroid.ui.utilityActionIconButtonColors
import kotlin.math.roundToInt

@Composable
internal fun VoiceResultTracks(
    session: VoiceSessionState,
    onTogglePreviewTrack: (VoicePreviewTrackOption) -> Unit,
    onPreviewTrackSeek: (VoicePreviewTrackOption, Int) -> Unit,
    onExportResult: () -> Unit,
    onShareOutput: () -> Unit,
) {
    val saveResultDescription = stringResource(R.string.voice_action_save_result)
    val shareResultDescription = stringResource(R.string.library_action_share)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VoiceResultTrackRow(
            track = VoicePreviewTrackOption.Input,
            title = stringResource(R.string.voice_result_input_track),
            totalSamples = session.inputPcm.size,
            sampleRateHz = session.sampleRateHz,
            isPlaying = session.isPreviewPlaying && session.previewTrack == VoicePreviewTrackOption.Input,
            positionSamples =
                if (session.previewTrack == VoicePreviewTrackOption.Input) {
                    session.previewPositionSamples
                } else {
                    0
                },
            enabled = session.hasInputAudio && !session.isRecording && !session.isLoadingInput && !session.isProcessing,
            onTogglePreviewTrack = onTogglePreviewTrack,
            onPreviewTrackSeek = onPreviewTrackSeek,
            trailingAction = null,
            testTag = "voice-result-input-track",
        )
        VoiceResultTrackRow(
            track = VoicePreviewTrackOption.Output,
            title = stringResource(R.string.voice_result_output_track),
            totalSamples = session.processedPcm.size,
            sampleRateHz = session.sampleRateHz,
            isPlaying = session.isPreviewPlaying && session.previewTrack == VoicePreviewTrackOption.Output,
            positionSamples =
                if (session.previewTrack == VoicePreviewTrackOption.Output) {
                    session.previewPositionSamples
                } else {
                    0
                },
            enabled = session.canPreview || (session.isPreviewPlaying && session.previewTrack == VoicePreviewTrackOption.Output),
            onTogglePreviewTrack = onTogglePreviewTrack,
            onPreviewTrackSeek = onPreviewTrackSeek,
            trailingAction = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onShareOutput,
                        enabled = session.canExportResult,
                        colors = utilityActionIconButtonColors(),
                        modifier =
                            Modifier
                                .testTag("voice-result-share-button")
                                .semantics { contentDescription = shareResultDescription },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(
                        onClick = onExportResult,
                        enabled = session.canExportResult,
                        colors = utilityActionIconButtonColors(),
                        modifier =
                            Modifier
                                .testTag("voice-result-download-button")
                                .semantics { contentDescription = saveResultDescription },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            },
            testTag = "voice-result-output-track",
        )
    }
}

@Composable
private fun VoiceResultTrackRow(
    track: VoicePreviewTrackOption,
    title: String,
    totalSamples: Int,
    sampleRateHz: Int,
    isPlaying: Boolean,
    positionSamples: Int,
    enabled: Boolean,
    onTogglePreviewTrack: (VoicePreviewTrackOption) -> Unit,
    onPreviewTrackSeek: (VoicePreviewTrackOption, Int) -> Unit,
    trailingAction: (@Composable () -> Unit)?,
    testTag: String,
) {
    val visualTokens = appThemeVisualTokens()
    val clampedPosition = positionSamples.coerceIn(0, totalSamples)
    val playDescription = stringResource(R.string.audio_action_play)
    val pauseDescription = stringResource(R.string.audio_action_pause)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onTogglePreviewTrack(track) },
                enabled = enabled,
                colors = utilityActionIconButtonColors(),
                modifier =
                    Modifier
                        .testTag("$testTag-play")
                        .semantics {
                            contentDescription =
                                if (isPlaying) {
                                    pauseDescription
                                } else {
                                    playDescription
                                }
                        },
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatDurationMillis(samplesToMillis(totalSamples, sampleRateHz)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            trailingAction?.invoke()
        }
        Slider(
            value = clampedPosition.toFloat(),
            onValueChange = { onPreviewTrackSeek(track, it.roundToInt()) },
            enabled = enabled && totalSamples > 0,
            valueRange = 0f..totalSamples.coerceAtLeast(1).toFloat(),
            colors =
                SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = visualTokens.timelineInactiveTrackColor,
                ),
            modifier = Modifier.testTag("$testTag-slider"),
        )
    }
}
