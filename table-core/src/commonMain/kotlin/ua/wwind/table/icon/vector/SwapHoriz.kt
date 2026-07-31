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

/** Material `Icons.Filled.SwapHoriz`. */
internal val SwapHorizIcon: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "SwapHoriz",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black), strokeLineWidth = 1.0f) {
                moveTo(6.99f, 11.0f)
                lineTo(3.0f, 15.0f)
                lineToRelative(3.99f, 4.0f)
                verticalLineToRelative(-3.0f)
                horizontalLineTo(14.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineTo(6.99f)
                verticalLineToRelative(-3.0f)
                close()
                moveTo(21.0f, 9.0f)
                lineToRelative(-3.99f, -4.0f)
                verticalLineToRelative(3.0f)
                horizontalLineTo(10.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(7.01f)
                verticalLineToRelative(3.0f)
                lineTo(21.0f, 9.0f)
                close()
            }
        }.build()
}
