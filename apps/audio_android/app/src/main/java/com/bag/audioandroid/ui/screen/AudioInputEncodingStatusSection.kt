package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.AudioInputEncodingAnalysis
import com.bag.audioandroid.ui.model.TransportModeOption

@Composable
internal fun InputEncodingStatusSection(
    transportMode: TransportModeOption,
    analysis: AudioInputEncodingAnalysis,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        InputEncodingMessage(
            transportMode = transportMode,
            analysis = analysis,
            modifier = Modifier.fillMaxWidth(),
        )
        if (transportMode == TransportModeOption.Mini && !analysis.isBlockingInvalid && analysis.morseNotation.isNotBlank()) {
            SelectionContainer {
                Text(
                    text = analysis.morseNotation,
                    style =
                        MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InputEncodingMessage(
    transportMode: TransportModeOption,
    analysis: AudioInputEncodingAnalysis,
    modifier: Modifier = Modifier,
) {
    val message =
        when {
            transportMode == TransportModeOption.Mini && analysis.isBlockingInvalid ->
                stringResource(
                    R.string.audio_morse_unsupported_characters,
                    analysis.unsupportedCharacters.joinToString(" "),
                )
            transportMode == TransportModeOption.Pro && analysis.isBlockingInvalid ->
                stringResource(
                    R.string.audio_pro_unsupported_characters,
                    analysis.unsupportedCharacters.joinToString(" "),
                )
            transportMode == TransportModeOption.Mini -> stringResource(R.string.audio_input_encoding_valid_mini)
            transportMode == TransportModeOption.Pro -> stringResource(R.string.audio_input_encoding_valid_pro)
            else -> null
        }
    if (message == null) {
        return
    }
    Text(
        text = message,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color =
            if (analysis.isBlockingInvalid) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
    )
}

@Composable
internal fun InputEncodingRulesDialog(
    transportMode: TransportModeOption,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.audio_input_encoding_rules_title))
        },
        text = {
            Text(
                text =
                    stringResource(
                        when (transportMode) {
                            TransportModeOption.Mini -> R.string.audio_input_encoding_rules_mini
                            TransportModeOption.Pro -> R.string.audio_input_encoding_rules_pro
                            TransportModeOption.Flash,
                            TransportModeOption.Ultra,
                            -> R.string.audio_input_encoding_rules_unrestricted
                        },
                    ),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.audio_input_encoding_rules_done))
            }
        },
    )
}
