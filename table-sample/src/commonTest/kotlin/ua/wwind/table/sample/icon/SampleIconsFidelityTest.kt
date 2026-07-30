package ua.wwind.table.sample.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.VectorPath
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

/**
 * Temporary. Proves every vendored sample icon is identical to the Material original while both are
 * still on the classpath. Deleted together with material-icons-extended.
 */
class SampleIconsFidelityTest {
    @Test
    fun vendoredSampleIconsMatchMaterialOriginals() {
        checkSampleIcon("Settings", SampleIcons.Settings, Icons.Filled.Settings)
        checkSampleIcon("Edit", SampleIcons.Edit, Icons.Filled.Edit)
        checkSampleIcon("Link", SampleIcons.Link, Icons.Filled.Link)
        checkSampleIcon("LinkOff", SampleIcons.LinkOff, Icons.Filled.LinkOff)
        checkSampleIcon("ExpandLess", SampleIcons.ExpandLess, Icons.Filled.ExpandLess)
        checkSampleIcon("ExpandMore", SampleIcons.ExpandMore, Icons.Filled.ExpandMore)
        checkSampleIcon("Reorder", SampleIcons.Reorder, Icons.Filled.Reorder)
        checkSampleIcon("Star", SampleIcons.Star, Icons.Filled.Star)
        checkSampleIcon("BarChart", SampleIcons.BarChart, Icons.Filled.BarChart)
        checkSampleIcon("Close", SampleIcons.Close, Icons.Filled.Close)
        checkSampleIcon("Delete", SampleIcons.Delete, Icons.Filled.Delete)
    }
}

private fun checkSampleIcon(
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
