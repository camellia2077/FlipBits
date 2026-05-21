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
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.appSegmentedButtonColors
import com.bag.audioandroid.ui.model.CustomBrandThemeImportParseResult
import com.bag.audioandroid.ui.model.CustomBrandThemeSettings
import com.bag.audioandroid.ui.model.CustomThemeImportError
import com.bag.audioandroid.ui.model.CustomThemeImportMode
import com.bag.audioandroid.ui.model.DefaultCustomBrandThemeSettings
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.findDuplicateImportedThemePresetId
import com.bag.audioandroid.ui.model.parseCustomBrandThemeImportText
import com.bag.audioandroid.ui.model.parseCustomMaterialThemeImportText
import com.bag.audioandroid.ui.model.toBatchConfigText
import com.bag.audioandroid.ui.model.toConfigText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal enum class DuplicateImportMode {
    Brand,
    Material,
}

internal data class CustomBrandThemeBatchImportPreview(
    val mode: DuplicateImportMode,
    val importedSettings: List<CustomBrandThemeSettings>,
    val duplicateCount: Int,
) {
    val totalCount: Int
        get() = importedSettings.size

    val newCount: Int
        get() = totalCount - duplicateCount
}

internal class ConfigThemeAppearanceDialogState(
    showCustomThemeDialogState: MutableState<Boolean>,
    showCustomMaterialThemeDialogState: MutableState<Boolean>,
    customThemeDialogPresetIdState: MutableState<String?>,
    showCustomThemeImportDialogState: MutableState<Boolean>,
    showCustomMaterialThemeImportDialogState: MutableState<Boolean>,
    showCustomThemeExportDialogState: MutableState<Boolean>,
    pendingBatchImportState: MutableState<CustomBrandThemeBatchImportPreview?>,
    duplicateImportModeState: MutableState<DuplicateImportMode?>,
    duplicateImportCandidateState: MutableState<CustomBrandThemeSettings?>,
    duplicateImportPresetIdState: MutableState<String?>,
) {
    var showCustomThemeDialog by showCustomThemeDialogState
    var showCustomMaterialThemeDialog by showCustomMaterialThemeDialogState
    var customThemeDialogPresetId by customThemeDialogPresetIdState
    var showCustomThemeImportDialog by showCustomThemeImportDialogState
    var showCustomMaterialThemeImportDialog by showCustomMaterialThemeImportDialogState
    var showCustomThemeExportDialog by showCustomThemeExportDialogState
    var pendingBatchImport by pendingBatchImportState
    var duplicateImportMode by duplicateImportModeState
    var duplicateImportCandidate by duplicateImportCandidateState
    var duplicateImportPresetId by duplicateImportPresetIdState

    fun openCreateCustomBrandTheme() {
        customThemeDialogPresetId = null
        showCustomThemeDialog = true
    }

    fun openEditCustomBrandTheme(presetId: String?) {
        customThemeDialogPresetId = presetId
        showCustomThemeDialog = true
    }

    fun dismissCustomThemeDialog() {
        showCustomThemeDialog = false
        customThemeDialogPresetId = null
    }

    fun openCustomMaterialThemeDialog() {
        showCustomMaterialThemeDialog = true
    }

    fun dismissCustomMaterialThemeDialog() {
        showCustomMaterialThemeDialog = false
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

    fun stageBatchImport(preview: CustomBrandThemeBatchImportPreview) {
        pendingBatchImport = preview
    }

    fun dismissBatchImportDialog() {
        pendingBatchImport = null
    }

    fun stageDuplicateImport(
        importedSettings: CustomBrandThemeSettings,
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
    existing: List<CustomBrandThemeSettings>,
    imported: CustomBrandThemeSettings,
    mode: DuplicateImportMode,
    onSave: (CustomBrandThemeSettings, String?) -> Unit,
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
    settings: CustomBrandThemeSettings,
    presetId: String?,
    mode: DuplicateImportMode,
    onBrandSave: (CustomBrandThemeSettings, String?) -> Unit,
    onMaterialSave: (CustomBrandThemeSettings, String?) -> Unit,
) {
    when (mode) {
        DuplicateImportMode.Brand -> onBrandSave(settings, presetId)
        DuplicateImportMode.Material -> onMaterialSave(settings, presetId)
    }
}

internal fun confirmBatchThemeImport(
    preview: CustomBrandThemeBatchImportPreview,
    onBrandImport: (List<CustomBrandThemeSettings>) -> Unit,
    onMaterialImport: (List<CustomBrandThemeSettings>) -> Unit,
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
    val showCustomThemeImportDialog = rememberSaveable { mutableStateOf(false) }
    val showCustomMaterialThemeImportDialog = rememberSaveable { mutableStateOf(false) }
    val showCustomThemeExportDialog = rememberSaveable { mutableStateOf(false) }
    val pendingBatchImport = remember { mutableStateOf<CustomBrandThemeBatchImportPreview?>(null) }
    val duplicateImportMode = remember { mutableStateOf<DuplicateImportMode?>(null) }
    val duplicateImportCandidate = remember { mutableStateOf<CustomBrandThemeSettings?>(null) }
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
    customBrandThemePresets: List<CustomBrandThemeSettings>,
    onCustomBrandThemeSaved: (CustomBrandThemeSettings, String?) -> Unit,
    onCustomBrandThemeDeleted: (String) -> Unit,
    customMaterialThemePresets: List<CustomBrandThemeSettings>,
    customMaterialThemeSettings: CustomBrandThemeSettings,
    onCustomMaterialThemeSaved: (CustomBrandThemeSettings, String?) -> Unit,
    onCustomMaterialThemeDeleted: (String) -> Unit,
    onSingleMaterialThemeImported: (CustomBrandThemeSettings) -> Unit,
    onSingleThemeImported: (CustomBrandThemeSettings) -> Unit,
    onBatchMaterialThemesImported: (List<CustomBrandThemeSettings>) -> Unit,
    onBatchThemesImported: (List<CustomBrandThemeSettings>) -> Unit,
    selectedCustomBrandThemePreset: CustomBrandThemeSettings?,
    clipboard: Clipboard,
    coroutineScope: CoroutineScope,
    onConfirmBatchImport: (CustomBrandThemeBatchImportPreview) -> Unit,
    onConfirmDuplicateOverwrite: (CustomBrandThemeSettings, String, DuplicateImportMode) -> Unit,
    onConfirmDuplicateAddCopy: (CustomBrandThemeSettings, DuplicateImportMode) -> Unit,
) {
    if (dialogState.showCustomThemeDialog) {
        val editingSettings =
            dialogState.customThemeDialogPresetId?.let { presetId ->
                customBrandThemePresets.firstOrNull { it.presetId == presetId }
            }
        CustomBrandThemeDialog(
            initialSettings = editingSettings ?: customBrandThemePresets.firstOrNull() ?: DefaultCustomBrandThemeSettings,
            isCreatingNew = editingSettings == null,
            canDelete = editingSettings != null && customBrandThemePresets.size > 1,
            selectedThemeMode = selectedThemeMode,
            onDismiss = dialogState::dismissCustomThemeDialog,
            onSave = { settings ->
                onCustomBrandThemeSaved(settings, dialogState.customThemeDialogPresetId)
                dialogState.dismissCustomThemeDialog()
            },
            onDelete = {
                val presetId = dialogState.customThemeDialogPresetId ?: return@CustomBrandThemeDialog
                onCustomBrandThemeDeleted(presetId)
                dialogState.dismissCustomThemeDialog()
            },
        )
    }
    if (dialogState.showCustomMaterialThemeDialog) {
        CustomBrandThemeDialog(
            initialSettings = customMaterialThemeSettings,
            isCreatingNew = false,
            canDelete = customMaterialThemePresets.size > 1,
            mode = CustomBrandThemeDialogMode.MaterialSingleColor,
            selectedThemeMode = selectedThemeMode,
            onDismiss = dialogState::dismissCustomMaterialThemeDialog,
            onSave = { settings ->
                onCustomMaterialThemeSaved(settings, customMaterialThemeSettings.presetId)
                dialogState.dismissCustomMaterialThemeDialog()
            },
            onDelete = {
                onCustomMaterialThemeDeleted(customMaterialThemeSettings.presetId)
                dialogState.dismissCustomMaterialThemeDialog()
            },
        )
    }
    if (dialogState.showCustomThemeImportDialog) {
        CustomBrandThemeImportDialog(
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
        CustomBrandThemeExportDialog(
            selectedConfigText = selectedCustomBrandThemePreset?.toConfigText().orEmpty(),
            allConfigText = customBrandThemePresets.toBatchConfigText(),
            canExportSelected = selectedCustomBrandThemePreset != null,
            clipboard = clipboard,
            coroutineScope = coroutineScope,
            onDismiss = dialogState::dismissCustomThemeExportDialog,
        )
    }
    dialogState.pendingBatchImport?.let { preview ->
        CustomBrandThemeBatchImportConfirmDialog(
            preview = preview,
            onDismiss = dialogState::dismissBatchImportDialog,
            onImport = { onConfirmBatchImport(preview) },
        )
    }
    val duplicateImportMode = dialogState.duplicateImportMode
    val duplicateImportCandidate = dialogState.duplicateImportCandidate
    val duplicateImportPresetId = dialogState.duplicateImportPresetId
    if (duplicateImportMode != null && duplicateImportCandidate != null && duplicateImportPresetId != null) {
        AlertDialog(
            onDismissRequest = dialogState::dismissDuplicateImport,
            title = { Text(text = stringResource(R.string.config_custom_brand_theme_import_duplicate_title)) },
            text = { Text(text = stringResource(R.string.config_custom_brand_theme_import_duplicate_message)) },
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
                    Text(text = stringResource(R.string.config_custom_brand_theme_import_overwrite))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onConfirmDuplicateAddCopy(duplicateImportCandidate, duplicateImportMode)
                    },
                ) {
                    Text(text = stringResource(R.string.config_custom_brand_theme_import_add_copy))
                }
            },
        )
    }
}

