package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.component.ActionButton
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption

@Composable
internal fun AudioInputActionsCard(
    transportMode: TransportModeOption,
    selectedFlashVoicingStyle: FlashVoicingStyleOption,
    onFlashVoicingStyleSelected: (FlashVoicingStyleOption) -> Unit,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onEncode: () -> Unit,
    flashVoicingExpanded: Boolean,
    onToggleFlashVoicingExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AudioInputCardHeader(
                title = stringResource(R.string.audio_input_label),
                transportMode = transportMode,
                flashVoicingExpanded = flashVoicingExpanded,
                onToggleFlashVoicingExpanded = onToggleFlashVoicingExpanded
            )
            if (transportMode == TransportModeOption.Flash && flashVoicingExpanded) {
                FlashVoicingSelectorSection(
                    selectedFlashVoicingStyle = selectedFlashVoicingStyle,
                    onFlashVoicingStyleSelected = onFlashVoicingStyleSelected
                )
            }
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                label = { Text(stringResource(R.string.audio_input_label)) },
                placeholder = { Text(stringResource(transportMode.exampleTextResId)) },
                supportingText = { Text(stringResource(transportMode.charsetHintResId)) },
                minLines = 1,
                maxLines = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
            )

            ActionButton(stringResource(R.string.audio_action_encode), onEncode)
        }
    }
}

@Composable
private fun AudioInputCardHeader(
    title: String,
    transportMode: TransportModeOption,
    flashVoicingExpanded: Boolean,
    onToggleFlashVoicingExpanded: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.weight(1f))
        if (transportMode == TransportModeOption.Flash) {
            IconButton(onClick = onToggleFlashVoicingExpanded) {
                Icon(
                    imageVector = if (flashVoicingExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = stringResource(
                        if (flashVoicingExpanded) {
                            R.string.audio_action_collapse_flash_style
                        } else {
                            R.string.audio_action_expand_flash_style
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun FlashVoicingSelectorSection(
    selectedFlashVoicingStyle: FlashVoicingStyleOption,
    onFlashVoicingStyleSelected: (FlashVoicingStyleOption) -> Unit
) {
    val selectedStyleLabel = stringResource(selectedFlashVoicingStyle.labelResId)

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.audio_flash_style_title, selectedStyleLabel),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.audio_flash_style_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FlashVoicingStyleOption.entries.forEach { option ->
            val selected = option == selectedFlashVoicingStyle
            Surface(
                tonalElevation = if (selected) 6.dp else 1.dp,
                shadowElevation = if (selected) 2.dp else 0.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFlashVoicingStyleSelected(option) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(option.labelResId),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) {
                        Text(
                            text = stringResource(R.string.config_palette_selected),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AudioResultCard(
    resultText: String,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onDecode: () -> Unit,
    onClearInput: () -> Unit,
    onClearResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val resultScrollState = rememberScrollState()

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.audio_result_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onClearResult,
                    enabled = resultText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.audio_action_clear_result)
                    )
                }
                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = stringResource(
                            if (expanded) {
                                R.string.audio_action_collapse_result
                            } else {
                                R.string.audio_action_expand_result
                            }
                        )
                    )
                }
            }
            if (expanded) {
                if (resultText.isBlank()) {
                    Text(
                        text = stringResource(R.string.audio_result_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(resultScrollState)
                    ) {
                        Text(
                            text = resultText,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    text = stringResource(R.string.audio_action_decode),
                    onClick = onDecode,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    text = stringResource(R.string.audio_action_clear),
                    onClick = onClearInput,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AudioSectionHeader(
    title: String,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    expandDescription: String,
    collapseDescription: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onToggleExpanded) {
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) {
                    collapseDescription
                } else {
                    expandDescription
                }
            )
        }
    }
}
