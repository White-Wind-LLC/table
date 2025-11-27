package ua.wwind.table.filter.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

/**
 * Describes the UI and behavior of a column filter for values of type [T].
 * Each subtype enumerates supported [constraints] and, when needed, parsing/formatting helpers.
 */
@Immutable
public sealed class TableFilterType<T>(
    public open val constraints: ImmutableList<FilterConstraint>,
) {
    @Immutable
    /** Text filter supporting contains/starts/ends/equals constraints. */
    public data class TextTableFilter(
        override val constraints: ImmutableList<FilterConstraint> =
            persistentListOf(
                FilterConstraint.CONTAINS,
                FilterConstraint.STARTS_WITH,
                FilterConstraint.ENDS_WITH,
                FilterConstraint.EQUALS,
            ),
    ) : TableFilterType<String>(constraints)

    @Immutable
    /** Numeric filter with optional range slider support via [rangeOptions] and [delegate]. */
    public data class NumberTableFilter<T : Number>(
        override val constraints: ImmutableList<FilterConstraint> =
            persistentListOf(
                FilterConstraint.GT,
                FilterConstraint.GTE,
                FilterConstraint.LT,
                FilterConstraint.LTE,
                FilterConstraint.EQUALS,
                FilterConstraint.NOT_EQUALS,
                FilterConstraint.BETWEEN,
            ),
        val delegate: NumberFilterDelegate<T>,
        val rangeOptions: Pair<T, T>? = null,
    ) : TableFilterType<T>(constraints) {
        /** Parsing/formatting and comparison helpers for numeric types. */
        public interface NumberFilterDelegate<T : Number> {
            public val regex: Regex
            public val default: T

            public fun parse(input: String): T?

            public fun format(value: T): String

            public fun toSliderValue(value: T): Float

            public fun fromSliderValue(value: Float): T

            public fun compare(
                a: T,
                b: T,
            ): Boolean
        }

        /** Default helpers for Int values. */
        public object IntDelegate : NumberFilterDelegate<Int> {
            public override val regex: Regex = Regex("^-?\\d*$")
            public override val default: Int = 0

            public override fun parse(input: String): Int? = input.toIntOrNull()

            public override fun format(value: Int): String = value.toString()

            public override fun toSliderValue(value: Int): Float = value.toFloat()

            public override fun fromSliderValue(value: Float): Int = value.roundToInt()

            public override fun compare(
                a: Int,
                b: Int,
            ): Boolean = a <= b
        }

        /** Default helpers for Double values. */
        public object DoubleDelegate : NumberFilterDelegate<Double> {
            public override val regex: Regex = Regex("^-?\\d*\\.?\\d*$")
            public override val default: Double = 0.0

            public override fun parse(input: String): Double? = input.toDoubleOrNull()

            public override fun format(value: Double): String = value.toString()

            public override fun toSliderValue(value: Double): Float = value.toFloat()

            public override fun fromSliderValue(value: Float): Double = value.toDouble()

            public override fun compare(
                a: Double,
                b: Double,
            ): Boolean = a <= b
        }
    }

    @Immutable
    /** Boolean filter with a single EQUALS constraint. */
    public data class BooleanTableFilter(
        override val constraints: ImmutableList<FilterConstraint> =
            persistentListOf(
                FilterConstraint.EQUALS,
            ),
        val getTitle: @Composable ((BooleanType) -> String)? = null,
    ) : TableFilterType<Boolean>(constraints)

    @Immutable
    /** Date filter with comparison and equals constraints. */
    public data class DateTableFilter(
        override val constraints: ImmutableList<FilterConstraint> =
            persistentListOf(
                FilterConstraint.GT,
                FilterConstraint.GTE,
                FilterConstraint.LT,
                FilterConstraint.LTE,
                FilterConstraint.EQUALS,
                FilterConstraint.BETWEEN,
            ),
    ) : TableFilterType<LocalDate>(constraints)

    @Immutable
    /** Disabled filter type used when filtering must be explicitly turned off for a column. */
    public data object DisabledTableFilter : TableFilterType<Nothing>(
        constraints = persistentListOf(),
    )

    @Immutable
    /** Enum filter supporting IN/NOT_IN/EQUALS with custom [getTitle] provider. */
    public data class EnumTableFilter<T : Enum<T>>(
        val options: ImmutableList<T>,
        override val constraints: ImmutableList<FilterConstraint> =
            persistentListOf(
                FilterConstraint.IN,
                FilterConstraint.NOT_IN,
                FilterConstraint.EQUALS,
            ),
        val getTitle: @Composable (T) -> String,
    ) : TableFilterType<ImmutableList<T>>(constraints)

    @Immutable
    /**
     * Custom filter type that delegates UI rendering and state logic to user-provided implementations.
     * Allows full control over filter UI while maintaining visual consistency with built-in filters.
     *
     * @param T the type of filter state managed by the custom filter
     * @param renderFilter provides UI rendering callbacks for main panel and fast filter
     * @param stateProvider provides state introspection callbacks for active state detection and chip text
     */
    public data class CustomTableFilter<T>(
        val renderFilter: CustomFilterRenderer<T>,
        val stateProvider: CustomFilterStateProvider<T>,
    ) : TableFilterType<T>(constraints = persistentListOf())

    /**
     * Actions interface for custom filters to control apply/clear behavior.
     * Custom filters return this from RenderPanel to enable FilterPanelActions integration.
     */
    public interface CustomFilterActions {
        /**
         * Apply the current filter state. Called when user clicks Apply button (manual mode).
         */
        public fun applyFilter()

        /**
         * Clear the filter state. Called when user clicks Clear button.
         */
        public fun clearFilter()
    }
}

/**
 * Interface for rendering custom filter UI in different contexts.
 * Implementations control the complete visual appearance and interaction logic.
 *
 * @param T the type of filter state
 */
public interface CustomFilterRenderer<T> {
    /**
     * Render the main filter panel shown in a dropdown when the filter button is clicked.
     *
     * @param currentState the current filter state from the table
     * @param onDismiss callback to close the filter panel
     * @param onChange callback to update the filter state; pass null to clear the filter
     * @return CustomFilterActions to control apply/clear behavior
     */
    @Composable
    public fun RenderPanel(
        currentState: TableFilterState<T>?,
        onDismiss: () -> Unit,
        onChange: (TableFilterState<T>?) -> Unit,
    ): TableFilterType.CustomFilterActions

    /**
     * Render the fast filter shown inline in the fast filters row.
     * Return null or empty composable if fast filter is not supported.
     *
     * @param currentState the current filter state from the table
     * @param onChange callback to update the filter state; pass null to clear the filter
     */
    @Composable
    public fun RenderFastFilter(
        currentState: TableFilterState<T>?,
        onChange: (TableFilterState<T>?) -> Unit,
    )
}

/**
 * Interface for providing custom filter state introspection.
 * Used by the table to determine visual states and generate user-facing text.
 *
 * @param T the type of filter state
 */
public interface CustomFilterStateProvider<T> {
    /**
     * Build user-facing text to display in the active filters chip.
     * Return null to prevent the chip from being shown.
     *
     * @param state the current filter state from the table
     * @return chip text or null
     */
    @Composable
    public fun buildChipText(state: TableFilterState<T>?): String?
}
