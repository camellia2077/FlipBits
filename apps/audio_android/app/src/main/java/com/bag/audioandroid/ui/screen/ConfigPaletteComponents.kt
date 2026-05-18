package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.bag.audioandroid.ui.model.PaletteFamily
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.theme.AppThemeAccentTokens
import com.bag.audioandroid.ui.theme.appThemeVisualTokens
import com.bag.audioandroid.ui.utilityActionIconButtonColors

internal data class PaletteGroupUi(
    val family: PaletteFamily,
    val options: List<PaletteOption>,
    val onAddOption: (() -> Unit)? = null,
    val onEditOption: (() -> Unit)? = null,
    val editActionLabel: String? = null,
    val iconActions: List<PaletteGroupIconAction> = emptyList(),
    val addActionLabel: String? = null,
    val onMoveOption: ((Int, Int) -> Unit)? = null,
)

internal data class PaletteGroupIconAction(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

@Composable
internal fun PaletteSwatch(
    accentTokens: AppThemeAccentTokens,
    option: PaletteOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val paletteLabel =
        option.titleOverride
            ?: androidx.compose.ui.res
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
    expanded: Boolean = true,
    onExpandedChanged: ((Boolean) -> Unit)? = null,
) {
    if (group.family == PaletteFamily.Custom) {
        MaterialCustomPaletteSection(
            accentTokens = accentTokens,
            group = group,
            selectedPalette = selectedPalette,
            onPaletteSelected = onPaletteSelected,
            expanded = expanded,
        )
        return
    }

    val visualTokens = appThemeVisualTokens()
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = visualTokens.groupContainerColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(group.options.first().previewColor, CircleShape),
                )
                Text(
                    text =
                        androidx.compose.ui.res
                            .stringResource(group.family.titleResId),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                onExpandedChanged?.let { onExpandedChanged ->
                    IconButton(
                        onClick = { onExpandedChanged(!expanded) },
                        colors = utilityActionIconButtonColors(),
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription =
                                if (expanded) {
                                    androidx.compose.ui.res
                                        .stringResource(com.bag.audioandroid.R.string.config_palette_collapse)
                                } else {
                                    androidx.compose.ui.res
                                        .stringResource(com.bag.audioandroid.R.string.config_palette_expand)
                                },
                        )
                    }
                }
            }
            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialBuiltInRowSpacing)) {
                    group.options.chunked(MaterialBuiltInColumns).forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MaterialBuiltInCardSpacing),
                        ) {
                            rowOptions.forEach { option ->
                                MaterialBuiltInPaletteRow(
                                    accentTokens = accentTokens,
                                    option = option,
                                    selected = option.id == selectedPalette.id,
                                    onClick = { onPaletteSelected(option) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(MaterialBuiltInColumns - rowOptions.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialBuiltInPaletteRow(
    accentTokens: AppThemeAccentTokens,
    option: PaletteOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor =
        if (selected) {
            accentTokens.selectionBorderAccentTint
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = if (selected) 2.dp else 0.dp,
        shape = MaterialTheme.shapes.medium,
        modifier =
            modifier
                .clickable(onClick = onClick),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(borderColor, CircleShape)
                            .padding(if (selected) SelectedOutlineWidth else 1.dp),
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
                Text(
                    text =
                        option.titleOverride ?: androidx.compose.ui.res
                            .stringResource(option.titleResId),
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            if (selected) {
                Text(
                    text =
                        androidx.compose.ui.res
                            .stringResource(com.bag.audioandroid.R.string.config_palette_selected),
                    style = MaterialTheme.typography.labelMedium,
                    color = accentTokens.selectionBorderAccentTint,
                    fontWeight = FontWeight.SemiBold,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(accentTokens.selectionBorderAccentTint.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun MaterialCustomPaletteSection(
    accentTokens: AppThemeAccentTokens,
    group: PaletteGroupUi,
    selectedPalette: PaletteOption,
    onPaletteSelected: (PaletteOption) -> Unit,
    expanded: Boolean,
) {
    val visualTokens = appThemeVisualTokens()
    val optionIds = group.options.map { it.id }
    val dragState =
        rememberReorderDragState(
            itemIds = optionIds,
            estimatedRowHeightPx = EstimatedMaterialCustomRowHeightPx,
            rowSpacingPx = MaterialCustomRowSpacingPx,
            thresholdFraction = MaterialCustomReorderThresholdFraction,
        )
    val currentOptions by rememberUpdatedState(group.options)
    val currentOnMoveOption by rememberUpdatedState(group.onMoveOption)
    val hapticFeedback = LocalHapticFeedback.current

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = visualTokens.groupContainerColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                group.iconActions.forEach { action ->
                    IconButton(
                        onClick = action.onClick,
                        enabled = action.enabled,
                        colors = utilityActionIconButtonColors(),
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.label,
                        )
                    }
                }
                if (group.onEditOption != null && group.editActionLabel != null) {
                    TextButton(onClick = group.onEditOption) {
                        Text(text = group.editActionLabel)
                    }
                }
                if (group.onAddOption != null && group.addActionLabel != null) {
                    TextButton(onClick = group.onAddOption) {
                        Text(text = group.addActionLabel)
                    }
                }
            }

            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialCustomRowSpacing)) {
                    group.options.forEachIndexed { index, option ->
                        key(option.id) {
                            Box(
                                modifier = Modifier.zIndex(dragState.zIndexFor(option.id)),
                            ) {
                                MaterialCustomPaletteRow(
                                    accentTokens = accentTokens,
                                    option = option,
                                    selected = option.id == selectedPalette.id,
                                    onClick = { onPaletteSelected(option) },
                                    modifier =
                                        Modifier
                                            .onGloballyPositioned { coordinates ->
                                                dragState.onItemMeasured(
                                                    itemId = option.id,
                                                    heightPx = coordinates.size.height.toFloat(),
                                                )
                                            }.then(
                                                if (group.onMoveOption != null && group.options.size > 1) {
                                                    Modifier
                                                        .reorderableLongPressDrag(
                                                            enabled = true,
                                                            itemId = option.id,
                                                            itemIds = currentOptions.map { it.id },
                                                            dragState = dragState,
                                                            hapticFeedback = hapticFeedback,
                                                            onMove = currentOnMoveOption,
                                                        )
                                                } else {
                                                    Modifier
                                                },
                                            ),
                                    dragOffsetY = dragState.dragOffsetFor(option.id),
                                )
                                if (dragState.shouldShowPreviewBefore(index)) {
                                    ReorderDropIndicator(accentTokens = accentTokens)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialCustomPaletteRow(
    accentTokens: AppThemeAccentTokens,
    option: PaletteOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragOffsetY: Float = 0f,
) {
    val borderColor =
        if (selected) {
            accentTokens.selectionBorderAccentTint
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = if (selected) 2.dp else 0.dp,
        shape = MaterialTheme.shapes.medium,
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = dragOffsetY }
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
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(borderColor, CircleShape)
                        .padding(if (selected) SelectedOutlineWidth else 1.dp),
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text =
                        option.titleOverride ?: androidx.compose.ui.res
                            .stringResource(option.titleResId),
                    fontWeight = FontWeight.Medium,
                )
                if (selected) {
                    Text(
                        text =
                            androidx.compose.ui.res
                                .stringResource(com.bag.audioandroid.R.string.config_palette_selected),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private val MaterialCustomRowSpacing = 8.dp
private val MaterialBuiltInRowSpacing = 8.dp
private val MaterialBuiltInCardSpacing = 8.dp
private const val MaterialBuiltInColumns = 2
private const val MaterialCustomRowSpacingPx = 8f
private const val MaterialCustomReorderThresholdFraction = 0.42f
private const val EstimatedMaterialCustomRowHeightPx = 56f
