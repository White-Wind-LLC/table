package ua.wwind.table.config

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test

class FontScaledDimensionsTest {
    private val standard = TableDefaults.standardDimensions()
    private val style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
    private val typography = TableTypography(header = style, body = style, footer = style, groupHeader = style)

    private fun density(fontScale: Float) = Density(density = 1f, fontScale = fontScale)

    @Test
    fun `at 1x nothing changes`() {
        assertThat(standard.scaledForFont(typography, density(1f))).isSameInstanceAs(standard)
    }

    @Test
    fun `at 0_85x nothing changes`() {
        assertThat(standard.scaledForFont(typography, density(0.85f))).isSameInstanceAs(standard)
    }

    @Test
    fun `opting out keeps the heights at 2x`() {
        val dimensions = standard.copy(scaleWithFontSize = false)
        assertThat(dimensions.scaledForFont(typography, density(2f))).isSameInstanceAs(dimensions)
    }

    @Test
    fun `at 2x each height grows by one extra line`() {
        val scaled = standard.scaledForFont(typography, density(2f))
        assertThat(scaled.rowHeight).isEqualTo(72.dp)
        assertThat(scaled.headerHeight).isEqualTo(76.dp)
        assertThat(scaled.footerHeight).isEqualTo(72.dp)
        assertThat(scaled.fastFilterRowHeight).isEqualTo(60.dp)
        assertThat(scaled.defaultColumnWidth).isEqualTo(standard.defaultColumnWidth)
    }

    @Test
    fun `each height follows its own style`() {
        val custom =
            TableTypography(
                header = TextStyle(lineHeight = 10.sp),
                body = TextStyle(lineHeight = 20.sp),
                footer = TextStyle(lineHeight = 30.sp),
                groupHeader = TextStyle(lineHeight = 5.sp),
            )
        val scaled = standard.scaledForFont(custom, density(2f))
        assertThat(scaled.headerHeight).isEqualTo(66.dp)
        assertThat(scaled.rowHeight).isEqualTo(72.dp)
        assertThat(scaled.footerHeight).isEqualTo(82.dp)
        assertThat(scaled.fastFilterRowHeight).isEqualTo(60.dp)
    }

    @Test
    fun `row height takes the larger of the body and group header growth`() {
        val custom = typography.copy(groupHeader = TextStyle(lineHeight = 30.sp))
        val scaled = standard.scaledForFont(custom, density(2f))
        assertThat(scaled.rowHeight).isEqualTo(82.dp)
        assertThat(scaled.fastFilterRowHeight).isEqualTo(60.dp)
    }

    @Test
    fun `an unspecified line height falls back to 1_2 times the font size`() {
        val fallback = TextStyle(fontSize = 10.sp)
        val custom = TableTypography(header = fallback, body = fallback, footer = fallback, groupHeader = fallback)
        val scaled = standard.scaledForFont(custom, density(2f))
        assertThat(scaled.headerHeight.value).isCloseTo(68f, 0.01f)
    }

    @Test
    fun `a style with neither line height nor font size adds nothing`() {
        val empty = TextStyle(fontSize = TextUnit.Unspecified, lineHeight = TextUnit.Unspecified)
        val custom = TableTypography(header = empty, body = empty, footer = empty, groupHeader = empty)
        assertThat(standard.scaledForFont(custom, density(2f))).isSameInstanceAs(standard)
    }
}
