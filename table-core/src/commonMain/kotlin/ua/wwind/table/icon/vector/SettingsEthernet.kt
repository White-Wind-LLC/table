/*
 * Icon path data derived from Material Icons (https://github.com/google/material-design-icons),
 * Copyright (C) Google LLC, licensed under the Apache License, Version 2.0.
 *
 * Path data copied verbatim from the icon's 24px SVG. Do not edit the path data by hand — see the
 * header of TableIcons.kt for how to add an icon.
 */

@file:Suppress("VariableNaming", "ktlint:standard:property-naming")

package ua.wwind.table.icon.vector

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Material `Icons.Rounded.SettingsEthernet` (src/action/settings_ethernet/materialiconsround). */
internal val SettingsEthernetIcon: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "SettingsEthernet",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData =
                addPathNodes(
                    "M7.71,6.71L7.71,6.71c-0.39-0.39-1.02-0.39-1.41,0l-4.59,4.59c-0.39,0.39-0.39," +
                        "1.02,0,1.41l4.59,4.59c0.39,0.39,1.02,0.39,1.41,0l0,0c0.39-0.39,0.39-1.02,0-1.41L3.83," +
                        "12l3.88-3.88C8.09,7.73,8.09,7.09,7.71,6.71zM16.29,6.71L16.29,6.71c-0.39,0.39-0.39,1.02," +
                        "0,1.41L20.17,12l-3.88,3.88c-0.39,0.39-0.39,1.02,0,1.41l0,0c0.39,0.39,1.02,0.39,1.41,0l4.59-" +
                        "4.59c0.39-0.39,0.39-1.02,0-1.41l-4.59-4.59C17.32,6.32,16.68,6.32,16.29,6.71zM8,13L8,13c0.55," +
                        "0,1-0.45,1-1v0c0-0.55-0.45-1-1-1h0c-0.55,0-1,0.45-1,1v0C7,12.55,7.45,13,8,13zM12,13L12,13" +
                        "c0.55,0,1-0.45,1-1v0c0-0.55-0.45-1-1-1h0c-0.55,0-1,0.45-1,1v0C11,12.55,11.45,13,12,13zM16," +
                        "11L16,11c-0.55,0-1,0.45-1,1v0c0,0.55,0.45,1,1,1h0c0.55,0,1-0.45,1-1v0C17,11.45,16.55,11,16,11z",
                ),
            fill = SolidColor(Color.Black),
        ).build()
}