@Composable
private fun CustomMaterialThemeImportDialog(
    onDismiss: () -> Unit,
    onImport: (List<CustomBrandThemeSettings>) -> Unit,
) {
    var configText by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }
    val parseResult = remember(configText) { parseCustomMaterialThemeImportText(configText) }
    val context = LocalContext.current
    val errorMessage =
        if (showError && parseResult is CustomBrandThemeImportParseResult.Invalid) {
            formatCustomThemeImportErrorMessage(context, parseResult.error)
        } else {
            null
        }
    AlertDialog(
        onDismissRequest = onDismiss,
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
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (parseResult) {
                        is CustomBrandThemeImportParseResult.Valid -> onImport(parseResult.settings)
                        is CustomBrandThemeImportParseResult.Invalid -> showError = true
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
private fun CustomBrandThemeImportDialog(
    onDismiss: () -> Unit,
    onImport: (List<CustomBrandThemeSettings>) -> Unit,
) {
    var configText by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }
    val parseResult = remember(configText) { parseCustomBrandThemeImportText(configText) }
    val context = LocalContext.current
    val errorMessage =
        if (showError && parseResult is CustomBrandThemeImportParseResult.Invalid) {
            formatCustomThemeImportErrorMessage(context, parseResult.error)
        } else {
            null
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.config_custom_brand_theme_import_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.config_custom_brand_theme_import_description))
                OutlinedTextField(
                    value = configText,
                    onValueChange = {
                        configText = it
                        showError = false
                    },
                    minLines = 6,
                    maxLines = 10,
                    label = { Text(text = stringResource(R.string.config_custom_brand_theme_import_config_label)) },
                    isError = showError,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(text = errorMessage)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (parseResult) {
                        is CustomBrandThemeImportParseResult.Valid -> onImport(parseResult.settings)
                        is CustomBrandThemeImportParseResult.Invalid -> showError = true
                    }
                },
            ) {
                Text(text = stringResource(R.string.config_custom_brand_theme_import))
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
                        R.string.config_custom_theme_import_error_wrong_target_dual_tone,
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
private fun CustomBrandThemeExportDialog(
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
                CustomBrandThemeExportScope.Selected
            } else {
                CustomBrandThemeExportScope.All
            },
        )
    }
    val effectiveExportScope =
        if (exportScope == CustomBrandThemeExportScope.Selected && !canExportSelected) {
            CustomBrandThemeExportScope.All
        } else {
            exportScope
        }
    val configText =
        when (effectiveExportScope) {
            CustomBrandThemeExportScope.Selected -> selectedConfigText
            CustomBrandThemeExportScope.All -> allConfigText
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.config_custom_brand_theme_export_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.config_custom_brand_theme_export_description))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    CustomBrandThemeExportScope.entries.forEachIndexed { index, scope ->
                        SegmentedButton(
                            selected = effectiveExportScope == scope,
                            onClick = { exportScope = scope },
                            shape =
                                SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = CustomBrandThemeExportScope.entries.size,
                                ),
                            enabled = scope != CustomBrandThemeExportScope.Selected || canExportSelected,
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
                    label = { Text(text = stringResource(R.string.config_custom_brand_theme_import_config_label)) },
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
                Text(text = stringResource(R.string.config_custom_brand_theme_copy_config))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

private enum class CustomBrandThemeExportScope(
    val labelResId: Int,
) {
    Selected(R.string.config_custom_brand_theme_export_scope_selected),
    All(R.string.config_custom_brand_theme_export_scope_all),
}

@Composable
private fun CustomBrandThemeBatchImportConfirmDialog(
    preview: CustomBrandThemeBatchImportPreview,
    onDismiss: () -> Unit,
    onImport: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.config_custom_brand_theme_batch_import_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text =
                        stringResource(
                            R.string.config_custom_brand_theme_batch_import_summary,
                            preview.totalCount,
                            preview.newCount,
                            preview.duplicateCount,
                        ),
                )
                Text(text = stringResource(R.string.config_custom_brand_theme_batch_import_duplicate_note))
            }
        },
        confirmButton = {
            TextButton(
                enabled = preview.importedSettings.isNotEmpty(),
                onClick = onImport,
            ) {
                Text(text = stringResource(R.string.config_custom_brand_theme_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

internal fun buildCustomBrandThemeBatchImportPreview(
    existing: List<CustomBrandThemeSettings>,
    imported: List<CustomBrandThemeSettings>,
    mode: DuplicateImportMode,
): CustomBrandThemeBatchImportPreview {
    val duplicateCount =
        imported.count { importedSettings ->
            findDuplicateImportedThemePresetId(
                existing = existing,
                imported = importedSettings,
                mode = mode.toCustomThemeImportMode(),
            ) != null
        }
    return CustomBrandThemeBatchImportPreview(
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
