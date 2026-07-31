/*
 * The index of icons only the sample app draws. Each icon's path data lives in its own file under
 * the `vector` subpackage; this object is hand-maintained and only maps names onto them.
 *
 * Path data derived from Material Icons (https://github.com/google/material-design-icons),
 * Copyright (C) Google LLC, licensed under the Apache License, Version 2.0. It was generated from
 * org.jetbrains.compose.material:material-icons-extended:1.7.3, which is deprecated and no longer
 * tracks Compose releases.
 *
 * To add an icon: restore the generator and the dependency from commit 9e41031
 * (`git show 9e41031:table-core/src/jvmTest/kotlin/ua/wwind/table/icon/IconDumpTest.kt`), put the
 * builder it emits in `vector/<Name>.kt`, and add a forwarding property below. Failing that,
 * transcribe the 24x24 path from https://github.com/google/material-design-icons.
 */

package ua.wwind.table.sample.icon

import androidx.compose.ui.graphics.vector.ImageVector
import ua.wwind.table.sample.icon.vector.BarChartIcon
import ua.wwind.table.sample.icon.vector.CloseIcon
import ua.wwind.table.sample.icon.vector.DeleteIcon
import ua.wwind.table.sample.icon.vector.EditIcon
import ua.wwind.table.sample.icon.vector.ExpandLessIcon
import ua.wwind.table.sample.icon.vector.ExpandMoreIcon
import ua.wwind.table.sample.icon.vector.LinkIcon
import ua.wwind.table.sample.icon.vector.LinkOffIcon
import ua.wwind.table.sample.icon.vector.ReorderIcon
import ua.wwind.table.sample.icon.vector.SettingsIcon
import ua.wwind.table.sample.icon.vector.StarIcon

/**
 * Icons used only by the sample app. Deliberately not part of the library's public API.
 *
 * All 11 icons are `Icons.Filled.*` from Material.
 */
@Suppress("VariableNaming", "ktlint:standard:property-naming")
internal object SampleIcons {
    /** Material `Icons.Filled.Settings`. */
    val Settings: ImageVector get() = SettingsIcon

    /** Material `Icons.Filled.Edit`. */
    val Edit: ImageVector get() = EditIcon

    /** Material `Icons.Filled.Link`. */
    val Link: ImageVector get() = LinkIcon

    /** Material `Icons.Filled.LinkOff`. */
    val LinkOff: ImageVector get() = LinkOffIcon

    /** Material `Icons.Filled.ExpandLess`. */
    val ExpandLess: ImageVector get() = ExpandLessIcon

    /** Material `Icons.Filled.ExpandMore`. */
    val ExpandMore: ImageVector get() = ExpandMoreIcon

    /** Material `Icons.Filled.Reorder`. */
    val Reorder: ImageVector get() = ReorderIcon

    /** Material `Icons.Filled.Star`. */
    val Star: ImageVector get() = StarIcon

    /** Material `Icons.Filled.BarChart`. */
    val BarChart: ImageVector get() = BarChartIcon

    /** Material `Icons.Filled.Close`. */
    val Close: ImageVector get() = CloseIcon

    /** Material `Icons.Filled.Delete`. */
    val Delete: ImageVector get() = DeleteIcon
}
