package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.component.ActionButton
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.MorseSpeedOption
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.analyzeAudioInputEncoding
import com.bag.audioandroid.ui.theme.appThemeAccentTokens

private data class AudioInputActionSelectorState(
    val showFlashVoicingSelector: Boolean,
    val showMorseSpeedSelector: Boolean,
)

private data class AudioInputActionRenderState(
    val isEncodingBusy: Boolean,
    val canEncodeInput: Boolean,
    val enabled: Boolean,
    val showInputEncodingStatus: Boolean,
    val selectorState: AudioInputActionSelectorState,
)

@Composable
internal fun AudioInputActionsCard(
    selectedThemeStyle: ThemeStyleOption,
    transportMode: TransportModeOption,
    isCodecBusy: Boolean,
    encodeProgressDisplay: EncodeProgressDisplayModel?,
    isEncodeCancelling: Boolean,
    isFlashVoicingEnabled: Boolean,
    selectedFlashVoicingStyle: FlashVoicingStyleOption,
    onFlashVoicingStyleSelected: (FlashVoicingStyleOption) -> Unit,
    selectedMorseSpeed: MorseSpeedOption,
    onMorseSpeedSelected: (MorseSpeedOption) -> Unit,
    inputCardExpanded: Boolean,
    onToggleInputCardExpanded: () -> Unit,
    inputText: String,
    inputPlaceholderText: String,
    onInputTextChange: (String) -> Unit,
    onOpenInputEditor: () -> Unit,
    sampleInputLength: SampleInputLengthOption,
    onSampleInputLengthSelected: (SampleInputLengthOption) -> Unit,
    onRandomizeSampleInput: () -> Unit,
    onClearInput: () -> Unit,
    onEncode: () -> Unit,
    onCancelEncode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentTokens = appThemeAccentTokens()
    val inputEncodingAnalysis =
        remember(transportMode, inputText) {
            analyzeAudioInputEncoding(transportMode, inputText)
        }
    val renderState =
        remember(
            transportMode,
            isCodecBusy,
            encodeProgressDisplay,
            inputText,
            inputEncodingAnalysis.isBlockingInvalid,
        ) {
            audioInputActionRenderState(
                transportMode = transportMode,
                isCodecBusy = isCodecBusy,
                encodeProgressDisplay = encodeProgressDisplay,
                inputText = inputText,
                isBlockingInvalid = inputEncodingAnalysis.isBlockingInvalid,
            )
        }
    var showInputRules by remember { mutableStateOf(false) }
    if (showInputRules) {
        InputEncodingRulesDialog(
            transportMode = transportMode,
            onDismiss = { showInputRules = false },
        )
    }

    AudioSectionContainer(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AudioInputCardHeader(
                title = stringResource(R.string.audio_input_label),
                enabled = renderState.enabled,
                expanded = inputCardExpanded,
                onToggleExpanded = onToggleInputCardExpanded,
            )
            if (inputCardExpanded) {
                if (renderState.selectorState.showFlashVoicingSelector) {
                    FlashVoicingSelectorSection(
                        enabled = renderState.enabled,
                        isFlashVoicingEnabled = isFlashVoicingEnabled,
                        selectedFlashVoicingStyle = selectedFlashVoicingStyle,
                        onFlashVoicingStyleSelected = onFlashVoicingStyleSelected,
                    )
                }
                if (renderState.selectorState.showMorseSpeedSelector) {
                    MorseSpeedSelectorSection(
                        enabled = renderState.enabled,
                        selectedMorseSpeed = selectedMorseSpeed,
                        onMorseSpeedSelected = onMorseSpeedSelected,
                    )
                }
                AudioInputToolbar(
                    enabled = renderState.enabled,
                    onOpenInputEditor = onOpenInputEditor,
                    sampleInputLength = sampleInputLength,
                    onSampleInputLengthSelected = onSampleInputLengthSelected,
                    onRandomizeSampleInput = onRandomizeSampleInput,
                    onClearInput = onClearInput,
                )

                AudioInputTextFieldSection(
                    selectedThemeStyle = selectedThemeStyle,
                    transportMode = transportMode,
                    enabled = renderState.enabled,
                    inputText = inputText,
                    inputPlaceholderText = inputPlaceholderText,
                    onInputTextChange = onInputTextChange,
                    onOpenInputRules = { showInputRules = true },
                )

                if (renderState.showInputEncodingStatus) {
                    InputEncodingStatusSection(
                        transportMode = transportMode,
                        analysis = inputEncodingAnalysis,
                        inputText = inputText,
                    )
                }

                AudioEncodeStatusSection(
                    encodeProgressDisplay = encodeProgressDisplay,
                    isEncodingBusy = renderState.isEncodingBusy,
                )

                if (renderState.isEncodingBusy) {
                    BusyAudioEncodeActionRow(
                        encodeProgressDisplay = requireNotNull(encodeProgressDisplay),
                        isEncodeCancelling = isEncodeCancelling,
                        onCancelEncode = onCancelEncode,
                        accentBorderColor = accentTokens.selectionBorderAccentTint,
                    )
                } else {
                    IdleAudioEncodeActionButton(
                        enabled = renderState.enabled && renderState.canEncodeInput,
                        onEncode = onEncode,
                        textColor = accentTokens.disclosureAccentTint,
                        borderColor = accentTokens.selectionBorderAccentTint,
                    )
                }
            }
        }
    }
}

