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

/** Material `Icons.Filled.FilterAltOutlined`. */
internal val FilterAltOutlinedIcon: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "FilterAltOutlined",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black), strokeLineWidth = 1.0f) {
                moveTo(7.0f, 6.0f)
                horizontalLineToRelative(10.0f)
                lineToRelative(-5.01f, 6.3f)
                lineTo(7.0f, 6.0f)
                close()
                moveTo(4.25f, 5.61f)
                curveTo(6.27f, 8.2f, 10.0f, 13.0f, 10.0f, 13.0f)
                verticalLineToRelative(6.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                horizontalLineToRelative(2.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                verticalLineToRelative(-6.0f)
                curveToRelative(0.0f, 0.0f, 3.72f, -4.8f, 5.74f, -7.39f)
                curveTo(20.25f, 4.95f, 19.78f, 4.0f, 18.95f, 4.0f)
                horizontalLineTo(5.04f)
                curveTo(4.21f, 4.0f, 3.74f, 4.95f, 4.25f, 5.61f)
                close()
            }
        }.build()
}
