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
    modifier: Modifier = Modifier,
) {
    val visualTokens = appThemeVisualTokens()
    val activeAnnotationContainer = Color.Transparent
    val lyricsAccentTextColor = playbackLyricsAccentTextColor()
    val focusColor = MaterialTheme.colorScheme.primary
    val onFocusColor = MaterialTheme.colorScheme.onPrimary
    val activeAnnotationTint = lyricsAccentTextColor
    val containerColor = visualTokens.followTokenContainerColor
    val tokenColor =
        if (isActive) {
            lyricsAccentTextColor
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val inactiveRawColor = MaterialTheme.colorScheme.onSurfaceVariant
    val inactiveCharacterColor = MaterialTheme.colorScheme.onSurfaceVariant
    val annotationByteGroups = annotationByteGroupsForMode(annotationMode, rawDisplayUnits)
    val characterDisplayUnits = remember(token) { characterDisplayUnits(token) }
    val shouldShowCharacterTrack = remember(token, characterDisplayUnits) { shouldShowCharacterTrack(token, characterDisplayUnits) }
    val rawText =
        remember(
            annotationByteGroups,
            activeByteIndexWithinToken,
            isActive,
            inactiveRawColor,
            activeAnnotationContainer,
            activeAnnotationTint,
        ) {
            buildAnnotatedString {
                annotationByteGroups.forEachIndexed { index, group ->
                    val isActiveByte = isActive && index == activeByteIndexWithinToken
                    if (index > 0 && !isActiveByte) {
                        append(" ")
                    }
                    withStyle(
                        SpanStyle(
                            color = if (isActiveByte) onFocusColor else inactiveRawColor,
                            background = if (isActiveByte) focusColor else Color.Transparent,
                            fontWeight = if (isActiveByte) FontWeight.Bold else FontWeight.Medium,
                        ),
                    ) {
                        if (isActiveByte) {
                            append(if (index > 0) " $group " else "$group ")
                        } else {
                            append(group)
                        }
                    }
                }
            }
        }
    val characterTrackText =
        remember(
            characterDisplayUnits,
            activeByteIndexWithinToken,
            isActive,
            inactiveCharacterColor,
            lyricsAccentTextColor,
        ) {
            buildAnnotatedString {
                characterDisplayUnits.forEachIndexed { index, unit ->
                    val isActiveCharacter =
                        isActive &&
                            activeByteIndexWithinToken >= unit.byteStartIndexWithinToken &&
                            activeByteIndexWithinToken <
                            unit.byteStartIndexWithinToken + unit.byteCount
                    if (index > 0 && !isActiveCharacter) {
                        append(" ")
                    }
                    withStyle(
                        SpanStyle(
                            color = if (isActiveCharacter) onFocusColor else inactiveCharacterColor,
                            background = if (isActiveCharacter) focusColor else Color.Transparent,
                            fontWeight = if (isActiveCharacter) FontWeight.Bold else FontWeight.Medium,
                        ),
                    ) {
                        if (isActiveCharacter) {
                            append(if (index > 0) " ${unit.text} " else "${unit.text} ")
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
            if (shouldShowCharacterTrack) {
                Text(
                    text = characterTrackText,
                    color = inactiveCharacterColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                )
            }
        }
    }
}

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

private fun shouldShowCharacterTrack(
    token: String,
    characterDisplayUnits: List<CharacterDisplayUnit>,
): Boolean {
    if (characterDisplayUnits.size < 2) {
        return false
    }
    val trimmed = token.trim()
    if (trimmed.isEmpty()) {
        return false
    }
    if (trimmed.all { Character.isDigit(it) }) {
        return false
    }
    if (trimmed.all { !Character.isLetterOrDigit(it) }) {
        return false
    }
    return true
}