private fun audioInputActionRenderState(
    transportMode: TransportModeOption,
    isCodecBusy: Boolean,
    encodeProgressDisplay: EncodeProgressDisplayModel?,
    inputText: String,
    isBlockingInvalid: Boolean,
): AudioInputActionRenderState {
    val isEncodingBusy = isCodecBusy && encodeProgressDisplay != null
    return AudioInputActionRenderState(
        isEncodingBusy = isEncodingBusy,
        canEncodeInput = !isBlockingInvalid,
        enabled = !isCodecBusy,
        showInputEncodingStatus = inputText.isNotEmpty(),
        selectorState =
            AudioInputActionSelectorState(
                showFlashVoicingSelector = transportMode == TransportModeOption.Flash,
                showMorseSpeedSelector = transportMode == TransportModeOption.Mini,
            ),
    )
}

@Composable
private fun BusyAudioEncodeActionRow(
    encodeProgressDisplay: EncodeProgressDisplayModel,
    isEncodeCancelling: Boolean,
    onCancelEncode: () -> Unit,
    accentBorderColor: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActionButton(
            text = stringResource(R.string.audio_action_encode_busy_progress, encodeProgressDisplay.percent),
            onClick = {},
            enabled = false,
            borderColor = accentBorderColor,
            borderWidth = 2.dp,
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            text =
                stringResource(
                    if (isEncodeCancelling) {
                        R.string.audio_action_cancel_encode_busy
                    } else {
                        R.string.audio_action_cancel_encode
                    },
                ),
            onClick = onCancelEncode,
            enabled = !isEncodeCancelling,
            borderColor = accentBorderColor,
            borderWidth = 2.dp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun IdleAudioEncodeActionButton(
    enabled: Boolean,
    onEncode: () -> Unit,
    textColor: androidx.compose.ui.graphics.Color,
    borderColor: androidx.compose.ui.graphics.Color,
) {
    ActionButton(
        text = stringResource(R.string.audio_action_encode),
        onClick = onEncode,
        enabled = enabled,
        textColor = textColor,
        borderColor = borderColor,
        borderWidth = 2.dp,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("audio-encode-button"),
    )
}

@Composable
internal fun AudioInputMetricsSummaryRow(
    transportMode: TransportModeOption,
    characterCount: Int,
    byteCount: Int,
    onOpenInputRules: () -> Unit,
) {
    val summaryText =
        when (transportMode) {
            TransportModeOption.Mini,
            TransportModeOption.Pro,
            ->
                stringResource(
                    R.string.audio_input_ascii_metrics,
                    characterCount,
                    byteCount,
                )

            TransportModeOption.Flash,
            TransportModeOption.Ultra,
            ->
                stringResource(
                    R.string.audio_input_metrics,
                    characterCount,
                    byteCount,
                )
        }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = summaryText,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onOpenInputRules) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = stringResource(R.string.audio_input_encoding_rules),
            )
        }
    }
}

@Composable
private fun AudioInputCardHeader(
    title: String,
    enabled: Boolean,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    val accentTokens = appThemeAccentTokens()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = accentTokens.disclosureAccentTint,
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = onToggleExpanded,
            enabled = enabled,
        ) {
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription =
                    stringResource(
                        if (expanded) {
                            R.string.audio_action_collapse_input
                        } else {
                            R.string.audio_action_expand_input
                        },
                    ),
                tint = accentTokens.disclosureAccentTint,
            )
        }
    }
}
