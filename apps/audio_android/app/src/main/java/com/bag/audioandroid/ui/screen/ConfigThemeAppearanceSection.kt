package com.bag.audioandroid.ui.screen

import android.content.ClipData
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Input
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.appSegmentedButtonColors
import com.bag.audioandroid.ui.model.BrandThemeOption
import com.bag.audioandroid.ui.model.CustomBrandThemeImportParseResult
import com.bag.audioandroid.ui.model.CustomBrandThemeSettings
import com.bag.audioandroid.ui.model.CustomThemeImportError
import com.bag.audioandroid.ui.model.CustomThemeImportMode
import com.bag.audioandroid.ui.model.DefaultCustomBrandThemeSettings
import com.bag.audioandroid.ui.model.PaletteFamily
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.findDuplicateImportedThemePresetId
import com.bag.audioandroid.ui.model.parseCustomBrandThemeImportText
import com.bag.audioandroid.ui.model.parseCustomMaterialThemeImportText
import com.bag.audioandroid.ui.model.toBatchConfigText
import com.bag.audioandroid.ui.model.toConfigText
import com.bag.audioandroid.ui.model.toMaterialBatchConfigText
import com.bag.audioandroid.ui.theme.AppThemeAccentTokens
import com.bag.audioandroid.ui.theme.customBrandThemeOptionId
import com.bag.audioandroid.ui.theme.customMaterialPalette
import com.bag.audioandroid.ui.theme.isCustomMaterialPaletteId
import com.bag.audioandroid.ui.utilityActionIconButtonColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class ConfigThemeAppearanceBrandThemeGroups(
    val sacredMachineThemes: List<BrandThemeOption>,
    val ancientDynastyThemes: List<BrandThemeOption>,
    val immortalRotThemes: List<BrandThemeOption>,
    val scarletCarnageThemes: List<BrandThemeOption>,
    val exquisiteFallThemes: List<BrandThemeOption>,
    val labyrinthOfMutabilityThemes: List<BrandThemeOption>,
)

private data class ConfigThemeAppearanceSectionRenderState(
    val isBrandStyle: Boolean,
    val materialPaletteGroups: List<PaletteGroupUi>,
    val brandThemeGroups: ConfigThemeAppearanceBrandThemeGroups,
    val selectedCustomBrandThemePreset: CustomBrandThemeSettings?,
)

private data class ConfigMaterialPaletteExpansionState(
    val isCustomExpanded: Boolean,
    val onCustomExpandedChanged: (Boolean) -> Unit,
    val isBuiltInExpanded: Boolean,
    val onBuiltInExpandedChanged: (Boolean) -> Unit,
    val isRedsExpanded: Boolean,
    val onRedsExpandedChanged: (Boolean) -> Unit,
    val isOrangesExpanded: Boolean,
    val onOrangesExpandedChanged: (Boolean) -> Unit,
    val isYellowsExpanded: Boolean,
    val onYellowsExpandedChanged: (Boolean) -> Unit,
    val isGreensExpanded: Boolean,
    val onGreensExpandedChanged: (Boolean) -> Unit,
    val isBluesExpanded: Boolean,
    val onBluesExpandedChanged: (Boolean) -> Unit,
    val isPurplesExpanded: Boolean,
    val onPurplesExpandedChanged: (Boolean) -> Unit,
    val isNeutralsExpanded: Boolean,
    val onNeutralsExpandedChanged: (Boolean) -> Unit,
) {
    fun isExpanded(family: PaletteFamily): Boolean =
        when (family) {
            PaletteFamily.Custom -> isCustomExpanded
            PaletteFamily.Reds -> isRedsExpanded
            PaletteFamily.Oranges -> isOrangesExpanded
            PaletteFamily.Yellows -> isYellowsExpanded
            PaletteFamily.Greens -> isGreensExpanded
            PaletteFamily.Blues -> isBluesExpanded
            PaletteFamily.Purples -> isPurplesExpanded
            PaletteFamily.Neutrals -> isNeutralsExpanded
            PaletteFamily.Brand -> true
        }

    fun onExpandedChanged(family: PaletteFamily): (Boolean) -> Unit =
        when (family) {
            PaletteFamily.Custom -> onCustomExpandedChanged
            PaletteFamily.Reds -> onRedsExpandedChanged
            PaletteFamily.Oranges -> onOrangesExpandedChanged
            PaletteFamily.Yellows -> onYellowsExpandedChanged
            PaletteFamily.Greens -> onGreensExpandedChanged
            PaletteFamily.Blues -> onBluesExpandedChanged
            PaletteFamily.Purples -> onPurplesExpandedChanged
            PaletteFamily.Neutrals -> onNeutralsExpandedChanged
            PaletteFamily.Brand -> { _: Boolean -> }
        }
}

private data class ConfigThemeAppearanceBrandThemeExpansionState(
    val isCustomBrandThemeExpanded: Boolean,
    val onCustomBrandThemeExpandedChanged: (Boolean) -> Unit,
    val isSacredMachineBrandThemeExpanded: Boolean,
    val onSacredMachineBrandThemeExpandedChanged: (Boolean) -> Unit,
    val isAncientDynastyBrandThemeExpanded: Boolean,
    val onAncientDynastyBrandThemeExpandedChanged: (Boolean) -> Unit,
    val isImmortalRotBrandThemeExpanded: Boolean,
    val onImmortalRotBrandThemeExpandedChanged: (Boolean) -> Unit,
    val isScarletCarnageBrandThemeExpanded: Boolean,
    val onScarletCarnageBrandThemeExpandedChanged: (Boolean) -> Unit,
    val isExquisiteFallBrandThemeExpanded: Boolean,
    val onExquisiteFallBrandThemeExpandedChanged: (Boolean) -> Unit,
    val isLabyrinthOfMutabilityBrandThemeExpanded: Boolean,
    val onLabyrinthOfMutabilityBrandThemeExpandedChanged: (Boolean) -> Unit,
)

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

