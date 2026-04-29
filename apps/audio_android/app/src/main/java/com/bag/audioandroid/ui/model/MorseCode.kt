package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import com.bag.audioandroid.R

enum class MorseSpeedOption(
    @param:StringRes val labelResId: Int,
    private val frameSampleNumerator: Int,
    private val frameSampleDenominator: Int,
) {
    Slow(R.string.audio_morse_speed_slow, 3, 2),
    Standard(R.string.audio_morse_speed_standard, 1, 1),
    Fast(R.string.audio_morse_speed_fast, 1, 2),
    ;

    fun frameSamples(defaultFrameSamples: Int): Int =
        ((defaultFrameSamples * frameSampleNumerator) / frameSampleDenominator).coerceAtLeast(1)

    companion object {
        val default: MorseSpeedOption = Standard
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
            normalizedText
                .split(' ')
                .joinToString(" / ") { word ->
                    word.mapNotNull(::morsePatternForChar).joinToString(" ")
                }
}

internal fun analyzeMorseText(text: String): MorseTextAnalysis {
    val unsupported = linkedSetOf<Char>()
    val normalized = StringBuilder(text.length)
    var previousWasSpace = false

    text.forEach { raw ->
        val ch = raw.uppercaseChar()
        if (ch == ' ') {
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

    return MorseTextAnalysis(
        normalizedText = normalized.toString(),
        unsupportedCharacters = unsupported.toList(),
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
