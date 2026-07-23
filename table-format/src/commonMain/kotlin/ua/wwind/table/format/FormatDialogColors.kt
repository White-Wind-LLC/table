package ua.wwind.table.format

import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Colors for the format dialog and its portable content; mirrors the color parameters of Material3 `AlertDialog`.
 * [FormatDialog] uses all four; [FormatDialogContent] applies only [titleContentColor] and [textContentColor] and
 * leaves [containerColor] / [tonalElevation] to its host container.
 */
@Immutable
public data class FormatDialogColors(
    public val containerColor: Color,
    public val titleContentColor: Color,
    public val textContentColor: Color,
    public val tonalElevation: Dp,
)

/** Default factory for [FormatDialogColors], resolving to the Material3 `AlertDialog` defaults. */
public object FormatDialogDefaults {
    @Composable
    public fun colors(
        containerColor: Color = AlertDialogDefaults.containerColor,
        titleContentColor: Color = AlertDialogDefaults.titleContentColor,
        textContentColor: Color = AlertDialogDefaults.textContentColor,
        tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    ): FormatDialogColors =
        FormatDialogColors(
            containerColor = containerColor,
            titleContentColor = titleContentColor,
            textContentColor = textContentColor,
            tonalElevation = tonalElevation,
        )
}
