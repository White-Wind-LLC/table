package ua.wwind.table.format

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import ua.wwind.table.format.component.FormatColorField
import ua.wwind.table.format.component.FormatDropdownField
import ua.wwind.table.format.data.TableFormatHorizontalAlignment
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.format.data.TableFormatTextStyle
import ua.wwind.table.format.data.TableFormatVerticalAlignment
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

@Composable
public fun <E : Enum<E>, FILTER> FormatDialogDesignTab(
    item: TableFormatRule<E, FILTER>,
    onChange: (TableFormatRule<E, FILTER>) -> Unit,
    strings: StringProvider,
) {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = spacedBy(8.dp),
    ) {
        FormatDropdownField(
            currentValue = item.cellStyle.textStyle,
            getTitle = { value -> strings.get(value.uiString) },
            values = TableFormatTextStyle.entries,
            label = {
                Text(strings.get(UiString.FormatLabelTypography))
            },
            onClick = { value ->
                onChange(item.copy(cellStyle = item.cellStyle.copy(textStyle = value)))
            },
            modifier = Modifier.fillMaxWidth(),
        )
        FormatColorField(
            color = item.cellStyle.contentColor?.toColor(),
            label = strings.get(UiString.FormatContentColor),
            onClick = { color ->
                println(color)
                onChange(item.copy(cellStyle = item.cellStyle.copy(contentColor = color?.toArgb())))
            },
            modifier = Modifier.fillMaxWidth(),
            strings = strings,
        )
        FormatColorField(
            color = item.cellStyle.backgroundColor?.toColor(),
            label = strings.get(UiString.FormatBackgroundColor),
            onClick = { color ->
                onChange(item.copy(cellStyle = item.cellStyle.copy(backgroundColor = color?.toArgb())))
            },
            modifier = Modifier.fillMaxWidth(),
            strings = strings,
        )
        FormatDropdownField(
            currentValue = item.cellStyle.vertical,
            getTitle = { value -> strings.get(value.uiString) },
            label = {
                Text(strings.get(UiString.FormatLabelVerticalAlignment))
            },
            values = TableFormatVerticalAlignment.entries,
            onClick = { value ->
                onChange(item.copy(cellStyle = item.cellStyle.copy(vertical = value)))
            },
            modifier = Modifier.fillMaxWidth(),
        )
        FormatDropdownField(
            currentValue = item.cellStyle.horizontal,
            getTitle = { value -> strings.get(value.uiString) },
            values = TableFormatHorizontalAlignment.entries,
            label = {
                Text(strings.get(UiString.FormatLabelHorizontalAlignment))
            },
            onClick = { value ->
                onChange(item.copy(cellStyle = item.cellStyle.copy(horizontal = value)))
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