private class ConfigThemeAppearanceDialogState(
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

@Composable
internal fun ConfigThemeAppearanceSection(
    selectedThemeStyle: ThemeStyleOption,
    onThemeStyleSelected: (ThemeStyleOption) -> Unit,
    selectedBrandTheme: BrandThemeOption,
    onBrandThemeSelected: (BrandThemeOption) -> Unit,
    customBrandThemes: List<BrandThemeOption>,
    customBrandThemePresets: List<CustomBrandThemeSettings>,
    onCustomBrandThemeSaved: (CustomBrandThemeSettings, String?) -> Unit,
    onCustomBrandThemeDeleted: (String) -> Unit,
    onCustomBrandThemesImported: (List<CustomBrandThemeSettings>) -> Unit,
    onCustomBrandThemesReordered: (Int, Int) -> Unit,
    customMaterialThemePresets: List<CustomBrandThemeSettings>,
    customMaterialThemeSettings: CustomBrandThemeSettings,
    onCustomMaterialThemeSaved: (CustomBrandThemeSettings, String?) -> Unit,
    onCustomMaterialThemeDeleted: (String) -> Unit,
    onCustomMaterialThemesImported: (List<CustomBrandThemeSettings>) -> Unit,
    onCustomMaterialThemesReordered: (Int, Int) -> Unit,
    onCreateCustomMaterialTheme: () -> Unit,
    selectedThemeMode: ThemeModeOption,
    onThemeModeSelected: (ThemeModeOption) -> Unit,
    isExpanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    isCustomMaterialThemeExpanded: Boolean,
    onCustomMaterialThemeExpandedChanged: (Boolean) -> Unit,
    isBuiltInMaterialPalettesExpanded: Boolean,
    onBuiltInMaterialPalettesExpandedChanged: (Boolean) -> Unit,
    isMaterialRedsPaletteExpanded: Boolean,
    onMaterialRedsPaletteExpandedChanged: (Boolean) -> Unit,
    isMaterialOrangesPaletteExpanded: Boolean,
    onMaterialOrangesPaletteExpandedChanged: (Boolean) -> Unit,
    isMaterialYellowsPaletteExpanded: Boolean,
    onMaterialYellowsPaletteExpandedChanged: (Boolean) -> Unit,
    isMaterialGreensPaletteExpanded: Boolean,
    onMaterialGreensPaletteExpandedChanged: (Boolean) -> Unit,
    isMaterialBluesPaletteExpanded: Boolean,
    onMaterialBluesPaletteExpandedChanged: (Boolean) -> Unit,
    isMaterialPurplesPaletteExpanded: Boolean,
    onMaterialPurplesPaletteExpandedChanged: (Boolean) -> Unit,
    isMaterialNeutralsPaletteExpanded: Boolean,
    onMaterialNeutralsPaletteExpandedChanged: (Boolean) -> Unit,
    isCustomBrandThemeExpanded: Boolean,
    onCustomBrandThemeExpandedChanged: (Boolean) -> Unit,
    isSacredMachineBrandThemeExpanded: Boolean,
    onSacredMachineBrandThemeExpandedChanged: (Boolean) -> Unit,
    isAncientDynastyBrandThemeExpanded: Boolean,
    onAncientDynastyBrandThemeExpandedChanged: (Boolean) -> Unit,
    isImmortalRotBrandThemeExpanded: Boolean,
    onImmortalRotBrandThemeExpandedChanged: (Boolean) -> Unit,
    isScarletCarnageBrandThemeExpanded: Boolean,
    onScarletCarnageBrandThemeExpandedChanged: (Boolean) -> Unit,
    isExquisiteFallBrandThemeExpanded: Boolean,
    onExquisiteFallBrandThemeExpandedChanged: (Boolean) -> Unit,
    isLabyrinthOfMutabilityBrandThemeExpanded: Boolean,
    onLabyrinthOfMutabilityBrandThemeExpandedChanged: (Boolean) -> Unit,
    selectedPalette: PaletteOption,
    onPaletteSelected: (PaletteOption) -> Unit,
    materialPalettes: List<PaletteOption>,
    brandThemes: List<BrandThemeOption>,
    accentTokens: AppThemeAccentTokens,
) {
    val editCustomMaterialLabel = stringResource(R.string.config_custom_brand_theme_edit)
    val dialogState = rememberConfigThemeAppearanceDialogState()
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val copyConfigLabel = stringResource(R.string.config_custom_brand_theme_copy_config)
    val copyAllConfigLabel = stringResource(R.string.config_custom_theme_copy_all)
    val addCustomLabel = stringResource(R.string.config_custom_brand_theme_add)
    val importCustomLabel = stringResource(R.string.config_custom_brand_theme_import)
    val renderState =
        rememberConfigThemeAppearanceSectionRenderState(
            selectedThemeStyle = selectedThemeStyle,
            selectedPalette = selectedPalette,
            materialPalettes = materialPalettes,
            customMaterialThemePresets = customMaterialThemePresets,
            editCustomMaterialLabel = editCustomMaterialLabel,
            addCustomLabel = addCustomLabel,
            importCustomLabel = importCustomLabel,
            onCreateCustomMaterialTheme = onCreateCustomMaterialTheme,
            onCustomMaterialThemesReordered = onCustomMaterialThemesReordered,
            onOpenCustomMaterialThemeDialog = dialogState::openCustomMaterialThemeDialog,
            onOpenCustomMaterialThemeImportDialog = dialogState::openCustomMaterialThemeImportDialog,
            onCopyAllCustomMaterialThemes = {
                coroutineScope.launch {
                    clipboard.setClipEntry(
                        ClipEntry(
                            ClipData.newPlainText(
                                "FlipBits custom material color config",
                                customMaterialThemePresets.toMaterialBatchConfigText(),
                            ),
                        ),
                    )
                }
            },
            copyAllConfigLabel = copyAllConfigLabel,
            brandThemes = brandThemes,
            selectedBrandTheme = selectedBrandTheme,
            customBrandThemePresets = customBrandThemePresets,
        )
    val brandThemeExpansionState =
        ConfigThemeAppearanceBrandThemeExpansionState(
            isCustomBrandThemeExpanded = isCustomBrandThemeExpanded,
            onCustomBrandThemeExpandedChanged = onCustomBrandThemeExpandedChanged,
            isSacredMachineBrandThemeExpanded = isSacredMachineBrandThemeExpanded,
            onSacredMachineBrandThemeExpandedChanged = onSacredMachineBrandThemeExpandedChanged,
            isAncientDynastyBrandThemeExpanded = isAncientDynastyBrandThemeExpanded,
            onAncientDynastyBrandThemeExpandedChanged = onAncientDynastyBrandThemeExpandedChanged,
            isImmortalRotBrandThemeExpanded = isImmortalRotBrandThemeExpanded,
            onImmortalRotBrandThemeExpandedChanged = onImmortalRotBrandThemeExpandedChanged,
            isScarletCarnageBrandThemeExpanded = isScarletCarnageBrandThemeExpanded,
            onScarletCarnageBrandThemeExpandedChanged = onScarletCarnageBrandThemeExpandedChanged,
            isExquisiteFallBrandThemeExpanded = isExquisiteFallBrandThemeExpanded,
            onExquisiteFallBrandThemeExpandedChanged = onExquisiteFallBrandThemeExpandedChanged,
            isLabyrinthOfMutabilityBrandThemeExpanded = isLabyrinthOfMutabilityBrandThemeExpanded,
            onLabyrinthOfMutabilityBrandThemeExpandedChanged = onLabyrinthOfMutabilityBrandThemeExpandedChanged,
        )
    val materialPaletteExpansionState =
        ConfigMaterialPaletteExpansionState(
            isCustomExpanded = isCustomMaterialThemeExpanded,
            onCustomExpandedChanged = onCustomMaterialThemeExpandedChanged,
            isBuiltInExpanded = isBuiltInMaterialPalettesExpanded,
            onBuiltInExpandedChanged = onBuiltInMaterialPalettesExpandedChanged,
            isRedsExpanded = isMaterialRedsPaletteExpanded,
            onRedsExpandedChanged = onMaterialRedsPaletteExpandedChanged,
            isOrangesExpanded = isMaterialOrangesPaletteExpanded,
            onOrangesExpandedChanged = onMaterialOrangesPaletteExpandedChanged,
            isYellowsExpanded = isMaterialYellowsPaletteExpanded,
            onYellowsExpandedChanged = onMaterialYellowsPaletteExpandedChanged,
            isGreensExpanded = isMaterialGreensPaletteExpanded,
            onGreensExpandedChanged = onMaterialGreensPaletteExpandedChanged,
            isBluesExpanded = isMaterialBluesPaletteExpanded,
            onBluesExpandedChanged = onMaterialBluesPaletteExpandedChanged,
            isPurplesExpanded = isMaterialPurplesPaletteExpanded,
            onPurplesExpandedChanged = onMaterialPurplesPaletteExpandedChanged,
            isNeutralsExpanded = isMaterialNeutralsPaletteExpanded,
            onNeutralsExpandedChanged = onMaterialNeutralsPaletteExpandedChanged,
        )
    val copySelectedBrandThemeConfig: (BrandThemeOption) -> Unit =
        remember(clipboard, coroutineScope) {
            { option ->
                coroutineScope.launch {
                    copyCustomThemeConfig(
                        clipboard = clipboard,
                        settings =
                            CustomBrandThemeSettings(
                                displayName = option.titleOverride ?: option.id,
                                primaryHex = option.primaryColor.toHexString(),
                                secondaryHex = option.secondaryColor.toHexString(),
                                outlineHexOrNull = option.outlineColor.toHexString(),
                            ),
                    )
                }
            }
        }
    val copyAllCustomBrandThemeConfigs: () -> Unit =
        remember(clipboard, coroutineScope, customBrandThemePresets) {
            {
                coroutineScope.launch {
                    clipboard.setClipEntry(
                        ClipEntry(
                            ClipData.newPlainText(
                                "FlipBits custom theme config",
                                customBrandThemePresets.toBatchConfigText(),
                            ),
                        ),
                    )
                }
            }
        }
    AutoExpandSelectedBrandThemeSection(
        isBrandStyle = renderState.isBrandStyle,
        selectedBrandTheme = selectedBrandTheme,
        expansionState = brandThemeExpansionState,
    )
    ConfigThemeAppearanceDialogHost(
        dialogState = dialogState,
        selectedThemeMode = selectedThemeMode,
        customBrandThemePresets = customBrandThemePresets,
        onCustomBrandThemeSaved = onCustomBrandThemeSaved,
        onCustomBrandThemeDeleted = onCustomBrandThemeDeleted,
        customMaterialThemePresets = customMaterialThemePresets,
        customMaterialThemeSettings = customMaterialThemeSettings,
        onCustomMaterialThemeSaved = onCustomMaterialThemeSaved,
        onCustomMaterialThemeDeleted = onCustomMaterialThemeDeleted,
        onSingleMaterialThemeImported = { importedSettings ->
            val duplicatePresetId =
                findDuplicateImportedThemePresetId(
                    existing = customMaterialThemePresets,
                    imported = importedSettings,
                    mode = CustomThemeImportMode.Material,
                )
            if (duplicatePresetId == null) {
                onCustomMaterialThemeSaved(importedSettings, null)
            } else {
                dialogState.stageDuplicateImport(
                    importedSettings = importedSettings,
                    duplicatePresetId = duplicatePresetId,
                    mode = DuplicateImportMode.Material,
                )
            }
        },
        onSingleThemeImported = { importedSettings ->
            val duplicatePresetId =
                findDuplicateImportedThemePresetId(
                    existing = customBrandThemePresets,
                    imported = importedSettings,
                    mode = CustomThemeImportMode.DualTone,
                )
            if (duplicatePresetId == null) {
                onCustomBrandThemeSaved(importedSettings, null)
            } else {
                dialogState.stageDuplicateImport(
                    importedSettings = importedSettings,
                    duplicatePresetId = duplicatePresetId,
                    mode = DuplicateImportMode.Brand,
                )
            }
        },
        onBatchMaterialThemesImported = { importedSettings ->
            val preview =
                buildCustomBrandThemeBatchImportPreview(
                    existing = customMaterialThemePresets,
                    imported = importedSettings,
                    mode = DuplicateImportMode.Material,
                )
            if (preview.duplicateCount == 0) {
                onCustomMaterialThemesImported(importedSettings)
            } else {
                dialogState.stageBatchImport(preview)
            }
        },
        onBatchThemesImported = { importedSettings ->
            val preview =
                buildCustomBrandThemeBatchImportPreview(
                    existing = customBrandThemePresets,
                    imported = importedSettings,
                    mode = DuplicateImportMode.Brand,
                )
            if (preview.duplicateCount == 0) {
                onCustomBrandThemesImported(importedSettings)
            } else {
                dialogState.stageBatchImport(preview)
            }
        },
        selectedCustomBrandThemePreset = renderState.selectedCustomBrandThemePreset,
        clipboard = clipboard,
        coroutineScope = coroutineScope,
        onConfirmBatchImport = { preview ->
            when (preview.mode) {
                DuplicateImportMode.Brand -> onCustomBrandThemesImported(preview.importedSettings)
                DuplicateImportMode.Material -> onCustomMaterialThemesImported(preview.importedSettings)
            }
            dialogState.dismissBatchImportDialog()
        },
        onConfirmDuplicateOverwrite = { settings, presetId, mode ->
            when (mode) {
                DuplicateImportMode.Brand -> onCustomBrandThemeSaved(settings, presetId)
                DuplicateImportMode.Material -> onCustomMaterialThemeSaved(settings, presetId)
            }
            dialogState.dismissDuplicateImport()
        },
        onConfirmDuplicateAddCopy = { settings, mode ->
            when (mode) {
                DuplicateImportMode.Brand -> onCustomBrandThemeSaved(settings, null)
                DuplicateImportMode.Material -> onCustomMaterialThemeSaved(settings, null)
            }
            dialogState.dismissDuplicateImport()
        },
    )

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ExpandableCardHeader(
                accentTokens = accentTokens,
                title = stringResource(R.string.config_theme_appearance_title),
                subtitle = stringResource(R.string.config_theme_style_subtitle),
                expanded = isExpanded,
                onToggleExpanded = { onExpandedChanged(!isExpanded) },
                contentDescription =
                    stringResource(
                        if (isExpanded) {
                            R.string.config_theme_appearance_collapse
                        } else {
                            R.string.config_theme_appearance_expand
                        },
                    ),
            )

            if (isExpanded) {
                ConfigThemeAppearanceExpandedContent(
                    accentTokens = accentTokens,
                    selectedThemeStyle = selectedThemeStyle,
                    onThemeStyleSelected = onThemeStyleSelected,
                    isBrandStyle = renderState.isBrandStyle,
                    selectedThemeMode = selectedThemeMode,
                    onThemeModeSelected = onThemeModeSelected,
                    selectedPalette = selectedPalette,
                    onPaletteSelected = onPaletteSelected,
                    materialPaletteGroups = renderState.materialPaletteGroups,
                    onReorderCustomMaterialThemes = onCustomMaterialThemesReordered,
                    materialPaletteExpansionState = materialPaletteExpansionState,
                    selectedBrandTheme = selectedBrandTheme,
                    onBrandThemeSelected = onBrandThemeSelected,
                    customBrandThemes = customBrandThemes,
                    customBrandThemePresets = customBrandThemePresets,
                    brandThemeGroups = renderState.brandThemeGroups,
                    brandThemeExpansionState = brandThemeExpansionState,
                    selectedCustomBrandThemePreset = renderState.selectedCustomBrandThemePreset,
                    copyConfigLabel = copyConfigLabel,
                    copyAllConfigLabel = copyAllConfigLabel,
                    copySelectedBrandThemeConfig = copySelectedBrandThemeConfig,
                    copyAllCustomBrandThemeConfigs = copyAllCustomBrandThemeConfigs,
                    onReorderCustomBrandThemes = onCustomBrandThemesReordered,
                    onOpenCreateCustomBrandTheme = dialogState::openCreateCustomBrandTheme,
                    onOpenEditCustomBrandTheme = { option ->
                        dialogState.openEditCustomBrandTheme(
                            customBrandThemePresets
                                .firstOrNull { preset -> option.id == customBrandThemeOptionId(preset.presetId) }
                                ?.presetId,
                        )
                    },
                    onOpenImportCustomBrandTheme = dialogState::openCustomThemeImportDialog,
                )
            }
        }
    }
}

