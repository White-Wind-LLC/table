package ua.wwind.table.sample

/**
 * Columns enum is used as a stable column key and to wire conditional formatting to fields.
 */
enum class PersonColumn {
    // Real Person fields
    NAME,
    AGE,
    ACTIVE,
    ID,
    EMAIL,
    CITY,
    COUNTRY,
    DEPARTMENT,
    SALARY,
    RATING,

    // Computed fields
    AGE_GROUP,
}