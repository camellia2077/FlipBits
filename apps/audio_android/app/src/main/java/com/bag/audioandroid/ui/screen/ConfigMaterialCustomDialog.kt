package com.bag.audioandroid.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.audioInputTextFieldColors
import com.bag.audioandroid.ui.model.CustomFactionThemeSettings
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.theme.LocalAudioEncodeGlyphColors
import com.bag.audioandroid.ui.theme.audioEncodeGlyphColorsForMaterial
import com.bag.audioandroid.ui.theme.customMaterialPalette
import com.bag.audioandroid.ui.theme.factionThemeColorOrNull
import com.bag.audioandroid.ui.theme.normalizeCustomMaterialThemeSettings
import com.bag.audioandroid.ui.theme.normalizeFactionThemeHex
import com.bag.audioandroid.ui.theme.randomCustomFactionThemeColors

@Composable
internal fun ConfigMaterialCustomDialog(
    initialSettings: CustomFactionThemeSettings,
    onDismiss: () -> Unit,
    onSave: (CustomFactionThemeSettings) -> Unit,
) {
    var primaryHex by rememberSaveable { mutableStateOf(initialSettings.primaryHex) }
    val normalizedPrimary = remember(primaryHex) { normalizeFactionThemeHex(primaryHex) }
    val previewPrimary = remember(normalizedPrimary) { factionThemeColorOrNull(normalizedPrimary ?: "") }
    val canSave = normalizedPrimary != null
    val isPreviewDark = (previewPrimary?.luminance() ?: 1f) < 0.46f
    val previewPalette =
        remember(primaryHex, normalizedPrimary, initialSettings) {
            customMaterialPalette(
                normalizeCustomMaterialThemeSettings(
                    initialSettings.copy(primaryHex = normalizedPrimary ?: initialSettings.primaryHex),
                ),
            )
        }
    val previewGlyphColors =
        remember(previewPalette, isPreviewDark) {
            audioEncodeGlyphColorsForMaterial(
                colorScheme =
                    if (isPreviewDark) {
                        previewPalette.darkScheme
                    } else {
                        previewPalette.lightScheme
                    },
                isDarkTheme = isPreviewDark,
            )
        }
    val previewContainerColor by animateColorAsState(
        targetValue = previewPrimary ?: MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "materialCustomDialogContainer",
    )
    val previewContentColor by animateColorAsState(
        targetValue =
            if (previewPrimary == null) {
                MaterialTheme.colorScheme.onSurface
            } else if (isPreviewDark) {
                Color.White
            } else {
                Color(0xFF1A1C1E)
            },
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "materialCustomDialogContent",
    )
    val previewBorderColor by animateColorAsState(
        targetValue = previewContentColor.copy(alpha = 0.42f),
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "materialCustomDialogBorder",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = previewContainerColor,
        titleContentColor = previewContentColor,
        textContentColor = previewContentColor,
        title = {
            Text(text = stringResource(R.string.config_custom_faction_theme_dialog_title_edit))
        },
        text = {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompositionLocalProvider(LocalAudioEncodeGlyphColors provides previewGlyphColors) {
                        AudioEncodeGlyph(
                            encodeProgress = 0.65f,
                            isEncodingBusy = false,
                            baseSize = 72.dp,
                        )
                    }
                    IconButton(
                        onClick = { primaryHex = randomCustomFactionThemeColors().primaryHex },
                        colors =
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = previewContentColor.copy(alpha = 0.12f),
                                contentColor = previewContentColor,
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Casino,
                            contentDescription = stringResource(R.string.config_custom_faction_theme_randomize),
                        )
                    }
                }
                MaterialCustomHexField(
                    value = primaryHex,
                    onValueChange = { primaryHex = it },
                    isError = normalizedPrimary == null,
                    previewColor = previewPrimary,
                    fallbackSwatchColor = MaterialTheme.colorScheme.surfaceVariant,
                    fallbackBorderColor = previewBorderColor,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        normalizeCustomMaterialThemeSettings(
                            initialSettings.copy(primaryHex = normalizedPrimary ?: initialSettings.primaryHex),
                        ),
                    )
                },
                enabled = canSave,
                colors =
                    androidx.compose.material3.ButtonDefaults
                        .textButtonColors(contentColor = previewContentColor),
            ) {
                Text(text = stringResource(R.string.config_custom_faction_theme_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors =
                    androidx.compose.material3.ButtonDefaults
                        .textButtonColors(contentColor = previewContentColor),
            ) {
                Text(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun MaterialCustomHexField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    previewColor: Color?,
    fallbackSwatchColor: Color,
    fallbackBorderColor: Color,
) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(sanitizeMaterialCustomHexInput(it)) },
            singleLine = true,
            label = {
                Text(
                    text = stringResource(R.string.config_custom_faction_theme_background_label),
                    fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Medium,
                )
            },
            isError = isError,
            modifier =
                Modifier
                    .weight(1f)
                    .onFocusChanged { isFocused = it.isFocused },
            shape = MaterialTheme.shapes.medium,
            colors = audioInputTextFieldColors(ThemeStyleOption.FactionTheme),
            supportingText = {
                if (isError) {
                    Text(text = stringResource(R.string.config_custom_faction_theme_invalid_hex))
                }
            },
        )
        MaterialCustomPreviewSwatch(
            previewColor = previewColor,
            fallbackColor = fallbackSwatchColor,
            borderColor = fallbackBorderColor,
        )
    }
}

private fun sanitizeMaterialCustomHexInput(input: String): String {
    val trimmed = input.trim()
    val hasHashPrefix = trimmed.startsWith("#")
    val hexChars =
        trimmed
            .replace("#", "")
            .filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
            .take(6)
            .uppercase()
    return if (hasHashPrefix) {
        "#$hexChars"
    } else {
        hexChars
    }
}

@Composable
private fun MaterialCustomPreviewSwatch(
    previewColor: Color?,
    fallbackColor: Color,
    borderColor: Color,
) {
    Box(
        modifier =
            Modifier
                .size(42.dp)
                .background(borderColor, MaterialTheme.shapes.small)
                .padding(1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .background(previewColor ?: fallbackColor, MaterialTheme.shapes.small),
        )
    }
}
