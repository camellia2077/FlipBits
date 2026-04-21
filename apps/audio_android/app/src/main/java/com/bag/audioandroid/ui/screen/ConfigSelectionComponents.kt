package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.BrandThemeOption
import com.bag.audioandroid.ui.model.PaletteFamily
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.theme.AppThemeAccentTokens

internal data class PaletteGroupUi(
    val family: PaletteFamily,
    val options: List<PaletteOption>,
)

@Composable
internal fun ExpandableCardHeader(
    accentTokens: AppThemeAccentTokens,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    contentDescription: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        IconButton(onClick = onToggleExpanded) {
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = contentDescription,
                tint = accentTokens.disclosureAccentTint,
            )
        }
    }
}

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
    val usesStrongSelectedState =
        selected &&
            (
                option.id == "mars_relic" ||
                    option.id == "scarlet_guard" ||
                    option.id == "black_crimson_rite"
            )
    // These three themes need a stronger selected row treatment because their selected state can
    // visually merge with the surrounding card. Give them a clearer container, border, and badge.
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

@Composable
internal fun SelectionRow(
    accentTokens: AppThemeAccentTokens,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Surface(
        tonalElevation = if (selected) 6.dp else 1.dp,
        shadowElevation = if (selected) 2.dp else 0.dp,
        shape = MaterialTheme.shapes.medium,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                SelectedBadge(
                    text = stringResource(R.string.config_palette_selected),
                    tint = accentTokens.selectionLabelAccentTint,
                )
            }
        }
    }
}

@Composable
internal fun SelectedBadge(
    text: String,
    tint: Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            color = tint,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun PaletteSwatch(
    accentTokens: AppThemeAccentTokens,
    option: PaletteOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val paletteLabel = stringResource(option.titleResId)
    val borderColor =
        if (selected) {
            accentTokens.selectionBorderAccentTint
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .semantics {
                    contentDescription = paletteLabel
                }.selectable(
                    selected = selected,
                    onClick = onClick,
                    role = Role.RadioButton,
                ).background(MaterialTheme.colorScheme.surface, CircleShape)
                .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(borderColor, CircleShape)
                    .padding(if (selected) 3.dp else 1.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(option.previewColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
internal fun PaletteGroupSection(
    accentTokens: AppThemeAccentTokens,
    group: PaletteGroupUi,
    selectedPalette: PaletteOption,
    onPaletteSelected: (PaletteOption) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(group.family.titleResId),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                group.options.forEach { option ->
                    PaletteSwatch(
                        accentTokens = accentTokens,
                        option = option,
                        selected = option.id == selectedPalette.id,
                        onClick = { onPaletteSelected(option) },
                    )
                }
            }
        }
    }
}
