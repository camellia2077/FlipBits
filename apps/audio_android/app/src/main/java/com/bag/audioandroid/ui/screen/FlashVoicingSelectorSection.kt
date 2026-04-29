package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    isFlashVoicingEnabled: Boolean,
    selectedFlashVoicingStyle: FlashVoicingStyleOption,
    onFlashVoicingStyleSelected: (FlashVoicingStyleOption) -> Unit,
) {
    val effectiveSelectedStyle =
        if (isFlashVoicingEnabled) {
            selectedFlashVoicingStyle
        } else {
            FlashVoicingStyleOption.CodedBurst
        }
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
                    text = stringResource(R.string.audio_flash_style_title, stringResource(effectiveSelectedStyle.labelResId)),
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
                SelectionRow(
                    accentTokens = accentTokens,
                    label = stringResource(option.labelResId),
                    selected = option == effectiveSelectedStyle,
                    onClick = { onFlashVoicingStyleSelected(option) },
                    enabled = enabled,
                )
            }
        }
    }
}
