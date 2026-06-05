package com.bag.audioandroid.ui.screen

import android.content.ClipData
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.appSegmentedButtonColors
import com.bag.audioandroid.ui.audioInputTextFieldColors
import com.bag.audioandroid.ui.model.CustomFactionThemeImportParseResult
import com.bag.audioandroid.ui.model.CustomFactionThemeSettings
import com.bag.audioandroid.ui.model.CustomThemeImportError
import com.bag.audioandroid.ui.model.CustomThemeImportMode
import com.bag.audioandroid.ui.model.DefaultCustomFactionThemeSettings
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.findDuplicateImportedThemePresetId
import com.bag.audioandroid.ui.model.parseCustomFactionThemeImportText
import com.bag.audioandroid.ui.model.parseCustomMaterialThemeImportText
import com.bag.audioandroid.ui.model.toBatchConfigText
import com.bag.audioandroid.ui.model.toConfigText
import com.bag.audioandroid.ui.theme.appThemeVisualTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal enum class DuplicateImportMode {
    Brand,
    Material,
}

internal data class CustomFactionThemeBatchImportPreview(
    val mode: DuplicateImportMode,
    val importedSettings: List<CustomFactionThemeSettings>,
    val duplicateCount: Int,
) {
    val totalCount: Int
        get() = importedSettings.size

    val newCount: Int
        get() = totalCount - duplicateCount
}

private data class ThemeDialogColors(
    val containerColor: Color,
    val contentColor: Color,
)

internal class ConfigThemeAppearanceDialogState(
    showCustomThemeDialogState: MutableState<Boolean>,
    showCustomMaterialThemeDialogState: MutableState<Boolean>,
    customThemeDialogPresetIdState: MutableState<String?>,
    customMaterialThemeDialogSettingsState: MutableState<CustomFactionThemeSettings?>,
    customMaterialThemeDialogReplacePresetIdState: MutableState<String?>,
    showCustomThemeImportDialogState: MutableState<Boolean>,
    showCustomMaterialThemeImportDialogState: MutableState<Boolean>,
    showCustomThemeExportDialogState: MutableState<Boolean>,
    pendingBatchImportState: MutableState<CustomFactionThemeBatchImportPreview?>,
    duplicateImportModeState: MutableState<DuplicateImportMode?>,
    duplicateImportCandidateState: MutableState<CustomFactionThemeSettings?>,
    duplicateImportPresetIdState: MutableState<String?>,
) {
    var showCustomThemeDialog by showCustomThemeDialogState
    var showCustomMaterialThemeDialog by showCustomMaterialThemeDialogState
    var customThemeDialogPresetId by customThemeDialogPresetIdState
    var customMaterialThemeDialogSettings by customMaterialThemeDialogSettingsState
    var customMaterialThemeDialogReplacePresetId by customMaterialThemeDialogReplacePresetIdState
    var showCustomThemeImportDialog by showCustomThemeImportDialogState
    var showCustomMaterialThemeImportDialog by showCustomMaterialThemeImportDialogState
    var showCustomThemeExportDialog by showCustomThemeExportDialogState
    var pendingBatchImport by pendingBatchImportState
    var duplicateImportMode by duplicateImportModeState
    var duplicateImportCandidate by duplicateImportCandidateState
    var duplicateImportPresetId by duplicateImportPresetIdState

    fun openCreateCustomFactionTheme() {
        customThemeDialogPresetId = null
        showCustomThemeDialog = true
    }

    fun openEditCustomFactionTheme(presetId: String?) {
        customThemeDialogPresetId = presetId
        showCustomThemeDialog = true
    }

    fun dismissCustomThemeDialog() {
        showCustomThemeDialog = false
        customThemeDialogPresetId = null
    }

    fun openCreateCustomMaterialTheme(settings: CustomFactionThemeSettings) {
        customMaterialThemeDialogSettings = settings
        customMaterialThemeDialogReplacePresetId = null
        showCustomMaterialThemeDialog = true
    }

    fun openEditCustomMaterialTheme(settings: CustomFactionThemeSettings) {
        customMaterialThemeDialogSettings = settings
        customMaterialThemeDialogReplacePresetId = settings.presetId
        showCustomMaterialThemeDialog = true
    }

    fun dismissCustomMaterialThemeDialog() {
        showCustomMaterialThemeDialog = false
        customMaterialThemeDialogSettings = null
        customMaterialThemeDialogReplacePresetId = null
    }

    fun openCustomThemeImportDialog() {
        showCustomThemeImportDialog = true
    }

    fun dismissCustomThemeImportDialog() {
        showCustomThemeImportDialog = false
    }

    fun openCustomThemeExportDialog() {
        showCustomThemeExportDialog = true
    }

    fun dismissCustomThemeExportDialog() {
        showCustomThemeExportDialog = false
    }

    fun openCustomMaterialThemeImportDialog() {
        showCustomMaterialThemeImportDialog = true
    }

    fun dismissCustomMaterialThemeImportDialog() {
        showCustomMaterialThemeImportDialog = false
    }

    fun stageBatchImport(preview: CustomFactionThemeBatchImportPreview) {
        pendingBatchImport = preview
    }

    fun dismissBatchImportDialog() {
        pendingBatchImport = null
    }

    fun stageDuplicateImport(
        importedSettings: CustomFactionThemeSettings,
        duplicatePresetId: String,
        mode: DuplicateImportMode,
    ) {
        duplicateImportMode = mode
        duplicateImportCandidate = importedSettings
        duplicateImportPresetId = duplicatePresetId
    }

    fun dismissDuplicateImport() {
        duplicateImportMode = null
        duplicateImportCandidate = null
        duplicateImportPresetId = null
    }
}

