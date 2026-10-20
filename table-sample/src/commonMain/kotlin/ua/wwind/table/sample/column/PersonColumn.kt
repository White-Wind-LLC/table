package ua.wwind.table.sample.column

/** Columns enum is used as a stable column key and to wire conditional formatting to fields. */
enum class PersonColumn {
    SELECTION,
    EXPAND,

    // Real Person fields
    NAME,
    AGE,
    ACTIVE,
    ID,
    EMAIL,
    CITY,
    COUNTRY,
    DEPARTMENT,
    POSITION,
    SALARY,
    RATING,
    HIRE_DATE,
    NOTES,

    // Computed fields
    AGE_GROUP,

    /**
     * Not a column of the table: a dropped column that saved filters may still name. The nested filter demo
     * filters on it to show the chip of a key the table does not know.
     */
    REGION,
}

/** The keys the table has columns for: every [PersonColumn] but [PersonColumn.REGION]. */
val personTableColumns: List<PersonColumn> = PersonColumn.entries - PersonColumn.REGION
