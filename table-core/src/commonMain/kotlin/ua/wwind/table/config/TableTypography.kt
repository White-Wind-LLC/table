package ua.wwind.table.config

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle

/**
 * Text styles of the table rows, applied with `ProvideTextStyle` so cell content that reads
 * `LocalTextStyle` picks them up. A cell style's `textStyle` still merges over [body].
 */
@Immutable
public data class TableTypography(
    val header: TextStyle,
    val body: TextStyle,
    val footer: TextStyle,
    val groupHeader: TextStyle,
)

/** Set by the table root; the default only serves components composed outside a table. */
internal val LocalTableTypography: ProvidableCompositionLocal<TableTypography> =
    staticCompositionLocalOf {
        TableTypography(
            header = TextStyle.Default,
            body = TextStyle.Default,
            footer = TextStyle.Default,
            groupHeader = TextStyle.Default,
        )
    }
