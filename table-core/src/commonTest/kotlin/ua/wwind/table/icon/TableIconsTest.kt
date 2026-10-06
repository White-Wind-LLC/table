package ua.wwind.table.icon

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorPath
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThanOrEqualTo
import kotlin.test.Test

class TableIconsTest {
    private val icons: Map<String, ImageVector> =
        mapOf(
            "PushPin" to TableIcons.PushPin,
            "PushPinOutlined" to TableIcons.PushPinOutlined,
            "Visibility" to TableIcons.Visibility,
            "VisibilityOff" to TableIcons.VisibilityOff,
            "SettingsEthernet" to TableIcons.SettingsEthernet,
            "SettingsBackupRestore" to TableIcons.SettingsBackupRestore,
            "TableRows" to TableIcons.TableRows,
            "MoreVert" to TableIcons.MoreVert,
        )

    @Test
    fun `column menu icons are 24dp vectors with path data`() {
        icons.values.forEach { icon ->
            assertThat(icon.viewportWidth).isEqualTo(24f)
            assertThat(icon.root.size).isGreaterThan(0)
        }
    }

    // A mistyped path command or coordinate shifts or collapses the glyph, which the checks above
    // cannot see. The bounds of the parsed outline must sit inside the viewport and fill a real
    // share of it (MoreVert is a 4x16 column of dots).
    @Test
    fun `column menu icon outlines are non-empty and inside the viewport`() {
        icons.forEach { (name, icon) ->
            val paths = icon.root.filterIsInstance<VectorPath>()
            assertThat(paths.size, name).isGreaterThan(0)
            val bounds = paths.map { PathParser().addPathNodes(it.pathData).toPath().getBounds() }
            val left = bounds.minOf { it.left }
            val top = bounds.minOf { it.top }
            val right = bounds.maxOf { it.right }
            val bottom = bounds.maxOf { it.bottom }
            assertThat(left, "$name left").isGreaterThanOrEqualTo(0f)
            assertThat(top, "$name top").isGreaterThanOrEqualTo(0f)
            assertThat(right, "$name right").isLessThanOrEqualTo(24f)
            assertThat(bottom, "$name bottom").isLessThanOrEqualTo(24f)
            // MoreVert is three dots in a 4-wide column, so only the longer side has to be large.
            assertThat(maxOf(right - left, bottom - top), "$name extent").isGreaterThanOrEqualTo(MIN_EXTENT)
            assertThat(minOf(right - left, bottom - top), "$name thin side").isGreaterThanOrEqualTo(MIN_THIN_EXTENT)
        }
    }

    private companion object {
        const val MIN_EXTENT = 8f
        const val MIN_THIN_EXTENT = 4f
    }
}
