package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.theme.AppThemeAccentTokens

@Composable
internal fun ConfigLanguageSection(
    selectedLanguage: AppLanguageOption,
    onLanguageSelected: (AppLanguageOption) -> Unit,
    isExpanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    accentTokens: AppThemeAccentTokens,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ExpandableCardHeader(
                accentTokens = accentTokens,
                title = stringResource(R.string.config_language_title),
                subtitle = stringResource(R.string.config_language_subtitle),
                expanded = isExpanded,
                onToggleExpanded = { onExpandedChanged(!isExpanded) },
                contentDescription =
                    stringResource(
                        if (isExpanded) {
                            R.string.config_language_collapse
                        } else {
                            R.string.config_language_expand
                        },
                    ),
            )

            if (isExpanded) {
                AppLanguageOption.entries.forEach { option ->
                    SelectionRow(
                        accentTokens = accentTokens,
                        label = stringResource(option.labelResId),
                        selected = option == selectedLanguage,
                        onClick = { onLanguageSelected(option) },
                    )
                }
            }
        }
    }
}
