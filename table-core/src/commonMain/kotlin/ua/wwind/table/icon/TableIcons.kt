/*
 * The index of icons this library draws. Each icon is a vector-drawable XML file in
 * src/commonMain/composeResources/drawable; this object is hand-maintained and only maps public
 * names onto the generated resource accessors.
 *
 * Path data derived from Material Icons (https://github.com/google/material-design-icons),
 * Copyright (C) Google LLC, licensed under the Apache License, Version 2.0.
 *
 * To add an icon: take its 24dp vector drawable (Android Studio's Vector Asset, or the icon's XML
 * from https://github.com/google/material-design-icons), save it as
 * composeResources/drawable/<name>_<style>.xml — e.g. close_rounded.xml — and add a forwarding
 * property below. Keep android:autoMirrored="true" for AutoMirrored icons.
 */

package ua.wwind.table.icon

import org.jetbrains.compose.resources.DrawableResource
import ua.wwind.table.generated.resources.Res
import ua.wwind.table.generated.resources.add_rounded
import ua.wwind.table.generated.resources.arrow_downward_rounded
import ua.wwind.table.generated.resources.arrow_drop_up_rounded
import ua.wwind.table.generated.resources.arrow_upward_rounded
import ua.wwind.table.generated.resources.check_rounded
import ua.wwind.table.generated.resources.close_rounded
import ua.wwind.table.generated.resources.content_copy_rounded
import ua.wwind.table.generated.resources.delete_rounded
import ua.wwind.table.generated.resources.drag_indicator_filled
import ua.wwind.table.generated.resources.error_outline_rounded
import ua.wwind.table.generated.resources.filter_alt_filled
import ua.wwind.table.generated.resources.filter_alt_outlined
import ua.wwind.table.generated.resources.format_color_reset_filled
import ua.wwind.table.generated.resources.keyboard_arrow_left_auto_mirrored_rounded
import ua.wwind.table.generated.resources.keyboard_arrow_right_auto_mirrored_rounded
import ua.wwind.table.generated.resources.more_vert_rounded
import ua.wwind.table.generated.resources.push_pin_outlined
import ua.wwind.table.generated.resources.push_pin_rounded
import ua.wwind.table.generated.resources.save_rounded
import ua.wwind.table.generated.resources.settings_backup_restore_rounded
import ua.wwind.table.generated.resources.settings_ethernet_rounded
import ua.wwind.table.generated.resources.sort_auto_mirrored_outlined
import ua.wwind.table.generated.resources.swap_horiz_filled
import ua.wwind.table.generated.resources.table_rows_rounded
import ua.wwind.table.generated.resources.visibility_off_rounded
import ua.wwind.table.generated.resources.visibility_rounded

/**
 * Icons drawn by the table, as Compose resources. Draw one with
 * `Icon(painterResource(TableIcons.Close), contentDescription)`.
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
    public val Close: DrawableResource get() = Res.drawable.close_rounded

    /** Material `Icons.AutoMirrored.Rounded.KeyboardArrowLeft`. */
    public val KeyboardArrowLeft: DrawableResource get() = Res.drawable.keyboard_arrow_left_auto_mirrored_rounded

    /** Material `Icons.AutoMirrored.Rounded.KeyboardArrowRight`. */
    public val KeyboardArrowRight: DrawableResource get() = Res.drawable.keyboard_arrow_right_auto_mirrored_rounded

    /** Material `Icons.Rounded.ArrowUpward`. */
    public val ArrowUpward: DrawableResource get() = Res.drawable.arrow_upward_rounded

    /** Material `Icons.Rounded.ArrowDownward`. */
    public val ArrowDownward: DrawableResource get() = Res.drawable.arrow_downward_rounded

    /** Material `Icons.AutoMirrored.Outlined.Sort`. */
    public val Sort: DrawableResource get() = Res.drawable.sort_auto_mirrored_outlined

    /** Material `Icons.Filled.FilterAltFilled`. */
    public val FilterAltFilled: DrawableResource get() = Res.drawable.filter_alt_filled

    /** Material `Icons.Filled.FilterAltOutlined`. */
    public val FilterAltOutlined: DrawableResource get() = Res.drawable.filter_alt_outlined

    /** Material `Icons.Filled.DragIndicator` (the Rounded variant is byte-identical). */
    public val DragIndicator: DrawableResource get() = Res.drawable.drag_indicator_filled

    /** Material `Icons.Filled.SwapHoriz`. */
    public val SwapHoriz: DrawableResource get() = Res.drawable.swap_horiz_filled

    /** Material `Icons.Rounded.Add`. */
    public val Add: DrawableResource get() = Res.drawable.add_rounded

    /** Material `Icons.Rounded.Delete`. */
    public val Delete: DrawableResource get() = Res.drawable.delete_rounded

    /** Material `Icons.Rounded.ContentCopy`. */
    public val ContentCopy: DrawableResource get() = Res.drawable.content_copy_rounded

    /** Material `Icons.Rounded.Save`. */
    public val Save: DrawableResource get() = Res.drawable.save_rounded

    /** Material `Icons.Rounded.ArrowDropUp`. */
    public val ArrowDropUp: DrawableResource get() = Res.drawable.arrow_drop_up_rounded

    /** Material `Icons.Rounded.Check`. */
    public val Check: DrawableResource get() = Res.drawable.check_rounded

    /** Material `Icons.Filled.FormatColorReset`. */
    public val FormatColorReset: DrawableResource get() = Res.drawable.format_color_reset_filled

    /** Material `Icons.Rounded.PushPin`. */
    public val PushPin: DrawableResource get() = Res.drawable.push_pin_rounded

    /** Material `Icons.Outlined.PushPin`. */
    public val PushPinOutlined: DrawableResource get() = Res.drawable.push_pin_outlined

    /** Material `Icons.Rounded.Visibility`. */
    public val Visibility: DrawableResource get() = Res.drawable.visibility_rounded

    /** Material `Icons.Rounded.VisibilityOff`. */
    public val VisibilityOff: DrawableResource get() = Res.drawable.visibility_off_rounded

    /** Material `Icons.Rounded.SettingsEthernet`. */
    public val SettingsEthernet: DrawableResource get() = Res.drawable.settings_ethernet_rounded

    /** Material `Icons.Rounded.SettingsBackupRestore`. */
    public val SettingsBackupRestore: DrawableResource get() = Res.drawable.settings_backup_restore_rounded

    /** Material `Icons.Rounded.TableRows`. */
    public val TableRows: DrawableResource get() = Res.drawable.table_rows_rounded

    /** Material `Icons.Rounded.ErrorOutline`. */
    public val ErrorOutline: DrawableResource get() = Res.drawable.error_outline_rounded

    /** Material `Icons.Rounded.MoreVert`. */
    public val MoreVert: DrawableResource get() = Res.drawable.more_vert_rounded
}
