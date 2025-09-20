package ua.wwind.table.format.data

public data class TableFormatRule<FIELD, FILTER>(
    val id: Long,
    val enabled: Boolean = true,
    val base: Boolean = false,
    val columns: List<FIELD> = emptyList(),
    val cellStyle: TableCellStyleConfig = TableCellStyleConfig(),
    val filter: FILTER,
) {
    public companion object {
        public fun <FIELD, FILTER> new(
            id: Long,
            filter: FILTER,
        ): TableFormatRule<FIELD, FILTER> = TableFormatRule(id = id, filter = filter)
    }
}
