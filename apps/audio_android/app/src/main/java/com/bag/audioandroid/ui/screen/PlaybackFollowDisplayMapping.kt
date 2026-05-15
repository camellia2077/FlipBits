package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.TextFollowCharacterKind
import com.bag.audioandroid.domain.TextFollowCharacterViewData
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import java.nio.charset.StandardCharsets

internal data class MorseLetterDisplayGroup(
    val text: String,
    val morse: String,
    val byteStartIndexWithinToken: Int,
    val byteCount: Int,
)

internal fun morseLetterDisplayGroups(
    token: String,
    rawDisplayUnits: List<TextFollowRawDisplayUnitViewData>,
): List<MorseLetterDisplayGroup> =
    morseLetterDisplayGroups(
        token = token,
        characterDisplayUnits = characterDisplayUnits(token),
        rawDisplayUnits = rawDisplayUnits,
        annotationByteGroups = annotationByteGroupsForMode(PlaybackFollowViewMode.Morse, rawDisplayUnits),
    )

internal fun morseLetterDisplayGroups(
    token: String,
    characterDisplayUnits: List<CharacterDisplayUnit>,
    rawDisplayUnits: List<TextFollowRawDisplayUnitViewData>,
    annotationByteGroups: List<String>,
): List<MorseLetterDisplayGroup> {
    if (token.isEmpty() || characterDisplayUnits.isEmpty()) {
        return annotationByteGroups.mapIndexed { index, group ->
            MorseLetterDisplayGroup(
                text = group,
                morse = group,
                byteStartIndexWithinToken = index,
                byteCount = 1,
            )
        }
    }
    return characterDisplayUnits
        .mapNotNull { character ->
            val firstByte =
                rawDisplayUnits.firstOrNull { unit ->
                    unit.byteIndexWithinToken >= character.byteStartIndexWithinToken &&
                        unit.byteIndexWithinToken < character.byteStartIndexWithinToken + character.byteCount
                } ?: return@mapNotNull null
            val morse = annotationByteGroups.getOrNull(rawDisplayUnits.indexOf(firstByte)).orEmpty()
            if (morse.isBlank()) {
                null
            } else {
                MorseLetterDisplayGroup(
                    text = character.text,
                    morse = morse,
                    byteStartIndexWithinToken = character.byteStartIndexWithinToken,
                    byteCount = character.byteCount,
                )
            }
        }.ifEmpty {
            annotationByteGroups.mapIndexed { index, group ->
                MorseLetterDisplayGroup(
                    text = group,
                    morse = group,
                    byteStartIndexWithinToken = index,
                    byteCount = 1,
                )
            }
        }
}

internal data class CharacterDisplayUnit(
    val text: String,
    val byteStartIndexWithinToken: Int,
    val byteCount: Int,
)

internal fun characterDisplayUnits(
    token: String,
    textCharacters: List<TextFollowCharacterViewData>,
): List<CharacterDisplayUnit> =
    if (textCharacters.isNotEmpty()) {
        textCharacters
            .sortedWith(
                compareBy(
                    TextFollowCharacterViewData::characterIndexWithinToken,
                    TextFollowCharacterViewData::byteIndexWithinToken,
                ),
            ).map { entry ->
                CharacterDisplayUnit(
                    text = entry.layoutText,
                    byteStartIndexWithinToken = entry.byteIndexWithinToken,
                    byteCount = entry.byteCount,
                )
            }
    } else {
        characterDisplayUnits(token)
    }

internal fun characterDisplayUnits(token: String): List<CharacterDisplayUnit> {
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

private val TextFollowCharacterViewData.layoutText: String
    get() =
        when (kind) {
            TextFollowCharacterKind.Visible -> text
            // Separator characters still need layout width so token highlighting
            // can stay aligned with the byte timeline while remaining visually blank.
            TextFollowCharacterKind.Space,
            TextFollowCharacterKind.Newline,
            TextFollowCharacterKind.SeparatorOther,
            -> "\u00A0".repeat(text.codePointCount(0, text.length).coerceAtLeast(1))
        }

internal enum class AnnotationDividerStyle {
    Thin,
    Strong,
}

internal fun annotationDividerStylesByBoundary(
    rawDisplayUnits: List<TextFollowRawDisplayUnitViewData>,
    fallbackCharacterDisplayUnits: List<CharacterDisplayUnit> = emptyList(),
): Map<Int, AnnotationDividerStyle> {
    if (rawDisplayUnits.isNotEmpty()) {
        val sortedUnits = rawDisplayUnits.sortedBy(TextFollowRawDisplayUnitViewData::byteIndexWithinToken)
        if (sortedUnits.any { it.characterByteCount > 0 || it.isCharacterStart || it.isCharacterEnd }) {
            return buildMap(sortedUnits.size) {
                for (index in 0 until sortedUnits.lastIndex) {
                    val unit = sortedUnits[index]
                    put(
                        unit.byteIndexWithinToken + unit.byteCount,
                        if (unit.isCharacterEnd) {
                            AnnotationDividerStyle.Strong
                        } else {
                            AnnotationDividerStyle.Thin
                        },
                    )
                }
            }
        }
    }

    if (fallbackCharacterDisplayUnits.isEmpty()) {
        return emptyMap()
    }
    return buildMap {
        fallbackCharacterDisplayUnits
            .dropLast(1)
            .forEach { unit ->
                put(unit.byteStartIndexWithinToken + unit.byteCount, AnnotationDividerStyle.Strong)
            }
        if (rawDisplayUnits.isNotEmpty()) {
            val sortedUnits = rawDisplayUnits.sortedBy(TextFollowRawDisplayUnitViewData::byteIndexWithinToken)
            for (index in 0 until sortedUnits.lastIndex) {
                val boundaryIndex =
                    sortedUnits[index].byteIndexWithinToken + sortedUnits[index].byteCount
                putIfAbsent(boundaryIndex, AnnotationDividerStyle.Thin)
            }
        }
    }
}

internal data class HexNibbleGroup(
    val hex: String,
    val binary: String,
)

internal fun hexNibbleGroups(group: String): List<HexNibbleGroup> =
    group
        .mapNotNull { hexChar ->
            hexChar.digitToIntOrNull(radix = 16)?.let { value ->
                HexNibbleGroup(
                    hex = hexChar.uppercaseChar().toString(),
                    binary = value.toString(radix = 2).padStart(4, '0'),
                )
            }
        }.ifEmpty {
            listOf(HexNibbleGroup(hex = group, binary = "0000"))
        }
