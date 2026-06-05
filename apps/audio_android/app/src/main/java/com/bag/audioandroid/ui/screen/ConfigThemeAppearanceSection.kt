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
import com.bag.audioandroid.ui.model.CustomFactionThemeSettings
import com.bag.audioandroid.ui.model.FactionThemeExportEntry
import com.bag.audioandroid.ui.model.FactionThemeOption
import com.bag.audioandroid.ui.model.PaletteFamily
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.toBatchConfigText
import com.bag.audioandroid.ui.model.toFactionThemeExportBatchConfigText
import com.bag.audioandroid.ui.model.toMaterialBatchConfigText
import com.bag.audioandroid.ui.theme.AppThemeAccentTokens
import com.bag.audioandroid.ui.theme.DefaultCustomMaterialPaletteSettings
import com.bag.audioandroid.ui.theme.customFactionThemeOptionId
import com.bag.audioandroid.ui.theme.customMaterialPalette
import com.bag.audioandroid.ui.theme.customMaterialPaletteId
import com.bag.audioandroid.ui.theme.isCustomMaterialPaletteId
import com.bag.audioandroid.ui.theme.normalizeCustomMaterialThemeSettings
import com.bag.audioandroid.ui.utilityActionIconButtonColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

private data class ConfigThemeAppearanceFactionThemeGroups(
    val sacredMachineThemes: List<FactionThemeOption>,
    val ancientDynastyThemes: List<FactionThemeOption>,
    val immortalRotThemes: List<FactionThemeOption>,
    val scarletCarnageThemes: List<FactionThemeOption>,
    val exquisiteFallThemes: List<FactionThemeOption>,
    val labyrinthOfMutabilityThemes: List<FactionThemeOption>,
)

private data class ConfigThemeAppearanceSectionRenderState(
    val isBrandStyle: Boolean,
    val materialPaletteGroups: List<PaletteGroupUi>,
    val factionThemeGroups: ConfigThemeAppearanceFactionThemeGroups,
    val selectedCustomFactionThemePreset: CustomFactionThemeSettings?,
)

