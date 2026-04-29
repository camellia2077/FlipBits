package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.ui.model.PaletteFamily
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.theme.AppThemeAccentTokens
import com.bag.audioandroid.ui.theme.appThemeVisualTokens

internal data class PaletteGroupUi(
    val family: PaletteFamily,
    val options: List<PaletteOption>,
)

@Composable
internal fun PaletteSwatch(
    accentTokens: AppThemeAccentTokens,
    option: PaletteOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val paletteLabel =
        androidx.compose.ui.res
            .stringResource(option.titleResId)
    val borderColor =
        if (selected) {
            accentTokens.selectionBorderAccentTint
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    val ringWidth = if (selected) SelectedOutlineWidth else 1.dp
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .semantics { contentDescription = paletteLabel }
                .selectable(
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
                    .padding(ringWidth),
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
    val visualTokens = appThemeVisualTokens()
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = visualTokens.groupContainerColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text =
                    androidx.compose.ui.res
                        .stringResource(group.family.titleResId),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
