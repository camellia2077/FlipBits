package com.bag.audioandroid.ui.screen

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Input
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.appSegmentedButtonColors
import com.bag.audioandroid.ui.model.BrandThemeOption
import com.bag.audioandroid.ui.model.CustomBrandThemeImportParseResult
import com.bag.audioandroid.ui.model.CustomBrandThemeSettings
import com.bag.audioandroid.ui.model.DefaultCustomBrandThemeSettings
import com.bag.audioandroid.ui.model.PaletteFamily
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.hasSameConfigAs
import com.bag.audioandroid.ui.model.parseCustomBrandThemeImportText
import com.bag.audioandroid.ui.model.toBatchConfigText
import com.bag.audioandroid.ui.model.toConfigText
import com.bag.audioandroid.ui.theme.AppThemeAccentTokens
import com.bag.audioandroid.ui.theme.customBrandThemeOptionId
import com.bag.audioandroid.ui.theme.customMaterialPalette
import com.bag.audioandroid.ui.theme.isCustomMaterialPaletteId
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    customMaterialThemePresets: List<CustomBrandThemeSettings>,
    customMaterialThemeSettings: CustomBrandThemeSettings,
    onCustomMaterialThemeSaved: (CustomBrandThemeSettings) -> Unit,
    onCreateCustomMaterialTheme: () -> Unit,
    selectedThemeMode: ThemeModeOption,
    onThemeModeSelected: (ThemeModeOption) -> Unit,
    isExpanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
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
    val isBrandStyle = selectedThemeStyle == ThemeStyleOption.BrandDualTone
    val isCustomMaterialSelected = !isBrandStyle && isCustomMaterialPaletteId(selectedPalette.id)
    val editCustomMaterialLabel = stringResource(R.string.config_custom_brand_theme_edit)
    var showCustomThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showCustomMaterialThemeDialog by rememberSaveable { mutableStateOf(false) }
    var customThemeDialogPresetId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCustomThemeImportDialog by rememberSaveable { mutableStateOf(false) }
    var showCustomThemeExportDialog by rememberSaveable { mutableStateOf(false) }
    var pendingBatchImport by remember { mutableStateOf<CustomBrandThemeBatchImportPreview?>(null) }
    var duplicateImportCandidate by remember { mutableStateOf<CustomBrandThemeSettings?>(null) }
    var duplicateImportPresetId by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val copyConfigLabel = stringResource(R.string.config_custom_brand_theme_copy_config)

    val materialPaletteGroups =
        remember(materialPalettes, customMaterialThemePresets, isCustomMaterialSelected, editCustomMaterialLabel) {
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
                        onEditOption =
                            if (family == PaletteFamily.Custom && isCustomMaterialSelected) {
                                { showCustomMaterialThemeDialog = true }
                            } else {
                                null
                            },
                        editActionLabel =
                            if (family == PaletteFamily.Custom && isCustomMaterialSelected) {
                                editCustomMaterialLabel
                            } else {
                                null
                            },
                    )
                }
            }
        }
    val sacredMachineThemes =
        remember(brandThemes) {
            brandThemes.filter {
                it.groupTitleResId == R.string.config_dual_tone_group_sacred_machine
            }
        }
    val ancientDynastyThemes =
        remember(brandThemes) {
            brandThemes.filter {
                it.groupTitleResId == R.string.config_dual_tone_group_ancient_dynasty
            }
        }
    val immortalRotThemes =
        remember(brandThemes) {
            brandThemes.filter {
                it.groupTitleResId == R.string.config_dual_tone_group_immortal_rot
            }
        }
    val scarletCarnageThemes =
        remember(brandThemes) {
            brandThemes.filter {
                it.groupTitleResId == R.string.config_dual_tone_group_scarlet_carnage
            }
        }
    val exquisiteFallThemes =
        remember(brandThemes) {
            brandThemes.filter {
                it.groupTitleResId == R.string.config_dual_tone_group_exquisite_fall
            }
        }
    val labyrinthOfMutabilityThemes =
        remember(brandThemes) {
            brandThemes.filter {
                it.groupTitleResId == R.string.config_dual_tone_group_labyrinth_of_mutability
            }
        }
    val selectedCustomBrandThemePreset =
        remember(selectedBrandTheme.id, customBrandThemePresets) {
            customBrandThemePresets.firstOrNull { preset ->
                selectedBrandTheme.id == customBrandThemeOptionId(preset.presetId)
            }
        }
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

    LaunchedEffect(isBrandStyle, selectedBrandTheme.id, brandThemes) {
        if (!isBrandStyle) {
            return@LaunchedEffect
        }
        when (selectedBrandTheme.groupTitleResId) {
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

    if (showCustomThemeDialog) {
        val editingSettings =
            customThemeDialogPresetId?.let { presetId ->
                customBrandThemePresets.firstOrNull { it.presetId == presetId }
            }
        CustomBrandThemeDialog(
            initialSettings = editingSettings ?: customBrandThemePresets.firstOrNull() ?: DefaultCustomBrandThemeSettings,
            isCreatingNew = editingSettings == null,
            canDelete = editingSettings != null && customBrandThemePresets.size > 1,
            onDismiss = {
                showCustomThemeDialog = false
                customThemeDialogPresetId = null
            },
            onSave = { settings ->
                onCustomBrandThemeSaved(settings, customThemeDialogPresetId)
                showCustomThemeDialog = false
                customThemeDialogPresetId = null
            },
            onDelete = {
                val presetId = customThemeDialogPresetId ?: return@CustomBrandThemeDialog
                onCustomBrandThemeDeleted(presetId)
                showCustomThemeDialog = false
                customThemeDialogPresetId = null
            },
        )
    }
    if (showCustomMaterialThemeDialog) {
        ConfigMaterialCustomDialog(
            initialSettings = customMaterialThemeSettings,
            onDismiss = { showCustomMaterialThemeDialog = false },
            onSave = { settings ->
                onCustomMaterialThemeSaved(settings)
                showCustomMaterialThemeDialog = false
            },
        )
    }
    if (showCustomThemeImportDialog) {
        CustomBrandThemeImportDialog(
            onDismiss = { showCustomThemeImportDialog = false },
            onImport = { importedSettings ->
                if (importedSettings.size == 1) {
                    val singleSettings = importedSettings.single()
                    val duplicate =
                        customBrandThemePresets.firstOrNull { preset ->
                            preset.hasSameConfigAs(singleSettings)
                        }
                    if (duplicate == null) {
                        onCustomBrandThemeSaved(singleSettings, null)
                    } else {
                        duplicateImportCandidate = singleSettings
                        duplicateImportPresetId = duplicate.presetId
                    }
                } else {
                    pendingBatchImport =
                        buildCustomBrandThemeBatchImportPreview(
                            existing = customBrandThemePresets,
                            imported = importedSettings,
                        )
                }
                showCustomThemeImportDialog = false
            },
        )
    }
    if (showCustomThemeExportDialog) {
        CustomBrandThemeExportDialog(
            selectedConfigText = selectedCustomBrandThemePreset?.toConfigText().orEmpty(),
            allConfigText = customBrandThemePresets.toBatchConfigText(),
            canExportSelected = selectedCustomBrandThemePreset != null,
            clipboard = clipboard,
            coroutineScope = coroutineScope,
            onDismiss = { showCustomThemeExportDialog = false },
        )
    }
    pendingBatchImport?.let { preview ->
        CustomBrandThemeBatchImportConfirmDialog(
            preview = preview,
            onDismiss = { pendingBatchImport = null },
            onImport = {
                onCustomBrandThemesImported(preview.newSettings)
                pendingBatchImport = null
            },
        )
    }
    if (duplicateImportCandidate != null && duplicateImportPresetId != null) {
        AlertDialog(
            onDismissRequest = {
                duplicateImportCandidate = null
                duplicateImportPresetId = null
            },
            title = { Text(text = stringResource(R.string.config_custom_brand_theme_import_duplicate_title)) },
            text = { Text(text = stringResource(R.string.config_custom_brand_theme_import_duplicate_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val settings = duplicateImportCandidate ?: return@TextButton
                        val presetId = duplicateImportPresetId ?: return@TextButton
                        onCustomBrandThemeSaved(settings, presetId)
                        duplicateImportCandidate = null
                        duplicateImportPresetId = null
                    },
                ) {
                    Text(text = stringResource(R.string.config_custom_brand_theme_import_overwrite))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val settings = duplicateImportCandidate ?: return@TextButton
                        onCustomBrandThemeSaved(settings, null)
                        duplicateImportCandidate = null
                        duplicateImportPresetId = null
                    },
                ) {
                    Text(text = stringResource(R.string.config_custom_brand_theme_import_add_copy))
                }
            },
        )
    }

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
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            materialPaletteGroups.forEach { group ->
                                PaletteGroupSection(
                                    accentTokens = accentTokens,
                                    group = group,
                                    selectedPalette = selectedPalette,
                                    onPaletteSelected = onPaletteSelected,
                                )
                            }
                        }
                        Text(
                            text =
                                if (isCustomMaterialPaletteId(selectedPalette.id)) {
                                    stringResource(R.string.palette_family_custom)
                                } else {
                                    "${stringResource(selectedPalette.family.titleResId)} · " +
                                        stringResource(selectedPalette.titleResId)
                                },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                } else {
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
                                expanded = isCustomBrandThemeExpanded,
                                onExpandedChanged = onCustomBrandThemeExpandedChanged,
                                onBrandThemeSelected = onBrandThemeSelected,
                                onEditBrandTheme = { option ->
                                    customThemeDialogPresetId =
                                        customBrandThemePresets
                                            .firstOrNull { preset -> option.id == customBrandThemeOptionId(preset.presetId) }
                                            ?.presetId
                                    showCustomThemeDialog = true
                                },
                                copyConfigLabel = copyConfigLabel,
                                onCopyConfig = copySelectedBrandThemeConfig,
                                actionLabel = stringResource(R.string.config_custom_brand_theme_add),
                                onActionClick = {
                                    customThemeDialogPresetId = null
                                    showCustomThemeDialog = true
                                },
                                iconActions =
                                    listOf(
                                        BrandThemeSectionIconAction(
                                            label = stringResource(R.string.config_custom_brand_theme_import),
                                            icon = Icons.AutoMirrored.Rounded.Input,
                                            onClick = { showCustomThemeImportDialog = true },
                                        ),
                                        BrandThemeSectionIconAction(
                                            label = copyConfigLabel,
                                            icon = Icons.Rounded.ContentCopy,
                                            enabled = selectedCustomBrandThemePreset != null,
                                            onClick = { showCustomThemeExportDialog = true },
                                        ),
                                    ),
                            )
                            BrandThemeSection(
                                accentTokens = accentTokens,
                                title = stringResource(R.string.config_dual_tone_group_sacred_machine),
                                options = sacredMachineThemes,
                                selectedBrandTheme = selectedBrandTheme,
                                expanded = isSacredMachineBrandThemeExpanded,
                                onExpandedChanged = onSacredMachineBrandThemeExpandedChanged,
                                onBrandThemeSelected = onBrandThemeSelected,
                                copyConfigLabel = copyConfigLabel,
                                onCopyConfig = copySelectedBrandThemeConfig,
                            )
                            BrandThemeSection(
                                accentTokens = accentTokens,
                                title = stringResource(R.string.config_dual_tone_group_ancient_dynasty),
                                options = ancientDynastyThemes,
                                selectedBrandTheme = selectedBrandTheme,
                                expanded = isAncientDynastyBrandThemeExpanded,
                                onExpandedChanged = onAncientDynastyBrandThemeExpandedChanged,
                                onBrandThemeSelected = onBrandThemeSelected,
                                copyConfigLabel = copyConfigLabel,
                                onCopyConfig = copySelectedBrandThemeConfig,
                            )
                            BrandThemeSection(
                                accentTokens = accentTokens,
                                title = stringResource(R.string.config_dual_tone_group_scarlet_carnage),
                                options = scarletCarnageThemes,
                                selectedBrandTheme = selectedBrandTheme,
                                expanded = isScarletCarnageBrandThemeExpanded,
                                onExpandedChanged = onScarletCarnageBrandThemeExpandedChanged,
                                onBrandThemeSelected = onBrandThemeSelected,
                                copyConfigLabel = copyConfigLabel,
                                onCopyConfig = copySelectedBrandThemeConfig,
                            )
                            BrandThemeSection(
                                accentTokens = accentTokens,
                                title = stringResource(R.string.config_dual_tone_group_labyrinth_of_mutability),
                                options = labyrinthOfMutabilityThemes,
                                selectedBrandTheme = selectedBrandTheme,
                                expanded = isLabyrinthOfMutabilityBrandThemeExpanded,
                                onExpandedChanged = onLabyrinthOfMutabilityBrandThemeExpandedChanged,
                                onBrandThemeSelected = onBrandThemeSelected,
                                copyConfigLabel = copyConfigLabel,
                                onCopyConfig = copySelectedBrandThemeConfig,
                            )
                            BrandThemeSection(
                                accentTokens = accentTokens,
                                title = stringResource(R.string.config_dual_tone_group_immortal_rot),
                                options = immortalRotThemes,
                                selectedBrandTheme = selectedBrandTheme,
                                expanded = isImmortalRotBrandThemeExpanded,
                                onExpandedChanged = onImmortalRotBrandThemeExpandedChanged,
                                onBrandThemeSelected = onBrandThemeSelected,
                                copyConfigLabel = copyConfigLabel,
                                onCopyConfig = copySelectedBrandThemeConfig,
                            )
                            BrandThemeSection(
                                accentTokens = accentTokens,
                                title = stringResource(R.string.config_dual_tone_group_exquisite_fall),
                                options = exquisiteFallThemes,
                                selectedBrandTheme = selectedBrandTheme,
                                expanded = isExquisiteFallBrandThemeExpanded,
                                onExpandedChanged = onExquisiteFallBrandThemeExpandedChanged,
                                onBrandThemeSelected = onBrandThemeSelected,
                                copyConfigLabel = copyConfigLabel,
                                onCopyConfig = copySelectedBrandThemeConfig,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomBrandThemeImportDialog(
    onDismiss: () -> Unit,
    onImport: (List<CustomBrandThemeSettings>) -> Unit,
) {
    var configText by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }
    val parseResult = remember(configText) { parseCustomBrandThemeImportText(configText) }
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
                        if (showError) {
                            Text(text = stringResource(R.string.config_custom_brand_theme_import_invalid))
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
                        CustomBrandThemeImportParseResult.Invalid -> showError = true
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
                            preview.newSettings.size,
                            preview.duplicateCount,
                        ),
                )
                Text(text = stringResource(R.string.config_custom_brand_theme_batch_import_duplicate_note))
            }
        },
        confirmButton = {
            TextButton(
                enabled = preview.newSettings.isNotEmpty(),
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

private data class CustomBrandThemeBatchImportPreview(
    val totalCount: Int,
    val duplicateCount: Int,
    val newSettings: List<CustomBrandThemeSettings>,
)

private fun buildCustomBrandThemeBatchImportPreview(
    existing: List<CustomBrandThemeSettings>,
    imported: List<CustomBrandThemeSettings>,
): CustomBrandThemeBatchImportPreview {
    val newSettings =
        imported.filterNot { importedSettings ->
            existing.any { preset -> preset.hasSameConfigAs(importedSettings) }
        }
    return CustomBrandThemeBatchImportPreview(
        totalCount = imported.size,
        duplicateCount = imported.size - newSettings.size,
        newSettings = newSettings,
    )
}

private fun androidx.compose.ui.graphics.Color.toHexString(): String {
    val red = (this.red * 255f).roundToInt().coerceIn(0, 255)
    val green = (this.green * 255f).roundToInt().coerceIn(0, 255)
    val blue = (this.blue * 255f).roundToInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", red, green, blue)
}