internal fun ConfigThemeAppearanceDialogState.stageSingleImportedTheme(
    existing: List<CustomFactionThemeSettings>,
    imported: CustomFactionThemeSettings,
    mode: DuplicateImportMode,
    onSave: (CustomFactionThemeSettings, String?) -> Unit,
) {
    val duplicatePresetId =
        findDuplicateImportedThemePresetId(
            existing = existing,
            imported = imported,
            mode = mode.toCustomThemeImportMode(),
        )
    if (duplicatePresetId == null) {
        onSave(imported, null)
    } else {
        stageDuplicateImport(
            importedSettings = imported,
            duplicatePresetId = duplicatePresetId,
            mode = mode,
        )
    }
}

internal fun saveImportedTheme(
    settings: CustomFactionThemeSettings,
    presetId: String?,
    mode: DuplicateImportMode,
    onBrandSave: (CustomFactionThemeSettings, String?) -> Unit,
    onMaterialSave: (CustomFactionThemeSettings, String?) -> Unit,
) {
    when (mode) {
        DuplicateImportMode.Brand -> onBrandSave(settings, presetId)
        DuplicateImportMode.Material -> onMaterialSave(settings, presetId)
    }
}

internal fun confirmBatchThemeImport(
    preview: CustomFactionThemeBatchImportPreview,
    onBrandImport: (List<CustomFactionThemeSettings>) -> Unit,
    onMaterialImport: (List<CustomFactionThemeSettings>) -> Unit,
) {
    when (preview.mode) {
        DuplicateImportMode.Brand -> onBrandImport(preview.importedSettings)
        DuplicateImportMode.Material -> onMaterialImport(preview.importedSettings)
    }
}

@Composable
internal fun rememberConfigThemeAppearanceDialogState(): ConfigThemeAppearanceDialogState {
    val showCustomThemeDialog = rememberSaveable { mutableStateOf(false) }
    val showCustomMaterialThemeDialog = rememberSaveable { mutableStateOf(false) }
    val customThemeDialogPresetId = rememberSaveable { mutableStateOf<String?>(null) }
    val customMaterialThemeDialogSettings = remember { mutableStateOf<CustomFactionThemeSettings?>(null) }
    val customMaterialThemeDialogReplacePresetId = rememberSaveable { mutableStateOf<String?>(null) }
    val showCustomThemeImportDialog = rememberSaveable { mutableStateOf(false) }
    val showCustomMaterialThemeImportDialog = rememberSaveable { mutableStateOf(false) }
    val showCustomThemeExportDialog = rememberSaveable { mutableStateOf(false) }
    val pendingBatchImport = remember { mutableStateOf<CustomFactionThemeBatchImportPreview?>(null) }
    val duplicateImportMode = remember { mutableStateOf<DuplicateImportMode?>(null) }
    val duplicateImportCandidate = remember { mutableStateOf<CustomFactionThemeSettings?>(null) }
    val duplicateImportPresetId = remember { mutableStateOf<String?>(null) }
    return remember(
        showCustomThemeDialog,
        showCustomMaterialThemeDialog,
        customThemeDialogPresetId,
        showCustomThemeImportDialog,
        showCustomMaterialThemeImportDialog,
        showCustomThemeExportDialog,
        pendingBatchImport,
        duplicateImportMode,
        duplicateImportCandidate,
        duplicateImportPresetId,
    ) {
        ConfigThemeAppearanceDialogState(
            showCustomThemeDialogState = showCustomThemeDialog,
            showCustomMaterialThemeDialogState = showCustomMaterialThemeDialog,
            customThemeDialogPresetIdState = customThemeDialogPresetId,
            customMaterialThemeDialogSettingsState = customMaterialThemeDialogSettings,
            customMaterialThemeDialogReplacePresetIdState = customMaterialThemeDialogReplacePresetId,
            showCustomThemeImportDialogState = showCustomThemeImportDialog,
            showCustomMaterialThemeImportDialogState = showCustomMaterialThemeImportDialog,
            showCustomThemeExportDialogState = showCustomThemeExportDialog,
            pendingBatchImportState = pendingBatchImport,
            duplicateImportModeState = duplicateImportMode,
            duplicateImportCandidateState = duplicateImportCandidate,
            duplicateImportPresetIdState = duplicateImportPresetId,
        )
    }
}

