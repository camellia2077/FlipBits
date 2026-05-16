package com.bag.audioandroid.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.BuildConfig
import kotlin.math.max

@Composable
internal fun MorseLetterBlock(
    letter: String,
    morse: String,
    isActive: Boolean,
    isPast: Boolean,
    activeBitIndex: Int,
    isActiveBitTone: Boolean,
    focusColor: Color,
    onFocusColor: Color,
    inactiveColor: Color,
) {
    val letterColor =
        when {
            isActive -> onFocusColor
            isPast -> focusColor
            else -> inactiveColor
        }
    val letterBackground = if (isActive) focusColor else Color.Transparent
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text =
                buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = letterColor,
                            background = letterBackground,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        ),
                    ) {
                        append(letter)
                    }
                },
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
        PlaybackByteBlock(
            group = morse,
            mode = PlaybackFollowViewMode.Morse,
            isActive = isActive,
            isPast = isPast,
            activeBitIndex = activeBitIndex,
            isActiveBitTone = isActiveBitTone,
            focusColor = focusColor,
            onFocusColor = onFocusColor,
            inactiveColor = inactiveColor,
            modifier = Modifier.wrapContentWidth(),
        )
    }
}

internal fun morsePatternVisualWidthDp(morse: String): Dp {
    val symbolCount = morse.count { it == '.' || it == '-' }
    if (symbolCount == 0) {
        return MorseDotWidth
    }
    val symbolWidths =
        morse.fold(0.dp) { width, symbol ->
            width +
                when (symbol) {
                    '.' -> MorseDotWidth
                    '-' -> MorseDashWidth
                    else -> 0.dp
                }
        }
    val gapWidth = if (symbolCount > 1) MorseElementGap * (symbolCount - 1).toFloat() else 0.dp
    return symbolWidths + gapWidth
}

internal fun morseLetterVisualWidthDp(
    letter: String,
    morse: String,
): Dp = max(morsePatternVisualWidthDp(morse).value, morseLabelWidthDp(letter).value).dp

private fun morseLabelWidthDp(letter: String): Dp =
    when {
        letter.isEmpty() -> MorseLetterMinimumLabelWidth
        letter.length > 1 ->
            (
                MorseLetterMinimumLabelWidth +
                    MorseExtraLabelWidthPerCharacter * (letter.length - 1).toFloat()
            ).coerceAtLeast(MorseLetterMinimumLabelWidth)
        else -> MorseLetterMinimumLabelWidth
    }

@Composable
internal fun PlaybackHexByteBlock(
    group: String,
    isActive: Boolean,
    isPast: Boolean,
    activeBitIndex: Int,
    isActiveBitTone: Boolean,
    focusColor: Color,
    onFocusColor: Color,
    inactiveColor: Color,
) {
    val nibbles = hexNibbleGroups(group)
    val currentNibbleIndex =
        if (isActive && activeBitIndex >= 4 && nibbles.size > 1) {
            1
        } else {
            0
        }.coerceAtMost(nibbles.lastIndex.coerceAtLeast(0))
    val binaryText = nibbles.joinToString(separator = "") { it.binary }
    PlaybackHexByteVisualBlock(
        hexText = group.uppercase(),
        binaryText = binaryText,
        isActive = isActive,
        isPast = isPast,
        activeNibbleIndex = if (isActive) currentNibbleIndex else -1,
        activeBitIndex = if (isActive) activeBitIndex else -1,
        isActiveBitTone = isActiveBitTone,
        focusColor = focusColor,
        onFocusColor = onFocusColor,
        inactiveColor = inactiveColor,
    )
}

