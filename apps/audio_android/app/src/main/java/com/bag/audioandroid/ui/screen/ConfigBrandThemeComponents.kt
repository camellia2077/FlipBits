package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.BrandThemeOption
import com.bag.audioandroid.ui.theme.AppThemeAccentTokens
import com.bag.audioandroid.ui.utilityActionIconButtonColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BrandThemeSection(
    accentTokens: AppThemeAccentTokens,
    title: String,
    options: List<BrandThemeOption>,
    selectedBrandTheme: BrandThemeOption,
    expanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    onBrandThemeSelected: (BrandThemeOption) -> Unit,
    onEditBrandTheme: ((BrandThemeOption) -> Unit)? = null,
    copyConfigLabel: String? = null,
    onCopyConfig: ((BrandThemeOption) -> Unit)? = null,
    onMoveOption: ((Int, Int) -> Unit)? = null,
    editActionLabel: String? = null,
    onEditActionClick: (() -> Unit)? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    iconActions: List<BrandThemeSectionIconAction> = emptyList(),
    stackHeaderActions: Boolean = false,
    copyConfigIconOnly: Boolean = false,
    editSelectedOnly: Boolean = false,
) {
    if (options.isEmpty()) {
        return
    }

    val selectedOption = options.firstOrNull { it.id == selectedBrandTheme.id }
    val visibleOptions =
        if (expanded) {
            options
        } else {
            selectedOption?.let(::listOf) ?: emptyList()
        }
    val visibleOptionIds = visibleOptions.map { it.id }
    val showCollapsedSelectionActions = selectedOption != null
    val showHeaderActions = expanded || showCollapsedSelectionActions
    val dragState =
        rememberReorderDragState(
            itemIds = visibleOptionIds,
            estimatedRowHeightPx = EstimatedBrandThemeRowHeightPx,
            rowSpacingPx = BrandThemeRowSpacingPx,
            thresholdFraction = ReorderSwapThresholdFraction,
        )
    val currentOptions by rememberUpdatedState(options)
    val currentOnMoveOption by rememberUpdatedState(newValue = onMoveOption)
    val hapticFeedback = LocalHapticFeedback.current
    val expandContentDescription =
        if (expanded) {
            stringResource(R.string.config_palette_collapse)
        } else {
            stringResource(R.string.config_palette_expand)
        }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (stackHeaderActions) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${visibleOptions.size}/${options.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = { onExpandedChanged(!expanded) },
                        colors = utilityActionIconButtonColors(),
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = expandContentDescription,
                            tint = accentTokens.disclosureAccentTint,
                        )
                    }
                }
                if (showHeaderActions) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        iconActions.forEach { action ->
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
                        if (editActionLabel != null && onEditActionClick != null) {
                            TextButton(onClick = onEditActionClick) {
                                Text(text = editActionLabel)
                            }
                        }
                        if (actionLabel != null && onActionClick != null) {
                            TextButton(onClick = onActionClick) {
                                Text(text = actionLabel)
                            }
                        }
                    }
                }
            }
        } else {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onExpandedChanged(!expanded) }
                        .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${visibleOptions.size}/${options.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    iconActions.forEach { action ->
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
                    if (editActionLabel != null && onEditActionClick != null) {
                        TextButton(onClick = onEditActionClick) {
                            Text(text = editActionLabel)
                        }
                    }
                    if (actionLabel != null && onActionClick != null) {
                        TextButton(onClick = onActionClick) {
                            Text(text = actionLabel)
                        }
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = accentTokens.disclosureAccentTint,
                    )
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(BrandThemeRowSpacing),
        ) {
            visibleOptions.forEachIndexed { index, option ->
                key(option.id) {
                    Box(
                        modifier = Modifier.zIndex(dragState.zIndexFor(option.id)),
                    ) {
                        BrandThemeRow(
                            accentTokens = accentTokens,
                            option = option,
                            selected = option.id == selectedBrandTheme.id,
                            onClick = { onBrandThemeSelected(option) },
                            onEdit = onEditBrandTheme?.let { callback -> { callback(option) } },
                            copyConfigLabel = copyConfigLabel,
                            onCopyConfig = onCopyConfig?.let { callback -> { callback(option) } },
                            copyConfigIconOnly = copyConfigIconOnly,
                            editSelectedOnly = editSelectedOnly,
                            modifier =
                                Modifier
                                    .onGloballyPositioned { coordinates ->
                                        dragState.onItemMeasured(
                                            itemId = option.id,
                                            heightPx = coordinates.size.height.toFloat(),
                                        )
                                    }.then(
                                        if (onMoveOption != null && expanded && options.size > 1) {
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
                        if (expanded && dragState.shouldShowPreviewBefore(index)) {
                            ReorderDropIndicator(accentTokens = accentTokens)
                        }
                    }
                }
            }
        }
    }
}

internal data class BrandThemeSectionIconAction(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

@Composable
internal fun BrandThemeRow(
    accentTokens: AppThemeAccentTokens,
    option: BrandThemeOption,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    copyConfigLabel: String? = null,
    onCopyConfig: (() -> Unit)? = null,
    copyConfigIconOnly: Boolean = false,
    editSelectedOnly: Boolean = false,
    modifier: Modifier = Modifier,
    dragOffsetY: Float = 0f,
) {
    val editContentDescription = stringResource(R.string.config_custom_brand_theme_edit)
    val optionTitle = option.titleOverride ?: stringResource(option.titleResId)
    val optionDescription = option.descriptionOverride ?: stringResource(option.descriptionResId)
    val optionAccessibility = option.accessibilityLabelOverride ?: stringResource(option.accessibilityLabelResId)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = if (selected) 2.dp else 0.dp,
        shape = MaterialTheme.shapes.medium,
        border =
            if (selected) {
                BorderStroke(
                    width = SelectedOutlineWidth,
                    color = accentTokens.selectionBorderAccentTint,
                )
            } else {
                null
            },
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
            BrandThemePreview(
                primaryColor = option.primaryColor,
                secondaryColor = option.secondaryColor,
                outlineColor = option.outlineColor,
                textColor = option.secondaryColor,
                contentDescription = optionAccessibility,
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = optionTitle,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = optionDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (selected) {
                    SelectedBadge(
                        text = stringResource(R.string.config_palette_selected),
                    )
                }
                if (selected && copyConfigLabel != null && onCopyConfig != null) {
                    if (copyConfigIconOnly) {
                        IconButton(
                            onClick = onCopyConfig,
                            colors = utilityActionIconButtonColors(),
                            modifier =
                                Modifier.semantics {
                                    contentDescription = copyConfigLabel
                                },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = null,
                            )
                        }
                    } else {
                        TextButton(onClick = onCopyConfig) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(text = copyConfigLabel)
                        }
                    }
                }
                if (onEdit != null && (!editSelectedOnly || selected)) {
                    IconButton(
                        onClick = onEdit,
                        colors = utilityActionIconButtonColors(),
                        modifier =
                            Modifier.semantics {
                                contentDescription = editContentDescription
                            },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun BrandThemePreview(
    primaryColor: Color,
    secondaryColor: Color,
    outlineColor: Color,
    textColor: Color,
    contentDescription: String,
) {
    val previewShape = RoundedCornerShape(6.dp)

    Box(
        modifier =
            Modifier
                .size(width = 70.dp, height = 45.dp)
                .clip(previewShape)
                .border(2.25.dp, outlineColor, previewShape)
                .semantics { this.contentDescription = contentDescription },
    ) {
        Row(modifier = Modifier.matchParentSize()) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(primaryColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "A",
                    color = textColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(secondaryColor),
            )
        }
    }
}

private const val ReorderSwapThresholdFraction = 0.42f
private val BrandThemeRowSpacing = 8.dp
private const val BrandThemeRowSpacingPx = 8f
private const val EstimatedBrandThemeRowHeightPx = 64f
