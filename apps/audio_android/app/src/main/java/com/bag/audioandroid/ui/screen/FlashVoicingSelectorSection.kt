package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.theme.appThemeAccentTokens

@Composable
internal fun FlashVoicingSelectorSection(
    enabled: Boolean,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    selectedFlashVoicingStyle: FlashVoicingStyleOption,
    onFlashVoicingStyleSelected: (FlashVoicingStyleOption) -> Unit,
) {
    val selectedStyleLabel = stringResource(selectedFlashVoicingStyle.labelResId)
    val accentTokens = appThemeAccentTokens()

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.audio_flash_style_title, selectedStyleLabel),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accentTokens.disclosureAccentTint,
                )
                if (expanded) {
                    Text(
                        text = stringResource(R.string.audio_flash_style_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(
                onClick = onToggleExpanded,
                enabled = enabled,
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription =
                        stringResource(
                            if (expanded) {
                                R.string.audio_action_collapse_flash_style
                            } else {
                                R.string.audio_action_expand_flash_style
                            },
                        ),
                    tint = accentTokens.disclosureAccentTint,
                )
            }
        }
        if (expanded) {
            FlashVoicingStyleOption.entries.forEach { option ->
                val selected = option == selectedFlashVoicingStyle
                // Keep flash voicing selection visually aligned with Config rows: stronger
                // container, outline, and selected badge instead of relying on a plain text change.
                val selectedContainerColor =
                    if (selected) {
                        lerp(
                            accentTokens.selectionLabelAccentTint,
                            MaterialTheme.colorScheme.surface,
                            0.82f,
                        )
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                Surface(
                    color = selectedContainerColor,
                    tonalElevation = if (selected) 6.dp else 1.dp,
                    shadowElevation = if (selected) 2.dp else 0.dp,
                    shape = MaterialTheme.shapes.medium,
                    border =
                        if (selected) {
                            BorderStroke(
                                width = 2.dp,
                                color = accentTokens.selectionBorderAccentTint,
                            )
                        } else {
                            null
                        },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) { onFlashVoicingStyleSelected(option) },
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(option.labelResId),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        if (selected) {
                            Surface(
                                color =
                                    lerp(
                                        accentTokens.selectionLabelAccentTint,
                                        MaterialTheme.colorScheme.surface,
                                        0.78f,
                                    ),
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = accentTokens.selectionLabelAccentTint,
                                    )
                                    Text(
                                        text = stringResource(R.string.config_palette_selected),
                                        color = accentTokens.selectionLabelAccentTint,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
