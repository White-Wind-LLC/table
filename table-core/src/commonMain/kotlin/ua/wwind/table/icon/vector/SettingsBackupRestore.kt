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

/** Material `Icons.Rounded.SettingsBackupRestore` (src/action/settings_backup_restore/materialiconsround). */
internal val SettingsBackupRestoreIcon: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "SettingsBackupRestore",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData =
                addPathNodes(
                    "M11.77,3c-2.65,0.07-5,1.28-6.6,3.16L3.85,4.85C3.54,4.54,3,4.76,3,5.21V9.5C3," +
                        "9.78,3.22,10,3.5,10h4.29c0.45,0,0.67-0.54,0.35-0.85L6.59,7.59C7.88,6.02,9.82,5," +
                        "12c4.32,0,7.74,3.94,6.86,8.41c-0.54,2.77-2.81,4.98-5.58,5.47c-3.8,0.68-7.18-1.74," +
                        "8.05-5.16C5.11,13.3,4.71,13,4.27,13h0c-0.65,0-1.14,0.61-0.98,1.23C4.28,18.12,7.8," +
                        "21,12,21c5.06,0,9.14-4.17,9-9.26C20.86,6.86,16.65,2.88,11.77,3zM14,12c0-1.1-0.9," +
                        "2-2-2s-2,0.9-2,2s0.9,2,2,2S14,13.1,14,12z",
                ),
            fill = SolidColor(Color.Black),
        ).build()
}
