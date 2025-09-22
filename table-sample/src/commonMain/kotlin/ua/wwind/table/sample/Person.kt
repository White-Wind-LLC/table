package ua.wwind.table.sample

/**
 * Person data model with 10 fields for table demonstration.
 */
data class Person(
    val name: String,
    val age: Int,
    val active: Boolean,
    val id: Int,
    val email: String,
    val city: String,
    val country: String,
    val department: String,
    val salary: Int,
    val rating: Int,
    /** Multiline notes to demonstrate dynamic row height in table. */
    val notes: String = """
        This is a sample multi-line note for demo purposes.
        It spans multiple lines to showcase dynamic row height.
        You can add more content here as needed.
    """.trimIndent(),
)
