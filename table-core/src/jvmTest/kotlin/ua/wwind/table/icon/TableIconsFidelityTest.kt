package ua.wwind.table.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.VectorPath
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

/**
 * Temporary. Proves every vendored icon is identical to the Material original while both are still
 * on the classpath. Deleted together with material-icons-extended.
 */
class TableIconsFidelityTest {
    @Test
    fun vendoredIconsMatchMaterialOriginals() {
        checkIcon("Close", TableIcons.Close, Icons.Rounded.Close)
        checkIcon("KeyboardArrowLeft", TableIcons.KeyboardArrowLeft, Icons.AutoMirrored.Rounded.KeyboardArrowLeft)
        checkIcon("KeyboardArrowRight", TableIcons.KeyboardArrowRight, Icons.AutoMirrored.Rounded.KeyboardArrowRight)
        checkIcon("ArrowUpward", TableIcons.ArrowUpward, Icons.Rounded.ArrowUpward)
        checkIcon("ArrowDownward", TableIcons.ArrowDownward, Icons.Rounded.ArrowDownward)
        checkIcon("Sort", TableIcons.Sort, Icons.AutoMirrored.Outlined.Sort)
        checkIcon("FilterAltFilled", TableIcons.FilterAltFilled, Icons.Rounded.FilterAlt)
        checkIcon("FilterAltOutlined", TableIcons.FilterAltOutlined, Icons.Outlined.FilterAlt)
        checkIcon("DragIndicator", TableIcons.DragIndicator, Icons.Filled.DragIndicator)
        checkIcon("SwapHoriz", TableIcons.SwapHoriz, Icons.Filled.SwapHoriz)
        checkIcon("Add", TableIcons.Add, Icons.Rounded.Add)
        checkIcon("Delete", TableIcons.Delete, Icons.Rounded.Delete)
        checkIcon("ContentCopy", TableIcons.ContentCopy, Icons.Rounded.ContentCopy)
        checkIcon("Save", TableIcons.Save, Icons.Rounded.Save)
        checkIcon("ArrowDropUp", TableIcons.ArrowDropUp, Icons.Rounded.ArrowDropUp)
        checkIcon("Check", TableIcons.Check, Icons.Rounded.Check)
        checkIcon("FormatColorReset", TableIcons.FormatColorReset, Icons.Filled.FormatColorReset)
    }
}

private fun checkIcon(
    label: String,
    vendored: ImageVector,
    original: ImageVector,
) {
    assertThat(vendored.defaultWidth, "$label defaultWidth").isEqualTo(original.defaultWidth)
    assertThat(vendored.defaultHeight, "$label defaultHeight").isEqualTo(original.defaultHeight)
    assertThat(vendored.viewportWidth, "$label viewportWidth").isEqualTo(original.viewportWidth)
    assertThat(vendored.viewportHeight, "$label viewportHeight").isEqualTo(original.viewportHeight)
    assertThat(vendored.autoMirror, "$label autoMirror").isEqualTo(original.autoMirror)
    assertThat(vendored.pathCommands(), "$label pathData").isEqualTo(original.pathCommands())
}

private fun ImageVector.pathCommands(): List<PathNode> = root.filterIsInstance<VectorPath>().flatMap { it.pathData }
