package ua.wwind.table.format.data

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import ua.wwind.table.config.TableMotion

@Immutable
public data class FormatDialogSettings(
    val copiedItemHighlightDuration: Long = 3000,
    val copiedItemHighlightColor: Color = Color.Unspecified,
    /** Whether the dialog animates; [TableMotion.System] follows the platform's reduced-motion setting. */
    val motion: TableMotion = TableMotion.System,
)
