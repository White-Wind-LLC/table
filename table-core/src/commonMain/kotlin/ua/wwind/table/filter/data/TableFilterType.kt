package ua.wwind.table.filter.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

@Immutable
public sealed class TableFilterType<T>(
    public open val constraints: List<FilterConstraint>,
) {
    @Immutable
    public data class TextTableFilter(
        override val constraints: List<FilterConstraint> =
            listOf(
                FilterConstraint.CONTAINS,
                FilterConstraint.STARTS_WITH,
                FilterConstraint.ENDS_WITH,
                FilterConstraint.EQUALS,
            ),
    ) : TableFilterType<String>(constraints)

    @Immutable
    public data class NumberTableFilter<T : Number>(
        override val constraints: List<FilterConstraint> =
            listOf(
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
    public data class BooleanTableFilter(
        override val constraints: List<FilterConstraint> =
            listOf(
                FilterConstraint.EQUALS,
            ),
        val getTitle: @Composable ((BooleanType) -> String)? = null,
    ) : TableFilterType<Boolean>(constraints)

    @Immutable
    public data class DateTableFilter(
        override val constraints: List<FilterConstraint> =
            listOf(
                FilterConstraint.GT,
                FilterConstraint.GTE,
                FilterConstraint.LT,
                FilterConstraint.LTE,
                FilterConstraint.EQUALS,
            ),
    ) : TableFilterType<LocalDate>(constraints)

    @Immutable
    public data class EnumTableFilter<T : Enum<T>>(
        val options: List<T>,
        override val constraints: List<FilterConstraint> =
            listOf(
                FilterConstraint.IN,
                FilterConstraint.NOT_IN,
                FilterConstraint.EQUALS,
            ),
        val getTitle: @Composable (T) -> String,
    ) : TableFilterType<List<T>>(constraints)
}
