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
}
