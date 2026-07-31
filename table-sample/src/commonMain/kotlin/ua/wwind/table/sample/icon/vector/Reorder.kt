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

/** Material `Icons.Filled.Reorder`. */
internal val ReorderIcon: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "Reorder",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black), strokeLineWidth = 1.0f) {
                moveTo(3.0f, 15.0f)
                horizontalLineToRelative(18.0f)
                verticalLineToRelative(-2.0f)
                lineTo(3.0f, 13.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(3.0f, 19.0f)
                horizontalLineToRelative(18.0f)
                verticalLineToRelative(-2.0f)
                lineTo(3.0f, 17.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(3.0f, 11.0f)
                horizontalLineToRelative(18.0f)
                lineTo(21.0f, 9.0f)
                lineTo(3.0f, 9.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(3.0f, 5.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(18.0f)
                lineTo(21.0f, 5.0f)
                lineTo(3.0f, 5.0f)
                close()
            }
        }.build()
}
