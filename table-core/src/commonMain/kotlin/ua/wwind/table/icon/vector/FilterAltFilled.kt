/*
 * Icon path data derived from Material Icons (https://github.com/google/material-design-icons),
 * Copyright (C) Google LLC, licensed under the Apache License, Version 2.0.
 *
 * Generated, not hand-written. Do not edit the path data by hand — see the header of
 * TableIcons.kt for how to regenerate it or add an icon.
 */

@file:Suppress("VariableNaming", "ktlint:standard:property-naming")

package ua.wwind.table.icon.vector

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Material `Icons.Filled.FilterAltFilled`. */
internal val FilterAltFilledIcon: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "FilterAltFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black), strokeLineWidth = 1.0f) {
                moveTo(4.25f, 5.61f)
                curveTo(6.57f, 8.59f, 10.0f, 13.0f, 10.0f, 13.0f)
                verticalLineToRelative(5.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(0.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineToRelative(-5.0f)
                curveToRelative(0.0f, 0.0f, 3.43f, -4.41f, 5.75f, -7.39f)
                curveTo(20.26f, 4.95f, 19.79f, 4.0f, 18.95f, 4.0f)
                horizontalLineTo(5.04f)
                curveTo(4.21f, 4.0f, 3.74f, 4.95f, 4.25f, 5.61f)
                close()
            }
        }.build()
}
