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

/** Material `Icons.Outlined.PushPin` (src/content/push_pin/materialiconsoutlined). */
internal val PushPinOutlinedIcon: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "PushPinOutlined",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData =
                addPathNodes(
                    "M14,4v5c0,1.12,0.37,2.16,1,3H9c0.65-0.86,1-1.9,1-3V4H14M17,2H7C6.45,2,6,2.45," +
                        "6,3c0,0.55,0.45,1,1,1c0,0,0,0,0,0l1,0v5c0,1.66-1.34,3-3,3v2h5.97v7l1,1l1-1v-7H19v-2" +
                        "c0,0,0,0,0,0c-1.66,0-3-1.34-3-3V4l1,0c0,0,0,0,0,0c0.55,0,1-0.45,1-1C18,2.45,17.55,2,17," +
                        "2L17,2z",
                ),
            fill = SolidColor(Color.Black),
        ).build()
}
