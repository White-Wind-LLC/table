package ua.wwind.table.platform

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlin.math.abs

/**
 * A locale's numeric date pattern reduced to its field order and separators. Formatting reads the
 * fields straight from [LocalDate], so every platform shows the same proleptic Gregorian date with a
 * four-digit year and ASCII digits.
 */
internal class DatePattern private constructor(
    private val parts: List<Part>,
) {
    fun format(date: LocalDate): String =
        parts.joinToString("") { part ->
            when (part) {
                Part.Year -> date.year.formatYear()
                Part.Month -> date.month.number.twoDigits()
                Part.Day -> date.day.twoDigits()
                is Part.Literal -> part.text
            }
        }

    private sealed interface Part {
        data object Year : Part

        data object Month : Part

        data object Day : Part

        data class Literal(
            val text: String,
        ) : Part
    }

    /** Splits a pattern into fields and the literal text between them. */
    private class Parser(
        private val pattern: String,
    ) {
        private val parts = mutableListOf<Part>()
        private val literal = StringBuilder()
        private var i = 0

        fun parse(): List<Part> {
            while (i < pattern.length) {
                val c = pattern[i]
                when {
                    c == QUOTE -> readQuoted()
                    c.isAsciiLetter() -> readField(c)
                    else -> literal.append(pattern[i++])
                }
            }
            flushLiteral()
            return parts
        }

        // `''` is a quote; otherwise the text runs to the next lone quote, and `''` inside it is a quote.
        private fun readQuoted() {
            i++
            if (pattern.getOrNull(i) == QUOTE) {
                literal.append(QUOTE)
                i++
                return
            }
            while (i < pattern.length && !(pattern[i] == QUOTE && pattern.getOrNull(i + 1) != QUOTE)) {
                if (pattern[i] == QUOTE) i++
                literal.append(pattern[i++])
            }
            i++
        }

        private fun readField(letter: Char) {
            while (i < pattern.length && pattern[i] == letter) i++
            val field =
                when (letter) {
                    'y', 'u' -> Part.Year
                    'M', 'L' -> Part.Month
                    'd' -> Part.Day
                    else -> null
                }
            if (field != null) {
                flushLiteral()
                parts += field
            }
        }

        private fun flushLiteral() {
            if (literal.isEmpty()) return
            parts += Part.Literal(literal.toString())
            literal.clear()
        }

        private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'
    }

    companion object {
        private const val QUOTE = '\''

        /**
         * Parses a CLDR date pattern (`dd.MM.y`, `y'年'M'月'd'日'`). Any run of `y`/`u`, `M`/`L` or `d` is
         * that field at full width; other letters (era, weekday) are dropped; quoted text is literal.
         */
        fun parse(pattern: String): DatePattern = DatePattern(Parser(pattern).parse())

        private fun Int.twoDigits(): String = toString().padStart(2, '0')

        private fun Int.formatYear(): String {
            val digits = abs(this).toString().padStart(4, '0')
            return if (this < 0) "-$digits" else digits
        }
    }
}
