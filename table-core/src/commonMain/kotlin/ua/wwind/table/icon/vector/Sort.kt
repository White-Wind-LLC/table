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

/** Material `Icons.AutoMirrored.Outlined.Sort`. */
internal val SortIcon: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "Sort",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = true,
        ).apply {
            path(fill = SolidColor(Color.Black), strokeLineWidth = 1.0f) {
                moveTo(3.0f, 18.0f)
                horizontalLineToRelative(6.0f)
                verticalLineToRelative(-2.0f)
                lineTo(3.0f, 16.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(3.0f, 6.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(18.0f)
                lineTo(21.0f, 6.0f)
                lineTo(3.0f, 6.0f)
                close()
                moveTo(3.0f, 13.0f)
                horizontalLineToRelative(12.0f)
                verticalLineToRelative(-2.0f)
                lineTo(3.0f, 11.0f)
                verticalLineToRelative(2.0f)
                close()
            }
        }.build()
}
