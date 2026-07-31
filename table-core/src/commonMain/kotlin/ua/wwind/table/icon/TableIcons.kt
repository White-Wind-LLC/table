/*
 * Icon path data derived from Material Icons (https://github.com/google/material-design-icons),
 * Copyright (C) Google LLC, licensed under the Apache License, Version 2.0.
 *
 * Generated from org.jetbrains.compose.material:material-icons-extended:1.7.3, which is deprecated
 * and no longer tracks Compose releases. Do not edit by hand.
 *
 * To add an icon: restore the generator and the dependency from commit 9e41031
 * (`git show 9e41031:table-core/src/jvmTest/kotlin/ua/wwind/table/icon/IconDumpTest.kt`), or
 * transcribe the 24x24 path from https://github.com/google/material-design-icons.
 */

package ua.wwind.table.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Icons drawn by the table. Vendored so the library ships no icon dependency.
 *
 * Naming convention: each property is named after the plain Material icon (e.g. [Close], [Add]).
 * A suffix is added only where two variants of the same icon are both needed in the table, as with
 * [FilterAltFilled] and [FilterAltOutlined]; the suffix describes the glyph's appearance (solid vs.
 * outline), not the Material style family — Rounded, Filled, Outlined — the glyph was drawn from. See
 * each property's doc comment for its exact Material source.
 */
