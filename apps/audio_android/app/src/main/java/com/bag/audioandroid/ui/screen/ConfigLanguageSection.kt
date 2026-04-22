package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.theme.AppThemeAccentTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConfigLanguageSection(
    selectedLanguage: AppLanguageOption,
    onLanguageSelected: (AppLanguageOption) -> Unit,
    isExpanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    accentTokens: AppThemeAccentTokens,
) {
    if (isExpanded) {
        ModalBottomSheet(
            onDismissRequest = { onExpandedChanged(false) },
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    androidx.compose.material3.Text(
                        text = stringResource(R.string.config_language_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    androidx.compose.material3.Text(
                        text = stringResource(R.string.config_language_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                AppLanguageOption.entries.forEach { option ->
                    SelectionRow(
                        accentTokens = accentTokens,
                        label = stringResource(option.labelResId),
                        selected = option == selectedLanguage,
                        onClick = {
                            onLanguageSelected(option)
                            onExpandedChanged(false)
                        },
                    )
                }
            }
        }
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChanged(true) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                androidx.compose.material3.Text(
                    text = stringResource(R.string.config_language_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                androidx.compose.material3.Text(
                    text = stringResource(selectedLanguage.labelResId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = stringResource(R.string.config_language_expand),
                tint = accentTokens.disclosureAccentTint,
            )
        }
    }
}
