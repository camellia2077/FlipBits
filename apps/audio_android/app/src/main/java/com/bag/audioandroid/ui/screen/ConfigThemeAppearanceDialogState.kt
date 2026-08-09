package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.bag.audioandroid.ui.model.CustomFactionThemeSettings
import com.bag.audioandroid.ui.model.findDuplicateImportedThemePresetId

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

internal data class ThemeDialogColors(
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
