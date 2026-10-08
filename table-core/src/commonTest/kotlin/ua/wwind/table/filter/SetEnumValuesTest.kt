package ua.wwind.table.filter

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import ua.wwind.table.filter.component.main.enumm.setEnumValues
import kotlin.test.Test

/** Select all / None over the options shown in the enum filter (issue #104). */
class SetEnumValuesTest {
    private enum class Color { Red, Green, Blue, Black }

    @Test
    fun `select all appends missing values and keeps the others`() {
        val result = setEnumValues(listOf(Color.Black, Color.Green), listOf(Color.Red, Color.Green), selected = true)
        assertThat(result).containsExactly(Color.Black, Color.Green, Color.Red)
    }

    @Test
    fun `none removes only the given values`() {
        val result =
            setEnumValues(listOf(Color.Black, Color.Green, Color.Red), listOf(Color.Red, Color.Green), selected = false)
        assertThat(result).containsExactly(Color.Black)
    }

    @Test
    fun `none over every selected value empties the selection`() {
        assertThat(setEnumValues(listOf(Color.Blue), Color.entries, selected = false)).isEmpty()
    }
}
