package ua.wwind.table.sample.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

@Composable
internal actual fun largeFontDensity(): Density = Density(LocalDensity.current.density, fontScale = LARGE_FONT_SCALE)
