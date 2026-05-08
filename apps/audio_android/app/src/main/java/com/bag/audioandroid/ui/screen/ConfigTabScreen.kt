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
import com.bag.audioandroid.ui.model.SampleDecorationStyleOption
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
    selectedThemeMode: ThemeModeOption,
    onThemeModeSelected: (ThemeModeOption) -> Unit,
    isThemeAppearanceExpanded: Boolean,
    onThemeAppearanceExpandedChanged: (Boolean) -> Unit,
    isDemoModeEnabled: Boolean,
    onDemoModeEnabledChange: (Boolean) -> Unit,
    isSampleDecorationEnabled: Boolean,
    onSampleDecorationEnabledChange: (Boolean) -> Unit,
    sampleDecorationStyle: SampleDecorationStyleOption,
    onSampleDecorationStyleSelected: (SampleDecorationStyleOption) -> Unit,
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
            selectedThemeMode = selectedThemeMode,
            onThemeModeSelected = onThemeModeSelected,
            isExpanded = isThemeAppearanceExpanded,
            onExpandedChanged = onThemeAppearanceExpandedChanged,
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(stringResource(R.string.config_demo_mode_title), fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.config_demo_mode_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Switch(
                    checked = isDemoModeEnabled,
                    onCheckedChange = onDemoModeEnabledChange,
                )
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

                if (isSampleDecorationEnabled) {
                    Text(
                        text = stringResource(R.string.config_sample_decoration_style_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SampleDecorationStyleOption.entries.forEach { option ->
                            androidx.compose.material3.FilterChip(
                                selected = sampleDecorationStyle == option,
                                onClick = { onSampleDecorationStyleSelected(option) },
                                label = { Text(stringResource(option.labelResId)) },
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
