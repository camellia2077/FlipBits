package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.audioInputTextFieldColors
import com.bag.audioandroid.ui.model.CustomBrandThemeSettings
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.theme.AudioEncodeGlyphColors
import com.bag.audioandroid.ui.theme.DefaultAppThemeAccentTokens
import com.bag.audioandroid.ui.theme.DefaultAppThemeVisualTokens
import com.bag.audioandroid.ui.theme.LocalAppThemeAccentTokens
import com.bag.audioandroid.ui.theme.LocalAppThemeVisualTokens
import com.bag.audioandroid.ui.theme.LocalAudioEncodeGlyphColors
import com.bag.audioandroid.ui.theme.appThemeAccentTokens
import com.bag.audioandroid.ui.theme.appThemeVisualTokens
import com.bag.audioandroid.ui.theme.brandThemeColorOrNull
import com.bag.audioandroid.ui.theme.normalizeBrandThemeHex
import com.bag.audioandroid.ui.theme.normalizeBrandThemeHexOrNull
import com.bag.audioandroid.ui.theme.randomCustomBrandThemeColors
import com.bag.audioandroid.ui.utilityActionIconButtonColors

@Composable
internal fun CustomBrandThemeDialog(
    initialSettings: CustomBrandThemeSettings,
    isCreatingNew: Boolean,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (CustomBrandThemeSettings) -> Unit,
    onDelete: () -> Unit,
) {
    var displayName by rememberSaveable { mutableStateOf(initialSettings.displayName) }
    var backgroundHex by rememberSaveable { mutableStateOf(initialSettings.backgroundHex) }
    var accentHex by rememberSaveable { mutableStateOf(initialSettings.accentHex) }
    var outlineHexValue by rememberSaveable { mutableStateOf(initialSettings.outlineHexOrNull ?: "") }

    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    val trimmedDisplayName = displayName.trim()
    
    val normalizedBackground = remember(backgroundHex) { normalizeBrandThemeHex(backgroundHex) }
    val normalizedAccent = remember(accentHex) { normalizeBrandThemeHex(accentHex) }
    val normalizedOutline = remember(outlineHexValue) { normalizeBrandThemeHexOrNull(outlineHexValue) }

    val canSave =
        trimmedDisplayName.isNotEmpty() &&
            normalizedBackground != null &&
            normalizedAccent != null &&
            (outlineHexValue.isBlank() || normalizedOutline != null)

    val previewBackground = remember(normalizedBackground) { brandThemeColorOrNull(normalizedBackground ?: "") }
    val previewAccent = remember(normalizedAccent) { brandThemeColorOrNull(normalizedAccent ?: "") }
    val previewOutline = remember(normalizedOutline, previewAccent, outlineHexValue) {
        if (outlineHexValue.isBlank()) {
            previewAccent
        } else {
            brandThemeColorOrNull(normalizedOutline ?: "")
        }
    }

    val dialogContentColor = remember(previewBackground) {
        if (previewBackground != null) {
            if (previewBackground.luminance() < 0.5f) Color.White else Color(0xFF1A1C1E) // BrandInkLight
        } else {
            Color.Unspecified
        }
    }

    val previewVisualTokens = remember(previewBackground, previewAccent, dialogContentColor) {
        DefaultAppThemeVisualTokens.copy(
            themeStyle = ThemeStyleOption.BrandDualTone,
            inputContainerColor = if (previewBackground != null && previewAccent != null) {
                lerp(previewBackground, previewAccent, 0.08f)
            } else {
                previewBackground ?: Color.Transparent
            },
            inputContentColor = if (dialogContentColor != Color.Unspecified) dialogContentColor else Color.White,
            inputFocusedBorderColor = previewAccent ?: Color.Unspecified,
            inputUnfocusedBorderColor = (previewAccent ?: Color.Unspecified).copy(alpha = 0.42f),
            modalContainerColor = previewBackground ?: Color.Transparent,
            modalContentColor = if (dialogContentColor != Color.Unspecified) dialogContentColor else Color.White,
        )
    }

    val previewAccentTokens = remember(previewAccent) {
        DefaultAppThemeAccentTokens.copy(
            disclosureAccentTint = previewAccent ?: Color.Unspecified,
            actionAccentTint = previewAccent ?: Color.Unspecified,
            selectionLabelAccentTint = previewAccent ?: Color.Unspecified,
            selectionBorderAccentTint = previewAccent ?: Color.Unspecified,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = previewVisualTokens.modalContainerColor,
        titleContentColor = previewVisualTokens.modalContentColor,
        textContentColor = previewVisualTokens.modalContentColor,
        title = {
            Text(
                text = stringResource(
                    if (isCreatingNew) R.string.config_custom_brand_theme_dialog_title_new
                    else R.string.config_custom_brand_theme_dialog_title_edit
                )
            )
        },
        text = {
            CompositionLocalProvider(
                LocalAppThemeVisualTokens provides previewVisualTokens,
                LocalAppThemeAccentTokens provides previewAccentTokens,
                androidx.compose.material3.LocalContentColor provides previewVisualTokens.modalContentColor,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val random = randomCustomBrandThemeColors()
                                    backgroundHex = random.backgroundHex
                                    accentHex = random.accentHex
                                    outlineHexValue = random.outlineHex ?: ""
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = previewAccent?.copy(alpha = 0.15f) ?: Color.Unspecified,
                                    contentColor = previewAccent ?: Color.Unspecified
                                ),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Casino,
                                    contentDescription = stringResource(R.string.config_custom_brand_theme_randomize),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            val previewGlyphColors = AudioEncodeGlyphColors(
                                primarySplit = previewAccent ?: Color.Unspecified,
                                secondarySplit = previewBackground ?: Color.Unspecified,
                                outline = previewOutline ?: previewAccent ?: Color.Unspecified,
                            )
                            CompositionLocalProvider(LocalAudioEncodeGlyphColors provides previewGlyphColors) {
                                AudioEncodeGlyph(
                                    encodeProgress = 0.65f,
                                    isEncodingBusy = false,
                                    baseSize = 72.dp,
                                )
                            }
                        }
                    }
                    var isNameFocused by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        singleLine = true,
                        label = {
                            Text(
                                text = stringResource(R.string.config_custom_brand_theme_name_label),
                                fontWeight = if (isNameFocused) FontWeight.SemiBold else FontWeight.Medium
                            )
                        },
                        isError = trimmedDisplayName.isEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isNameFocused = it.isFocused },
                        shape = MaterialTheme.shapes.medium,
                        textStyle = LocalTextStyle.current.copy(
                            fontWeight = if (isNameFocused) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        colors = audioInputTextFieldColors(ThemeStyleOption.BrandDualTone),
                        supportingText = {
                            if (trimmedDisplayName.isEmpty()) {
                                Text(text = stringResource(R.string.config_custom_brand_theme_invalid_name))
                            }
                        },
                    )
                    HexColorField(
                        value = backgroundHex,
                        onValueChange = { backgroundHex = it },
                        label = stringResource(R.string.config_custom_brand_theme_background_label),
                        isError = normalizedBackground == null,
                        previewColor = previewBackground,
                        fallbackSwatchColor = previewVisualTokens.supportSurfaceColor,
                        fallbackBorderColor = previewVisualTokens.subtleOutlineColor,
                    )
                    HexColorField(
                        value = accentHex,
                        onValueChange = { accentHex = it },
                        label = stringResource(R.string.config_custom_brand_theme_accent_label),
                        isError = normalizedAccent == null,
                        previewColor = previewAccent,
                        fallbackSwatchColor = previewVisualTokens.supportSurfaceColor,
                        fallbackBorderColor = previewVisualTokens.subtleOutlineColor,
                    )
                    HexColorField(
                        value = outlineHexValue,
                        onValueChange = { outlineHexValue = it },
                        label = stringResource(R.string.config_custom_brand_theme_outline_label),
                        isError = outlineHexValue.isNotBlank() && normalizedOutline == null,
                        previewColor = previewOutline,
                        fallbackSwatchColor = previewVisualTokens.supportSurfaceColor,
                        fallbackBorderColor = previewVisualTokens.subtleOutlineColor,
                        supportingText = stringResource(R.string.config_custom_brand_theme_outline_optional),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        CustomBrandThemeSettings(
                            displayName = trimmedDisplayName,
                            backgroundHex = normalizedBackground!!,
                            accentHex = normalizedAccent!!,
                            outlineHexOrNull = normalizedOutline,
                        )
                    )
                },
                enabled = canSave,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = previewAccent ?: MaterialTheme.colorScheme.primary
                )
            ) {
                Text(text = stringResource(R.string.config_custom_brand_theme_save))
            }
        },
        dismissButton = {
            Row {
                if (canDelete) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(text = stringResource(R.string.config_custom_brand_theme_delete))
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = previewAccent ?: MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(text = stringResource(R.string.config_custom_brand_theme_delete_title)) },
            text = { 
                val message = stringResource(R.string.config_custom_brand_theme_delete_message, displayName)
                Text(text = message) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(text = stringResource(R.string.config_custom_brand_theme_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = previewAccent ?: MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun HexColorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    previewColor: Color?,
    fallbackSwatchColor: Color,
    fallbackBorderColor: Color,
    supportingText: String? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            label = { 
                Text(
                    text = label,
                    fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Medium
                ) 
            },
            isError = isError,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { isFocused = it.isFocused },
            shape = MaterialTheme.shapes.medium,
            textStyle = LocalTextStyle.current.copy(
                fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Medium
            ),
            colors = audioInputTextFieldColors(ThemeStyleOption.BrandDualTone),
            supportingText = {
                when {
                    isError -> Text(text = stringResource(R.string.config_custom_brand_theme_invalid_hex))
                    supportingText != null -> Text(text = supportingText)
                }
            },
        )
        ColorPreviewSwatch(
            color = previewColor,
            fallbackColor = fallbackSwatchColor,
            borderColor = fallbackBorderColor,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun ColorPreviewSwatch(
    color: Color?,
    fallbackColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(32.dp),
        color = color ?: fallbackColor,
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    ) {
        if (color == null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(fallbackColor),
            )
        }
    }
}
