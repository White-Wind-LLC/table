package ua.wwind.table.platform

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.datetime.LocalDate
import kotlin.test.Test

class DatePatternTest {
    private val date = LocalDate(2026, 3, 7)

    private fun render(
        pattern: String,
        date: LocalDate = this.date,
    ) = DatePattern.parse(pattern).format(date)

    @Test
    fun `fields take full width whatever the pattern width`() {
        assertThat(render("M/d/yy")).isEqualTo("03/07/2026")
        assertThat(render("dd.MM.yyyy")).isEqualTo("07.03.2026")
        assertThat(render("y-LL-dd")).isEqualTo("2026-03-07")
    }

    @Test
    fun `quoted text is literal`() {
        assertThat(render("y'年'M'月'd'日'")).isEqualTo("2026年03月07日")
        assertThat(render("d 'de' M 'de' y")).isEqualTo("07 de 03 de 2026")
        assertThat(render("d''M''y")).isEqualTo("07'03'2026")
        assertThat(render("d 'o''clock' y")).isEqualTo("07 o'clock 2026")
    }

    @Test
    fun `unknown fields are dropped`() {
        assertThat(render("GGG y/MM/dd")).isEqualTo(" 2026/03/07")
    }

    @Test
    fun `year is padded to four digits`() {
        assertThat(render("y-MM-dd", LocalDate(5, 1, 2))).isEqualTo("0005-01-02")
        assertThat(render("y-MM-dd", LocalDate(-5, 1, 2))).isEqualTo("-0005-01-02")
        assertThat(render("y-MM-dd", LocalDate(12345, 1, 2))).isEqualTo("12345-01-02")
    }
}
