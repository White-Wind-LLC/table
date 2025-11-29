package ua.wwind.table.sample.config

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

/**
 * Configuration for cell padding in different display modes
 */
object CellPadding {
    /**
     * Standard padding for cells - provides comfortable spacing
     */
    val standard = PaddingValues(horizontal = 16.dp)

    /**
     * Compact padding for cells - minimal spacing for dense layouts
     */
    val compact = PaddingValues(horizontal = 8.dp)
}
