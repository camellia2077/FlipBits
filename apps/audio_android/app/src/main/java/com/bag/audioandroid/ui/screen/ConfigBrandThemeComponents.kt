package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.BrandThemeOption
import com.bag.audioandroid.ui.theme.AppThemeAccentTokens

@Composable
internal fun BrandThemeSection(
    accentTokens: AppThemeAccentTokens,
    title: String,
    options: List<BrandThemeOption>,
    selectedBrandTheme: BrandThemeOption,
    onBrandThemeSelected: (BrandThemeOption) -> Unit,
) {
    if (options.isEmpty()) {
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        options.forEach { option ->
            BrandThemeRow(
                accentTokens = accentTokens,
                option = option,
                selected = option.id == selectedBrandTheme.id,
                onClick = { onBrandThemeSelected(option) },
            )
        }
    }
}

@Composable
internal fun BrandThemeRow(
    accentTokens: AppThemeAccentTokens,
    option: BrandThemeOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val usesStrongSelectedState = selected && option.id in StrongSelectedBrandThemeIds
    val selectedContainerColor =
        when (option.id) {
            "mars_relic" -> lerp(option.secondaryColor, option.primaryColor, 0.10f)
            "scarlet_guard" -> lerp(option.secondaryColor, option.primaryColor, 0.14f)
            "black_crimson_rite" -> lerp(option.primaryColor, option.secondaryColor, 0.22f)
            else -> MaterialTheme.colorScheme.surface
        }
    Surface(
        color = if (usesStrongSelectedState) selectedContainerColor else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 6.dp else 1.dp,
        shadowElevation = if (selected) 2.dp else 0.dp,
        shape = MaterialTheme.shapes.medium,
        border =
            if (selected) {
                BorderStroke(
                    width = if (usesStrongSelectedState) 2.dp else 1.dp,
                    color = accentTokens.selectionBorderAccentTint,
                )
            } else {
                null
            },
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandThemePreview(
                primaryColor = option.primaryColor,
                secondaryColor = option.secondaryColor,
                contentDescription = stringResource(option.accessibilityLabelResId),
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(option.titleResId),
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(option.descriptionResId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                if (usesStrongSelectedState) {
                    Surface(
                        color =
                            lerp(
                                accentTokens.selectionLabelAccentTint,
                                MaterialTheme.colorScheme.surface,
                                0.78f,
                            ),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            SelectedBadge(
                                text = stringResource(R.string.config_palette_selected),
                                tint = accentTokens.selectionLabelAccentTint,
                            )
                        }
                    }
                } else {
                    SelectedBadge(
                        text = stringResource(R.string.config_palette_selected),
                        tint = accentTokens.selectionLabelAccentTint,
                    )
                }
            }
        }
    }
}

@Composable
internal fun BrandThemePreview(
    primaryColor: Color,
    secondaryColor: Color,
    contentDescription: String,
) {
    Row(
        modifier =
            Modifier
                .size(width = 56.dp, height = 36.dp)
                .semantics { this.contentDescription = contentDescription },
    ) {
        Box(
            modifier =
                Modifier
                    .size(width = 28.dp, height = 36.dp)
                    .background(primaryColor),
        )
        Box(
            modifier =
                Modifier
                    .size(width = 28.dp, height = 36.dp)
                    .background(secondaryColor),
        )
    }
}

private val StrongSelectedBrandThemeIds =
    setOf(
        "mars_relic",
        "scarlet_guard",
        "black_crimson_rite",
    )
