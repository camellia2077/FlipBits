package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import com.bag.audioandroid.domain.TextFollowTimelineEntry
import com.bag.audioandroid.ui.playerChromeColors
import com.bag.audioandroid.ui.playerSegmentedButtonColors

internal enum class PlaybackFollowViewMode(
    val titleResId: Int,
) {
    Hex(R.string.audio_follow_view_hex),
    Binary(R.string.audio_follow_view_binary),
}

@Composable
internal fun PlaybackDataFollowSection(
    followData: PayloadFollowViewData,
    displayedSamples: Int,
    modifier: Modifier = Modifier,
    initialAnnotationMode: PlaybackFollowViewMode = PlaybackFollowViewMode.Hex,
) {
    var selectedAnnotationMode by rememberSaveable { mutableStateOf(initialAnnotationMode.name) }
    val presentationState =
        rememberPlaybackFollowPresentationState(
            followData = followData,
            displayedSamples = displayedSamples,
            selectedAnnotationModeName = selectedAnnotationMode,
        )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("playback-follow-section"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.audio_follow_title),
            style = MaterialTheme.typography.titleSmall,
        )
        if (!followData.followAvailable) {
            Text(
                text = stringResource(R.string.audio_follow_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        if (!followData.textFollowAvailable || followData.textTokens.isEmpty()) {
            Text(
                text = stringResource(R.string.audio_follow_text_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        PlaybackFollowAnnotationModeSwitcher(
            selectedMode = presentationState.followViewMode,
            onModeSelected = { selectedAnnotationMode = it.name },
        )
        PlaybackFollowTokenStrip(
            followData = followData,
            presentationState = presentationState,
        )
    }
}

@Composable
private fun rememberPlaybackFollowPresentationState(
    followData: PayloadFollowViewData,
    displayedSamples: Int,
    selectedAnnotationModeName: String,
): PlaybackFollowPresentationState {
    val followViewMode =
        remember(selectedAnnotationModeName) {
            PlaybackFollowViewMode.entries.firstOrNull { it.name == selectedAnnotationModeName }
                ?: PlaybackFollowViewMode.Hex
        }
    val activeTextIndex = remember(followData.textTokenTimeline, displayedSamples) {
        activeTextTimelineIndex(followData.textTokenTimeline, displayedSamples)
    }
    val rawDisplayUnitsByToken =
        remember(
            followData.textRawDisplayUnits,
        ) {
            followData.textRawDisplayUnits.groupBy(TextFollowRawDisplayUnitViewData::tokenIndex)
        }
    val activeByteIndexWithinToken =
        remember(
            activeTextIndex,
            displayedSamples,
            rawDisplayUnitsByToken,
        ) {
            activeByteIndexWithinToken(
                activeTextIndex = activeTextIndex,
                displayedSamples = displayedSamples,
                rawDisplayUnitsByToken = rawDisplayUnitsByToken,
            )
        }
    return remember(
        followViewMode,
        activeTextIndex,
        activeByteIndexWithinToken,
        rawDisplayUnitsByToken,
    ) {
        PlaybackFollowPresentationState(
            followViewMode = followViewMode,
            activeTextIndex = activeTextIndex,
            activeByteIndexWithinToken = activeByteIndexWithinToken,
            rawDisplayUnitsByToken = rawDisplayUnitsByToken,
        )
    }
}

@Composable
private fun PlaybackFollowAnnotationModeSwitcher(
    selectedMode: PlaybackFollowViewMode,
    onModeSelected: (PlaybackFollowViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("follow-annotation-switcher"),
    ) {
        PlaybackFollowViewMode.entries.forEachIndexed { index, option ->
            val optionLabel = stringResource(option.titleResId)
            SegmentedButton(
                selected = selectedMode == option,
                onClick = { onModeSelected(option) },
                modifier =
                    Modifier
                        .testTag("follow-annotation-${option.name.lowercase()}")
                        .semantics { contentDescription = optionLabel },
                shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = PlaybackFollowViewMode.entries.size,
                    ),
                colors = playerSegmentedButtonColors(),
                label = { Text(text = optionLabel) },
            )
        }
    }
}

@Composable
private fun PlaybackFollowTokenStrip(
    followData: PayloadFollowViewData,
    presentationState: PlaybackFollowPresentationState,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(presentationState.activeTextIndex) {
        if (presentationState.activeTextIndex >= 0) {
            listState.animateScrollToItem(presentationState.activeTextIndex)
        }
    }

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 188.dp, max = 240.dp)
                .testTag("follow-token-strip"),
    ) {
        itemsIndexed(followData.textTokens) { index, token ->
            PlaybackFollowTokenCard(
                token = token,
                rawDisplayUnits = presentationState.rawDisplayUnitsByToken[index].orEmpty(),
                annotationMode = presentationState.followViewMode,
                isActive = index == presentationState.activeTextIndex,
                activeByteIndexWithinToken =
                    if (index == presentationState.activeTextIndex) {
                        presentationState.activeByteIndexWithinToken
                    } else {
                        -1
                    },
            )
        }
    }
}

internal fun annotationByteGroupsForMode(
    mode: PlaybackFollowViewMode,
    rawDisplayUnits: List<TextFollowRawDisplayUnitViewData>,
): List<String> =
    rawDisplayUnits.map { unit ->
        when (mode) {
            PlaybackFollowViewMode.Hex -> unit.hexText
            PlaybackFollowViewMode.Binary -> unit.binaryText
        }
    }

@Composable
private fun PlaybackFollowTokenCard(
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

private fun activeTextTimelineIndex(
    entries: List<TextFollowTimelineEntry>,
    displayedSamples: Int,
): Int =
    entries.indexOfLast { entry ->
        displayedSamples >= entry.startSample &&
            displayedSamples < entry.startSample + entry.sampleCount
    }

private fun activeByteIndexWithinToken(
    activeTextIndex: Int,
    displayedSamples: Int,
    rawDisplayUnitsByToken: Map<Int, List<TextFollowRawDisplayUnitViewData>>,
): Int {
    if (activeTextIndex < 0) {
        return -1
    }
    val rawDisplayUnits = rawDisplayUnitsByToken[activeTextIndex].orEmpty()
    val activeByte =
        rawDisplayUnits.firstOrNull { entry ->
            displayedSamples >= entry.startSample &&
                displayedSamples < entry.startSample + entry.sampleCount
        } ?: return -1
    return activeByte.byteIndexWithinToken
}

@Immutable
private data class PlaybackFollowPresentationState(
    val followViewMode: PlaybackFollowViewMode,
    val activeTextIndex: Int,
    val activeByteIndexWithinToken: Int,
    val rawDisplayUnitsByToken: Map<Int, List<TextFollowRawDisplayUnitViewData>>,
)
