package ua.wwind.table.data

/**
 * Sort direction used by the table.
 */
public enum class SortOrder(
    public val value: Int,
) {
    ASCENDING(1),
    DESCENDING(-1),
}
