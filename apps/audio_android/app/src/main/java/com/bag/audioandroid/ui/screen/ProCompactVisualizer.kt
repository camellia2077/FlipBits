package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.theme.appThemeVisualTokens

@Composable
internal fun ProCompactVisualizer(
    followData: PayloadFollowViewData,
    displayedSamples: Int,
    frameSamples: Int,
    modifier: Modifier = Modifier,
) {
    val state = rememberProEncodingVisualizationState(followData, displayedSamples, frameSamples)
    if (state == null) {
        Text(
            text = stringResource(R.string.audio_pro_visual_waiting_follow),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.testTag("pro-compact-visualizer-empty"),
        )
        return
    }

    Column(
        modifier = modifier.testTag("pro-compact-visualizer"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ProCurrentMappingCard(
            state = state,
            modifier = Modifier.fillMaxWidth(),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = appThemeVisualTokens().supportSurfaceColor,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text =
                        stringResource(
                            R.string.audio_pro_visual_token_mapping,
                            state.tokenByteMapping.tokenIndex,
                            state.tokenByteMapping.tokenText.ifBlank { "?" },
                            state.tokenByteMapping.byteIndexWithinToken + 1,
                            state.tokenByteMapping.byteCountWithinUnit.coerceAtLeast(1),
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ProCurrentMappingCard(
    state: ProEncodingVisualizationState,
    modifier: Modifier = Modifier,
) {
    val visualTokens = appThemeVisualTokens()
    val byteExplanation = state.byteExplanation
    val currentSymbol = state.currentSymbol
    val highBinary = byteExplanation.byteBinary.take(4).padEnd(4, '-')
    val lowBinary =
        byteExplanation.byteBinary
            .drop(4)
            .take(4)
            .padEnd(4, '-')
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = visualTokens.supportSurfaceColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "0x${byteExplanation.byteHex}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = proSymbolSlotLabel(currentSymbol.slotIndexWithinByte),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.audio_pro_visual_nibble_value, currentSymbol.nibbleHex),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ProNibbleMappingChip(
                    title = stringResource(R.string.audio_pro_visual_high_nibble),
                    hexValue = byteExplanation.highNibbleHex,
                    binaryValue = highBinary,
                    isActive = byteExplanation.isHighNibbleCurrent,
                    modifier = Modifier.weight(1f),
                )
                ProNibbleMappingChip(
                    title = stringResource(R.string.audio_pro_visual_low_nibble),
                    hexValue = byteExplanation.lowNibbleHex,
                    binaryValue = lowBinary,
                    isActive = !byteExplanation.isHighNibbleCurrent,
                    modifier = Modifier.weight(1f),
                )
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(visualTokens.subtleOutlineColor.copy(alpha = 0.28f)),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProDualFrequencyValue(
                    label = stringResource(R.string.audio_pro_visual_low_group),
                    frequencyHz = currentSymbol.lowFreqHz,
                    modifier = Modifier.weight(1f),
                )
                ProDualFrequencyValue(
                    label = stringResource(R.string.audio_pro_visual_high_group),
                    frequencyHz = currentSymbol.highFreqHz,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ProNibbleMappingChip(
    title: String,
    hexValue: String,
    binaryValue: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val visualTokens = appThemeVisualTokens()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color =
            if (isActive) {
                visualTokens.supportStrongSurfaceColor
            } else {
                visualTokens.selectionUnselectedContainerColor
            },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = hexValue,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
            )
            Text(
                text = binaryValue,
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}

@Composable
private fun ProDualFrequencyValue(
    label: String,
    frequencyHz: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(
                    color = appThemeVisualTokens().selectionUnselectedContainerColor,
                    shape = RoundedCornerShape(16.dp),
                ).padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = frequencyHz.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Hz",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
