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
)