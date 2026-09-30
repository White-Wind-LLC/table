package ua.wwind.table.filter

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.datetime.LocalDate
import ua.wwind.table.filter.component.main.date.datePickerMillisToLocalDate
import ua.wwind.table.filter.component.main.date.toDatePickerMillis
import kotlin.test.Test

class DatePickerMillisTest {
    // 2026-03-15T00:00:00Z — what DatePickerState reports for 15 March, whatever the system zone.
    private val march15UtcMidnight = 1_773_532_800_000L

    @Test
    fun `date converts to utc midnight`() {
        assertThat(LocalDate(2026, 3, 15).toDatePickerMillis()).isEqualTo(march15UtcMidnight)
    }

    @Test
    fun `picker millis convert back to the same day`() {
        assertThat(datePickerMillisToLocalDate(march15UtcMidnight)).isEqualTo(LocalDate(2026, 3, 15))
    }

    @Test
    fun `round trip keeps the date at the year boundaries`() {
        listOf(LocalDate(2026, 1, 1), LocalDate(2025, 12, 31), LocalDate(2024, 2, 29)).forEach { date ->
            assertThat(datePickerMillisToLocalDate(date.toDatePickerMillis())).isEqualTo(date)
        }
    }
}
