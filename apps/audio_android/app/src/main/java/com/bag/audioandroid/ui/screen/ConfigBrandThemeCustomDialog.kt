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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.theme.appThemeVisualTokens
import com.bag.audioandroid.ui.utilityActionIconButtonColors
import com.bag.audioandroid.ui.model.CustomBrandThemeSettings
import com.bag.audioandroid.ui.theme.brandThemeColorOrNull
import com.bag.audioandroid.ui.theme.normalizeBrandThemeHex
import com.bag.audioandroid.ui.theme.normalizeBrandThemeHexOrNull
import com.bag.audioandroid.ui.theme.randomCustomBrandThemeColors

@Composable
internal fun CustomBrandThemeDialog(
    initialSettings: CustomBrandThemeSettings,
    isCreatingNew: Boolean,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (CustomBrandThemeSettings) -> Unit,
    onDelete: () -> Unit,
) {
    val visualTokens = appThemeVisualTokens()
    // Keep preset editing state in a dedicated file so the section/row composables
    // stay focused on list presentation and selection behavior.
    var displayName by rememberSaveable { mutableStateOf(initialSettings.displayName) }
    var backgroundHex by rememberSaveable { mutableStateOf(initialSettings.backgroundHex) }
    var accentHex by rememberSaveable { mutableStateOf(initialSettings.accentHex) }
    var outlineHex by rememberSaveable { mutableStateOf(initialSettings.outlineHexOrNull.orEmpty()) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    val trimmedDisplayName = displayName.trim()
    val normalizedBackground = normalizeBrandThemeHex(backgroundHex)
    val normalizedAccent = normalizeBrandThemeHex(accentHex)
    val normalizedOutline = normalizeBrandThemeHexOrNull(outlineHex)
    val canSave =
        trimmedDisplayName.isNotEmpty() &&
            normalizedBackground != null &&
            normalizedAccent != null &&
            (outlineHex.isBlank() || normalizedOutline != null)
    val previewBackground = brandThemeColorOrNull(backgroundHex)
    val previewAccent = brandThemeColorOrNull(accentHex)
    val previewOutline =
        if (outlineHex.isBlank()) {
            previewAccent
        } else {
            brandThemeColorOrNull(outlineHex)
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text =
                    stringResource(
                        if (isCreatingNew) {
                            R.string.config_custom_brand_theme_dialog_title_new
                        } else {
                            R.string.config_custom_brand_theme_dialog_title_edit
                        },
                    ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.config_custom_brand_theme_dialog_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.config_custom_brand_theme_randomize_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            val randomColors = randomCustomBrandThemeColors()
                            backgroundHex = randomColors.backgroundHex
                            accentHex = randomColors.accentHex
                            outlineHex = randomColors.outlineHex
                        },
                        colors = utilityActionIconButtonColors(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Casino,
                            contentDescription = stringResource(R.string.config_custom_brand_theme_randomize),
                        )
                    }
                }
                if (previewBackground != null && previewAccent != null && (outlineHex.isBlank() || previewOutline != null)) {
                    BrandThemePreview(
                        backgroundColor = previewBackground,
                        accentColor = previewAccent,
                        outlineColor = previewOutline ?: previewAccent,
                        contentDescription = stringResource(R.string.brand_theme_custom_accessibility),
                    )
                }
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    singleLine = true,
                    label = { Text(text = stringResource(R.string.config_custom_brand_theme_name_label)) },
                    isError = trimmedDisplayName.isEmpty(),
                    modifier = Modifier.fillMaxWidth(),
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
                    fallbackSwatchColor = visualTokens.supportSurfaceColor,
                    fallbackBorderColor = visualTokens.subtleOutlineColor,
                )
                HexColorField(
                    value = accentHex,
                    onValueChange = { accentHex = it },
                    label = stringResource(R.string.config_custom_brand_theme_accent_label),
                    isError = normalizedAccent == null,
                    previewColor = previewAccent,
                    fallbackSwatchColor = visualTokens.supportSurfaceColor,
                    fallbackBorderColor = visualTokens.subtleOutlineColor,
                )
                HexColorField(
                    value = outlineHex,
                    onValueChange = { outlineHex = it },
                    label = stringResource(R.string.config_custom_brand_theme_outline_label),
                    isError = outlineHex.isNotBlank() && normalizedOutline == null,
                    previewColor = previewOutline,
                    fallbackSwatchColor = visualTokens.supportSurfaceColor,
                    fallbackBorderColor = visualTokens.subtleOutlineColor,
                    supportingText = stringResource(R.string.config_custom_brand_theme_outline_optional),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        CustomBrandThemeSettings(
                            presetId = initialSettings.presetId,
                            displayName = trimmedDisplayName,
                            backgroundHex = normalizedBackground ?: return@TextButton,
                            accentHex = normalizedAccent ?: return@TextButton,
                            outlineHexOrNull = normalizedOutline,
                        ),
                    )
                },
            ) {
                Text(text = stringResource(R.string.config_custom_brand_theme_save))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canDelete) {
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text(text = stringResource(R.string.config_custom_brand_theme_delete))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            }
        },
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(text = stringResource(R.string.config_custom_brand_theme_delete_title)) },
            text = { Text(text = stringResource(R.string.config_custom_brand_theme_delete_message, trimmedDisplayName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                ) {
                    Text(text = stringResource(R.string.config_custom_brand_theme_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            label = { Text(text = label) },
            isError = isError,
            modifier = Modifier.weight(1f),
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
