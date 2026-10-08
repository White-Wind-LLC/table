package ua.wwind.table.format.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import kotlin.math.min

/** The WCAG 2 AA minimum contrast ratio for normal text. */
internal const val MIN_TEXT_CONTRAST = 4.5

private const val LUMINANCE_OFFSET = 0.05
private const val RGB_DIGITS = 6
private const val ARGB_DIGITS = 8
private const val OPAQUE_ALPHA = 0xFF000000
private const val RGB_MASK = 0xFFFFFFL
private const val ARGB_MASK = 0xFFFFFFFFL
private const val HEX_RADIX = 16

/** `#RRGGBB` for an opaque color, `#AARRGGBB` otherwise, and empty for [Color.Unspecified]. */
internal fun Color.toHex(): String {
    if (!isSpecified) return ""
    val argb = toArgb().toLong() and ARGB_MASK
    val digits = if (alpha == 1f) RGB_DIGITS else ARGB_DIGITS
    val masked = if (digits == RGB_DIGITS) argb and RGB_MASK else argb
    return "#" + masked.toString(HEX_RADIX).uppercase().padStart(digits, '0')
}

/** Parses `#RRGGBB` or `#AARRGGBB` (the `#` is optional); `null` for anything else. */
internal fun parseHexColor(text: String): Color? {
    val digits = text.trim().removePrefix("#")
    if (digits.length != RGB_DIGITS && digits.length != ARGB_DIGITS) return null
    if (!digits.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' }) return null
    val value = digits.toLong(HEX_RADIX)
    return Color(if (digits.length == RGB_DIGITS) value or OPAQUE_ALPHA else value)
}

/**
 * The WCAG 2 contrast ratio of two colors, from 1 to 21. A translucent [background] is composited over
 * white and a translucent [foreground] over the background.
 */
internal fun contrastRatio(
    foreground: Color,
    background: Color,
): Double {
    val solidBackground = background.compositeOver(Color.White)
    val fg = foreground.compositeOver(solidBackground).luminance().toDouble()
    val bg = solidBackground.luminance().toDouble()
    return (max(fg, bg) + LUMINANCE_OFFSET) / (min(fg, bg) + LUMINANCE_OFFSET)
}

/** Black or white, whichever contrasts more with this color as a background. */
internal fun Color.readableContentColor(): Color =
    if (contrastRatio(Color.Black, this) >= contrastRatio(Color.White, this)) Color.Black else Color.White