@Composable
internal fun ConfigThemeAppearanceDialogHost(
    dialogState: ConfigThemeAppearanceDialogState,
    selectedThemeMode: ThemeModeOption,
    customFactionThemePresets: List<CustomFactionThemeSettings>,
    onCustomFactionThemeSaved: (CustomFactionThemeSettings, String?) -> Unit,
    onCustomFactionThemeDeleted: (String) -> Unit,
    customMaterialThemePresets: List<CustomFactionThemeSettings>,
    customMaterialThemeSettings: CustomFactionThemeSettings,
    onCustomMaterialThemeSaved: (CustomFactionThemeSettings, String?) -> Unit,
    onCustomMaterialThemeDeleted: (String) -> Unit,
    onSingleMaterialThemeImported: (CustomFactionThemeSettings) -> Unit,
    onSingleThemeImported: (CustomFactionThemeSettings) -> Unit,
    onBatchMaterialThemesImported: (List<CustomFactionThemeSettings>) -> Unit,
    onBatchThemesImported: (List<CustomFactionThemeSettings>) -> Unit,
    selectedCustomFactionThemePreset: CustomFactionThemeSettings?,
    clipboard: Clipboard,
    coroutineScope: CoroutineScope,
    onConfirmBatchImport: (CustomFactionThemeBatchImportPreview) -> Unit,
    onConfirmDuplicateOverwrite: (CustomFactionThemeSettings, String, DuplicateImportMode) -> Unit,
    onConfirmDuplicateAddCopy: (CustomFactionThemeSettings, DuplicateImportMode) -> Unit,
) {
    if (dialogState.showCustomThemeDialog) {
        val editingSettings =
            dialogState.customThemeDialogPresetId?.let { presetId ->
                customFactionThemePresets.firstOrNull { it.presetId == presetId }
            }
        CustomFactionThemeDialog(
            initialSettings = editingSettings ?: customFactionThemePresets.firstOrNull() ?: DefaultCustomFactionThemeSettings,
            isCreatingNew = editingSettings == null,
            canDelete = editingSettings != null && customFactionThemePresets.size > 1,
            selectedThemeMode = selectedThemeMode,
            onDismiss = dialogState::dismissCustomThemeDialog,
            onSave = { settings ->
                onCustomFactionThemeSaved(settings, dialogState.customThemeDialogPresetId)
                dialogState.dismissCustomThemeDialog()
            },
            onDelete = {
                val presetId = dialogState.customThemeDialogPresetId ?: return@CustomFactionThemeDialog
                onCustomFactionThemeDeleted(presetId)
                dialogState.dismissCustomThemeDialog()
            },
        )
    }
    if (dialogState.showCustomMaterialThemeDialog) {
        val editingMaterialSettings = dialogState.customMaterialThemeDialogSettings ?: customMaterialThemeSettings
        val replaceMaterialPresetId = dialogState.customMaterialThemeDialogReplacePresetId
        CustomFactionThemeDialog(
            initialSettings = editingMaterialSettings,
            isCreatingNew = replaceMaterialPresetId == null,
            canDelete = replaceMaterialPresetId != null && customMaterialThemePresets.size > 1,
            mode = CustomFactionThemeDialogMode.MaterialSingleColor,
            selectedThemeMode = selectedThemeMode,
            onDismiss = dialogState::dismissCustomMaterialThemeDialog,
            onSave = { settings ->
                onCustomMaterialThemeSaved(settings, replaceMaterialPresetId)
                dialogState.dismissCustomMaterialThemeDialog()
            },
            onDelete = {
                val presetId = replaceMaterialPresetId ?: return@CustomFactionThemeDialog
                onCustomMaterialThemeDeleted(presetId)
                dialogState.dismissCustomMaterialThemeDialog()
            },
        )
    }
    if (dialogState.showCustomThemeImportDialog) {
        CustomFactionThemeImportDialog(
            onDismiss = dialogState::dismissCustomThemeImportDialog,
            onImport = { importedSettings ->
                if (importedSettings.size == 1) {
                    onSingleThemeImported(importedSettings.single())
                } else {
                    onBatchThemesImported(importedSettings)
                }
                dialogState.dismissCustomThemeImportDialog()
            },
        )
    }
    if (dialogState.showCustomMaterialThemeImportDialog) {
        CustomMaterialThemeImportDialog(
            onDismiss = dialogState::dismissCustomMaterialThemeImportDialog,
            onImport = { importedSettings ->
                if (importedSettings.size == 1) {
                    onSingleMaterialThemeImported(importedSettings.single())
                } else {
                    onBatchMaterialThemesImported(importedSettings)
                }
                dialogState.dismissCustomMaterialThemeImportDialog()
            },
        )
    }
    if (dialogState.showCustomThemeExportDialog) {
        CustomFactionThemeExportDialog(
            selectedConfigText = selectedCustomFactionThemePreset?.toConfigText().orEmpty(),
            allConfigText = customFactionThemePresets.toBatchConfigText(),
            canExportSelected = selectedCustomFactionThemePreset != null,
            clipboard = clipboard,
            coroutineScope = coroutineScope,
            onDismiss = dialogState::dismissCustomThemeExportDialog,
        )
    }
    dialogState.pendingBatchImport?.let { preview ->
        CustomFactionThemeBatchImportConfirmDialog(
            preview = preview,
            onDismiss = dialogState::dismissBatchImportDialog,
            onImport = { onConfirmBatchImport(preview) },
        )
    }
    val duplicateImportMode = dialogState.duplicateImportMode
    val duplicateImportCandidate = dialogState.duplicateImportCandidate
    val duplicateImportPresetId = dialogState.duplicateImportPresetId
    if (duplicateImportMode != null && duplicateImportCandidate != null && duplicateImportPresetId != null) {
        val dialogColors = rememberThemeDialogColors()
        AlertDialog(
            onDismissRequest = dialogState::dismissDuplicateImport,
            containerColor = dialogColors.containerColor,
            titleContentColor = dialogColors.contentColor,
            textContentColor = dialogColors.contentColor,
            title = { Text(text = stringResource(R.string.config_custom_faction_theme_import_duplicate_title)) },
            text = { Text(text = stringResource(R.string.config_custom_faction_theme_import_duplicate_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmDuplicateOverwrite(
                            duplicateImportCandidate,
                            duplicateImportPresetId,
                            duplicateImportMode,
                        )
                    },
                ) {
                    Text(text = stringResource(R.string.config_custom_faction_theme_import_overwrite))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onConfirmDuplicateAddCopy(duplicateImportCandidate, duplicateImportMode)
                    },
                ) {
                    Text(text = stringResource(R.string.config_custom_faction_theme_import_add_copy))
                }
            },
        )
    }
}

