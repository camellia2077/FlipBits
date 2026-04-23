package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import com.bag.audioandroid.ui.playerChromeColors

@Composable
internal fun PlaybackFollowTokenCard(
    token: String,
    rawDisplayUnits: List<TextFollowRawDisplayUnitViewData>,
    annotationMode: PlaybackFollowViewMode,
    isActive: Boolean,
    activeByteIndexWithinToken: Int,
    modifier: Modifier = Modifier,
) {
    val playerColors = playerChromeColors()
    val containerColor =
        if (isActive) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        }
    val tokenColor =
        if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val inactiveRawColor = MaterialTheme.colorScheme.onSurfaceVariant
    val annotationByteGroups = annotationByteGroupsForMode(annotationMode, rawDisplayUnits)
    val rawText =
        remember(
            annotationByteGroups,
            activeByteIndexWithinToken,
            isActive,
            inactiveRawColor,
            playerColors.annotationChipContainer,
            playerColors.annotationChipContent,
        ) {
            buildAnnotatedString {
                annotationByteGroups.forEachIndexed { index, group ->
                    if (index > 0) {
                        append(" ")
                    }
                    val isActiveByte = isActive && index == activeByteIndexWithinToken
                    withStyle(
                        SpanStyle(
                            color =
                                if (isActiveByte) {
                                    playerColors.annotationChipContent
                                } else {
                                    inactiveRawColor
                                },
                            background =
                                if (isActiveByte) {
                                    playerColors.annotationChipContainer
                                } else {
                                    Color.Transparent
                                },
                            fontWeight =
                                if (isActiveByte) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Medium
                                },
                        ),
                    ) {
                        append(group)
                    }
                }
            }
        }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.large,
        modifier =
            modifier
                .widthIn(min = 92.dp, max = 220.dp)
                .testTag(
                    if (isActive) {
                        "follow-token-active"
                    } else {
                        "follow-token"
                    },
                ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = token,
                color = tokenColor,
                style =
                    if (isActive) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            Surface(
                color =
                    if (isActive) {
                        playerColors.annotationChipContainer.copy(alpha = 0.28f)
                    } else {
                        Color.Transparent
                    },
                contentColor =
                    if (isActive) {
                        playerColors.annotationChipContent
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = rawText,
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                )
            }
        }
    }
}
