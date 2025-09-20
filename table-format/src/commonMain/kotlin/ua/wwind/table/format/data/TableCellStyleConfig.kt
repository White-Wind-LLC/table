package ua.wwind.table.format.data

public data class TableCellStyleConfig(
    val backgroundColor: Int? = null,
    val contentColor: Int? = null,
    val textStyle: TableFormatTextStyle? = null,
    val horizontal: TableFormatHorizontalAlignment? = null,
    val vertical: TableFormatVerticalAlignment? = null,
)
