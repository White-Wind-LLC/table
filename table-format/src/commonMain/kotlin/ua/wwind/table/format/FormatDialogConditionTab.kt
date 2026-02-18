package ua.wwind.table.format

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import ua.wwind.table.component.TableTextField
import ua.wwind.table.component.TableTextFieldDefaults
import ua.wwind.table.filter.component.FilterDropdownAnyField
import ua.wwind.table.filter.component.FilterDropdownField
import ua.wwind.table.filter.component.collectAsEffect
import ua.wwind.table.filter.data.BooleanType
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.isNullCheck
import ua.wwind.table.filter.data.toUiString
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.format.scrollbar.VerticalScrollbarRenderer
import ua.wwind.table.format.scrollbar.VerticalScrollbarState
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Suppress("LongMethod", "CyclomaticComplexMethod", "UNCHECKED_CAST")
@Composable
public fun <E : Enum<E>, FILTER> FormatDialogConditionTab(
    item: TableFormatRule<E, FILTER>,
    getTitle: @Composable (E) -> String,
    filters: (TableFormatRule<E, FILTER>, onApply: (TableFormatRule<E, FILTER>) -> Unit) -> List<FormatFilterData<E>>,
    onChange: (TableFormatRule<E, FILTER>) -> Unit,
    strings: StringProvider,
    scrollbarRenderer: VerticalScrollbarRenderer? = null,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val listState = rememberLazyListState()
        Box {
            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().padding(end = 8.dp)) {
                val filterItems: List<FormatFilterData<E>> =
                    filters(item) { rule: TableFormatRule<E, FILTER> ->
                        onChange(rule)
                    }
                val visibleFilterItems =
                    filterItems.filter { formatFilter: FormatFilterData<E> ->
                        formatFilter.filterType !is TableFilterType.DisabledTableFilter
                    }
                items(
                    count = visibleFilterItems.size,
                    key = { index: Int -> visibleFilterItems[index].field },
                ) { index: Int ->
                    val filterData: FormatFilterData<E> = visibleFilterItems[index]
                    var expanded by remember { mutableStateOf(false) }
                    Column {
                        val builtTitle = buildFilterHeaderTitle(filterData = filterData, strings = strings)
                        val hasFilterValues = (filterData.filterState.values?.isNotEmpty() == true)
                        ListItem(
                            overlineContent =
                                if (builtTitle != null) {
                                    { Text(getTitle(filterData.field), maxLines = 1) }
                                } else {
                                    null
                                },
                            headlineContent = {
                                if (builtTitle != null) {
                                    Text(builtTitle, maxLines = 1)
                                } else {
                                    Text(getTitle(filterData.field), maxLines = 1)
                                }
                            },
                            leadingContent = {
                                IconButton(
                                    onClick = { expanded = !expanded },
                                ) {
                                    val rotation by animateFloatAsState(
                                        targetValue = if (!expanded) -180F else 0F,
                                        animationSpec = tween(durationMillis = 500),
                                        label = "expand condition",
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDropUp,
                                        contentDescription = null,
                                        modifier = Modifier.rotate(rotation),
                                    )
                                }
                            },
                            trailingContent =
                                if (hasFilterValues) {
                                    {
                                        IconButton(
                                            onClick = {
                                                val constraint =
                                                    filterData.filterState.constraint
                                                        ?: filterData.filterType.constraints.first()
                                                filterData.onChange(TableFilterState<Any?>(constraint, null))
                                            },
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Remove Filter",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                } else {
                                    null
                                },
                            modifier = Modifier.clickable { expanded = !expanded },
                        )
                        AnimatedVisibility(visible = expanded) {
                            Surface {
                                when (val filter = filterData.filterType) {
                                    is TableFilterType.TextTableFilter -> {
                                        FormatTextFilter(
                                            filter = filter,
                                            state = filterData.filterState as TableFilterState<String>,
                                            onChange = { newState -> filterData.onChange(newState as TableFilterState<*>) },
                                            strings = strings,
                                        )
                                    }

                                    is TableFilterType.BooleanTableFilter -> {
                                        FormatBooleanFilter(
                                            filter = filter,
                                            state = filterData.filterState as TableFilterState<Boolean>,
                                            onChange = { newState -> filterData.onChange(newState as TableFilterState<*>) },
                                            strings = strings,
                                        )
                                    }

                                    is TableFilterType.DateTableFilter -> {
                                        FormatDateFilter(
                                            filter = filter,
                                            state = filterData.filterState as TableFilterState<LocalDate>,
                                            onChange = { newState -> filterData.onChange(newState as TableFilterState<*>) },
                                            strings = strings,
                                        )
                                    }

                                    is TableFilterType.EnumTableFilter<*> -> {
                                        FormatEnumFilter(
                                            filter = filter,
                                            state = filterData.filterState as TableFilterState<*>,
                                            onChange = { newState -> filterData.onChange(newState as TableFilterState<*>) },
                                            strings = strings,
                                        )
                                    }

                                    is TableFilterType.NumberTableFilter<*> -> {
                                        FormatNumberFilter(
                                            filter = filter as TableFilterType.NumberTableFilter<Number>,
                                            state = filterData.filterState as TableFilterState<Number>,
                                            onChange = { newState -> filterData.onChange(newState as TableFilterState<*>) },
                                            strings = strings,
                                        )
                                    }

                                    is TableFilterType.CustomTableFilter<*, *> -> {
                                        // Custom filters are not supported in conditional formatting
                                        Text(
                                            text = "Custom filters are not supported in conditional formatting",
                                            modifier = Modifier.padding(16.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }

                                    is TableFilterType.DisabledTableFilter -> {
                                        // No Op
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
            scrollbarRenderer?.Render(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxHeight(),
                state = VerticalScrollbarState.LazyList(listState),
            )
        }
    }
}

@OptIn(FlowPreview::class)
@Suppress("FunctionNaming", "LongParameterList")
@Composable
private fun FormatTextFilter(
    filter: TableFilterType.TextTableFilter,
    state: TableFilterState<String>,
    onChange: (TableFilterState<String>) -> Unit,
    strings: StringProvider,
) {
    var constraint by remember { mutableStateOf(state.constraint ?: filter.constraints.first()) }
    var searchText by remember { mutableStateOf(state.values?.firstOrNull() ?: "") }
    LaunchedEffect(Unit) {
        snapshotFlow { searchText }
            .drop(1)
            .distinctUntilChanged()
            .collect {
                onChange(TableFilterState(constraint, listOf(searchText)))
            }
    }
    FlowRow(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = spacedBy(8.dp),
        horizontalArrangement = spacedBy(8.dp),
    ) {
        FilterDropdownField(
            currentValue = constraint,
            getTitle = { c -> strings.get(c.toUiString()) },
            values = filter.constraints,
            onClick = { constraint = it },
        )
        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
            },
            placeholder = { Text(strings.get(UiString.FilterSearchPlaceholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(FlowPreview::class)
@Suppress("FunctionNaming", "LongParameterList")
@Composable
private fun FormatBooleanFilter(
    filter: TableFilterType.BooleanTableFilter,
    state: TableFilterState<Boolean>,
    onChange: (TableFilterState<Boolean>) -> Unit,
    strings: StringProvider,
) {
    check(filter.constraints.size == 1 && filter.constraints.first() == FilterConstraint.EQUALS) {
        "Boolean filter supports only EQUALS constraint"
    }
    var valueState by remember { mutableStateOf(state.values?.firstOrNull() ?: false) }
    LaunchedEffect(Unit) {
        snapshotFlow { valueState }
            .drop(1)
            .distinctUntilChanged()
            .collect {
                onChange(TableFilterState(filter.constraints.first(), listOf(valueState)))
            }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = valueState,
                onClick = { valueState = true },
            )
            val title = filter.getTitle?.let { it(BooleanType.TRUE) } ?: strings.get(BooleanType.TRUE.toUiString())
            Text(title)
        }
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = !valueState,
                onClick = { valueState = false },
            )
            val title = filter.getTitle?.let { it(BooleanType.FALSE) } ?: strings.get(BooleanType.FALSE.toUiString())
            Text(title)
        }
    }
}

@OptIn(FlowPreview::class)
@Suppress("FunctionNaming", "LongParameterList")
@Composable
private fun FormatDateFilter(
    filter: TableFilterType.DateTableFilter,
    state: TableFilterState<LocalDate>,
    onChange: (TableFilterState<LocalDate>) -> Unit,
    strings: StringProvider,
) {
    var constraint by remember { mutableStateOf(state.constraint ?: filter.constraints.first()) }
    var firstDate by remember { mutableStateOf(state.values?.getOrNull(0)) }
    var secondDate by remember {
        mutableStateOf(
            if (constraint == FilterConstraint.BETWEEN) {
                state.values?.getOrNull(1)
            } else {
                null
            },
        )
    }
    val isNullConstraint = constraint.isNullCheck()
    val isBetween = constraint == FilterConstraint.BETWEEN
    LaunchedEffect(Unit) {
        snapshotFlow { Triple(constraint, firstDate, secondDate) }
            .drop(1)
            .distinctUntilChanged()
            .collect { (currentConstraint, currentFirst, currentSecond) ->
                val values =
                    when (currentConstraint) {
                        FilterConstraint.BETWEEN -> {
                            if (currentFirst != null && currentSecond != null) {
                                listOf(currentFirst, currentSecond)
                            } else {
                                null
                            }
                        }

                        FilterConstraint.IS_NULL, FilterConstraint.IS_NOT_NULL -> {
                            emptyList()
                        }

                        else -> {
                            currentFirst?.let { listOf(it) }
                        }
                    }
                onChange(TableFilterState(currentConstraint, values))
            }
    }
    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = spacedBy(8.dp),
    ) {
        FilterDropdownField(
            currentValue = constraint,
            getTitle = { c -> strings.get(c.toUiString()) },
            values = filter.constraints,
            onClick = { newConstraint ->
                constraint = newConstraint
                if (newConstraint != FilterConstraint.BETWEEN) {
                    secondDate = null
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (!isNullConstraint) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = spacedBy(8.dp),
            ) {
                DateField(
                    value = firstDate,
                    onDateSelected = { selectedDate ->
                        firstDate = selectedDate
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(strings.get(UiString.FilterRangeFromPlaceholder)) },
                    onClear = { firstDate = null },
                    strings = strings,
                )
                if (isBetween) {
                    DateField(
                        value = secondDate,
                        onDateSelected = { selectedDate ->
                            secondDate = selectedDate
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text(strings.get(UiString.FilterRangeToPlaceholder)) },
                        onClear = { secondDate = null },
                        strings = strings,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun DateField(
    value: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    onClear: () -> Unit = {},
    dateValidator: (Long) -> Boolean = { true },
    strings: StringProvider,
    contentPadding: PaddingValues = TableTextFieldDefaults.contentPadding(),
    showBorder: Boolean = true,
) {
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    interactionSource.interactions.collectAsEffect {
        if (it is PressInteraction.Release) showDatePickerDialog = true
    }
    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = value?.atStartOfDayIn(TimeZone.UTC)?.epochSeconds?.times(1000),
            yearRange = IntRange(1, 2100),
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = dateValidator(utcTimeMillis)
                },
        )
    TableTextField(
        value = value?.toFormatString().orEmpty(),
        onValueChange = {},
        placeholder =
            label ?: {
                Text(text = strings.get(UiString.DatePickerSelectDate), maxLines = 1)
            },
        readOnly = true,
        interactionSource = interactionSource,
        modifier = modifier,
        singleLine = true,
        contentPadding = contentPadding,
        showBorder = showBorder,
    )
    if (showDatePickerDialog) {
        val confirmEnabled by remember { derivedStateOf { datePickerState.selectedDateMillis != null } }
        DatePickerDialog(
            onDismissRequest = {
                showDatePickerDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePickerDialog = false
                        datePickerState.selectedDateMillis?.let {
                            onDateSelected(
                                Instant
                                    .fromEpochMilliseconds(it)
                                    .toLocalDateTime(TimeZone.currentSystemDefault())
                                    .date,
                            )
                        }
                    },
                    enabled = confirmEnabled,
                ) {
                    Text(strings.get(UiString.DatePickerConfirm))
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showDatePickerDialog = false
                            onClear()
                        },
                    ) {
                        Text(strings.get(UiString.DatePickerClear))
                    }
                    TextButton(
                        onClick = {
                            showDatePickerDialog = false
                        },
                    ) {
                        Text(strings.get(UiString.DatePickerCancel))
                    }
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun LocalDate.toFormatString(): String =
    this.format(
        LocalDate.Format {
            day()
            chars(".")
            monthNumber()
            chars(".")
            year()
        },
    )

@OptIn(FlowPreview::class)
@Suppress("FunctionNaming", "LongParameterList")
@Composable
private fun FormatEnumFilter(
    filter: TableFilterType.EnumTableFilter<*>,
    state: TableFilterState<*>,
    onChange: (TableFilterState<*>) -> Unit,
    strings: StringProvider,
) {
    var constraint by remember { mutableStateOf(state.constraint ?: filter.constraints.first()) }
    var selectedValues: List<Enum<*>> by remember { mutableStateOf((state.values as? List<Enum<*>>).orEmpty()) }
    LaunchedEffect(Unit) {
        snapshotFlow { selectedValues }
            .drop(1)
            .distinctUntilChanged()
            .collect {
                onChange(TableFilterState(constraint, selectedValues.takeIf { it.isNotEmpty() }))
            }
    }
    FlowRow(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = spacedBy(8.dp),
        horizontalArrangement = spacedBy(8.dp),
    ) {
        FilterDropdownField(
            currentValue = constraint,
            getTitle = { c -> strings.get(c.toUiString()) },
            values = filter.constraints,
            onClick = { constraint = it },
            modifier = Modifier.fillMaxWidth(),
        )
        when (constraint) {
            FilterConstraint.EQUALS -> {
                FilterDropdownAnyField(
                    currentValue = if (selectedValues.size == 1) selectedValues.first() else null,
                    getTitle = { anyItem -> (filter.getTitle as @Composable (Enum<*>) -> String).invoke(anyItem as Enum<*>) },
                    placeholder = strings.get(UiString.FilterSelectOnePlaceholder),
                    values = (filter.options as ImmutableList<Enum<*>>),
                    onClick = { anyItem -> selectedValues = listOf(anyItem as Enum<*>) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            FilterConstraint.IN, FilterConstraint.NOT_IN -> {
                FilterDropdownAnyField(
                    currentValue = null,
                    getTitle = { anyItem -> (filter.getTitle as @Composable (Enum<*>) -> String).invoke(anyItem as Enum<*>) },
                    placeholder =
                        if (selectedValues.isEmpty()) {
                            strings.get(UiString.FilterSelectManyPlaceholder)
                        } else {
                            selectedValues
                                .map { item ->
                                    (filter.getTitle as @Composable (Enum<*>) -> String).invoke(
                                        item as Enum<*>,
                                    )
                                }.joinToString(", ")
                        },
                    values = (filter.options as ImmutableList<Enum<*>>),
                    onClick = { anyItem ->
                        val enumItem = anyItem as Enum<*>
                        selectedValues =
                            if (selectedValues.contains(enumItem)) selectedValues - enumItem else selectedValues + enumItem
                    },
                    checked = { anyEnum -> selectedValues.contains(anyEnum as Enum<*>) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            FilterConstraint.IS_NULL, FilterConstraint.IS_NOT_NULL -> {}

            else -> {
                throw IllegalArgumentException("Unsupported constraint for list filter: $constraint")
            }
        }
    }
}

@OptIn(FlowPreview::class)
@Suppress("FunctionNaming", "LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@Composable
private fun <T : Number> FormatNumberFilter(
    filter: TableFilterType.NumberTableFilter<T>,
    state: TableFilterState<T>,
    onChange: (TableFilterState<T>) -> Unit,
    strings: StringProvider,
) {
    var constraint by remember { mutableStateOf(state.constraint ?: filter.constraints.first()) }
    val initialSingle = state.values?.firstOrNull()?.toString()
    val initialFrom = state.values?.getOrNull(0)?.toString()
    val initialTo = state.values?.getOrNull(1)?.toString()
    val min = filter.rangeOptions?.first ?: filter.delegate.default
    val max = filter.rangeOptions?.second ?: filter.delegate.default
    var firstText by remember { mutableStateOf(initialSingle ?: initialFrom ?: "") }
    var secondText by remember { mutableStateOf(initialTo ?: "") }
    val fromValue = filter.delegate.parse(firstText) ?: min
    val toValue = filter.delegate.parse(secondText) ?: max
    val isBetween = constraint == FilterConstraint.BETWEEN
    val isRangeValid = filter.delegate.compare(fromValue, toValue)
    LaunchedEffect(isBetween) {
        snapshotFlow { firstText to secondText }
            .drop(1)
            .distinctUntilChanged()
            .collect { (from, to) ->
                val fromVal = filter.delegate.parse(from)
                if (isBetween) {
                    val toVal = filter.delegate.parse(to)
                    if (fromVal != null && toVal != null && filter.delegate.compare(fromVal, toVal)) {
                        onChange(TableFilterState(constraint, listOf(fromVal, toVal)))
                    }
                } else {
                    onChange(TableFilterState(constraint, fromVal?.let { listOf(it) }))
                }
            }
    }
    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = spacedBy(8.dp),
    ) {
        FilterDropdownField(
            currentValue = constraint,
            getTitle = { c -> strings.get(c.toUiString()) },
            values = filter.constraints,
            onClick = { constraint = it },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = firstText,
                onValueChange = {
                    if (it.matches(filter.delegate.regex)) {
                        firstText = it
                    }
                },
                placeholder = {
                    Text(
                        strings.get(
                            if (isBetween) UiString.FilterRangeFromPlaceholder else UiString.FilterEnterNumberPlaceholder,
                        ),
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            if (isBetween) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = strings.get(UiString.FilterRangeIconDescription),
                )
                OutlinedTextField(
                    value = secondText,
                    onValueChange = {
                        if (it.matches(filter.delegate.regex)) {
                            secondText = it
                        }
                    },
                    placeholder = { Text(strings.get(UiString.FilterRangeToPlaceholder)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = !isRangeValid,
                )
            }
        }
        if (isBetween && filter.rangeOptions != null) {
            RangeSlider(
                value = filter.delegate.toSliderValue(fromValue)..filter.delegate.toSliderValue(toValue),
                onValueChange = { range ->
                    firstText = filter.delegate.fromSliderValue(range.start).toString()
                    secondText = filter.delegate.fromSliderValue(range.endInclusive).toString()
                },
                valueRange = min.toFloat()..max.toFloat(),
            )
        }
    }
}

@Suppress("CyclomaticComplexMethod", "ReturnCount")
@Composable
internal fun <E : Enum<E>> buildFilterHeaderTitle(
    filterData: FormatFilterData<E>,
    strings: StringProvider,
): String? {
    return when (val filter = filterData.filterType) {
        is TableFilterType.TextTableFilter -> {
            val fv = filterData.filterState as? TableFilterState<String> ?: return null
            val constraint = fv.constraint ?: return null
            if (constraint.isNullCheck()) {
                return strings.get(constraint.toUiString())
            }
            val value = fv.values?.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
            "${strings.get(constraint.toUiString())} \"$value\""
        }

        is TableFilterType.NumberTableFilter<*> -> {
            val fv = filterData.filterState as? TableFilterState<*> ?: return null
            val constraint = fv.constraint ?: return null
            if (constraint.isNullCheck()) {
                return strings.get(constraint.toUiString())
            }
            val valuesLocal = fv.values ?: return null
            when {
                constraint == FilterConstraint.BETWEEN && valuesLocal.size >= 2 -> {
                    val from = valuesLocal[0]
                    val to = valuesLocal[1]
                    "${strings.get(constraint.toUiString())} $from - $to"
                }

                valuesLocal.firstOrNull() != null -> {
                    "${strings.get(constraint.toUiString())} ${valuesLocal.firstOrNull()}"
                }

                else -> {
                    null
                }
            }
        }

        is TableFilterType.BooleanTableFilter -> {
            val fv = filterData.filterState as? TableFilterState<Boolean> ?: return null
            val value = fv.values?.firstOrNull() ?: return null
            val boolType = if (value) BooleanType.TRUE else BooleanType.FALSE
            val valueTitle = filter.getTitle?.invoke(boolType) ?: strings.get(boolType.toUiString())
            "${strings.get(FilterConstraint.EQUALS.toUiString())} $valueTitle"
        }

        is TableFilterType.DateTableFilter -> {
            val fv = filterData.filterState as? TableFilterState<LocalDate> ?: return null
            val constraint = fv.constraint ?: return null
            if (constraint.isNullCheck()) {
                return strings.get(constraint.toUiString())
            }
            val valuesLocal = fv.values ?: return null
            return when {
                constraint == FilterConstraint.BETWEEN && valuesLocal.size >= 2 -> {
                    val from = valuesLocal[0].toFormatString()
                    val to = valuesLocal[1].toFormatString()
                    "${strings.get(constraint.toUiString())} $from - $to"
                }

                valuesLocal.firstOrNull() != null -> {
                    "${strings.get(constraint.toUiString())} ${valuesLocal.first().toFormatString()}"
                }

                else -> {
                    null
                }
            }
        }

        is TableFilterType.EnumTableFilter<*> -> {
            val fv = filterData.filterState as? TableFilterState<*> ?: return null
            val constraint = fv.constraint ?: return null
            if (constraint.isNullCheck()) {
                return strings.get(constraint.toUiString())
            }
            val value = fv.values ?: return null
            when (constraint) {
                FilterConstraint.EQUALS -> {
                    val single = (value.singleOrNull() as? Enum<*>) ?: return null
                    "${strings.get(constraint.toUiString())} ${single.name}"
                }

                FilterConstraint.IN, FilterConstraint.NOT_IN -> {
                    if (value.isEmpty()) return null
                    val joined = value.joinToString(", ") { v -> (v as Enum<*>).name }
                    "${strings.get(constraint.toUiString())} $joined"
                }

                else -> {
                    null
                }
            }
        }

        is TableFilterType.CustomTableFilter<*, *> -> {
            // Custom filters are not supported in conditional formatting
            null
        }

        TableFilterType.DisabledTableFilter -> {
            null
        }
    }
}