@Composable
private fun CustomMaterialThemeImportDialog(
    onDismiss: () -> Unit,
    onImport: (List<CustomFactionThemeSettings>) -> Unit,
) {
    var configText by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }
    val parseResult = remember(configText) { parseCustomMaterialThemeImportText(configText) }
    val context = LocalContext.current
    val errorMessage =
        if (showError && parseResult is CustomFactionThemeImportParseResult.Invalid) {
            formatCustomThemeImportErrorMessage(context, parseResult.error)
        } else {
            null
        }
    val dialogColors = rememberThemeDialogColors()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogColors.containerColor,
        titleContentColor = dialogColors.contentColor,
        textContentColor = dialogColors.contentColor,
        title = { Text(text = stringResource(R.string.config_custom_material_theme_import_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.config_custom_material_theme_import_description))
                OutlinedTextField(
                    value = configText,
                    onValueChange = {
                        configText = it
                        showError = false
                    },
                    minLines = 6,
                    maxLines = 10,
                    label = { Text(text = stringResource(R.string.config_custom_material_theme_import_config_label)) },
                    isError = showError,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(text = errorMessage)
                        }
                    },
                    colors = audioInputTextFieldColors(appThemeVisualTokens().themeStyle),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (parseResult) {
                        is CustomFactionThemeImportParseResult.Valid -> onImport(parseResult.settings)
                        is CustomFactionThemeImportParseResult.Invalid -> showError = true
                    }
                },
            ) {
                Text(text = stringResource(R.string.config_custom_material_theme_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun CustomFactionThemeImportDialog(
    onDismiss: () -> Unit,
    onImport: (List<CustomFactionThemeSettings>) -> Unit,
) {
    var configText by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }
    val parseResult = remember(configText) { parseCustomFactionThemeImportText(configText) }
    val context = LocalContext.current
    val errorMessage =
        if (showError && parseResult is CustomFactionThemeImportParseResult.Invalid) {
            formatCustomThemeImportErrorMessage(context, parseResult.error)
        } else {
            null
        }
    val visualTokens = appThemeVisualTokens()
    val dialogColors = rememberThemeDialogColors()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogColors.containerColor,
        titleContentColor = dialogColors.contentColor,
        textContentColor = dialogColors.contentColor,
        title = { Text(text = stringResource(R.string.config_custom_faction_theme_import_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.config_custom_faction_theme_import_description))
                OutlinedTextField(
                    value = configText,
                    onValueChange = {
                        configText = it
                        showError = false
                    },
                    minLines = 6,
                    maxLines = 10,
                    label = { Text(text = stringResource(R.string.config_custom_faction_theme_import_config_label)) },
                    isError = showError,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(text = errorMessage)
                        }
                    },
                    colors = audioInputTextFieldColors(visualTokens.themeStyle),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (parseResult) {
                        is CustomFactionThemeImportParseResult.Valid -> onImport(parseResult.settings)
                        is CustomFactionThemeImportParseResult.Invalid -> showError = true
                    }
                },
            ) {
                Text(text = stringResource(R.string.config_custom_faction_theme_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

internal fun formatCustomThemeImportErrorMessage(
    context: Context,
    error: CustomThemeImportError,
): String {
    fun fieldLabel(field: String): String =
        when (field.lowercase()) {
            "name" -> context.getString(R.string.config_custom_theme_import_field_name)
            "primary" -> context.getString(R.string.config_custom_theme_import_field_primary)
            "secondary" -> context.getString(R.string.config_custom_theme_import_field_secondary)
            "outline" -> context.getString(R.string.config_custom_theme_import_field_outline)
            else -> field
        }

    return when (error) {
        CustomThemeImportError.EmptyInput ->
            context.getString(R.string.config_custom_theme_import_error_empty)
        is CustomThemeImportError.MalformedLine ->
            context.getString(R.string.config_custom_theme_import_error_malformed_line, error.lineNumber)
        is CustomThemeImportError.UnknownField ->
            context.getString(
                R.string.config_custom_theme_import_error_unknown_field,
                error.blockIndex,
                error.field,
            )
        is CustomThemeImportError.DuplicateField ->
            context.getString(
                R.string.config_custom_theme_import_error_duplicate_field,
                error.blockIndex,
                fieldLabel(error.field),
            )
        is CustomThemeImportError.MissingField ->
            context.getString(
                R.string.config_custom_theme_import_error_missing_field,
                error.blockIndex,
                fieldLabel(error.field),
            )
        is CustomThemeImportError.InvalidHex ->
            context.getString(
                R.string.config_custom_theme_import_error_invalid_hex,
                error.blockIndex,
                fieldLabel(error.field),
                error.value,
            )
        is CustomThemeImportError.WrongImportMode ->
            when (error.detectedMode) {
                CustomThemeImportMode.DualTone ->
                    context.getString(
                        R.string.config_custom_theme_import_error_wrong_target_faction_theme,
                        error.blockIndex,
                    )
                CustomThemeImportMode.Material ->
                    context.getString(
                        R.string.config_custom_theme_import_error_wrong_target_material,
                        error.blockIndex,
                    )
            }
    }
}

@Composable
private fun CustomFactionThemeExportDialog(
    selectedConfigText: String,
    allConfigText: String,
    canExportSelected: Boolean,
    clipboard: Clipboard,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
) {
    var exportScope by rememberSaveable {
        mutableStateOf(
            if (canExportSelected) {
                CustomFactionThemeExportScope.Selected
            } else {
                CustomFactionThemeExportScope.All
            },
        )
    }
    val effectiveExportScope =
        if (exportScope == CustomFactionThemeExportScope.Selected && !canExportSelected) {
            CustomFactionThemeExportScope.All
        } else {
            exportScope
        }
    val configText =
        when (effectiveExportScope) {
            CustomFactionThemeExportScope.Selected -> selectedConfigText
            CustomFactionThemeExportScope.All -> allConfigText
        }
    val dialogColors = rememberThemeDialogColors()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogColors.containerColor,
        titleContentColor = dialogColors.contentColor,
        textContentColor = dialogColors.contentColor,
        title = { Text(text = stringResource(R.string.config_custom_faction_theme_export_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.config_custom_faction_theme_export_description))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    CustomFactionThemeExportScope.entries.forEachIndexed { index, scope ->
                        SegmentedButton(
                            selected = effectiveExportScope == scope,
                            onClick = { exportScope = scope },
                            shape =
                                SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = CustomFactionThemeExportScope.entries.size,
                                ),
                            enabled = scope != CustomFactionThemeExportScope.Selected || canExportSelected,
                            colors = appSegmentedButtonColors(),
                        ) {
                            Text(text = stringResource(scope.labelResId))
                        }
                    }
                }
                OutlinedTextField(
                    value = configText,
                    onValueChange = {},
                    readOnly = true,
                    minLines = 8,
                    maxLines = 12,
                    label = { Text(text = stringResource(R.string.config_custom_faction_theme_import_config_label)) },
                    colors = audioInputTextFieldColors(appThemeVisualTokens().themeStyle),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("FlipBits custom theme config", configText)))
                        onDismiss()
                    }
                },
            ) {
                Text(text = stringResource(R.string.config_custom_faction_theme_copy_config))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

private enum class CustomFactionThemeExportScope(
    val labelResId: Int,
) {
    Selected(R.string.config_custom_faction_theme_export_scope_selected),
    All(R.string.config_custom_faction_theme_export_scope_all),
}

@Composable
private fun CustomFactionThemeBatchImportConfirmDialog(
    preview: CustomFactionThemeBatchImportPreview,
    onDismiss: () -> Unit,
    onImport: () -> Unit,
) {
    val dialogColors = rememberThemeDialogColors()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogColors.containerColor,
        titleContentColor = dialogColors.contentColor,
        textContentColor = dialogColors.contentColor,
        title = { Text(text = stringResource(R.string.config_custom_faction_theme_batch_import_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text =
                        stringResource(
                            R.string.config_custom_faction_theme_batch_import_summary,
                            preview.totalCount,
                            preview.newCount,
                            preview.duplicateCount,
                        ),
                )
                Text(text = stringResource(R.string.config_custom_faction_theme_batch_import_duplicate_note))
            }
        },
        confirmButton = {
            TextButton(
                enabled = preview.importedSettings.isNotEmpty(),
                onClick = onImport,
            ) {
                Text(text = stringResource(R.string.config_custom_faction_theme_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun rememberThemeDialogColors(): ThemeDialogColors {
    val visualTokens = appThemeVisualTokens()
    return remember(visualTokens) {
        ThemeDialogColors(
            containerColor = visualTokens.modalContainerColor,
            contentColor = visualTokens.modalContentColor,
        )
    }
}

internal fun buildCustomFactionThemeBatchImportPreview(
    existing: List<CustomFactionThemeSettings>,
    imported: List<CustomFactionThemeSettings>,
    mode: DuplicateImportMode,
): CustomFactionThemeBatchImportPreview {
    val duplicateCount =
        imported.count { importedSettings ->
            findDuplicateImportedThemePresetId(
                existing = existing,
                imported = importedSettings,
                mode = mode.toCustomThemeImportMode(),
            ) != null
        }
    return CustomFactionThemeBatchImportPreview(
        mode = mode,
        importedSettings = imported,
        duplicateCount = duplicateCount,
    )
}

private fun DuplicateImportMode.toCustomThemeImportMode(): CustomThemeImportMode =
    when (this) {
        DuplicateImportMode.Brand -> CustomThemeImportMode.DualTone
        DuplicateImportMode.Material -> CustomThemeImportMode.Material
    }
