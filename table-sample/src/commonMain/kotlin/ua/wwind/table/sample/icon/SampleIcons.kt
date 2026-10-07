/*
 * The index of icons only the sample app draws. Each icon is a vector-drawable XML file in
 * src/commonMain/composeResources/drawable; this object only maps names onto the generated accessors.
 *
 * Path data derived from Material Icons (https://github.com/google/material-design-icons),
 * Copyright (C) Google LLC, licensed under the Apache License, Version 2.0.
 *
 * To add an icon: save its 24dp vector drawable as composeResources/drawable/<name>_<style>.xml and
 * add a forwarding property below.
 */

package ua.wwind.table.sample.icon

import org.jetbrains.compose.resources.DrawableResource
import ua.wwind.table.sample.generated.resources.Res
import ua.wwind.table.sample.generated.resources.bar_chart_filled
import ua.wwind.table.sample.generated.resources.close_filled
import ua.wwind.table.sample.generated.resources.delete_filled
import ua.wwind.table.sample.generated.resources.edit_filled
import ua.wwind.table.sample.generated.resources.expand_less_filled
import ua.wwind.table.sample.generated.resources.expand_more_filled
import ua.wwind.table.sample.generated.resources.link_filled
import ua.wwind.table.sample.generated.resources.link_off_filled
import ua.wwind.table.sample.generated.resources.reorder_filled
import ua.wwind.table.sample.generated.resources.settings_filled
import ua.wwind.table.sample.generated.resources.star_filled

/**
 * Icons used only by the sample app. Deliberately not part of the library's public API.
 *
 * All 11 icons are `Icons.Filled.*` from Material.
 */
@Suppress("VariableNaming", "ktlint:standard:property-naming")
internal object SampleIcons {
    /** Material `Icons.Filled.Settings`. */
    val Settings: DrawableResource get() = Res.drawable.settings_filled

    /** Material `Icons.Filled.Edit`. */
    val Edit: DrawableResource get() = Res.drawable.edit_filled

    /** Material `Icons.Filled.Link`. */
    val Link: DrawableResource get() = Res.drawable.link_filled

    /** Material `Icons.Filled.LinkOff`. */
    val LinkOff: DrawableResource get() = Res.drawable.link_off_filled

    /** Material `Icons.Filled.ExpandLess`. */
    val ExpandLess: DrawableResource get() = Res.drawable.expand_less_filled

    /** Material `Icons.Filled.ExpandMore`. */
    val ExpandMore: DrawableResource get() = Res.drawable.expand_more_filled

    /** Material `Icons.Filled.Reorder`. */
    val Reorder: DrawableResource get() = Res.drawable.reorder_filled

    /** Material `Icons.Filled.Star`. */
    val Star: DrawableResource get() = Res.drawable.star_filled

    /** Material `Icons.Filled.BarChart`. */
    val BarChart: DrawableResource get() = Res.drawable.bar_chart_filled

    /** Material `Icons.Filled.Close`. */
    val Close: DrawableResource get() = Res.drawable.close_filled

    /** Material `Icons.Filled.Delete`. */
    val Delete: DrawableResource get() = Res.drawable.delete_filled
}
