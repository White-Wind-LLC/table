package ua.wwind.table.sample.model

/** Enum representing various job positions. */
enum class Position(
    val displayName: String,
) {
    JUNIOR_DEVELOPER("Junior Developer"),
    SENIOR_DEVELOPER("Senior Developer"),
    TEAM_LEAD("Team Lead"),
    PROJECT_MANAGER("Project Manager"),
    PRODUCT_MANAGER("Product Manager"),
    DESIGNER("Designer"),
    QA_ENGINEER("QA Engineer"),
    DEVOPS_ENGINEER("DevOps Engineer"),
    DATA_ANALYST("Data Analyst"),
    HR_SPECIALIST("HR Specialist"),
    ;

    override fun toString(): String = displayName
}
