package ua.wwind.table.component.body

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
internal fun <T> RowLeadingSection(
    cellWidth: Dp,
    height: Dp?,
    dividerThickness: Dp,
    rowLeading: @Composable (T) -> Unit,
    item: T,
) {
    Row {
        Box(
            modifier =
                Modifier
                    .then(if (height != null) Modifier.height(height) else Modifier)
                    .width(cellWidth),
            contentAlignment = Alignment.Center,
        ) { rowLeading(item) }
        VerticalDivider(
            modifier = if (height != null) Modifier.height(height) else Modifier.fillMaxHeight(),
            thickness = dividerThickness,
        )
    }
}
