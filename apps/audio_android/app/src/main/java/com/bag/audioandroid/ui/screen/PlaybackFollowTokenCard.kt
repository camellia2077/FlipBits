package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import com.bag.audioandroid.ui.playbackLyricsAccentTextColor
import com.bag.audioandroid.ui.theme.appThemeVisualTokens
import java.nio.charset.StandardCharsets

@Composable
internal fun PlaybackFollowTokenCard(
    token: String,
    rawDisplayUnits: List<TextFollowRawDisplayUnitViewData>,
    annotationMode: PlaybackFollowViewMode,
    isActive: Boolean,
    activeByteIndexWithinToken: Int,
    activeBitIndexWithinByte: Int = -1,
    isPast: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val visualTokens = appThemeVisualTokens()
    val activeAnnotationContainer = Color.Transparent
    val lyricsAccentTextColor = playbackLyricsAccentTextColor()
    val focusColor = MaterialTheme.colorScheme.primary
    val onFocusColor = MaterialTheme.colorScheme.onPrimary
    val activeAnnotationTint = lyricsAccentTextColor
    
    val containerColor = when {
        isActive -> visualTokens.followTokenContainerColor
        else -> Color.Transparent
    }
    
    val tokenColor = when {
        isActive -> lyricsAccentTextColor
        isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    val inactiveRawColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isPast) 0.5f else 1.0f)
    val inactiveCharacterColor = tokenColor
    val annotationByteGroups = annotationByteGroupsForMode(annotationMode, rawDisplayUnits)
    val characterDisplayUnits = remember(token) { characterDisplayUnits(token) }
    val tokenDisplayText =
        remember(
            token,
            characterDisplayUnits,
            activeByteIndexWithinToken,
            isActive,
            inactiveCharacterColor,
            lyricsAccentTextColor,
        ) {
            buildAnnotatedString {
                if (characterDisplayUnits.isEmpty()) {
                    append(token)
                    return@buildAnnotatedString
                }
                characterDisplayUnits.forEach { unit ->
                    val isActiveCharacter =
                        isActive &&
                            activeByteIndexWithinToken >= unit.byteStartIndexWithinToken &&
                            activeByteIndexWithinToken <
                            unit.byteStartIndexWithinToken + unit.byteCount
                    withStyle(
                        SpanStyle(
                            color = if (isActiveCharacter) onFocusColor else inactiveCharacterColor,
                            background = if (isActiveCharacter) focusColor else Color.Transparent,
                            fontWeight = if (isActiveCharacter) FontWeight.ExtraBold else FontWeight.Bold,
                        ),
                    ) {
                        if (isActiveCharacter) {
                            append(unit.text)
                        } else {
                            append(unit.text)
                        }
                    }
                }
            }
        }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.large,
        modifier =
            modifier
                .widthIn(min = PlaybackFollowTokenCardMinimumWidth, max = PlaybackFollowTokenCardMaximumWidth)
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
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = tokenDisplayText,
                color = tokenColor,
                style =
                    if (isActive) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                softWrap = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Surface(
                color =
                    if (isActive) {
                        activeAnnotationContainer
                    } else {
                        Color.Transparent
                    },
                contentColor =
                    if (isActive) {
                        activeAnnotationTint
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                when (annotationMode) {
                    PlaybackFollowViewMode.Binary -> {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            annotationByteGroups.chunked(BinaryByteGroupsPerRow).forEachIndexed { rowIndex, rowGroups ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    rowGroups.forEachIndexed { groupIndexInRow, group ->
                                        val byteIndex = rowIndex * BinaryByteGroupsPerRow + groupIndexInRow
                                        PlaybackByteBlock(
                                            group = group,
                                            mode = annotationMode,
                                            isActive = isActive && byteIndex == activeByteIndexWithinToken,
                                            isPast = isActive && byteIndex < activeByteIndexWithinToken,
                                            activeBitIndex = if (isActive && byteIndex == activeByteIndexWithinToken) activeBitIndexWithinByte else -1,
                                            focusColor = focusColor,
                                            onFocusColor = onFocusColor,
                                            inactiveColor = inactiveRawColor,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    PlaybackFollowViewMode.Hex -> {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            annotationByteGroups.chunked(HexByteGroupsPerRow).forEachIndexed { rowIndex, rowGroups ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    rowGroups.forEachIndexed { groupIndexInRow, group ->
                                        val byteIndex = rowIndex * HexByteGroupsPerRow + groupIndexInRow
                                        val isActiveByte = isActive && byteIndex == activeByteIndexWithinToken
                                        PlaybackByteBlock(
                                            group = group,
                                            mode = annotationMode,
                                            isActive = isActiveByte,
                                            isPast = isActive && byteIndex < activeByteIndexWithinToken,
                                            activeBitIndex = if (isActiveByte) activeBitIndexWithinByte else -1,
                                            focusColor = focusColor,
                                            onFocusColor = onFocusColor,
                                            inactiveColor = inactiveRawColor,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val PlaybackFollowTokenCardMinimumWidth = 92.dp
private val PlaybackFollowTokenCardMaximumWidth = 360.dp
private const val BinaryByteGroupsPerRow = 3
private const val HexByteGroupsPerRow = 8

private data class CharacterDisplayUnit(
    val text: String,
    val byteStartIndexWithinToken: Int,
    val byteCount: Int,
)

private fun characterDisplayUnits(token: String): List<CharacterDisplayUnit> {
    if (token.isEmpty()) {
        return emptyList()
    }
    val units = ArrayList<CharacterDisplayUnit>()
    var index = 0
    var byteStart = 0
    while (index < token.length) {
        val codePoint = token.codePointAt(index)
        val characterText = String(Character.toChars(codePoint))
        val byteCount = characterText.toByteArray(StandardCharsets.UTF_8).size.coerceAtLeast(1)
        units +=
            CharacterDisplayUnit(
                text = characterText,
                byteStartIndexWithinToken = byteStart,
                byteCount = byteCount,
            )
        byteStart += byteCount
        index += Character.charCount(codePoint)
    }
    return units
}

@Composable
private fun PlaybackByteBlock(
    group: String,
    mode: PlaybackFollowViewMode,
    isActive: Boolean,
    isPast: Boolean,
    activeBitIndex: Int,
    focusColor: Color,
    onFocusColor: Color,
    inactiveColor: Color,
) {
    val text = buildAnnotatedString {
        if (isActive) {
            when (mode) {
                PlaybackFollowViewMode.Binary -> {
                    group.forEachIndexed { bitIndex, bitChar ->
                        val isHistoryBit = bitIndex < activeBitIndex
                        val isCurrentBit = bitIndex == activeBitIndex
                        
                        val (textColor, bgColor, weight) = when {
                            isCurrentBit -> Triple(onFocusColor, focusColor, FontWeight.Bold)
                            isHistoryBit -> Triple(focusColor, Color.Transparent, FontWeight.Medium)
                            else -> Triple(inactiveColor, Color.Transparent, FontWeight.Medium)
                        }

                        withStyle(SpanStyle(color = textColor, background = bgColor, fontWeight = weight)) {
                            append(bitChar)
                        }
                    }
                }
                PlaybackFollowViewMode.Hex -> {
                    val currentNibbleIndex = if (activeBitIndex >= 0) activeBitIndex / 4 else -1
                    group.forEachIndexed { nibbleIndex, hexChar ->
                        val isHistoryNibble = nibbleIndex < currentNibbleIndex
                        val isCurrentNibble = nibbleIndex == currentNibbleIndex

                        val (textColor, bgColor, weight) = when {
                            isCurrentNibble -> Triple(onFocusColor, focusColor, FontWeight.Bold)
                            isHistoryNibble -> Triple(focusColor, Color.Transparent, FontWeight.Medium)
                            else -> Triple(inactiveColor, Color.Transparent, FontWeight.Medium)
                        }

                        withStyle(SpanStyle(color = textColor, background = bgColor, fontWeight = weight)) {
                            append(hexChar)
                        }
                    }
                }
            }
        } else {
            withStyle(
                SpanStyle(
                    color = if (isPast) focusColor else inactiveColor,
                    fontWeight = FontWeight.Medium,
                ),
            ) {
                append(group)
            }
        }
    }

    Text(
        text = text,
        style =
            MaterialTheme.typography.labelLarge.copy(
                fontFamily = FontFamily.Monospace,
            ),
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
    )
}
