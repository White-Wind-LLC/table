/*
 * The index of icons this library draws. Each icon's path data lives in its own file under the
 * `vector` subpackage; this object is hand-maintained and only maps public names onto them.
 *
 * Path data derived from Material Icons (https://github.com/google/material-design-icons),
 * Copyright (C) Google LLC, licensed under the Apache License, Version 2.0. It was generated from
 * org.jetbrains.compose.material:material-icons-extended:1.7.3, which is deprecated and no longer
 * tracks Compose releases.
 *
 * To add an icon: copy the `d` attribute of the icon's 24px SVG from
 * https://github.com/google/material-design-icons (src/<category>/<name>/<style>/24px.svg) into a
 * `vector/<Name>.kt` file built with `addPathNodes`, as `vector/PushPin.kt` does, and add a
 * forwarding property below. Keep `PathFillType.EvenOdd` when the SVG says `fill-rule="evenodd"`.
 */

package ua.wwind.table.icon

import androidx.compose.ui.graphics.vector.ImageVector
import ua.wwind.table.icon.vector.AddIcon
import ua.wwind.table.icon.vector.ArrowDownwardIcon
import ua.wwind.table.icon.vector.ArrowDropUpIcon
import ua.wwind.table.icon.vector.ArrowUpwardIcon
import ua.wwind.table.icon.vector.CheckIcon
import ua.wwind.table.icon.vector.CloseIcon
import ua.wwind.table.icon.vector.ContentCopyIcon
import ua.wwind.table.icon.vector.DeleteIcon
import ua.wwind.table.icon.vector.DragIndicatorIcon
import ua.wwind.table.icon.vector.FilterAltFilledIcon
import ua.wwind.table.icon.vector.FilterAltOutlinedIcon
import ua.wwind.table.icon.vector.FormatColorResetIcon
import ua.wwind.table.icon.vector.KeyboardArrowLeftIcon
import ua.wwind.table.icon.vector.KeyboardArrowRightIcon
import ua.wwind.table.icon.vector.MoreVertIcon
import ua.wwind.table.icon.vector.PushPinIcon
import ua.wwind.table.icon.vector.PushPinOutlinedIcon
import ua.wwind.table.icon.vector.SaveIcon
import ua.wwind.table.icon.vector.SettingsBackupRestoreIcon
import ua.wwind.table.icon.vector.SettingsEthernetIcon
import ua.wwind.table.icon.vector.SortIcon
import ua.wwind.table.icon.vector.SwapHorizIcon
import ua.wwind.table.icon.vector.TableRowsIcon
import ua.wwind.table.icon.vector.VisibilityIcon
import ua.wwind.table.icon.vector.VisibilityOffIcon

/**
 * Icons drawn by the table. Vendored so the library ships no icon dependency.
 *
 * Naming convention: each property is named after the plain Material icon (e.g. [Close], [Add]).
 * A suffix is added only where two variants of the same icon are both needed in the table, as with
 * [FilterAltFilled] and [FilterAltOutlined]; the suffix describes the glyph's appearance (solid vs.
 * outline), not the Material style family — Rounded, Filled, Outlined — the glyph was drawn from. See
 * each property's doc comment for its exact Material source.
 */
@Suppress("VariableNaming", "ktlint:standard:property-naming")
public object TableIcons {
    /** Material `Icons.Rounded.Close`. */
    public val Close: ImageVector get() = CloseIcon

    /** Material `Icons.AutoMirrored.Rounded.KeyboardArrowLeft`. */
    public val KeyboardArrowLeft: ImageVector get() = KeyboardArrowLeftIcon

    /** Material `Icons.AutoMirrored.Rounded.KeyboardArrowRight`. */
    public val KeyboardArrowRight: ImageVector get() = KeyboardArrowRightIcon

    /** Material `Icons.Rounded.ArrowUpward`. */
    public val ArrowUpward: ImageVector get() = ArrowUpwardIcon

    /** Material `Icons.Rounded.ArrowDownward`. */
    public val ArrowDownward: ImageVector get() = ArrowDownwardIcon

    /** Material `Icons.AutoMirrored.Outlined.Sort`. */
    public val Sort: ImageVector get() = SortIcon

    /** Material `Icons.Filled.FilterAltFilled`. */
    public val FilterAltFilled: ImageVector get() = FilterAltFilledIcon

    /** Material `Icons.Filled.FilterAltOutlined`. */
    public val FilterAltOutlined: ImageVector get() = FilterAltOutlinedIcon

    /** Material `Icons.Filled.DragIndicator` (the Rounded variant is byte-identical). */
    public val DragIndicator: ImageVector get() = DragIndicatorIcon

    /** Material `Icons.Filled.SwapHoriz`. */
    public val SwapHoriz: ImageVector get() = SwapHorizIcon

    /** Material `Icons.Rounded.Add`. */
    public val Add: ImageVector get() = AddIcon

    /** Material `Icons.Rounded.Delete`. */
    public val Delete: ImageVector get() = DeleteIcon

    /** Material `Icons.Rounded.ContentCopy`. */
    public val ContentCopy: ImageVector get() = ContentCopyIcon

    /** Material `Icons.Rounded.Save`. */
    public val Save: ImageVector get() = SaveIcon

    /** Material `Icons.Rounded.ArrowDropUp`. */
    public val ArrowDropUp: ImageVector get() = ArrowDropUpIcon

    /** Material `Icons.Rounded.Check`. */
    public val Check: ImageVector get() = CheckIcon

    /** Material `Icons.Filled.FormatColorReset`. */
    public val FormatColorReset: ImageVector get() = FormatColorResetIcon

    /** Material `Icons.Rounded.PushPin`. */
    public val PushPin: ImageVector get() = PushPinIcon

    /** Material `Icons.Outlined.PushPin`. */
    public val PushPinOutlined: ImageVector get() = PushPinOutlinedIcon

    /** Material `Icons.Rounded.Visibility`. */
    public val Visibility: ImageVector get() = VisibilityIcon

    /** Material `Icons.Rounded.VisibilityOff`. */
    public val VisibilityOff: ImageVector get() = VisibilityOffIcon

    /** Material `Icons.Rounded.SettingsEthernet`. */
    public val SettingsEthernet: ImageVector get() = SettingsEthernetIcon

    /** Material `Icons.Rounded.SettingsBackupRestore`. */
    public val SettingsBackupRestore: ImageVector get() = SettingsBackupRestoreIcon

    /** Material `Icons.Rounded.TableRows`. */
    public val TableRows: ImageVector get() = TableRowsIcon

    /** Material `Icons.Rounded.MoreVert`. */
    public val MoreVert: ImageVector get() = MoreVertIcon
}
