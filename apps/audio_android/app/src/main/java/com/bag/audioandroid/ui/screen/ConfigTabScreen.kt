package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.BrandThemeOption
import com.bag.audioandroid.ui.model.CustomBrandThemeSettings
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.theme.AppThemeAccentTokens

@Composable
@Suppress("LongMethod")
fun ConfigTabScreen(
    selectedLanguage: AppLanguageOption,
    onLanguageSelected: (AppLanguageOption) -> Unit,
    isLanguageExpanded: Boolean,
    onLanguageExpandedChanged: (Boolean) -> Unit,
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
    isThemeAppearanceExpanded: Boolean,
    onThemeAppearanceExpandedChanged: (Boolean) -> Unit,
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
    isSampleTextExpanded: Boolean,
    onSampleTextExpandedChanged: (Boolean) -> Unit,
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
    isDebugExpanded: Boolean,
    onDebugExpandedChanged: (Boolean) -> Unit,
    isDemoModeEnabled: Boolean,
    onDemoModeEnabledChange: (Boolean) -> Unit,
    isSampleAutoFillEnabled: Boolean,
    onSampleAutoFillEnabledChange: (Boolean) -> Unit,
    isSampleDecorationEnabled: Boolean,
    onSampleDecorationEnabledChange: (Boolean) -> Unit,
    isSavedAudioPlaybackDataStorageEnabled: Boolean,
    onSavedAudioPlaybackDataStorageEnabledChange: (Boolean) -> Unit,
    isFlashVisualPerfOverlayEnabled: Boolean,
    onFlashVisualPerfOverlayEnabledChange: (Boolean) -> Unit,
    selectedPalette: PaletteOption,
    onPaletteSelected: (PaletteOption) -> Unit,
    materialPalettes: List<PaletteOption>,
    brandThemes: List<BrandThemeOption>,
    accentTokens: AppThemeAccentTokens,
    onOpenAboutPage: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    Column(
        modifier =
            modifier
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    end = contentPadding.calculateEndPadding(layoutDirection),
                ).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ConfigLanguageSection(
            selectedLanguage = selectedLanguage,
            onLanguageSelected = onLanguageSelected,
            isExpanded = isLanguageExpanded,
            onExpandedChanged = onLanguageExpandedChanged,
            accentTokens = accentTokens,
        )

        ConfigThemeAppearanceSection(
            selectedThemeStyle = selectedThemeStyle,
            onThemeStyleSelected = onThemeStyleSelected,
            selectedBrandTheme = selectedBrandTheme,
            onBrandThemeSelected = onBrandThemeSelected,
            customBrandThemes = customBrandThemes,
            customBrandThemePresets = customBrandThemePresets,
            onCustomBrandThemeSaved = onCustomBrandThemeSaved,
            onCustomBrandThemeDeleted = onCustomBrandThemeDeleted,
            onCustomBrandThemesImported = onCustomBrandThemesImported,
            onCustomBrandThemesReordered = onCustomBrandThemesReordered,
            customMaterialThemePresets = customMaterialThemePresets,
            customMaterialThemeSettings = customMaterialThemeSettings,
            onCustomMaterialThemeSaved = onCustomMaterialThemeSaved,
            onCustomMaterialThemeDeleted = onCustomMaterialThemeDeleted,
            onCustomMaterialThemesImported = onCustomMaterialThemesImported,
            onCustomMaterialThemesReordered = onCustomMaterialThemesReordered,
            onCreateCustomMaterialTheme = onCreateCustomMaterialTheme,
            selectedThemeMode = selectedThemeMode,
            onThemeModeSelected = onThemeModeSelected,
            isExpanded = isThemeAppearanceExpanded,
            onExpandedChanged = onThemeAppearanceExpandedChanged,
            isCustomMaterialThemeExpanded = isCustomMaterialThemeExpanded,
            onCustomMaterialThemeExpandedChanged = onCustomMaterialThemeExpandedChanged,
            isBuiltInMaterialPalettesExpanded = isBuiltInMaterialPalettesExpanded,
            onBuiltInMaterialPalettesExpandedChanged = onBuiltInMaterialPalettesExpandedChanged,
            isMaterialRedsPaletteExpanded = isMaterialRedsPaletteExpanded,
            onMaterialRedsPaletteExpandedChanged = onMaterialRedsPaletteExpandedChanged,
            isMaterialOrangesPaletteExpanded = isMaterialOrangesPaletteExpanded,
            onMaterialOrangesPaletteExpandedChanged = onMaterialOrangesPaletteExpandedChanged,
            isMaterialYellowsPaletteExpanded = isMaterialYellowsPaletteExpanded,
            onMaterialYellowsPaletteExpandedChanged = onMaterialYellowsPaletteExpandedChanged,
            isMaterialGreensPaletteExpanded = isMaterialGreensPaletteExpanded,
            onMaterialGreensPaletteExpandedChanged = onMaterialGreensPaletteExpandedChanged,
            isMaterialBluesPaletteExpanded = isMaterialBluesPaletteExpanded,
            onMaterialBluesPaletteExpandedChanged = onMaterialBluesPaletteExpandedChanged,
            isMaterialPurplesPaletteExpanded = isMaterialPurplesPaletteExpanded,
            onMaterialPurplesPaletteExpandedChanged = onMaterialPurplesPaletteExpandedChanged,
            isMaterialNeutralsPaletteExpanded = isMaterialNeutralsPaletteExpanded,
            onMaterialNeutralsPaletteExpandedChanged = onMaterialNeutralsPaletteExpandedChanged,
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
            selectedPalette = selectedPalette,
            onPaletteSelected = onPaletteSelected,
            materialPalettes = materialPalettes,
            brandThemes = brandThemes,
            accentTokens = accentTokens,
        )

        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ExpandableCardHeader(
                    accentTokens = accentTokens,
                    title = "Debug",
                    subtitle = "Demo playback aids and visual diagnostics.",
                    expanded = isDebugExpanded,
                    onToggleExpanded = { onDebugExpandedChanged(!isDebugExpanded) },
                    contentDescription =
                        if (isDebugExpanded) {
                            "Collapse debug settings"
                        } else {
                            "Expand debug settings"
                        },
                )
                if (isDebugExpanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text("Demo mode", fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Show tap feedback and key action hints for recording.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        Switch(
                            checked = isDemoModeEnabled,
                            onCheckedChange = onDemoModeEnabledChange,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text("Visual perf overlay", fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(
                            onClick = { onFlashVisualPerfOverlayEnabledChange(!isFlashVisualPerfOverlayEnabled) },
                        ) {
                            Icon(
                                imageVector =
                                    if (isFlashVisualPerfOverlayEnabled) {
                                        Icons.Outlined.Visibility
                                    } else {
                                        Icons.Outlined.VisibilityOff
                                    },
                                contentDescription = null,
                                tint =
                                    if (isFlashVisualPerfOverlayEnabled) {
                                        accentTokens.disclosureAccentTint
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                        }
                    }
                }
            }
        }

        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ExpandableCardHeader(
                    accentTokens = accentTokens,
                    title = stringResource(R.string.config_sample_text_title),
                    subtitle = stringResource(R.string.config_sample_text_subtitle),
                    expanded = isSampleTextExpanded,
                    onToggleExpanded = { onSampleTextExpandedChanged(!isSampleTextExpanded) },
                    contentDescription =
                        if (isSampleTextExpanded) {
                            stringResource(R.string.config_sample_text_collapse)
                        } else {
                            stringResource(R.string.config_sample_text_expand)
                        },
                )
                if (isSampleTextExpanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(stringResource(R.string.config_sample_auto_fill_title), fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(R.string.config_sample_auto_fill_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = isSampleAutoFillEnabled,
                            onCheckedChange = onSampleAutoFillEnabledChange,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(stringResource(R.string.config_sample_decoration_title), fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(R.string.config_sample_decoration_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = isSampleDecorationEnabled,
                            onCheckedChange = onSampleDecorationEnabledChange,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                stringResource(R.string.config_saved_playback_data_title),
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                stringResource(R.string.config_saved_playback_data_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = isSavedAudioPlaybackDataStorageEnabled,
                            onCheckedChange = onSavedAudioPlaybackDataStorageEnabledChange,
                        )
                    }
                }
            }
        }

        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAboutPage() }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.config_about_title), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.config_about_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(">", color = accentTokens.disclosureAccentTint, fontWeight = FontWeight.Bold)
            }
        }

        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.height(contentPadding.calculateBottomPadding()),
        )
    }
}
