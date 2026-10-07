package ua.wwind.table.icon

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThanOrEqualTo
import assertk.assertions.isTrue
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.test.Test

// Resources are read from the classpath here; common tests on iOS and wasm cannot load them reliably.
@OptIn(ExperimentalTestApi::class)
class TableIconsTest {
    private val icons: Map<String, DrawableResource> =
        mapOf(
            "Close" to TableIcons.Close,
            "KeyboardArrowLeft" to TableIcons.KeyboardArrowLeft,
            "KeyboardArrowRight" to TableIcons.KeyboardArrowRight,
            "ArrowUpward" to TableIcons.ArrowUpward,
            "ArrowDownward" to TableIcons.ArrowDownward,
            "Sort" to TableIcons.Sort,
            "FilterAltFilled" to TableIcons.FilterAltFilled,
            "FilterAltOutlined" to TableIcons.FilterAltOutlined,
            "DragIndicator" to TableIcons.DragIndicator,
            "SwapHoriz" to TableIcons.SwapHoriz,
            "Add" to TableIcons.Add,
            "Delete" to TableIcons.Delete,
            "ContentCopy" to TableIcons.ContentCopy,
            "Save" to TableIcons.Save,
            "ArrowDropUp" to TableIcons.ArrowDropUp,
            "Check" to TableIcons.Check,
            "FormatColorReset" to TableIcons.FormatColorReset,
            "PushPin" to TableIcons.PushPin,
            "PushPinOutlined" to TableIcons.PushPinOutlined,
            "Visibility" to TableIcons.Visibility,
            "VisibilityOff" to TableIcons.VisibilityOff,
            "SettingsEthernet" to TableIcons.SettingsEthernet,
            "SettingsBackupRestore" to TableIcons.SettingsBackupRestore,
            "TableRows" to TableIcons.TableRows,
            "ErrorOutline" to TableIcons.ErrorOutline,
            "MoreVert" to TableIcons.MoreVert,
        )

    private fun loadAll(): Map<String, ImageVector> {
        val loaded = mutableMapOf<String, ImageVector>()
        runComposeUiTest {
            setContent { icons.forEach { (name, resource) -> loaded[name] = vectorResource(resource) } }
            waitForIdle()
        }
        return loaded
    }

    @Test
    fun `every icon loads as a 24 unit vector with paths`() {
        val loaded = loadAll()
        assertThat(loaded.size).isEqualTo(icons.size)
        loaded.forEach { (name, icon) ->
            assertThat(icon.viewportWidth, name).isEqualTo(24f)
            assertThat(icon.viewportHeight, name).isEqualTo(24f)
            assertThat(icon.root.size, name).isGreaterThan(0)
        }
    }

    // A mistyped path command or coordinate shifts or collapses the glyph, which the checks above
    // cannot see. The bounds of the parsed outline must sit inside the viewport and fill a real
    // share of it (MoreVert is a 4x16 column of dots, ArrowDropUp a 7.85-wide triangle).
    @Test
    fun `icon outlines are non-empty and inside the viewport`() {
        loadAll().forEach { (name, icon) ->
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
            assertThat(maxOf(right - left, bottom - top), "$name extent").isGreaterThanOrEqualTo(MIN_EXTENT)
            assertThat(minOf(right - left, bottom - top), "$name thin side").isGreaterThanOrEqualTo(MIN_THIN_EXTENT)
        }
    }

    @Test
    fun `auto mirrored icons keep mirroring`() {
        val mirrored = setOf("KeyboardArrowLeft", "KeyboardArrowRight", "Sort")
        loadAll().forEach { (name, icon) ->
            assertThat(icon.autoMirror, name).isEqualTo(name in mirrored)
        }
    }

    @Test
    fun `push pin keeps its even odd fill`() {
        val pin = loadAll().getValue("PushPin")
        assertThat(pin.root.filterIsInstance<VectorPath>().any { it.pathFillType == PathFillType.EvenOdd }).isTrue()
    }

    private companion object {
        const val MIN_EXTENT = 7f
        const val MIN_THIN_EXTENT = 2f
    }
}
