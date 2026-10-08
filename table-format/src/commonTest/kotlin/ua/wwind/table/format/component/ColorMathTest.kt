package ua.wwind.table.format.component

import androidx.compose.ui.graphics.Color
import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class ColorMathTest {
    @Test
    fun `opaque color formats as six hex digits`() {
        assertThat(Color(0xFF1E3A8A).toHex()).isEqualTo("#1E3A8A")
    }

    @Test
    fun `translucent color formats with its alpha first`() {
        assertThat(Color(0x801E3A8A).toHex()).isEqualTo("#801E3A8A")
    }

    @Test
    fun `unspecified color formats as empty`() {
        assertThat(Color.Unspecified.toHex()).isEqualTo("")
    }

    @Test
    fun `six digits parse as an opaque color`() {
        assertThat(parseHexColor("#1e3a8a")).isEqualTo(Color(0xFF1E3A8A))
    }

    @Test
    fun `eight digits parse with alpha and the hash is optional`() {
        assertThat(parseHexColor(" 801E3A8A ")).isEqualTo(Color(0x801E3A8A))
    }

    @Test
    fun `other input does not parse`() {
        assertThat(parseHexColor("#12345")).isNull()
        assertThat(parseHexColor("#GGGGGG")).isNull()
        assertThat(parseHexColor("")).isNull()
        assertThat(parseHexColor("+FFFFF")).isNull()
    }

    @Test
    fun `black on white has the maximum contrast`() {
        assertThat(contrastRatio(Color.Black, Color.White)).isBetween(20.99, 21.01)
    }

    @Test
    fun `contrast does not depend on the order of the colors`() {
        val a = Color(0xFF64748B)
        val b = Color(0xFFFACC15)
        assertThat(contrastRatio(a, b)).isEqualTo(contrastRatio(b, a))
    }

    @Test
    fun `slate gray on amber is below the WCAG minimum`() {
        assertThat(contrastRatio(Color(0xFF64748B), Color(0xFFFACC15))).isBetween(3.0, 3.2)
    }

    @Test
    fun `light backgrounds take black text and dark ones white`() {
        assertThat(Color(0xFFFEF3C7).readableContentColor()).isEqualTo(Color.Black)
        assertThat(Color(0xFFFACC15).readableContentColor()).isEqualTo(Color.Black)
        assertThat(Color(0xFF1E3A8A).readableContentColor()).isEqualTo(Color.White)
    }
}