private data class FactionThemeBuiltInSectionUi(
    val titleResId: Int,
    val options: List<FactionThemeOption>,
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

private data class ConfigThemeAppearanceFactionThemeExpansionState(
    val isCustomFactionThemeExpanded: Boolean,
    val onCustomFactionThemeExpandedChanged: (Boolean) -> Unit,
    val isSacredMachineFactionThemeExpanded: Boolean,
    val onSacredMachineFactionThemeExpandedChanged: (Boolean) -> Unit,
    val isAncientDynastyFactionThemeExpanded: Boolean,
    val onAncientDynastyFactionThemeExpandedChanged: (Boolean) -> Unit,
    val isImmortalRotFactionThemeExpanded: Boolean,
    val onImmortalRotFactionThemeExpandedChanged: (Boolean) -> Unit,
    val isScarletCarnageFactionThemeExpanded: Boolean,
    val onScarletCarnageFactionThemeExpandedChanged: (Boolean) -> Unit,
    val isExquisiteFallFactionThemeExpanded: Boolean,
    val onExquisiteFallFactionThemeExpandedChanged: (Boolean) -> Unit,
    val isLabyrinthOfMutabilityFactionThemeExpanded: Boolean,
    val onLabyrinthOfMutabilityFactionThemeExpandedChanged: (Boolean) -> Unit,
)

@Composable
internal fun ConfigThemeAppearanceSection(
    selectedThemeStyle: ThemeStyleOption,
    onThemeStyleSelected: (ThemeStyleOption) -> Unit,
    selectedFactionTheme: FactionThemeOption,
    onFactionThemeSelected: (FactionThemeOption) -> Unit,
    customFactionThemes: List<FactionThemeOption>,
    customFactionThemePresets: List<CustomFactionThemeSettings>,
    onCustomFactionThemeSaved: (CustomFactionThemeSettings, String?) -> Unit,
    onCustomFactionThemeDeleted: (String) -> Unit,
    onCustomFactionThemesImported: (List<CustomFactionThemeSettings>) -> Unit,
    onCustomFactionThemesReordered: (Int, Int) -> Unit,
    customMaterialThemePresets: List<CustomFactionThemeSettings>,
    customMaterialThemeSettings: CustomFactionThemeSettings,
    onCustomMaterialThemeSaved: (CustomFactionThemeSettings, String?) -> Unit,
    onCustomMaterialThemeDeleted: (String) -> Unit,
    onCustomMaterialThemesImported: (List<CustomFactionThemeSettings>) -> Unit,
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
    isCustomFactionThemeExpanded: Boolean,
    onCustomFactionThemeExpandedChanged: (Boolean) -> Unit,
    isSacredMachineFactionThemeExpanded: Boolean,
    onSacredMachineFactionThemeExpandedChanged: (Boolean) -> Unit,
    isAncientDynastyFactionThemeExpanded: Boolean,
    onAncientDynastyFactionThemeExpandedChanged: (Boolean) -> Unit,
    isImmortalRotFactionThemeExpanded: Boolean,
    onImmortalRotFactionThemeExpandedChanged: (Boolean) -> Unit,
    isScarletCarnageFactionThemeExpanded: Boolean,
    onScarletCarnageFactionThemeExpandedChanged: (Boolean) -> Unit,
    isExquisiteFallFactionThemeExpanded: Boolean,
    onExquisiteFallFactionThemeExpandedChanged: (Boolean) -> Unit,
    isLabyrinthOfMutabilityFactionThemeExpanded: Boolean,
    onLabyrinthOfMutabilityFactionThemeExpandedChanged: (Boolean) -> Unit,
    selectedPalette: PaletteOption,
    onPaletteSelected: (PaletteOption) -> Unit,
    materialPalettes: List<PaletteOption>,
    factionThemes: List<FactionThemeOption>,
    accentTokens: AppThemeAccentTokens,
) {
    val editCustomMaterialLabel = stringResource(R.string.config_custom_faction_theme_edit)
    val dialogState = rememberConfigThemeAppearanceDialogState()
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val copyConfigLabel = stringResource(R.string.config_custom_faction_theme_copy_config)
    val copyAllConfigLabel = stringResource(R.string.config_custom_theme_copy_all)
    val addCustomLabel = stringResource(R.string.config_custom_faction_theme_add)
    val importCustomLabel = stringResource(R.string.config_custom_faction_theme_import)
    val deleteCustomLabel = stringResource(R.string.config_custom_faction_theme_delete)
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
            onCreateCustomMaterialTheme = {
                val nextIndex = customMaterialThemePresets.size + 1
                dialogState.openCreateCustomMaterialTheme(
                    normalizeCustomMaterialThemeSettings(
                        DefaultCustomMaterialPaletteSettings.copy(
                            presetId = UUID.randomUUID().toString(),
                            displayName = "Custom $nextIndex",
                        ),
                    ),
                )
            },
            onCustomMaterialThemesReordered = onCustomMaterialThemesReordered,
            onCustomMaterialThemeDeleted = onCustomMaterialThemeDeleted,
            onOpenCustomMaterialThemeDialog = {
                dialogState.openEditCustomMaterialTheme(customMaterialThemeSettings)
            },
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
            factionThemes = factionThemes,
            selectedFactionTheme = selectedFactionTheme,
            customFactionThemePresets = customFactionThemePresets,
        )
    val factionThemeExpansionState =
        ConfigThemeAppearanceFactionThemeExpansionState(
            isCustomFactionThemeExpanded = isCustomFactionThemeExpanded,
            onCustomFactionThemeExpandedChanged = onCustomFactionThemeExpandedChanged,
            isSacredMachineFactionThemeExpanded = isSacredMachineFactionThemeExpanded,
            onSacredMachineFactionThemeExpandedChanged = onSacredMachineFactionThemeExpandedChanged,
            isAncientDynastyFactionThemeExpanded = isAncientDynastyFactionThemeExpanded,
            onAncientDynastyFactionThemeExpandedChanged = onAncientDynastyFactionThemeExpandedChanged,
            isImmortalRotFactionThemeExpanded = isImmortalRotFactionThemeExpanded,
            onImmortalRotFactionThemeExpandedChanged = onImmortalRotFactionThemeExpandedChanged,
            isScarletCarnageFactionThemeExpanded = isScarletCarnageFactionThemeExpanded,
            onScarletCarnageFactionThemeExpandedChanged = onScarletCarnageFactionThemeExpandedChanged,
            isExquisiteFallFactionThemeExpanded = isExquisiteFallFactionThemeExpanded,
            onExquisiteFallFactionThemeExpandedChanged = onExquisiteFallFactionThemeExpandedChanged,
            isLabyrinthOfMutabilityFactionThemeExpanded = isLabyrinthOfMutabilityFactionThemeExpanded,
            onLabyrinthOfMutabilityFactionThemeExpandedChanged = onLabyrinthOfMutabilityFactionThemeExpandedChanged,
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
    val localizedBuiltInFactionThemeNames =
        mapOf(
            "mars_relic" to stringResource(R.string.faction_theme_mars_relic_title),
            "scarlet_guard" to stringResource(R.string.faction_theme_scarlet_guard_title),
            "black_crimson_rite" to stringResource(R.string.faction_theme_black_crimson_rite_title),
            "xeno_code" to stringResource(R.string.faction_theme_xeno_code_title),
            "blood_soaked_ivory" to stringResource(R.string.faction_theme_scarlet_carnage_title),
            "brass_forge" to stringResource(R.string.faction_theme_brass_forge_title),
            "fires_of_fate" to stringResource(R.string.faction_theme_fires_of_fate_title),
            "arcane_abyss" to stringResource(R.string.faction_theme_labyrinth_of_mutability_title),
            "ecstatic_rapture" to stringResource(R.string.faction_theme_ecstatic_rapture_title),
            "velvet_nightmare" to stringResource(R.string.faction_theme_exquisite_fall_title),
            "toxic_effluence" to stringResource(R.string.faction_theme_immortal_rot_title),
            "plague_mire" to stringResource(R.string.faction_theme_plague_mire_title),
            "dynasty_revival" to stringResource(R.string.faction_theme_dynasty_revival_title),
            "sepulcher_cyan" to stringResource(R.string.faction_theme_sepulcher_cyan_title),
            "ancient_alloy" to stringResource(R.string.faction_theme_ancient_alloy_title),
            "tomb_sigil" to stringResource(R.string.faction_theme_tomb_sigil_title),
            "void_fluctuation" to stringResource(R.string.faction_theme_void_fluctuation_title),
            "crimson_decree" to stringResource(R.string.faction_theme_crimson_decree_title),
        )
    val toLocalizedFactionThemeExportEntry: (FactionThemeOption) -> FactionThemeExportEntry =
        { option ->
            FactionThemeExportEntry(
                displayName =
                    option.titleOverride
                        ?: localizedBuiltInFactionThemeNames[option.id]
                        ?: option.id,
                primaryHex = option.primaryColor.toHexString(),
                secondaryHex = option.secondaryColor.toHexString(),
                outlineHexOrNull = option.outlineColor.toHexString(),
            )
        }
    val copySelectedFactionThemeConfig: (FactionThemeOption) -> Unit =
        remember(clipboard, coroutineScope, localizedBuiltInFactionThemeNames) {
            { option ->
                coroutineScope.launch {
                    copyCustomThemeConfig(
                        clipboard = clipboard,
                        settings =
                            CustomFactionThemeSettings(
                                displayName =
                                    option.titleOverride
                                        ?: localizedBuiltInFactionThemeNames[option.id]
                                        ?: option.id,
                                primaryHex = option.primaryColor.toHexString(),
                                secondaryHex = option.secondaryColor.toHexString(),
                                outlineHexOrNull = option.outlineColor.toHexString(),
                            ),
                    )
                }
            }
        }
    val copyAllCustomFactionThemeConfigs: () -> Unit =
        remember(clipboard, coroutineScope, customFactionThemePresets) {
            {
                coroutineScope.launch {
                    clipboard.setClipEntry(
                        ClipEntry(
                            ClipData.newPlainText(
                                "FlipBits custom theme config",
                                customFactionThemePresets.toBatchConfigText(),
                            ),
                        ),
                    )
                }
            }
        }
    ConfigThemeAppearanceDialogHost(
        dialogState = dialogState,
        selectedThemeMode = selectedThemeMode,
        customFactionThemePresets = customFactionThemePresets,
        onCustomFactionThemeSaved = onCustomFactionThemeSaved,
        onCustomFactionThemeDeleted = onCustomFactionThemeDeleted,
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
                existing = customFactionThemePresets,
                imported = importedSettings,
                mode = DuplicateImportMode.Brand,
                onSave = onCustomFactionThemeSaved,
            )
        },
        onBatchMaterialThemesImported = { importedSettings ->
            val preview =
                buildCustomFactionThemeBatchImportPreview(
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
                buildCustomFactionThemeBatchImportPreview(
                    existing = customFactionThemePresets,
                    imported = importedSettings,
                    mode = DuplicateImportMode.Brand,
                )
            if (preview.duplicateCount == 0) {
                onCustomFactionThemesImported(importedSettings)
            } else {
                dialogState.stageBatchImport(preview)
            }
        },
        selectedCustomFactionThemePreset = renderState.selectedCustomFactionThemePreset,
        clipboard = clipboard,
        coroutineScope = coroutineScope,
        onConfirmBatchImport = { preview ->
            confirmBatchThemeImport(
                preview = preview,
                onBrandImport = onCustomFactionThemesImported,
                onMaterialImport = onCustomMaterialThemesImported,
            )
            dialogState.dismissBatchImportDialog()
        },
        onConfirmDuplicateOverwrite = { settings, presetId, mode ->
            saveImportedTheme(
                settings = settings,
                presetId = presetId,
                mode = mode,
                onBrandSave = onCustomFactionThemeSaved,
                onMaterialSave = onCustomMaterialThemeSaved,
            )
            dialogState.dismissDuplicateImport()
        },
        onConfirmDuplicateAddCopy = { settings, mode ->
            saveImportedTheme(
                settings = settings,
                presetId = null,
                mode = mode,
                onBrandSave = onCustomFactionThemeSaved,
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
                    selectedFactionTheme = selectedFactionTheme,
                    onFactionThemeSelected = onFactionThemeSelected,
                    customFactionThemes = customFactionThemes,
                    customFactionThemePresets = customFactionThemePresets,
                    factionThemeGroups = renderState.factionThemeGroups,
                    factionThemeExpansionState = factionThemeExpansionState,
                    selectedCustomFactionThemePreset = renderState.selectedCustomFactionThemePreset,
                    copyConfigLabel = copyConfigLabel,
                    copyAllConfigLabel = copyAllConfigLabel,
                    copySelectedFactionThemeConfig = copySelectedFactionThemeConfig,
                    copyAllCustomFactionThemeConfigs = copyAllCustomFactionThemeConfigs,
                    localizedBuiltInFactionThemeNames = localizedBuiltInFactionThemeNames,
                    clipboard = clipboard,
                    coroutineScope = coroutineScope,
                    onReorderCustomFactionThemes = onCustomFactionThemesReordered,
                    onDeleteCustomFactionTheme = { option ->
                        customFactionThemePresets
                            .firstOrNull { preset -> option.id == customFactionThemeOptionId(preset.presetId) }
                            ?.presetId
                            ?.let(onCustomFactionThemeDeleted)
                    },
                    canDeleteCustomFactionTheme = { _: FactionThemeOption -> customFactionThemePresets.size > 1 },
                    onOpenCreateCustomFactionTheme = dialogState::openCreateCustomFactionTheme,
                    onOpenEditCustomFactionTheme = { option ->
                        dialogState.openEditCustomFactionTheme(
                            customFactionThemePresets
                                .firstOrNull { preset -> option.id == customFactionThemeOptionId(preset.presetId) }
                                ?.presetId,
                        )
                    },
                    onOpenImportCustomFactionTheme = dialogState::openCustomThemeImportDialog,
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
    customMaterialThemePresets: List<CustomFactionThemeSettings>,
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
    factionThemes: List<FactionThemeOption>,
    selectedFactionTheme: FactionThemeOption,
    customFactionThemePresets: List<CustomFactionThemeSettings>,
): ConfigThemeAppearanceSectionRenderState {
    val isBrandStyle = selectedThemeStyle == ThemeStyleOption.FactionTheme
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
    val factionThemeGroups = rememberConfigThemeAppearanceFactionThemeGroups(factionThemes)
    val selectedCustomFactionThemePreset =
        remember(selectedFactionTheme.id, customFactionThemePresets) {
            customFactionThemePresets.firstOrNull { preset ->
                selectedFactionTheme.id == customFactionThemeOptionId(preset.presetId)
            }
        }
    return ConfigThemeAppearanceSectionRenderState(
        isBrandStyle = isBrandStyle,
        materialPaletteGroups = materialPaletteGroups,
        factionThemeGroups = factionThemeGroups,
        selectedCustomFactionThemePreset = selectedCustomFactionThemePreset,
    )
}

@Composable
private fun rememberMaterialPaletteGroups(
    isBrandStyle: Boolean,
    selectedPalette: PaletteOption,
    materialPalettes: List<PaletteOption>,
    customMaterialThemePresets: List<CustomFactionThemeSettings>,
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
    customMaterialThemePresets: List<CustomFactionThemeSettings>,
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
private fun rememberConfigThemeAppearanceFactionThemeGroups(
    factionThemes: List<FactionThemeOption>,
): ConfigThemeAppearanceFactionThemeGroups =
    remember(factionThemes) {
        ConfigThemeAppearanceFactionThemeGroups(
            sacredMachineThemes =
                factionThemes.filter {
                    it.groupTitleResId == R.string.config_faction_theme_group_sacred_machine
                },
            ancientDynastyThemes =
                factionThemes.filter {
                    it.groupTitleResId == R.string.config_faction_theme_group_ancient_dynasty
                },
            immortalRotThemes =
                factionThemes.filter {
                    it.groupTitleResId == R.string.config_faction_theme_group_immortal_rot
                },
            scarletCarnageThemes =
                factionThemes.filter {
                    it.groupTitleResId == R.string.config_faction_theme_group_scarlet_carnage
                },
            exquisiteFallThemes =
                factionThemes.filter {
                    it.groupTitleResId == R.string.config_faction_theme_group_exquisite_fall
                },
            labyrinthOfMutabilityThemes =
                factionThemes.filter {
                    it.groupTitleResId == R.string.config_faction_theme_group_labyrinth_of_mutability
                },
        )
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
    selectedFactionTheme: FactionThemeOption,
    onFactionThemeSelected: (FactionThemeOption) -> Unit,
    customFactionThemes: List<FactionThemeOption>,
    customFactionThemePresets: List<CustomFactionThemeSettings>,
    factionThemeGroups: ConfigThemeAppearanceFactionThemeGroups,
    factionThemeExpansionState: ConfigThemeAppearanceFactionThemeExpansionState,
    selectedCustomFactionThemePreset: CustomFactionThemeSettings?,
    copyConfigLabel: String,
    copyAllConfigLabel: String,
    copySelectedFactionThemeConfig: (FactionThemeOption) -> Unit,
    copyAllCustomFactionThemeConfigs: () -> Unit,
    localizedBuiltInFactionThemeNames: Map<String, String>,
    clipboard: Clipboard,
    coroutineScope: CoroutineScope,
    onReorderCustomFactionThemes: (Int, Int) -> Unit,
    onDeleteCustomFactionTheme: (FactionThemeOption) -> Unit,
    canDeleteCustomFactionTheme: (FactionThemeOption) -> Boolean,
    onOpenCreateCustomFactionTheme: () -> Unit,
    onOpenEditCustomFactionTheme: (FactionThemeOption) -> Unit,
    onOpenImportCustomFactionTheme: () -> Unit,
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
                text = stringResource(R.string.config_theme_mode_faction_theme_fixed),
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
            selectedFactionTheme = selectedFactionTheme,
            onFactionThemeSelected = onFactionThemeSelected,
            customFactionThemes = customFactionThemes,
            customFactionThemePresets = customFactionThemePresets,
            factionThemeGroups = factionThemeGroups,
            factionThemeExpansionState = factionThemeExpansionState,
            selectedCustomFactionThemePreset = selectedCustomFactionThemePreset,
            copyConfigLabel = copyConfigLabel,
            copyAllConfigLabel = copyAllConfigLabel,
            copySelectedFactionThemeConfig = copySelectedFactionThemeConfig,
            copyAllCustomFactionThemeConfigs = copyAllCustomFactionThemeConfigs,
            localizedBuiltInFactionThemeNames = localizedBuiltInFactionThemeNames,
            clipboard = clipboard,
            coroutineScope = coroutineScope,
            onReorderCustomFactionThemes = onReorderCustomFactionThemes,
            onDeleteCustomFactionTheme = onDeleteCustomFactionTheme,
            canDeleteCustomFactionTheme = canDeleteCustomFactionTheme,
            onOpenCreateCustomFactionTheme = onOpenCreateCustomFactionTheme,
            onOpenEditCustomFactionTheme = onOpenEditCustomFactionTheme,
            onOpenImportCustomFactionTheme = onOpenImportCustomFactionTheme,
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
    selectedFactionTheme: FactionThemeOption,
    onFactionThemeSelected: (FactionThemeOption) -> Unit,
    customFactionThemes: List<FactionThemeOption>,
    customFactionThemePresets: List<CustomFactionThemeSettings>,
    factionThemeGroups: ConfigThemeAppearanceFactionThemeGroups,
    factionThemeExpansionState: ConfigThemeAppearanceFactionThemeExpansionState,
    selectedCustomFactionThemePreset: CustomFactionThemeSettings?,
    copyConfigLabel: String,
    copyAllConfigLabel: String,
    copySelectedFactionThemeConfig: (FactionThemeOption) -> Unit,
    copyAllCustomFactionThemeConfigs: () -> Unit,
    localizedBuiltInFactionThemeNames: Map<String, String>,
    clipboard: Clipboard,
    coroutineScope: CoroutineScope,
    onReorderCustomFactionThemes: (Int, Int) -> Unit,
    onDeleteCustomFactionTheme: (FactionThemeOption) -> Unit,
    canDeleteCustomFactionTheme: (FactionThemeOption) -> Boolean,
    onOpenCreateCustomFactionTheme: () -> Unit,
    onOpenEditCustomFactionTheme: (FactionThemeOption) -> Unit,
    onOpenImportCustomFactionTheme: () -> Unit,
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
            text = stringResource(R.string.config_faction_theme_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.config_faction_theme_description),
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
            FactionThemeSection(
                accentTokens = accentTokens,
                title = stringResource(R.string.config_faction_theme_group_custom),
                options = customFactionThemes,
                selectedFactionTheme = selectedFactionTheme,
                expanded = factionThemeExpansionState.isCustomFactionThemeExpanded,
                onExpandedChanged = factionThemeExpansionState.onCustomFactionThemeExpandedChanged,
                onFactionThemeSelected = onFactionThemeSelected,
                onEditFactionTheme = { onOpenEditCustomFactionTheme(it) },
                editSelectedOnly = true,
                editActionLabel = stringResource(R.string.config_custom_faction_theme_edit),
                onMoveOption =
                    if (factionThemeExpansionState.isCustomFactionThemeExpanded) {
                        onReorderCustomFactionThemes
                    } else {
                        null
                    },
                onEditActionClick =
                    selectedFactionTheme
                        .takeIf { selectedCustomFactionThemePreset != null }
                        ?.let { { onOpenEditCustomFactionTheme(it) } },
                actionLabel = stringResource(R.string.config_custom_faction_theme_add),
                onActionClick = onOpenCreateCustomFactionTheme,
                iconActions =
                    listOf(
                        FactionThemeSectionIconAction(
                            label = stringResource(R.string.config_custom_faction_theme_import),
                            icon = Icons.AutoMirrored.Rounded.Input,
                            onClick = onOpenImportCustomFactionTheme,
                        ),
                        FactionThemeSectionIconAction(
                            label = copyAllConfigLabel,
                            icon = Icons.Rounded.ContentCopy,
                            enabled = customFactionThemePresets.isNotEmpty(),
                            onClick = copyAllCustomFactionThemeConfigs,
                        ),
                    ),
                stackHeaderActions = true,
                deleteActionLabel = stringResource(R.string.config_custom_faction_theme_delete),
                onDeleteFactionTheme = onDeleteCustomFactionTheme,
                canDeleteFactionTheme = canDeleteCustomFactionTheme,
            )
            brandBuiltInThemeSections(
                groups = factionThemeGroups,
                expansionState = factionThemeExpansionState,
            ).forEach { section ->
                FactionThemeSection(
                    accentTokens = accentTokens,
                    title = stringResource(section.titleResId),
                    options = section.options,
                    selectedFactionTheme = selectedFactionTheme,
                    expanded = section.expanded,
                    onExpandedChanged = section.onExpandedChanged,
                    iconActions =
                        if (section.expanded) {
                            val exportText =
                                section.options
                                    .map { option ->
                                        FactionThemeExportEntry(
                                            displayName =
                                                option.titleOverride
                                                    ?: localizedBuiltInFactionThemeNames[option.id]
                                                    ?: option.id,
                                            primaryHex = option.primaryColor.toHexString(),
                                            secondaryHex = option.secondaryColor.toHexString(),
                                            outlineHexOrNull = option.outlineColor.toHexString(),
                                        )
                                    }.toFactionThemeExportBatchConfigText()
                            listOf(
                                FactionThemeSectionIconAction(
                                    label = copyAllConfigLabel,
                                    icon = Icons.Rounded.ContentCopy,
                                    enabled = section.options.isNotEmpty(),
                                    onClick = {
                                        coroutineScope.launch {
                                            clipboard.setClipEntry(
                                                ClipEntry(
                                                    ClipData.newPlainText(
                                                        "FlipBits built-in faction theme config",
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
                    onFactionThemeSelected = onFactionThemeSelected,
                    copyConfigLabel = copyConfigLabel,
                    onCopyConfig = copySelectedFactionThemeConfig,
                    headerTitleColor = MaterialTheme.colorScheme.onSurface,
                    headerMetaColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun brandBuiltInThemeSections(
    groups: ConfigThemeAppearanceFactionThemeGroups,
    expansionState: ConfigThemeAppearanceFactionThemeExpansionState,
): List<FactionThemeBuiltInSectionUi> =
    listOf(
        FactionThemeBuiltInSectionUi(
            titleResId = R.string.config_faction_theme_group_sacred_machine,
            options = groups.sacredMachineThemes,
            expanded = expansionState.isSacredMachineFactionThemeExpanded,
            onExpandedChanged = expansionState.onSacredMachineFactionThemeExpandedChanged,
        ),
        FactionThemeBuiltInSectionUi(
            titleResId = R.string.config_faction_theme_group_ancient_dynasty,
            options = groups.ancientDynastyThemes,
            expanded = expansionState.isAncientDynastyFactionThemeExpanded,
            onExpandedChanged = expansionState.onAncientDynastyFactionThemeExpandedChanged,
        ),
        FactionThemeBuiltInSectionUi(
            titleResId = R.string.config_faction_theme_group_scarlet_carnage,
            options = groups.scarletCarnageThemes,
            expanded = expansionState.isScarletCarnageFactionThemeExpanded,
            onExpandedChanged = expansionState.onScarletCarnageFactionThemeExpandedChanged,
        ),
        FactionThemeBuiltInSectionUi(
            titleResId = R.string.config_faction_theme_group_labyrinth_of_mutability,
            options = groups.labyrinthOfMutabilityThemes,
            expanded = expansionState.isLabyrinthOfMutabilityFactionThemeExpanded,
            onExpandedChanged = expansionState.onLabyrinthOfMutabilityFactionThemeExpandedChanged,
        ),
        FactionThemeBuiltInSectionUi(
            titleResId = R.string.config_faction_theme_group_immortal_rot,
            options = groups.immortalRotThemes,
            expanded = expansionState.isImmortalRotFactionThemeExpanded,
            onExpandedChanged = expansionState.onImmortalRotFactionThemeExpandedChanged,
        ),
        FactionThemeBuiltInSectionUi(
            titleResId = R.string.config_faction_theme_group_exquisite_fall,
            options = groups.exquisiteFallThemes,
            expanded = expansionState.isExquisiteFallFactionThemeExpanded,
            onExpandedChanged = expansionState.onExquisiteFallFactionThemeExpandedChanged,
        ),
    )

private fun androidx.compose.ui.graphics.Color.toHexString(): String {
    val red = (this.red * 255f).roundToInt().coerceIn(0, 255)
    val green = (this.green * 255f).roundToInt().coerceIn(0, 255)
    val blue = (this.blue * 255f).roundToInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", red, green, blue)
}
