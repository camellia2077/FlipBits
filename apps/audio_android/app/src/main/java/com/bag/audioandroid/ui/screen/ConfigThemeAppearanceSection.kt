package com.bag.audioandroid.ui.screen

import android.content.ClipData
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.BrandThemeExportEntry
import com.bag.audioandroid.ui.model.BrandThemeOption
import com.bag.audioandroid.ui.model.CustomBrandThemeSettings
import com.bag.audioandroid.ui.model.PaletteFamily
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.toBatchConfigText
import com.bag.audioandroid.ui.model.toBrandThemeExportBatchConfigText
import com.bag.audioandroid.ui.model.toMaterialBatchConfigText
import com.bag.audioandroid.ui.theme.AppThemeAccentTokens
import com.bag.audioandroid.ui.theme.customBrandThemeOptionId
import com.bag.audioandroid.ui.theme.customMaterialPalette
import com.bag.audioandroid.ui.theme.customMaterialPaletteId
import com.bag.audioandroid.ui.theme.isCustomMaterialPaletteId
import com.bag.audioandroid.ui.utilityActionIconButtonColors
import kotlinx.coroutines.CoroutineScope
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

private data class BrandThemeBuiltInSectionUi(
    val titleResId: Int,
    val options: List<BrandThemeOption>,
    val expanded: Boolean,
    val onExpandedChanged: (Boolean) -> Unit,
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
    val deleteCustomLabel = stringResource(R.string.config_custom_brand_theme_delete)
    val renderState =
        rememberConfigThemeAppearanceSectionRenderState(
            selectedThemeStyle = selectedThemeStyle,
            selectedPalette = selectedPalette,
            materialPalettes = materialPalettes,
            customMaterialThemePresets = customMaterialThemePresets,
            editCustomMaterialLabel = editCustomMaterialLabel,
            addCustomLabel = addCustomLabel,
            importCustomLabel = importCustomLabel,
            deleteCustomLabel = deleteCustomLabel,
            onCreateCustomMaterialTheme = onCreateCustomMaterialTheme,
            onCustomMaterialThemesReordered = onCustomMaterialThemesReordered,
            onCustomMaterialThemeDeleted = onCustomMaterialThemeDeleted,
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
    val localizedBuiltInBrandThemeNames =
        mapOf(
            "mars_relic" to stringResource(R.string.brand_theme_mars_relic_title),
            "scarlet_guard" to stringResource(R.string.brand_theme_scarlet_guard_title),
            "black_crimson_rite" to stringResource(R.string.brand_theme_black_crimson_rite_title),
            "xeno_code" to stringResource(R.string.brand_theme_xeno_code_title),
            "blood_soaked_ivory" to stringResource(R.string.brand_theme_scarlet_carnage_title),
            "brass_forge" to stringResource(R.string.brand_theme_brass_forge_title),
            "fires_of_fate" to stringResource(R.string.brand_theme_fires_of_fate_title),
            "arcane_abyss" to stringResource(R.string.brand_theme_labyrinth_of_mutability_title),
            "ecstatic_rapture" to stringResource(R.string.brand_theme_ecstatic_rapture_title),
            "velvet_nightmare" to stringResource(R.string.brand_theme_exquisite_fall_title),
            "toxic_effluence" to stringResource(R.string.brand_theme_immortal_rot_title),
            "plague_mire" to stringResource(R.string.brand_theme_plague_mire_title),
            "dynasty_revival" to stringResource(R.string.brand_theme_dynasty_revival_title),
            "sepulcher_cyan" to stringResource(R.string.brand_theme_sepulcher_cyan_title),
            "ancient_alloy" to stringResource(R.string.brand_theme_ancient_alloy_title),
            "tomb_sigil" to stringResource(R.string.brand_theme_tomb_sigil_title),
        )
    val toLocalizedBrandThemeExportEntry: (BrandThemeOption) -> BrandThemeExportEntry =
        { option ->
            BrandThemeExportEntry(
                displayName =
                    option.titleOverride
                        ?: localizedBuiltInBrandThemeNames[option.id]
                        ?: option.id,
                primaryHex = option.primaryColor.toHexString(),
                secondaryHex = option.secondaryColor.toHexString(),
                outlineHexOrNull = option.outlineColor.toHexString(),
            )
        }
    val copySelectedBrandThemeConfig: (BrandThemeOption) -> Unit =
        remember(clipboard, coroutineScope, localizedBuiltInBrandThemeNames) {
            { option ->
                coroutineScope.launch {
                    copyCustomThemeConfig(
                        clipboard = clipboard,
                        settings =
                            CustomBrandThemeSettings(
                                displayName =
                                    option.titleOverride
                                        ?: localizedBuiltInBrandThemeNames[option.id]
                                        ?: option.id,
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
            dialogState.stageSingleImportedTheme(
                existing = customMaterialThemePresets,
                imported = importedSettings,
                mode = DuplicateImportMode.Material,
                onSave = onCustomMaterialThemeSaved,
            )
        },
        onSingleThemeImported = { importedSettings ->
            dialogState.stageSingleImportedTheme(
                existing = customBrandThemePresets,
                imported = importedSettings,
                mode = DuplicateImportMode.Brand,
                onSave = onCustomBrandThemeSaved,
            )
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
            confirmBatchThemeImport(
                preview = preview,
                onBrandImport = onCustomBrandThemesImported,
                onMaterialImport = onCustomMaterialThemesImported,
            )
            dialogState.dismissBatchImportDialog()
        },
        onConfirmDuplicateOverwrite = { settings, presetId, mode ->
            saveImportedTheme(
                settings = settings,
                presetId = presetId,
                mode = mode,
                onBrandSave = onCustomBrandThemeSaved,
                onMaterialSave = onCustomMaterialThemeSaved,
            )
            dialogState.dismissDuplicateImport()
        },
        onConfirmDuplicateAddCopy = { settings, mode ->
            saveImportedTheme(
                settings = settings,
                presetId = null,
                mode = mode,
                onBrandSave = onCustomBrandThemeSaved,
                onMaterialSave = onCustomMaterialThemeSaved,
            )
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
                    localizedBuiltInBrandThemeNames = localizedBuiltInBrandThemeNames,
                    clipboard = clipboard,
                    coroutineScope = coroutineScope,
                    onReorderCustomBrandThemes = onCustomBrandThemesReordered,
                    onDeleteCustomBrandTheme = { option ->
                        customBrandThemePresets
                            .firstOrNull { preset -> option.id == customBrandThemeOptionId(preset.presetId) }
                            ?.presetId
                            ?.let(onCustomBrandThemeDeleted)
                    },
                    canDeleteCustomBrandTheme = { _: BrandThemeOption -> customBrandThemePresets.size > 1 },
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
private fun rememberConfigThemeAppearanceSectionRenderState(
    selectedThemeStyle: ThemeStyleOption,
    selectedPalette: PaletteOption,
    materialPalettes: List<PaletteOption>,
    customMaterialThemePresets: List<CustomBrandThemeSettings>,
    editCustomMaterialLabel: String,
    addCustomLabel: String,
    importCustomLabel: String,
    deleteCustomLabel: String,
    onCreateCustomMaterialTheme: () -> Unit,
    onCustomMaterialThemesReordered: (Int, Int) -> Unit,
    onCustomMaterialThemeDeleted: (String) -> Unit,
    onOpenCustomMaterialThemeDialog: () -> Unit,
    onOpenCustomMaterialThemeImportDialog: () -> Unit,
    onCopyAllCustomMaterialThemes: () -> Unit,
    copyAllConfigLabel: String,
    brandThemes: List<BrandThemeOption>,
    selectedBrandTheme: BrandThemeOption,
    customBrandThemePresets: List<CustomBrandThemeSettings>,
): ConfigThemeAppearanceSectionRenderState {
    val isBrandStyle = selectedThemeStyle == ThemeStyleOption.BrandDualTone
    val materialPaletteGroups =
        rememberMaterialPaletteGroups(
            isBrandStyle = isBrandStyle,
            selectedPalette = selectedPalette,
            materialPalettes = materialPalettes,
            customMaterialThemePresets = customMaterialThemePresets,
            editCustomMaterialLabel = editCustomMaterialLabel,
            addCustomLabel = addCustomLabel,
            importCustomLabel = importCustomLabel,
            deleteCustomLabel = deleteCustomLabel,
            copyAllConfigLabel = copyAllConfigLabel,
            onCreateCustomMaterialTheme = onCreateCustomMaterialTheme,
            onCustomMaterialThemesReordered = onCustomMaterialThemesReordered,
            onCustomMaterialThemeDeleted = onCustomMaterialThemeDeleted,
            onOpenCustomMaterialThemeDialog = onOpenCustomMaterialThemeDialog,
            onOpenCustomMaterialThemeImportDialog = onOpenCustomMaterialThemeImportDialog,
            onCopyAllCustomMaterialThemes = onCopyAllCustomMaterialThemes,
        )
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
private fun rememberMaterialPaletteGroups(
    isBrandStyle: Boolean,
    selectedPalette: PaletteOption,
    materialPalettes: List<PaletteOption>,
    customMaterialThemePresets: List<CustomBrandThemeSettings>,
    editCustomMaterialLabel: String,
    addCustomLabel: String,
    importCustomLabel: String,
    deleteCustomLabel: String,
    copyAllConfigLabel: String,
    onCreateCustomMaterialTheme: () -> Unit,
    onCustomMaterialThemesReordered: (Int, Int) -> Unit,
    onCustomMaterialThemeDeleted: (String) -> Unit,
    onOpenCustomMaterialThemeDialog: () -> Unit,
    onOpenCustomMaterialThemeImportDialog: () -> Unit,
    onCopyAllCustomMaterialThemes: () -> Unit,
): List<PaletteGroupUi> {
    val isCustomMaterialSelected = !isBrandStyle && isCustomMaterialPaletteId(selectedPalette.id)
    return remember(
        materialPalettes,
        customMaterialThemePresets,
        isCustomMaterialSelected,
        editCustomMaterialLabel,
        addCustomLabel,
        importCustomLabel,
        deleteCustomLabel,
        copyAllConfigLabel,
        onCreateCustomMaterialTheme,
        onCustomMaterialThemesReordered,
        onCustomMaterialThemeDeleted,
        onOpenCustomMaterialThemeDialog,
        onOpenCustomMaterialThemeImportDialog,
        onCopyAllCustomMaterialThemes,
    ) {
        PaletteFamily.entries.mapNotNull { family ->
            materialPaletteGroupOrNull(
                family = family,
                materialPalettes = materialPalettes,
                customMaterialThemePresets = customMaterialThemePresets,
                isCustomMaterialSelected = isCustomMaterialSelected,
                editCustomMaterialLabel = editCustomMaterialLabel,
                addCustomLabel = addCustomLabel,
                importCustomLabel = importCustomLabel,
                deleteCustomLabel = deleteCustomLabel,
                copyAllConfigLabel = copyAllConfigLabel,
                onCreateCustomMaterialTheme = onCreateCustomMaterialTheme,
                onCustomMaterialThemesReordered = onCustomMaterialThemesReordered,
                onCustomMaterialThemeDeleted = onCustomMaterialThemeDeleted,
                onOpenCustomMaterialThemeDialog = onOpenCustomMaterialThemeDialog,
                onOpenCustomMaterialThemeImportDialog = onOpenCustomMaterialThemeImportDialog,
                onCopyAllCustomMaterialThemes = onCopyAllCustomMaterialThemes,
            )
        }
    }
}

private fun materialPaletteGroupOrNull(
    family: PaletteFamily,
    materialPalettes: List<PaletteOption>,
    customMaterialThemePresets: List<CustomBrandThemeSettings>,
    isCustomMaterialSelected: Boolean,
    editCustomMaterialLabel: String,
    addCustomLabel: String,
    importCustomLabel: String,
    deleteCustomLabel: String,
    copyAllConfigLabel: String,
    onCreateCustomMaterialTheme: () -> Unit,
    onCustomMaterialThemesReordered: (Int, Int) -> Unit,
    onCustomMaterialThemeDeleted: (String) -> Unit,
    onOpenCustomMaterialThemeDialog: () -> Unit,
    onOpenCustomMaterialThemeImportDialog: () -> Unit,
    onCopyAllCustomMaterialThemes: () -> Unit,
): PaletteGroupUi? {
    if (family == PaletteFamily.Brand) {
        return null
    }
    val options =
        when (family) {
            PaletteFamily.Custom -> customMaterialThemePresets.map(::customMaterialPalette)
            else -> materialPalettes.filter { it.family == family }
        }
    if (options.isEmpty()) {
        return null
    }
    if (family != PaletteFamily.Custom) {
        return PaletteGroupUi(family = family, options = options)
    }
    return PaletteGroupUi(
        family = family,
        options = options,
        onAddOption = onCreateCustomMaterialTheme,
        addActionLabel = addCustomLabel,
        onEditOption =
            if (isCustomMaterialSelected) {
                onOpenCustomMaterialThemeDialog
            } else {
                null
            },
        editActionLabel =
            if (isCustomMaterialSelected) {
                editCustomMaterialLabel
            } else {
                null
            },
        iconActions =
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
            ),
        onMoveOption = onCustomMaterialThemesReordered,
        deleteActionLabel = deleteCustomLabel,
        onDeleteOption = { option ->
            customMaterialThemePresets
                .firstOrNull { preset -> option.id == customMaterialPaletteId(preset.presetId) }
                ?.presetId
                ?.let(onCustomMaterialThemeDeleted)
        },
        canDeleteOption = { customMaterialThemePresets.size > 1 },
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
    localizedBuiltInBrandThemeNames: Map<String, String>,
    clipboard: Clipboard,
    coroutineScope: CoroutineScope,
    onReorderCustomBrandThemes: (Int, Int) -> Unit,
    onDeleteCustomBrandTheme: (BrandThemeOption) -> Unit,
    canDeleteCustomBrandTheme: (BrandThemeOption) -> Boolean,
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
            localizedBuiltInBrandThemeNames = localizedBuiltInBrandThemeNames,
            clipboard = clipboard,
            coroutineScope = coroutineScope,
            onReorderCustomBrandThemes = onReorderCustomBrandThemes,
            onDeleteCustomBrandTheme = onDeleteCustomBrandTheme,
            canDeleteCustomBrandTheme = canDeleteCustomBrandTheme,
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
    localizedBuiltInBrandThemeNames: Map<String, String>,
    clipboard: Clipboard,
    coroutineScope: CoroutineScope,
    onReorderCustomBrandThemes: (Int, Int) -> Unit,
    onDeleteCustomBrandTheme: (BrandThemeOption) -> Unit,
    canDeleteCustomBrandTheme: (BrandThemeOption) -> Boolean,
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
                deleteActionLabel = stringResource(R.string.config_custom_brand_theme_delete),
                onDeleteBrandTheme = onDeleteCustomBrandTheme,
                canDeleteBrandTheme = canDeleteCustomBrandTheme,
            )
            brandBuiltInThemeSections(
                groups = brandThemeGroups,
                expansionState = brandThemeExpansionState,
            ).forEach { section ->
                BrandThemeSection(
                    accentTokens = accentTokens,
                    title = stringResource(section.titleResId),
                    options = section.options,
                    selectedBrandTheme = selectedBrandTheme,
                    expanded = section.expanded,
                    onExpandedChanged = section.onExpandedChanged,
                    iconActions =
                        if (section.expanded) {
                            val exportText =
                                section.options
                                    .map { option ->
                                        BrandThemeExportEntry(
                                            displayName =
                                                option.titleOverride
                                                    ?: localizedBuiltInBrandThemeNames[option.id]
                                                    ?: option.id,
                                            primaryHex = option.primaryColor.toHexString(),
                                            secondaryHex = option.secondaryColor.toHexString(),
                                            outlineHexOrNull = option.outlineColor.toHexString(),
                                        )
                                    }.toBrandThemeExportBatchConfigText()
                            listOf(
                                BrandThemeSectionIconAction(
                                    label = copyAllConfigLabel,
                                    icon = Icons.Rounded.ContentCopy,
                                    enabled = section.options.isNotEmpty(),
                                    onClick = {
                                        coroutineScope.launch {
                                            clipboard.setClipEntry(
                                                ClipEntry(
                                                    ClipData.newPlainText(
                                                        "FlipBits built-in dual-tone config",
                                                        exportText,
                                                    ),
                                                ),
                                            )
                                        }
                                    },
                                ),
                            )
                        } else {
                            emptyList()
                        },
                    onBrandThemeSelected = onBrandThemeSelected,
                    copyConfigLabel = copyConfigLabel,
                    onCopyConfig = copySelectedBrandThemeConfig,
                    headerTitleColor = MaterialTheme.colorScheme.onSurface,
                    headerMetaColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun brandBuiltInThemeSections(
    groups: ConfigThemeAppearanceBrandThemeGroups,
    expansionState: ConfigThemeAppearanceBrandThemeExpansionState,
): List<BrandThemeBuiltInSectionUi> =
    listOf(
        BrandThemeBuiltInSectionUi(
            titleResId = R.string.config_dual_tone_group_sacred_machine,
            options = groups.sacredMachineThemes,
            expanded = expansionState.isSacredMachineBrandThemeExpanded,
            onExpandedChanged = expansionState.onSacredMachineBrandThemeExpandedChanged,
        ),
        BrandThemeBuiltInSectionUi(
            titleResId = R.string.config_dual_tone_group_ancient_dynasty,
            options = groups.ancientDynastyThemes,
            expanded = expansionState.isAncientDynastyBrandThemeExpanded,
            onExpandedChanged = expansionState.onAncientDynastyBrandThemeExpandedChanged,
        ),
        BrandThemeBuiltInSectionUi(
            titleResId = R.string.config_dual_tone_group_scarlet_carnage,
            options = groups.scarletCarnageThemes,
            expanded = expansionState.isScarletCarnageBrandThemeExpanded,
            onExpandedChanged = expansionState.onScarletCarnageBrandThemeExpandedChanged,
        ),
        BrandThemeBuiltInSectionUi(
            titleResId = R.string.config_dual_tone_group_labyrinth_of_mutability,
            options = groups.labyrinthOfMutabilityThemes,
            expanded = expansionState.isLabyrinthOfMutabilityBrandThemeExpanded,
            onExpandedChanged = expansionState.onLabyrinthOfMutabilityBrandThemeExpandedChanged,
        ),
        BrandThemeBuiltInSectionUi(
            titleResId = R.string.config_dual_tone_group_immortal_rot,
            options = groups.immortalRotThemes,
            expanded = expansionState.isImmortalRotBrandThemeExpanded,
            onExpandedChanged = expansionState.onImmortalRotBrandThemeExpandedChanged,
        ),
        BrandThemeBuiltInSectionUi(
            titleResId = R.string.config_dual_tone_group_exquisite_fall,
            options = groups.exquisiteFallThemes,
            expanded = expansionState.isExquisiteFallBrandThemeExpanded,
            onExpandedChanged = expansionState.onExquisiteFallBrandThemeExpandedChanged,
        ),
    )

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

private fun androidx.compose.ui.graphics.Color.toHexString(): String {
    val red = (this.red * 255f).roundToInt().coerceIn(0, 255)
    val green = (this.green * 255f).roundToInt().coerceIn(0, 255)
    val blue = (this.blue * 255f).roundToInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", red, green, blue)
}