@Composable
private fun rememberConfigThemeAppearanceDialogState(): ConfigThemeAppearanceDialogState {
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
private fun rememberConfigThemeAppearanceSectionRenderState(
    selectedThemeStyle: ThemeStyleOption,
    selectedPalette: PaletteOption,
    materialPalettes: List<PaletteOption>,
    customMaterialThemePresets: List<CustomBrandThemeSettings>,
    editCustomMaterialLabel: String,
    addCustomLabel: String,
    importCustomLabel: String,
    onCreateCustomMaterialTheme: () -> Unit,
    onCustomMaterialThemesReordered: (Int, Int) -> Unit,
    onOpenCustomMaterialThemeDialog: () -> Unit,
    onOpenCustomMaterialThemeImportDialog: () -> Unit,
    onCopyAllCustomMaterialThemes: () -> Unit,
    copyAllConfigLabel: String,
    brandThemes: List<BrandThemeOption>,
    selectedBrandTheme: BrandThemeOption,
    customBrandThemePresets: List<CustomBrandThemeSettings>,
): ConfigThemeAppearanceSectionRenderState {
    val isBrandStyle = selectedThemeStyle == ThemeStyleOption.BrandDualTone
    val isCustomMaterialSelected = !isBrandStyle && isCustomMaterialPaletteId(selectedPalette.id)
    val materialPaletteGroups =
        remember(
            materialPalettes,
            customMaterialThemePresets,
            isCustomMaterialSelected,
            editCustomMaterialLabel,
            copyAllConfigLabel,
        ) {
            PaletteFamily.entries.mapNotNull { family ->
                if (family == PaletteFamily.Brand) {
                    return@mapNotNull null
                }
                val options =
                    when (family) {
                        PaletteFamily.Custom -> customMaterialThemePresets.map(::customMaterialPalette)
                        else -> materialPalettes.filter { it.family == family }
                    }
                if (options.isEmpty()) {
                    null
                } else {
                    PaletteGroupUi(
                        family = family,
                        options = options,
                        onAddOption =
                            if (family == PaletteFamily.Custom) {
                                onCreateCustomMaterialTheme
                            } else {
                                null
                            },
                        addActionLabel =
                            if (family == PaletteFamily.Custom) {
                                addCustomLabel
                            } else {
                                null
                            },
                        onEditOption =
                            if (family == PaletteFamily.Custom && isCustomMaterialSelected) {
                                onOpenCustomMaterialThemeDialog
                            } else {
                                null
                            },
                        editActionLabel =
                            if (family == PaletteFamily.Custom && isCustomMaterialSelected) {
                                editCustomMaterialLabel
                            } else {
                                null
                            },
                        iconActions =
                            if (family == PaletteFamily.Custom) {
                                listOf(
                                    PaletteGroupIconAction(
                                        label = importCustomLabel,
                                        icon = Icons.AutoMirrored.Rounded.Input,
                                        onClick = onOpenCustomMaterialThemeImportDialog,
                                    ),
                                    PaletteGroupIconAction(
                                        label = copyAllConfigLabel,
                                        icon = Icons.Rounded.ContentCopy,
                                        enabled = customMaterialThemePresets.isNotEmpty(),
                                        onClick = onCopyAllCustomMaterialThemes,
                                    ),
                                )
                            } else {
                                emptyList()
                            },
                        onMoveOption =
                            if (family == PaletteFamily.Custom) {
                                onCustomMaterialThemesReordered
                            } else {
                                null
                            },
                    )
                }
            }
        }
    val brandThemeGroups = rememberConfigThemeAppearanceBrandThemeGroups(brandThemes)
    val selectedCustomBrandThemePreset =
        remember(selectedBrandTheme.id, customBrandThemePresets) {
            customBrandThemePresets.firstOrNull { preset ->
                selectedBrandTheme.id == customBrandThemeOptionId(preset.presetId)
            }
        }
    return ConfigThemeAppearanceSectionRenderState(
        isBrandStyle = isBrandStyle,
        materialPaletteGroups = materialPaletteGroups,
        brandThemeGroups = brandThemeGroups,
        selectedCustomBrandThemePreset = selectedCustomBrandThemePreset,
    )
}

@Composable
private fun rememberConfigThemeAppearanceBrandThemeGroups(brandThemes: List<BrandThemeOption>): ConfigThemeAppearanceBrandThemeGroups =
    remember(brandThemes) {
        ConfigThemeAppearanceBrandThemeGroups(
            sacredMachineThemes =
                brandThemes.filter {
                    it.groupTitleResId == R.string.config_dual_tone_group_sacred_machine
                },
            ancientDynastyThemes =
                brandThemes.filter {
                    it.groupTitleResId == R.string.config_dual_tone_group_ancient_dynasty
                },
            immortalRotThemes =
                brandThemes.filter {
                    it.groupTitleResId == R.string.config_dual_tone_group_immortal_rot
                },
            scarletCarnageThemes =
                brandThemes.filter {
                    it.groupTitleResId == R.string.config_dual_tone_group_scarlet_carnage
                },
            exquisiteFallThemes =
                brandThemes.filter {
                    it.groupTitleResId == R.string.config_dual_tone_group_exquisite_fall
                },
            labyrinthOfMutabilityThemes =
                brandThemes.filter {
                    it.groupTitleResId == R.string.config_dual_tone_group_labyrinth_of_mutability
                },
        )
    }

@Composable
private fun AutoExpandSelectedBrandThemeSection(
    isBrandStyle: Boolean,
    selectedBrandTheme: BrandThemeOption,
    expansionState: ConfigThemeAppearanceBrandThemeExpansionState,
) {
    LaunchedEffect(isBrandStyle, selectedBrandTheme.id, selectedBrandTheme.groupTitleResId) {
        if (!isBrandStyle) {
            return@LaunchedEffect
        }
        expansionState.ensureSelectedGroupExpanded(selectedBrandTheme.groupTitleResId)
    }
}

@Composable
private fun ConfigThemeAppearanceDialogHost(
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
    coroutineScope: kotlinx.coroutines.CoroutineScope,
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
private fun ConfigThemeAppearanceExpandedContent(
    accentTokens: AppThemeAccentTokens,
    selectedThemeStyle: ThemeStyleOption,
    onThemeStyleSelected: (ThemeStyleOption) -> Unit,
    isBrandStyle: Boolean,
    selectedThemeMode: ThemeModeOption,
    onThemeModeSelected: (ThemeModeOption) -> Unit,
    selectedPalette: PaletteOption,
    onPaletteSelected: (PaletteOption) -> Unit,
    materialPaletteGroups: List<PaletteGroupUi>,
    onReorderCustomMaterialThemes: (Int, Int) -> Unit,
    materialPaletteExpansionState: ConfigMaterialPaletteExpansionState,
    selectedBrandTheme: BrandThemeOption,
    onBrandThemeSelected: (BrandThemeOption) -> Unit,
    customBrandThemes: List<BrandThemeOption>,
    customBrandThemePresets: List<CustomBrandThemeSettings>,
    brandThemeGroups: ConfigThemeAppearanceBrandThemeGroups,
    brandThemeExpansionState: ConfigThemeAppearanceBrandThemeExpansionState,
    selectedCustomBrandThemePreset: CustomBrandThemeSettings?,
    copyConfigLabel: String,
    copyAllConfigLabel: String,
    copySelectedBrandThemeConfig: (BrandThemeOption) -> Unit,
    copyAllCustomBrandThemeConfigs: () -> Unit,
    onReorderCustomBrandThemes: (Int, Int) -> Unit,
    onOpenCreateCustomBrandTheme: () -> Unit,
    onOpenEditCustomBrandTheme: (BrandThemeOption) -> Unit,
    onOpenImportCustomBrandTheme: () -> Unit,
) {
    ThemeStyleOption.entries.forEach { option ->
        SelectionRow(
            accentTokens = accentTokens,
            label = stringResource(option.labelResId),
            selected = option == selectedThemeStyle,
            onClick = { onThemeStyleSelected(option) },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(R.string.config_theme_mode_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.config_theme_mode_subtitle),
            style = MaterialTheme.typography.bodySmall,
        )
        if (isBrandStyle) {
            Text(
                text = stringResource(R.string.config_theme_mode_brand_fixed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (!isBrandStyle) {
        ConfigThemeAppearanceMaterialContent(
            accentTokens = accentTokens,
            selectedThemeMode = selectedThemeMode,
            onThemeModeSelected = onThemeModeSelected,
            selectedPalette = selectedPalette,
            onPaletteSelected = onPaletteSelected,
            materialPaletteGroups = materialPaletteGroups,
            onReorderCustomMaterialThemes = onReorderCustomMaterialThemes,
            materialPaletteExpansionState = materialPaletteExpansionState,
        )
    } else {
        ConfigThemeAppearanceBrandContent(
            accentTokens = accentTokens,
            selectedThemeMode = selectedThemeMode,
            onThemeModeSelected = onThemeModeSelected,
            selectedBrandTheme = selectedBrandTheme,
            onBrandThemeSelected = onBrandThemeSelected,
            customBrandThemes = customBrandThemes,
            customBrandThemePresets = customBrandThemePresets,
            brandThemeGroups = brandThemeGroups,
            brandThemeExpansionState = brandThemeExpansionState,
            selectedCustomBrandThemePreset = selectedCustomBrandThemePreset,
            copyConfigLabel = copyConfigLabel,
            copyAllConfigLabel = copyAllConfigLabel,
            copySelectedBrandThemeConfig = copySelectedBrandThemeConfig,
            copyAllCustomBrandThemeConfigs = copyAllCustomBrandThemeConfigs,
            onReorderCustomBrandThemes = onReorderCustomBrandThemes,
            onOpenCreateCustomBrandTheme = onOpenCreateCustomBrandTheme,
            onOpenEditCustomBrandTheme = onOpenEditCustomBrandTheme,
            onOpenImportCustomBrandTheme = onOpenImportCustomBrandTheme,
        )
    }
}

@Composable
private fun ConfigThemeAppearanceMaterialContent(
    accentTokens: AppThemeAccentTokens,
    selectedThemeMode: ThemeModeOption,
    onThemeModeSelected: (ThemeModeOption) -> Unit,
    selectedPalette: PaletteOption,
    onPaletteSelected: (PaletteOption) -> Unit,
    materialPaletteGroups: List<PaletteGroupUi>,
    onReorderCustomMaterialThemes: (Int, Int) -> Unit,
    materialPaletteExpansionState: ConfigMaterialPaletteExpansionState,
) {
    val customPaletteGroup =
        materialPaletteGroups.firstOrNull { it.family == PaletteFamily.Custom }
    val builtInPaletteGroups =
        materialPaletteGroups.filterNot { it.family == PaletteFamily.Custom }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        ThemeModeOption.entries.forEach { option ->
            SelectionRow(
                accentTokens = accentTokens,
                label = stringResource(option.labelResId),
                selected = option == selectedThemeMode,
                onClick = { onThemeModeSelected(option) },
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.config_palette_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            customPaletteGroup?.let { group ->
                MaterialPaletteSectionHeader(
                    title = stringResource(R.string.config_palette_custom_section_title),
                    subtitle = stringResource(R.string.config_palette_custom_section_subtitle),
                    expanded = materialPaletteExpansionState.isCustomExpanded,
                    onExpandedChanged = materialPaletteExpansionState.onCustomExpandedChanged,
                )
                PaletteGroupSection(
                    accentTokens = accentTokens,
                    group = group.copy(onMoveOption = onReorderCustomMaterialThemes),
                    selectedPalette = selectedPalette,
                    onPaletteSelected = onPaletteSelected,
                    expanded = materialPaletteExpansionState.isCustomExpanded,
                )
            }
            if (builtInPaletteGroups.isNotEmpty()) {
                MaterialPaletteSectionHeader(
                    title = stringResource(R.string.config_palette_builtin_section_title),
                    subtitle = stringResource(R.string.config_palette_builtin_section_subtitle),
                    expanded = materialPaletteExpansionState.isBuiltInExpanded,
                    onExpandedChanged = materialPaletteExpansionState.onBuiltInExpandedChanged,
                )
                if (materialPaletteExpansionState.isBuiltInExpanded) {
                    builtInPaletteGroups.forEach { group ->
                        PaletteGroupSection(
                            accentTokens = accentTokens,
                            group = group,
                            selectedPalette = selectedPalette,
                            onPaletteSelected = onPaletteSelected,
                            expanded = materialPaletteExpansionState.isExpanded(group.family),
                            onExpandedChanged = materialPaletteExpansionState.onExpandedChanged(group.family),
                        )
                    }
                }
            }
        }
        Text(
            text =
                if (isCustomMaterialPaletteId(selectedPalette.id)) {
                    selectedPalette.titleOverride ?: stringResource(R.string.palette_family_custom)
                } else {
                    "${stringResource(selectedPalette.family.titleResId)} · " +
                        (selectedPalette.titleOverride ?: stringResource(selectedPalette.titleResId))
                },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun MaterialPaletteSectionHeader(
    title: String,
    subtitle: String,
    expanded: Boolean? = null,
    onExpandedChanged: ((Boolean) -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded != null && onExpandedChanged != null) {
            IconButton(
                onClick = { onExpandedChanged(!expanded) },
                colors = utilityActionIconButtonColors(),
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription =
                        if (expanded) {
                            stringResource(R.string.config_palette_collapse)
                        } else {
                            stringResource(R.string.config_palette_expand)
                        },
                )
            }
        }
    }
}

@Composable
private fun ConfigThemeAppearanceBrandContent(
    accentTokens: AppThemeAccentTokens,
    selectedThemeMode: ThemeModeOption,
    onThemeModeSelected: (ThemeModeOption) -> Unit,
    selectedBrandTheme: BrandThemeOption,
    onBrandThemeSelected: (BrandThemeOption) -> Unit,
    customBrandThemes: List<BrandThemeOption>,
    customBrandThemePresets: List<CustomBrandThemeSettings>,
    brandThemeGroups: ConfigThemeAppearanceBrandThemeGroups,
    brandThemeExpansionState: ConfigThemeAppearanceBrandThemeExpansionState,
    selectedCustomBrandThemePreset: CustomBrandThemeSettings?,
    copyConfigLabel: String,
    copyAllConfigLabel: String,
    copySelectedBrandThemeConfig: (BrandThemeOption) -> Unit,
    copyAllCustomBrandThemeConfigs: () -> Unit,
    onReorderCustomBrandThemes: (Int, Int) -> Unit,
    onOpenCreateCustomBrandTheme: () -> Unit,
    onOpenEditCustomBrandTheme: (BrandThemeOption) -> Unit,
    onOpenImportCustomBrandTheme: () -> Unit,
) {
    Column(
        modifier = Modifier.alpha(0.48f),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        ThemeModeOption.entries.forEach { option ->
            SelectionRow(
                accentTokens = accentTokens,
                label = stringResource(option.labelResId),
                selected = option == selectedThemeMode,
                onClick = { onThemeModeSelected(option) },
                enabled = false,
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.config_brand_theme_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.config_brand_theme_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BrandThemeSection(
                accentTokens = accentTokens,
                title = stringResource(R.string.config_dual_tone_group_custom),
                options = customBrandThemes,
                selectedBrandTheme = selectedBrandTheme,
                expanded = brandThemeExpansionState.isCustomBrandThemeExpanded,
                onExpandedChanged = brandThemeExpansionState.onCustomBrandThemeExpandedChanged,
                onBrandThemeSelected = onBrandThemeSelected,
                onEditBrandTheme = { onOpenEditCustomBrandTheme(it) },
                editSelectedOnly = true,
                editActionLabel = stringResource(R.string.config_custom_brand_theme_edit),
                onMoveOption =
                    if (brandThemeExpansionState.isCustomBrandThemeExpanded) {
                        onReorderCustomBrandThemes
                    } else {
                        null
                    },
                onEditActionClick =
                    selectedBrandTheme
                        .takeIf { selectedCustomBrandThemePreset != null }
                        ?.let { { onOpenEditCustomBrandTheme(it) } },
                actionLabel = stringResource(R.string.config_custom_brand_theme_add),
                onActionClick = onOpenCreateCustomBrandTheme,
                iconActions =
                    listOf(
                        BrandThemeSectionIconAction(
                            label = stringResource(R.string.config_custom_brand_theme_import),
                            icon = Icons.AutoMirrored.Rounded.Input,
                            onClick = onOpenImportCustomBrandTheme,
                        ),
                        BrandThemeSectionIconAction(
                            label = copyAllConfigLabel,
                            icon = Icons.Rounded.ContentCopy,
                            enabled = customBrandThemePresets.isNotEmpty(),
                            onClick = copyAllCustomBrandThemeConfigs,
                        ),
                    ),
                stackHeaderActions = true,
            )
            BrandThemeSection(
                accentTokens = accentTokens,
                title = stringResource(R.string.config_dual_tone_group_sacred_machine),
                options = brandThemeGroups.sacredMachineThemes,
                selectedBrandTheme = selectedBrandTheme,
                expanded = brandThemeExpansionState.isSacredMachineBrandThemeExpanded,
                onExpandedChanged = brandThemeExpansionState.onSacredMachineBrandThemeExpandedChanged,
                onBrandThemeSelected = onBrandThemeSelected,
                copyConfigLabel = copyConfigLabel,
                onCopyConfig = copySelectedBrandThemeConfig,
            )
            BrandThemeSection(
                accentTokens = accentTokens,
                title = stringResource(R.string.config_dual_tone_group_ancient_dynasty),
                options = brandThemeGroups.ancientDynastyThemes,
                selectedBrandTheme = selectedBrandTheme,
                expanded = brandThemeExpansionState.isAncientDynastyBrandThemeExpanded,
                onExpandedChanged = brandThemeExpansionState.onAncientDynastyBrandThemeExpandedChanged,
                onBrandThemeSelected = onBrandThemeSelected,
                copyConfigLabel = copyConfigLabel,
                onCopyConfig = copySelectedBrandThemeConfig,
            )
            BrandThemeSection(
                accentTokens = accentTokens,
                title = stringResource(R.string.config_dual_tone_group_scarlet_carnage),
                options = brandThemeGroups.scarletCarnageThemes,
                selectedBrandTheme = selectedBrandTheme,
                expanded = brandThemeExpansionState.isScarletCarnageBrandThemeExpanded,
                onExpandedChanged = brandThemeExpansionState.onScarletCarnageBrandThemeExpandedChanged,
                onBrandThemeSelected = onBrandThemeSelected,
                copyConfigLabel = copyConfigLabel,
                onCopyConfig = copySelectedBrandThemeConfig,
            )
            BrandThemeSection(
                accentTokens = accentTokens,
                title = stringResource(R.string.config_dual_tone_group_labyrinth_of_mutability),
                options = brandThemeGroups.labyrinthOfMutabilityThemes,
                selectedBrandTheme = selectedBrandTheme,
                expanded = brandThemeExpansionState.isLabyrinthOfMutabilityBrandThemeExpanded,
                onExpandedChanged = brandThemeExpansionState.onLabyrinthOfMutabilityBrandThemeExpandedChanged,
                onBrandThemeSelected = onBrandThemeSelected,
                copyConfigLabel = copyConfigLabel,
                onCopyConfig = copySelectedBrandThemeConfig,
            )
            BrandThemeSection(
                accentTokens = accentTokens,
                title = stringResource(R.string.config_dual_tone_group_immortal_rot),
                options = brandThemeGroups.immortalRotThemes,
                selectedBrandTheme = selectedBrandTheme,
                expanded = brandThemeExpansionState.isImmortalRotBrandThemeExpanded,
                onExpandedChanged = brandThemeExpansionState.onImmortalRotBrandThemeExpandedChanged,
                onBrandThemeSelected = onBrandThemeSelected,
                copyConfigLabel = copyConfigLabel,
                onCopyConfig = copySelectedBrandThemeConfig,
            )
            BrandThemeSection(
                accentTokens = accentTokens,
                title = stringResource(R.string.config_dual_tone_group_exquisite_fall),
                options = brandThemeGroups.exquisiteFallThemes,
                selectedBrandTheme = selectedBrandTheme,
                expanded = brandThemeExpansionState.isExquisiteFallBrandThemeExpanded,
                onExpandedChanged = brandThemeExpansionState.onExquisiteFallBrandThemeExpandedChanged,
                onBrandThemeSelected = onBrandThemeSelected,
                copyConfigLabel = copyConfigLabel,
                onCopyConfig = copySelectedBrandThemeConfig,
            )
        }
    }
}

private fun ConfigThemeAppearanceBrandThemeExpansionState.ensureSelectedGroupExpanded(groupTitleResId: Int) {
    when (groupTitleResId) {
        R.string.config_dual_tone_group_custom ->
            if (!isCustomBrandThemeExpanded) {
                onCustomBrandThemeExpandedChanged(true)
            }
        R.string.config_dual_tone_group_sacred_machine ->
            if (!isSacredMachineBrandThemeExpanded) {
                onSacredMachineBrandThemeExpandedChanged(true)
            }
        R.string.config_dual_tone_group_ancient_dynasty ->
            if (!isAncientDynastyBrandThemeExpanded) {
                onAncientDynastyBrandThemeExpandedChanged(true)
            }
        R.string.config_dual_tone_group_immortal_rot ->
            if (!isImmortalRotBrandThemeExpanded) {
                onImmortalRotBrandThemeExpandedChanged(true)
            }
        R.string.config_dual_tone_group_scarlet_carnage ->
            if (!isScarletCarnageBrandThemeExpanded) {
                onScarletCarnageBrandThemeExpandedChanged(true)
            }
        R.string.config_dual_tone_group_exquisite_fall ->
            if (!isExquisiteFallBrandThemeExpanded) {
                onExquisiteFallBrandThemeExpandedChanged(true)
            }
        R.string.config_dual_tone_group_labyrinth_of_mutability ->
            if (!isLabyrinthOfMutabilityBrandThemeExpanded) {
                onLabyrinthOfMutabilityBrandThemeExpandedChanged(true)
            }
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
                com.bag.audioandroid.ui.model.CustomThemeImportMode.DualTone ->
                    context.getString(
                        R.string.config_custom_theme_import_error_wrong_target_dual_tone,
                        error.blockIndex,
                    )
                com.bag.audioandroid.ui.model.CustomThemeImportMode.Material ->
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
    coroutineScope: kotlinx.coroutines.CoroutineScope,
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
                mode =
                    when (mode) {
                        DuplicateImportMode.Brand -> CustomThemeImportMode.DualTone
                        DuplicateImportMode.Material -> CustomThemeImportMode.Material
                    },
            ) != null
        }
    return CustomBrandThemeBatchImportPreview(
        mode = mode,
        importedSettings = imported,
        duplicateCount = duplicateCount,
    )
}

private fun androidx.compose.ui.graphics.Color.toHexString(): String {
    val red = (this.red * 255f).roundToInt().coerceIn(0, 255)
    val green = (this.green * 255f).roundToInt().coerceIn(0, 255)
    val blue = (this.blue * 255f).roundToInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", red, green, blue)
}
