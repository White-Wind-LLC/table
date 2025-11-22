package ua.wwind.table.sample.filter

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ua.wwind.table.filter.data.*
import ua.wwind.table.sample.Person
import kotlin.math.roundToInt

/**
 * State for numeric range filter with visual distribution.
 */
data class NumericRangeFilterState(
    val min: Int,
    val max: Int,
    val showOutliers: Boolean = false,
)

/**
 * Creates a visual numeric range filter for salary column with histogram.
 * Demonstrates the power of custom filters with rich UI.
 */
fun createSalaryRangeFilter(allData: List<Person>): TableFilterType.CustomTableFilter<NumericRangeFilterState> {
    return TableFilterType.CustomTableFilter(
        renderFilter = NumericRangeFilterRenderer(allData),
        stateProvider = NumericRangeFilterStateProvider(),
    )
}

/**
 * Renderer for numeric range filter with histogram visualization.
 */
private class NumericRangeFilterRenderer(
    private val allData: List<Person>,
) : CustomFilterRenderer<NumericRangeFilterState> {
    @Composable
    override fun RenderPanel(
        currentState: TableFilterState<NumericRangeFilterState>?,
        onDismiss: () -> Unit,
        onChange: (TableFilterState<NumericRangeFilterState>?) -> Unit,
    ): TableFilterType.CustomFilterActions {
        val dataMin = allData.minOfOrNull { it.salary } ?: 0
        val dataMax = allData.maxOfOrNull { it.salary } ?: 200000

        val current = currentState?.values?.firstOrNull() ?: NumericRangeFilterState(dataMin, dataMax)

        var rangeMin by remember { mutableStateOf(current.min.toFloat()) }
        var rangeMax by remember { mutableStateOf(current.max.toFloat()) }

        var minInput by remember { mutableStateOf(current.min.toString()) }
        var maxInput by remember { mutableStateOf(current.max.toString()) }

        // Flag to prevent circular updates
        var isUpdatingFromSlider by remember { mutableStateOf(false) }

        // Sync text fields with slider changes only
        LaunchedEffect(rangeMin, rangeMax) {
            if (isUpdatingFromSlider) {
                minInput = rangeMin.roundToInt().toString()
                maxInput = rangeMax.roundToInt().toString()
                isUpdatingFromSlider = false
            }
        }

        // Auto-apply on change
        val applyFilter = {
            val newState = NumericRangeFilterState(
                min = rangeMin.roundToInt(),
                max = rangeMax.roundToInt(),
            )
            onChange(
                TableFilterState(
                    constraint = FilterConstraint.BETWEEN,
                    values = listOf(newState),
                ),
            )
        }

        Column(
            modifier = Modifier.width(400.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Distribution histogram
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.BarChart,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        "Distribution",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SalaryHistogram(
                    data = allData.map { it.salary },
                    selectedRange = rangeMin.roundToInt()..rangeMax.roundToInt(),
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                )

                // Statistics - vertical layout
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "Min: $${dataMin}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val filtered = allData.count { it.salary in rangeMin.roundToInt()..rangeMax.roundToInt() }
                    Text(
                        "Matched: $filtered/${allData.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Max: $${dataMax}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider()

            // Range slider
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Select Range",
                    style = MaterialTheme.typography.bodyMedium,
                )

                RangeSlider(
                    value = rangeMin..rangeMax,
                    onValueChange = { range ->
                        isUpdatingFromSlider = true
                        rangeMin = range.start
                        rangeMax = range.endInclusive
                    },
                    onValueChangeFinished = {
                        applyFilter()
                    },
                    valueRange = dataMin.toFloat()..dataMax.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Input fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = minInput,
                        onValueChange = { newValue ->
                            minInput = newValue
                            newValue.toIntOrNull()?.let {
                                rangeMin = it.toFloat().coerceIn(dataMin.toFloat(), rangeMax)
                                applyFilter()
                            }
                        },
                        label = { Text("Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = maxInput,
                        onValueChange = { newValue ->
                            maxInput = newValue
                            newValue.toIntOrNull()?.let {
                                rangeMax = it.toFloat().coerceIn(rangeMin, dataMax.toFloat())
                                applyFilter()
                            }
                        },
                        label = { Text("Max") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
            }
        }

        // Return actions for FilterPanelActions
        return remember {
            object : TableFilterType.CustomFilterActions {
                override fun applyFilter() {
                    // In auto-apply mode, already applied
                    // In manual mode, apply current state
                    val newState = NumericRangeFilterState(
                        min = rangeMin.roundToInt(),
                        max = rangeMax.roundToInt(),
                    )
                    onChange(
                        TableFilterState(
                            constraint = FilterConstraint.BETWEEN,
                            values = listOf(newState),
                        ),
                    )
                }

                override fun clearFilter() {
                    rangeMin = dataMin.toFloat()
                    rangeMax = dataMax.toFloat()
                    onChange(null)
                    onDismiss()
                }
            }
        }
    }

    @Composable
    override fun RenderFastFilter(
        currentState: TableFilterState<NumericRangeFilterState>?,
        onChange: (TableFilterState<NumericRangeFilterState>?) -> Unit,
    ) {
        val dataMin = allData.minOfOrNull { it.salary } ?: 0
        val dataMax = allData.maxOfOrNull { it.salary } ?: 200000

        val current = currentState?.values?.firstOrNull()

        val selectedOption = when {
            current == null -> 3 // All
            current.max <= 50000 -> 0 // < 50k
            current.min >= 50000 && current.max <= 100000 -> 1 // 50k-100k
            current.min >= 100000 -> 2 // > 100k
            else -> 3 // Custom/All
        }

        val options = listOf("< 50k", "50k-100k", "> 100k", "All")

        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = index == selectedOption,
                    onClick = {
                        when (index) {
                            0 -> {
                                // < 50k
                                onChange(
                                    TableFilterState(
                                        constraint = FilterConstraint.BETWEEN,
                                        values = listOf(NumericRangeFilterState(dataMin, 50000)),
                                    ),
                                )
                            }

                            1 -> {
                                // 50k-100k
                                onChange(
                                    TableFilterState(
                                        constraint = FilterConstraint.BETWEEN,
                                        values = listOf(NumericRangeFilterState(50000, 100000)),
                                    ),
                                )
                            }

                            2 -> {
                                // > 100k
                                onChange(
                                    TableFilterState(
                                        constraint = FilterConstraint.BETWEEN,
                                        values = listOf(NumericRangeFilterState(100000, dataMax)),
                                    ),
                                )
                            }

                            3 -> {
                                // All - clear filter
                                onChange(null)
                            }
                        }
                    },
                    shape = androidx.compose.ui.graphics.RectangleShape,
                ) {
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/**
 * State provider for numeric range filter.
 */
private class NumericRangeFilterStateProvider : CustomFilterStateProvider<NumericRangeFilterState> {
    @Composable
    override fun buildChipText(state: TableFilterState<NumericRangeFilterState>?): String? {
        val range = state?.values?.firstOrNull() ?: return null
        return "$${range.min} - $${range.max}"
    }
}

/**
 * Histogram visualization for salary distribution.
 */
@Composable
private fun SalaryHistogram(
    data: List<Int>,
    selectedRange: IntRange,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

    // Calculate histogram bins
    val histogram = remember(data) {
        if (data.isEmpty()) return@remember emptyList()

        val min = data.minOrNull() ?: 0
        val max = data.maxOrNull() ?: 1
        val binCount = 20
        val binSize = ((max - min).toFloat() / binCount).coerceAtLeast(1f)

        val bins = MutableList(binCount) { 0 }

        data.forEach { value ->
            val binIndex = ((value - min) / binSize).toInt().coerceIn(0, binCount - 1)
            bins[binIndex]++
        }

        bins
    }

    val maxCount = histogram.maxOrNull() ?: 1

    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(8.dp),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val binWidth = width / histogram.size

            histogram.forEachIndexed { index, count ->
                val barHeight = (count.toFloat() / maxCount) * height
                val x = index * binWidth

                // Determine if this bin is in selected range
                val dataMin = data.minOrNull() ?: 0
                val dataMax = data.maxOrNull() ?: 1
                val binMin = dataMin + (index * (dataMax - dataMin) / histogram.size)
                val binMax = dataMin + ((index + 1) * (dataMax - dataMin) / histogram.size)

                val isInRange = !(binMax < selectedRange.first || binMin > selectedRange.last)

                drawRect(
                    color = if (isInRange) primaryColor else surfaceVariantColor,
                    topLeft = Offset(x, height - barHeight),
                    size = Size(binWidth * 0.9f, barHeight),
                )

                // Draw outline
                drawRect(
                    color = outlineColor.copy(alpha = 0.3f),
                    topLeft = Offset(x, height - barHeight),
                    size = Size(binWidth * 0.9f, barHeight),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f),
                )
            }
        }
    }
}
