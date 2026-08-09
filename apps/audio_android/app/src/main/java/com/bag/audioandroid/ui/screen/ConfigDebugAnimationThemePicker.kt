package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.FactionThemeOption

internal fun sanitizeAnimationDurationInput(input: String): String {
    val builder = StringBuilder()
    var hasDecimalSeparator = false
    var fractionalDigits = 0

    input.forEach { char ->
        when {
            char.isDigit() && !hasDecimalSeparator -> {
                if (builder.length < 3) {
                    builder.append(char)
                }
            }
            char.isDigit() && hasDecimalSeparator && fractionalDigits < 2 -> {
                builder.append(char)
                fractionalDigits += 1
            }
            char == '.' && !hasDecimalSeparator && builder.isNotEmpty() -> {
                builder.append(char)
                hasDecimalSeparator = true
            }
        }
    }

    return builder.toString()
}

internal enum class AnimationThemePickerTarget {
    First,
    Second,
}

internal enum class AnimationThemeMode {
    Single,
    Dual,
}

internal fun buildAnimationThemeOptions(
    factionThemes: List<FactionThemeOption>,
    selectedFactionTheme: FactionThemeOption,
): List<FactionThemeOption> {
    val builtInThemes = if (factionThemes.isNotEmpty()) factionThemes else listOf(selectedFactionTheme)
    return if (builtInThemes.any { it.id == selectedFactionTheme.id }) {
        builtInThemes
    } else {
        listOf(selectedFactionTheme) + builtInThemes
    }
}

@Composable
internal fun AnimationThemeSelector(
    label: String,
    theme: FactionThemeOption,
    selectedBorderColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, selectedBorderColor.copy(alpha = 0.7f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = factionThemeDisplayName(theme),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                FactionThemePreviewSwatch(theme = theme, modifier = Modifier.width(72.dp))
            }
        }
    }
}

@Composable
internal fun FactionThemePickerDialog(
    title: String,
    options: List<FactionThemeOption>,
    selectedThemeId: String,
    onDismiss: () -> Unit,
    onThemeSelected: (FactionThemeOption) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    options.forEach { option ->
                        val isSelected = option.id == selectedThemeId
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { onThemeSelected(option) },
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 0.dp,
                            border =
                                BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color =
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                        },
                                ),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = factionThemeDisplayName(option),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                )
                                FactionThemePreviewSwatch(
                                    theme = option,
                                    modifier = Modifier.width(84.dp),
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.common_cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun FactionThemePreviewSwatch(
    theme: FactionThemeOption,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .height(28.dp)
                .border(
                    BorderStroke(2.dp, theme.outlineColor),
                    shape = RoundedCornerShape(10.dp),
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(28.dp)
                    .background(
                        color = theme.primaryColor,
                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                    ),
        )
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(28.dp)
                    .background(
                        color = theme.secondaryColor,
                        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                    ),
        )
    }
}

@Composable
private fun factionThemeDisplayName(theme: FactionThemeOption): String = theme.titleOverride ?: stringResource(theme.titleResId)