@Suppress("VariableNaming", "ktlint:standard:property-naming")
public object TableIcons {
    /** Material `Icons.Rounded.Close`. */
    public val Close: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "Close",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black), strokeLineWidth = 1.0f) {
                    moveTo(18.3f, 5.71f)
                    curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                    lineTo(12.0f, 10.59f)
                    lineTo(7.11f, 5.7f)
                    curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                    curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0.0f, 1.41f)
                    lineTo(10.59f, 12.0f)
                    lineTo(5.7f, 16.89f)
                    curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0.0f, 1.41f)
                    curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                    lineTo(12.0f, 13.41f)
                    lineToRelative(4.89f, 4.89f)
                    curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                    curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0.0f, -1.41f)
                    lineTo(13.41f, 12.0f)
                    lineToRelative(4.89f, -4.89f)
                    curveToRelative(0.38f, -0.38f, 0.38f, -1.02f, 0.0f, -1.4f)
                    close()
                }
            }.build()
    }

    /** Material `Icons.AutoMirrored.Rounded.KeyboardArrowLeft`. */
    public val KeyboardArrowLeft: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "KeyboardArrowLeft",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
                autoMirror = true,
            ).apply {
                path(fill = SolidColor(Color.Black), strokeLineWidth = 1.0f) {
                    moveTo(14.71f, 15.88f)
                    lineTo(10.83f, 12.0f)
                    lineToRelative(3.88f, -3.88f)
                    curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0.0f, -1.41f)
                    curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                    lineTo(8.71f, 11.3f)
                    curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0.0f, 1.41f)
                    lineToRelative(4.59f, 4.59f)
                    curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                    curveToRelative(0.38f, -0.39f, 0.39f, -1.03f, 0.0f, -1.42f)
                    close()
                }
            }.build()
    }

    /** Material `Icons.AutoMirrored.Rounded.KeyboardArrowRight`. */
    public val KeyboardArrowRight: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "KeyboardArrowRight",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
                autoMirror = true,
            ).apply {
                path(fill = SolidColor(Color.Black), strokeLineWidth = 1.0f) {
                    moveTo(9.29f, 15.88f)
                    lineTo(13.17f, 12.0f)
                    lineTo(9.29f, 8.12f)
                    curveToRelative(-0.39f, -0.39f, -0.39f, -1.02f, 0.0f, -1.41f)
                    curveToRelative(0.39f, -0.39f, 1.02f, -0.39f, 1.41f, 0.0f)
                    lineToRelative(4.59f, 4.59f)
                    curveToRelative(0.39f, 0.39f, 0.39f, 1.02f, 0.0f, 1.41f)
                    lineTo(10.7f, 17.3f)
                    curveToRelative(-0.39f, 0.39f, -1.02f, 0.39f, -1.41f, 0.0f)
                    curveToRelative(-0.38f, -0.39f, -0.39f, -1.03f, 0.0f, -1.42f)
                    close()
                }
            }.build()
    }

    /** Material `Icons.Rounded.ArrowUpward`. */
    public val ArrowUpward: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "ArrowUpward",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black), strokeLineWidth = 1.0f) {
                    moveTo(13.0f, 19.0f)
                    verticalLineTo(7.83f)
                    lineToRelative(4.88f, 4.88f)
                    curveToRelative(0.39f, 0.39f, 1.03f, 0.39f, 1.42f, 0.0f)
                    curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0.0f, -1.41f)
                    lineToRelative(-6.59f, -6.59f)
                    curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                    lineToRelative(-6.6f, 6.58f)
                    curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0.0f, 1.41f)
                    curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                    lineTo(11.0f, 7.83f)
                    verticalLineTo(19.0f)
                    curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                    reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                    close()
                }
            }.build()
    }

    /** Material `Icons.Rounded.ArrowDownward`. */
    public val ArrowDownward: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "ArrowDownward",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black), strokeLineWidth = 1.0f) {
                    moveTo(11.0f, 5.0f)
                    verticalLineToRelative(11.17f)
                    lineToRelative(-4.88f, -4.88f)
                    curveToRelative(-0.39f, -0.39f, -1.03f, -0.39f, -1.42f, 0.0f)
                    curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0.0f, 1.41f)
                    lineToRelative(6.59f, 6.59f)
                    curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                    lineToRelative(6.59f, -6.59f)
                    curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0.0f, -1.41f)
                    curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                    lineTo(13.0f, 16.17f)
                    verticalLineTo(5.0f)
                    curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                    reflectiveCurveToRelative(-1.0f, 0.45f, -1.0f, 1.0f)
                    close()
                }
            }.build()
    }

    /** Material `Icons.AutoMirrored.Outlined.Sort`. */
    public val Sort: ImageVector by lazy {
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

    /**
     * Material `Icons.Rounded.FilterAlt`.
     *
     * Solid glyph, shown when a column filter is active.
     */
    public val FilterAltFilled: ImageVector by lazy {
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

    /**
     * Material `Icons.Outlined.FilterAlt`.
     *
     * Outline glyph, shown when a column filter is inactive.
     */
    public val FilterAltOutlined: ImageVector by lazy {
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

    /** Material `Icons.Filled.DragIndicator` (the Rounded variant is byte-identical). */
    public val DragIndicator: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "DragIndicator",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black), strokeLineWidth = 1.0f) {
                    moveTo(11.0f, 18.0f)
                    curveToRelative(0.0f, 1.1f, -0.9f, 2.0f, -2.0f, 2.0f)
                    reflectiveCurveToRelative(-2.0f, -0.9f, -2.0f, -2.0f)
                    reflectiveCurveToRelative(0.9f, -2.0f, 2.0f, -2.0f)
                    reflectiveCurveToRelative(2.0f, 0.9f, 2.0f, 2.0f)
                    close()
                    moveTo(9.0f, 10.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                    reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                    reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                    close()
                    moveTo(9.0f, 4.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                    reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                    reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                    close()
                    moveTo(15.0f, 8.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                    reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
                    reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                    close()
                    moveTo(15.0f, 10.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                    reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                    reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                    close()
                    moveTo(15.0f, 16.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                    reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                    reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                    close()
                }
            }.build()
    }

    /** Material `Icons.Filled.SwapHoriz`. */
    public val SwapHoriz: ImageVector by lazy {
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

    /** Material `Icons.Rounded.Add`. */
    public val Add: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "Add",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black), strokeLineWidth = 1.0f) {
                    moveTo(18.0f, 13.0f)
                    horizontalLineToRelative(-5.0f)
                    verticalLineToRelative(5.0f)
                    curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                    reflectiveCurveToRelative(-1.0f, -0.45f, -1.0f, -1.0f)
                    verticalLineToRelative(-5.0f)
                    horizontalLineTo(6.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                    reflectiveCurveToRelative(0.45f, -1.0f, 1.0f, -1.0f)
                    horizontalLineToRelative(5.0f)
                    verticalLineTo(6.0f)
                    curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(1.0f, 0.45f, 1.0f, 1.0f)
                    verticalLineToRelative(5.0f)
                    horizontalLineToRelative(5.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                    reflectiveCurveToRelative(-0.45f, 1.0f, -1.0f, 1.0f)
                    close()
                }
            }.build()
    }

    /** Material `Icons.Rounded.Delete`. */
    public val Delete: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "Delete",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black), strokeLineWidth = 1.0f) {
                    moveTo(6.0f, 19.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(8.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineTo(9.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    horizontalLineTo(8.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    verticalLineToRelative(10.0f)
                    close()
                    moveTo(18.0f, 4.0f)
                    horizontalLineToRelative(-2.5f)
                    lineToRelative(-0.71f, -0.71f)
                    curveToRelative(-0.18f, -0.18f, -0.44f, -0.29f, -0.7f, -0.29f)
                    horizontalLineTo(9.91f)
                    curveToRelative(-0.26f, 0.0f, -0.52f, 0.11f, -0.7f, 0.29f)
                    lineTo(8.5f, 4.0f)
                    horizontalLineTo(6.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                    reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(12.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                    close()
                }
            }.build()
    }

    /** Material `Icons.Rounded.ContentCopy`. */
    public val ContentCopy: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "ContentCopy",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black), strokeLineWidth = 1.0f) {
                    moveTo(15.0f, 20.0f)
                    horizontalLineTo(5.0f)
                    verticalLineTo(7.0f)
                    curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                    horizontalLineToRelative(0.0f)
                    curveTo(3.45f, 6.0f, 3.0f, 6.45f, 3.0f, 7.0f)
                    verticalLineToRelative(13.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(10.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    verticalLineToRelative(0.0f)
                    curveTo(16.0f, 20.45f, 15.55f, 20.0f, 15.0f, 20.0f)
                    close()
                    moveTo(20.0f, 16.0f)
                    verticalLineTo(4.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    horizontalLineTo(9.0f)
                    curveTo(7.9f, 2.0f, 7.0f, 2.9f, 7.0f, 4.0f)
                    verticalLineToRelative(12.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(9.0f)
                    curveTo(19.1f, 18.0f, 20.0f, 17.1f, 20.0f, 16.0f)
                    close()
                    moveTo(18.0f, 16.0f)
                    horizontalLineTo(9.0f)
                    verticalLineTo(4.0f)
                    horizontalLineToRelative(9.0f)
                    verticalLineTo(16.0f)
                    close()
                }
            }.build()
    }

    /** Material `Icons.Rounded.Save`. */
    public val Save: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "Save",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black), strokeLineWidth = 1.0f) {
                    moveTo(17.59f, 3.59f)
                    curveToRelative(-0.38f, -0.38f, -0.89f, -0.59f, -1.42f, -0.59f)
                    lineTo(5.0f, 3.0f)
                    curveToRelative(-1.11f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                    verticalLineToRelative(14.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(14.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    lineTo(21.0f, 7.83f)
                    curveToRelative(0.0f, -0.53f, -0.21f, -1.04f, -0.59f, -1.41f)
                    lineToRelative(-2.82f, -2.83f)
                    close()
                    moveTo(12.0f, 19.0f)
                    curveToRelative(-1.66f, 0.0f, -3.0f, -1.34f, -3.0f, -3.0f)
                    reflectiveCurveToRelative(1.34f, -3.0f, 3.0f, -3.0f)
                    reflectiveCurveToRelative(3.0f, 1.34f, 3.0f, 3.0f)
                    reflectiveCurveToRelative(-1.34f, 3.0f, -3.0f, 3.0f)
                    close()
                    moveTo(13.0f, 9.0f)
                    lineTo(7.0f, 9.0f)
                    curveToRelative(-1.1f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
                    reflectiveCurveToRelative(0.9f, -2.0f, 2.0f, -2.0f)
                    horizontalLineToRelative(6.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, 0.9f, 2.0f, 2.0f)
                    reflectiveCurveToRelative(-0.9f, 2.0f, -2.0f, 2.0f)
                    close()
                }
            }.build()
    }

    /** Material `Icons.Rounded.ArrowDropUp`. */
    public val ArrowDropUp: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "ArrowDropUp",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black), strokeLineWidth = 1.0f) {
                    moveTo(8.71f, 12.29f)
                    lineTo(11.3f, 9.7f)
                    curveToRelative(0.39f, -0.39f, 1.02f, -0.39f, 1.41f, 0.0f)
                    lineToRelative(2.59f, 2.59f)
                    curveToRelative(0.63f, 0.63f, 0.18f, 1.71f, -0.71f, 1.71f)
                    horizontalLineTo(9.41f)
                    curveToRelative(-0.89f, 0.0f, -1.33f, -1.08f, -0.7f, -1.71f)
                    close()
                }
            }.build()
    }

    /** Material `Icons.Rounded.Check`. */
    public val Check: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "Check",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black), strokeLineWidth = 1.0f) {
                    moveTo(9.0f, 16.17f)
                    lineTo(5.53f, 12.7f)
                    curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                    curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0.0f, 1.41f)
                    lineToRelative(4.18f, 4.18f)
                    curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                    lineTo(20.29f, 7.71f)
                    curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0.0f, -1.41f)
                    curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                    lineTo(9.0f, 16.17f)
                    close()
                }
            }.build()
    }

    /** Material `Icons.Filled.FormatColorReset`. */
    public val FormatColorReset: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "FormatColorReset",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black), strokeLineWidth = 1.0f) {
                    moveTo(18.0f, 14.0f)
                    curveToRelative(0.0f, -4.0f, -6.0f, -10.8f, -6.0f, -10.8f)
                    reflectiveCurveToRelative(-1.33f, 1.51f, -2.73f, 3.52f)
                    lineToRelative(8.59f, 8.59f)
                    curveToRelative(0.09f, -0.42f, 0.14f, -0.86f, 0.14f, -1.31f)
                    close()
                    moveTo(17.12f, 17.12f)
                    lineTo(12.5f, 12.5f)
                    lineTo(5.27f, 5.27f)
                    lineTo(4.0f, 6.55f)
                    lineToRelative(3.32f, 3.32f)
                    curveTo(6.55f, 11.32f, 6.0f, 12.79f, 6.0f, 14.0f)
                    curveToRelative(0.0f, 3.31f, 2.69f, 6.0f, 6.0f, 6.0f)
                    curveToRelative(1.52f, 0.0f, 2.9f, -0.57f, 3.96f, -1.5f)
                    lineToRelative(2.63f, 2.63f)
                    lineToRelative(1.27f, -1.27f)
                    lineToRelative(-2.74f, -2.74f)
                    close()
                }
            }.build()
    }
}
