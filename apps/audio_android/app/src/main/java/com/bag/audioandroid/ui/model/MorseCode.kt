package com.bag.audioandroid.ui.model

import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.bag.audioandroid.R

@Keep
enum class MorseSpeedOption(
    val id: String,
    @param:StringRes val labelResId: Int,
    private val frameSampleNumerator: Int,
    private val frameSampleDenominator: Int,
) {
    Wpm10("wpm10", R.string.audio_morse_speed_wpm10, 12, 5),
    Wpm15("wpm15", R.string.audio_morse_speed_wpm15, 8, 5),
    Wpm20("wpm20", R.string.audio_morse_speed_wpm20, 6, 5),
    ;

    fun frameSamples(defaultFrameSamples: Int): Int =
        ((defaultFrameSamples * frameSampleNumerator) / frameSampleDenominator).coerceAtLeast(1)

    companion object {
        val default: MorseSpeedOption = Wpm15

        fun fromFrameSamples(
            frameSamples: Int,
            defaultFrameSamples: Int = 2205,
        ): MorseSpeedOption =
            entries.minBy { option ->
                kotlin.math.abs(option.frameSamples(defaultFrameSamples) - frameSamples)
            }

        fun fromId(id: String?): MorseSpeedOption = entries.firstOrNull { it.id == id?.trim()?.lowercase() } ?: default
    }
}

internal data class MorseTextAnalysis(
    val normalizedText: String,
    val unsupportedCharacters: List<Char>,
) {
    val isValid: Boolean
        get() = unsupportedCharacters.isEmpty()

    val morseNotation: String
        get() =
            if (normalizedText.isBlank()) {
                ""
            } else {
                normalizedText
                    .split(' ')
                    .joinToString(" / ") { word ->
                        word.mapNotNull(::morsePatternForChar).joinToString(" ")
                    }
            }
}

internal fun analyzeMorseText(text: String): MorseTextAnalysis {
    val unsupported = linkedSetOf<Char>()
    val normalized = StringBuilder(text.length)
    var previousWasSpace = false
    var sawWhitespace = false

    text.forEach { raw ->
        val ch = raw.uppercaseChar()
        if (raw.isWhitespace()) {
            sawWhitespace = true
            if (!previousWasSpace && normalized.isNotEmpty()) {
                normalized.append(' ')
            }
            previousWasSpace = true
            return@forEach
        }
        previousWasSpace = false
        if (morsePatternForChar(ch) == null) {
            unsupported += raw
        } else {
            normalized.append(ch)
        }
    }
    while (normalized.isNotEmpty() && normalized.last() == ' ') {
        normalized.deleteAt(normalized.lastIndex)
    }
    if (normalized.isEmpty() && sawWhitespace) {
        normalized.append(' ')
    }

    return MorseTextAnalysis(
        normalizedText = normalized.toString(),
        unsupportedCharacters = unsupported.toList(),
    )
}

internal data class MorseNotationTranslation(
    val text: String,
    val invalidPatterns: List<String>,
    val isMorseLike: Boolean,
) {
    val isValid: Boolean
        get() = isMorseLike && invalidPatterns.isEmpty()
}

internal fun translateMorseNotation(notation: String): MorseNotationTranslation {
    val trimmed = notation.trim()
    if (trimmed.isEmpty()) {
        return MorseNotationTranslation(text = "", invalidPatterns = emptyList(), isMorseLike = false)
    }
    if (!trimmed.all(::isMorseNotationChar)) {
        return MorseNotationTranslation(text = "", invalidPatterns = emptyList(), isMorseLike = false)
    }

    val tokens = normalizedMorseNotationTokens(trimmed)
    val invalidPatterns = linkedSetOf<String>()
    val translated = StringBuilder()
    var pendingWordSpace = false

    tokens.forEach { token ->
        if (token == MorseWordSeparatorToken) {
            if (translated.isNotEmpty()) {
                pendingWordSpace = true
            }
            return@forEach
        }
        val letter = charForMorsePattern(token)
        if (letter == null) {
            invalidPatterns += token
            return@forEach
        }
        if (pendingWordSpace && translated.isNotEmpty()) {
            translated.append(' ')
        }
        pendingWordSpace = false
        translated.append(letter)
    }

    return MorseNotationTranslation(
        text = translated.toString(),
        invalidPatterns = invalidPatterns.toList(),
        isMorseLike = true,
    )
}

