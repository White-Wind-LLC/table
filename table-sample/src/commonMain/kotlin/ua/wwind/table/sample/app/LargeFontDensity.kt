package ua.wwind.table.sample.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density

internal const val LARGE_FONT_SCALE = 2f

/** The current density with the font scale set to [LARGE_FONT_SCALE], scaled as the platform would. */
@Composable
internal expect fun largeFontDensity(): Density
