/*
 * Icon path data derived from Material Icons (https://github.com/google/material-design-icons),
 * Copyright (C) Google LLC, licensed under the Apache License, Version 2.0.
 *
 * Generated, not hand-written. Do not edit the path data by hand — see the header of
 * SampleIcons.kt for how to regenerate it or add an icon.
 */

@file:Suppress("VariableNaming", "ktlint:standard:property-naming")

package ua.wwind.table.sample.icon.vector

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Material `Icons.Filled.LinkOff`. */
internal val LinkOffIcon: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "LinkOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black), strokeLineWidth = 1.0f) {
                moveTo(17.0f, 7.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(1.9f)
                horizontalLineToRelative(4.0f)
                curveToRelative(1.71f, 0.0f, 3.1f, 1.39f, 3.1f, 3.1f)
                curveToRelative(0.0f, 1.43f, -0.98f, 2.63f, -2.31f, 2.98f)
                lineToRelative(1.46f, 1.46f)
                curveTo(20.88f, 15.61f, 22.0f, 13.95f, 22.0f, 12.0f)
                curveToRelative(0.0f, -2.76f, -2.24f, -5.0f, -5.0f, -5.0f)
                close()
                moveTo(16.0f, 11.0f)
                horizontalLineToRelative(-2.19f)
                lineToRelative(2.0f, 2.0f)
                lineTo(16.0f, 13.0f)
                close()
                moveTo(2.0f, 4.27f)
                lineToRelative(3.11f, 3.11f)
                curveTo(3.29f, 8.12f, 2.0f, 9.91f, 2.0f, 12.0f)
                curveToRelative(0.0f, 2.76f, 2.24f, 5.0f, 5.0f, 5.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(-1.9f)
                lineTo(7.0f, 15.1f)
                curveToRelative(-1.71f, 0.0f, -3.1f, -1.39f, -3.1f, -3.1f)
                curveToRelative(0.0f, -1.59f, 1.21f, -2.9f, 2.76f, -3.07f)
                lineTo(8.73f, 11.0f)
                lineTo(8.0f, 11.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(2.73f)
                lineTo(13.0f, 15.27f)
                lineTo(13.0f, 17.0f)
                horizontalLineToRelative(1.73f)
                lineToRelative(4.01f, 4.0f)
                lineTo(20.0f, 19.74f)
                lineTo(3.27f, 3.0f)
                lineTo(2.0f, 4.27f)
                close()
            }
        }.build()
}