// UI-side preview mirrors the core Morse table for editing feedback only. Core
// validation/encoding remains the authority for the generated payload.
internal fun morsePatternForChar(ch: Char): String? =
    when (ch.uppercaseChar()) {
        'A' -> ".-"
        'B' -> "-..."
        'C' -> "-.-."
        'D' -> "-.."
        'E' -> "."
        'F' -> "..-."
        'G' -> "--."
        'H' -> "...."
        'I' -> ".."
        'J' -> ".---"
        'K' -> "-.-"
        'L' -> ".-.."
        'M' -> "--"
        'N' -> "-."
        'O' -> "---"
        'P' -> ".--."
        'Q' -> "--.-"
        'R' -> ".-."
        'S' -> "..."
        'T' -> "-"
        'U' -> "..-"
        'V' -> "...-"
        'W' -> ".--"
        'X' -> "-..-"
        'Y' -> "-.--"
        'Z' -> "--.."
        '0' -> "-----"
        '1' -> ".----"
        '2' -> "..---"
        '3' -> "...--"
        '4' -> "....-"
        '5' -> "....."
        '6' -> "-...."
        '7' -> "--..."
        '8' -> "---.."
        '9' -> "----."
        '.' -> ".-.-.-"
        ',' -> "--..--"
        '?' -> "..--.."
        '\'' -> ".----."
        '!' -> "-.-.--"
        '/' -> "-..-."
        '(' -> "-.--."
        ')' -> "-.--.-"
        '&' -> ".-..."
        ':' -> "---..."
        ';' -> "-.-.-."
        '=' -> "-...-"
        '+' -> ".-.-."
        '-' -> "-....-"
        '_' -> "..--.-"
        '"' -> ".-..-."
        '$' -> "...-..-"
        '@' -> ".--.-."
        else -> null
    }

private fun charForMorsePattern(pattern: String): Char? = MorsePatternToChar[pattern]

private fun isMorseNotationChar(ch: Char): Boolean =
    ch.isWhitespace() ||
        ch == '.' ||
        ch == '-' ||
        ch == '/' ||
        ch == '|' ||
        ch == '·' ||
        ch == '−' ||
        ch == '–' ||
        ch == '—'

private fun normalizedMorseNotationTokens(notation: String): List<String> {
    val normalized = mutableListOf<String>()
    val currentPattern = StringBuilder()
    var whitespaceCount = 0

    fun flushPattern() {
        if (currentPattern.isNotEmpty()) {
            normalized += currentPattern.toString()
            currentPattern.clear()
        }
    }

    fun flushWhitespace() {
        if (whitespaceCount >= MorseWordSeparatorWhitespaceCount) {
            flushPattern()
            normalized += MorseWordSeparatorToken
        } else if (whitespaceCount > 0) {
            flushPattern()
        }
        whitespaceCount = 0
    }

    notation.forEach { ch ->
        when {
            ch.isWhitespace() -> whitespaceCount++
            ch == '/' || ch == '|' -> {
                flushWhitespace()
                flushPattern()
                normalized += MorseWordSeparatorToken
            }
            else -> {
                flushWhitespace()
                currentPattern.append(normalizedMorseMark(ch))
            }
        }
    }
    flushWhitespace()
    flushPattern()

    return normalized
}

private fun normalizedMorseMark(ch: Char): Char =
    when (ch) {
        '·' -> '.'
        '−', '–', '—' -> '-'
        else -> ch
    }

private val MorsePatternToChar: Map<String, Char> =
    buildMap {
        val supportedCharacters =
            ('A'..'Z') +
                ('0'..'9') +
                listOf('.', ',', '?', '\'', '!', '/', '(', ')', '&', ':', ';', '=', '+', '-', '_', '"', '$', '@')
        supportedCharacters.forEach { ch ->
            morsePatternForChar(ch)?.let { pattern ->
                put(pattern, ch)
            }
        }
    }

private const val MorseWordSeparatorToken = "/"
private const val MorseWordSeparatorWhitespaceCount = 3