@Composable
private fun PlaybackHexByteVisualBlock(
    hexText: String,
    binaryText: String,
    isActive: Boolean,
    isPast: Boolean,
    activeNibbleIndex: Int,
    activeBitIndex: Int,
    isActiveBitTone: Boolean,
    focusColor: Color,
    onFocusColor: Color,
    inactiveColor: Color,
) {
    val annotatedHexText =
        buildAnnotatedString {
            hexText.forEachIndexed { nibbleIndex, hexChar ->
                val isCurrentNibble = isActive && isActiveBitTone && nibbleIndex == activeNibbleIndex
                val isHistoryNibble =
                    isPast ||
                        (
                            isActive &&
                                activeNibbleIndex >= 0 &&
                                (
                                    if (isActiveBitTone) {
                                        nibbleIndex < activeNibbleIndex
                                    } else {
                                        nibbleIndex <= activeNibbleIndex
                                    }
                                )
                        )
                val (textColor, backgroundColor, weight) =
                    when {
                        isCurrentNibble -> Triple(onFocusColor, focusColor, FontWeight.Bold)
                        isHistoryNibble -> Triple(focusColor, Color.Transparent, FontWeight.Medium)
                        else -> Triple(inactiveColor, Color.Transparent, FontWeight.Medium)
                    }
                withStyle(SpanStyle(color = textColor, background = backgroundColor, fontWeight = weight)) {
                    append(hexChar)
                }
            }
        }
    val annotatedBinaryText =
        buildAnnotatedString {
            binaryText.forEachIndexed { bitIndex, bitChar ->
                val isCurrentBit = isActive && isActiveBitTone && bitIndex == activeBitIndex
                val isHistoryBit =
                    isPast ||
                        (
                            isActive &&
                                activeBitIndex >= 0 &&
                                (
                                    if (isActiveBitTone) {
                                        bitIndex < activeBitIndex
                                    } else {
                                        bitIndex <= activeBitIndex
                                    }
                                )
                        )
                val (textColor, backgroundColor, weight) =
                    when {
                        isCurrentBit -> Triple(onFocusColor, focusColor, FontWeight.Bold)
                        isHistoryBit -> Triple(focusColor, Color.Transparent, FontWeight.Medium)
                        else -> Triple(inactiveColor, Color.Transparent, FontWeight.Medium)
                    }
                withStyle(SpanStyle(color = textColor, background = backgroundColor, fontWeight = weight)) {
                    append(bitChar)
                }
            }
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = annotatedHexText,
            style =
                MaterialTheme.typography.labelLarge.copy(
                    fontFamily = FontFamily.Monospace,
                ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = annotatedBinaryText,
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
internal fun PlaybackByteBlock(
    group: String,
    mode: PlaybackFollowViewMode,
    isActive: Boolean,
    isPast: Boolean,
    activeBitIndex: Int,
    isActiveBitTone: Boolean,
    tokenIndex: Int = -1,
    tokenText: String = "_",
    byteIndexWithinToken: Int = -1,
    globalByteOffset: Int = -1,
    displayedSamples: Int = -1,
    followData: com.bag.audioandroid.domain.PayloadFollowViewData? = null,
    focusColor: Color,
    onFocusColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
) {
    SideEffect {
        PlaybackFollowBinaryRenderTrace.record(
            mode = mode,
            group = group,
            isActive = isActive,
            activeBitIndex = activeBitIndex,
            isActiveBitTone = isActiveBitTone,
            tokenIndex = tokenIndex,
            tokenText = tokenText,
            byteIndexWithinToken = byteIndexWithinToken,
            globalByteOffset = globalByteOffset,
            displayedSamples = displayedSamples,
            followData = followData,
        )
    }
    val text =
        buildAnnotatedString {
            if (isActive) {
                when (mode) {
                    PlaybackFollowViewMode.Binary -> {
                        group.forEachIndexed { bitIndex, bitChar ->
                            val isCurrentBit = isActiveBitTone && bitIndex == activeBitIndex
                            val isHistoryBit =
                                if (isActiveBitTone) {
                                    bitIndex < activeBitIndex
                                } else {
                                    bitIndex <= activeBitIndex
                                }

                            val (textColor, bgColor, weight) =
                                when {
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
                            val isCurrentNibble = isActiveBitTone && nibbleIndex == currentNibbleIndex
                            val isHistoryNibble =
                                if (isActiveBitTone) {
                                    nibbleIndex < currentNibbleIndex
                                } else {
                                    nibbleIndex <= currentNibbleIndex
                                }

                            val (textColor, bgColor, weight) =
                                when {
                                    isCurrentNibble -> Triple(onFocusColor, focusColor, FontWeight.Bold)
                                    isHistoryNibble -> Triple(focusColor, Color.Transparent, FontWeight.Medium)
                                    else -> Triple(inactiveColor, Color.Transparent, FontWeight.Medium)
                                }

                            withStyle(SpanStyle(color = textColor, background = bgColor, fontWeight = weight)) {
                                append(hexChar)
                            }
                        }
                    }
                    PlaybackFollowViewMode.Morse -> {
                        group.forEachIndexed { elementIndex, elementChar ->
                            val isCurrentElement = isActiveBitTone && elementIndex == activeBitIndex
                            val isHistoryElement =
                                if (isActiveBitTone) {
                                    elementIndex < activeBitIndex
                                } else {
                                    elementIndex <= activeBitIndex
                                }

                            val (textColor, bgColor, weight) =
                                when {
                                    isCurrentElement -> Triple(onFocusColor, focusColor, FontWeight.Bold)
                                    isHistoryElement -> Triple(focusColor, Color.Transparent, FontWeight.Medium)
                                    else -> Triple(inactiveColor, Color.Transparent, FontWeight.Medium)
                                }

                            withStyle(SpanStyle(color = textColor, background = bgColor, fontWeight = weight)) {
                                append(elementChar)
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

    Box(
        modifier =
            if (mode == PlaybackFollowViewMode.Morse) {
                modifier.wrapContentWidth()
            } else {
                modifier
            },
        contentAlignment = Alignment.Center,
    ) {
        if (mode == PlaybackFollowViewMode.Morse) {
            MorsePatternVisual(
                morse = group,
                isActive = isActive,
                isPast = isPast,
                activeBitIndex = activeBitIndex,
                isActiveBitTone = isActiveBitTone,
                focusColor = focusColor,
                inactiveColor = inactiveColor,
            )
        } else {
            Text(
                modifier = Modifier.fillMaxWidth(),
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
    }
}

@Composable
private fun MorsePatternVisual(
    morse: String,
    isActive: Boolean,
    isPast: Boolean,
    activeBitIndex: Int,
    isActiveBitTone: Boolean,
    focusColor: Color,
    inactiveColor: Color,
) {
    val elements = morse.filter { it == '.' || it == '-' }
    if (elements.isEmpty()) {
        Spacer(modifier = Modifier.width(MorseDotWidth).height(MorseElementHeight))
        return
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(MorseElementGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        elements.forEachIndexed { index, element ->
            val isCurrentElement = isActive && isActiveBitTone && index == activeBitIndex
            val isHistoryElement =
                isPast ||
                    (
                        isActive &&
                            activeBitIndex >= 0 &&
                            if (isActiveBitTone) {
                                index < activeBitIndex
                            } else {
                                index <= activeBitIndex
                            }
                    )
            val elementColor =
                when {
                    isCurrentElement -> focusColor
                    isHistoryElement -> focusColor.copy(alpha = 0.84f)
                    else -> inactiveColor
                }
            Box(
                modifier =
                    Modifier
                        .width(if (element == '-') MorseDashWidth else MorseDotWidth)
                        .height(MorseElementHeight)
                        .background(
                            color = elementColor,
                            shape = MaterialTheme.shapes.extraSmall,
                        ),
            )
        }
    }
}

internal val MorseDotWidth = 6.dp
internal val MorseDashWidth = 16.dp
internal val MorseElementGap = 4.dp
internal val MorseElementHeight = 6.dp
private val MorseLetterMinimumLabelWidth = 10.dp
private val MorseExtraLabelWidthPerCharacter = 6.dp

private object PlaybackFollowBinaryRenderTrace {
    private const val Tag = "FlashAlignmentPerf"
    private const val ReportIntervalNanos = 500_000_000L
    private var lastReportNanos = 0L

    fun record(
        mode: PlaybackFollowViewMode,
        group: String,
        isActive: Boolean,
        activeBitIndex: Int,
        isActiveBitTone: Boolean,
        tokenIndex: Int,
        tokenText: String,
        byteIndexWithinToken: Int,
        globalByteOffset: Int,
        displayedSamples: Int,
        followData: com.bag.audioandroid.domain.PayloadFollowViewData?,
    ) {
        if (!BuildConfig.DEBUG) {
            return
        }
        if (mode != PlaybackFollowViewMode.Binary || !isActive || activeBitIndex < 0 || displayedSamples < 0) {
            return
        }
        val playbackSource = followData?.toFlashBitReadoutSource() ?: return
        val playbackGlobalBit = playbackSource.currentBitOffsetAtSample(displayedSamples.toFloat()) ?: return
        val renderedGlobalBit = (globalByteOffset * 8 + activeBitIndex).takeIf { globalByteOffset >= 0 } ?: return
        val renderedBitValue = group.filter { it == '0' || it == '1' }.getOrNull(activeBitIndex)?.toString() ?: "_"
        val playbackBitValue = playbackSource.bitByOffset[playbackGlobalBit]?.toString() ?: "_"
        val now = System.nanoTime()
        if (lastReportNanos != 0L && now - lastReportNanos < ReportIntervalNanos) {
            return
        }
        lastReportNanos = now
        try {
            Log.d(
                Tag,
                "reason=token-binary-render-playback " +
                    "sample=$displayedSamples token=$tokenIndex tokenText=${tokenText.logSafe()} " +
                    "tokenViewMode=binary byteIndexWithinToken=$byteIndexWithinToken globalByteOffset=$globalByteOffset " +
                    "renderedBitIndexWithinByte=$activeBitIndex renderedGlobalBitOffset=$renderedGlobalBit " +
                    "renderedBitValue=$renderedBitValue toneActive=$isActiveBitTone " +
                    "playbackGlobalBit=$playbackGlobalBit playbackCurrentBitValue=$playbackBitValue " +
                    "playbackMinusRenderedBit=${playbackGlobalBit - renderedGlobalBit}",
            )
        } catch (_: Throwable) {
        }
    }

    private fun String.logSafe(): String =
        replace('\n', ' ')
            .replace('\r', ' ')
            .take(64)
            .ifBlank { "_" }
}
