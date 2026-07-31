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

/** Material `Icons.Filled.Link`. */
internal val LinkIcon: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "Link",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black), strokeLineWidth = 1.0f) {
                moveTo(3.9f, 12.0f)
                curveToRelative(0.0f, -1.71f, 1.39f, -3.1f, 3.1f, -3.1f)
                horizontalLineToRelative(4.0f)
                lineTo(11.0f, 7.0f)
                lineTo(7.0f, 7.0f)
                curveToRelative(-2.76f, 0.0f, -5.0f, 2.24f, -5.0f, 5.0f)
                reflectiveCurveToRelative(2.24f, 5.0f, 5.0f, 5.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(-1.9f)
                lineTo(7.0f, 15.1f)
                curveToRelative(-1.71f, 0.0f, -3.1f, -1.39f, -3.1f, -3.1f)
                close()
                moveTo(8.0f, 13.0f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(-2.0f)
                lineTo(8.0f, 11.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(17.0f, 7.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(1.9f)
                horizontalLineToRelative(4.0f)
                curveToRelative(1.71f, 0.0f, 3.1f, 1.39f, 3.1f, 3.1f)
                reflectiveCurveToRelative(-1.39f, 3.1f, -3.1f, 3.1f)
                horizontalLineToRelative(-4.0f)
                lineTo(13.0f, 17.0f)
                horizontalLineToRelative(4.0f)
                curveToRelative(2.76f, 0.0f, 5.0f, -2.24f, 5.0f, -5.0f)
                reflectiveCurveToRelative(-2.24f, -5.0f, -5.0f, -5.0f)
                close()
            }
        }.build()
}
