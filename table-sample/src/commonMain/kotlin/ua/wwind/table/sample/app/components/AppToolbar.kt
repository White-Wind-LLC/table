package ua.wwind.table.sample.app.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.wwind.table.config.FixedSide

@Composable
fun AppToolbar(
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    useStripedRows: Boolean,
    onStripedRowsChange: (Boolean) -> Unit,
    showFastFilters: Boolean,
    onShowFastFiltersChange: (Boolean) -> Unit,
    enableDragToScroll: Boolean,
    onEnableDragToScrollChange: (Boolean) -> Unit,
    fixedColumnsCount: Int,
    onFixedColumnsCountChange: (Int) -> Unit,
    fixedColumnsSide: FixedSide,
    onFixedColumnsSideChange: (FixedSide) -> Unit,
    enableEditing: Boolean,
    onEnableEditingChange: (Boolean) -> Unit,
    onConditionalFormattingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier.fillMaxWidth().padding(12.dp).horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = onConditionalFormattingClick) { Text("Conditional formatting") }

        ToolbarSwitch(
            label = "Dark theme",
            checked = isDarkTheme,
            onCheckedChange = onDarkThemeChange,
        )

        ToolbarSwitch(
            label = "Stripped rows",
            checked = useStripedRows,
            onCheckedChange = onStripedRowsChange,
        )

        ToolbarSwitch(
            label = "Fast filters",
            checked = showFastFilters,
            onCheckedChange = onShowFastFiltersChange,
        )

        ToolbarSwitch(
            label = "Drag to scroll",
            checked = enableDragToScroll,
            onCheckedChange = onEnableDragToScrollChange,
        )

        FixedColumnsControl(
            count = fixedColumnsCount,
            onCountChange = onFixedColumnsCountChange,
        )

        FixedColumnsSideControl(
            side = fixedColumnsSide,
            onSideChange = onFixedColumnsSideChange,
        )

        ToolbarSwitch(
            label = "Cell editing",
            checked = enableEditing,
            onCheckedChange = onEnableEditingChange,
        )
    }
}

@Composable
private fun ToolbarSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun FixedColumnsControl(
    count: Int,
    onCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Fixed cols:")
        OutlinedButton(onClick = { if (count > 0) onCountChange(count - 1) }) { Text("-") }
        Text("$count")
        OutlinedButton(onClick = { onCountChange(count + 1) }) { Text("+") }
    }
}

@Composable
private fun FixedColumnsSideControl(
    side: FixedSide,
    onSideChange: (FixedSide) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Fixed Side: $side")
        Switch(
            checked = side == FixedSide.Right,
            onCheckedChange = { onSideChange(if (it) FixedSide.Right else FixedSide.Left) },
        )
    }
}
