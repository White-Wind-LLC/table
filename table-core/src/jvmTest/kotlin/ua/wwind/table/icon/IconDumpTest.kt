package ua.wwind.table.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.unit.dp
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** One icon to vendor: the property name we expose and the source [ImageVector]. */
private data class IconSpec(
    val propertyName: String,
    val source: ImageVector,
)

private val TABLE_ICONS: List<IconSpec> =
    listOf(
        IconSpec("Close", Icons.Rounded.Close),
        IconSpec("KeyboardArrowLeft", Icons.AutoMirrored.Rounded.KeyboardArrowLeft),
        IconSpec("KeyboardArrowRight", Icons.AutoMirrored.Rounded.KeyboardArrowRight),
        IconSpec("ArrowUpward", Icons.Rounded.ArrowUpward),
        IconSpec("ArrowDownward", Icons.Rounded.ArrowDownward),
        IconSpec("Sort", Icons.AutoMirrored.Outlined.Sort),
        IconSpec("FilterAltFilled", Icons.Rounded.FilterAlt),
        IconSpec("FilterAltOutlined", Icons.Outlined.FilterAlt),
        IconSpec("DragIndicator", Icons.Filled.DragIndicator),
        IconSpec("SwapHoriz", Icons.Filled.SwapHoriz),
        IconSpec("Add", Icons.Rounded.Add),
        IconSpec("Delete", Icons.Rounded.Delete),
        IconSpec("ContentCopy", Icons.Rounded.ContentCopy),
        IconSpec("Save", Icons.Rounded.Save),
        IconSpec("ArrowDropUp", Icons.Rounded.ArrowDropUp),
        IconSpec("Check", Icons.Rounded.Check),
        IconSpec("FormatColorReset", Icons.Filled.FormatColorReset),
    )

private val SAMPLE_ICONS: List<IconSpec> =
    listOf(
        IconSpec("Settings", Icons.Filled.Settings),
        IconSpec("Edit", Icons.Filled.Edit),
        IconSpec("Link", Icons.Filled.Link),
        IconSpec("LinkOff", Icons.Filled.LinkOff),
        IconSpec("ExpandLess", Icons.Filled.ExpandLess),
        IconSpec("ExpandMore", Icons.Filled.ExpandMore),
        IconSpec("Reorder", Icons.Filled.Reorder),
        IconSpec("Star", Icons.Filled.Star),
        IconSpec("BarChart", Icons.Filled.BarChart),
        IconSpec("Close", Icons.Filled.Close),
        IconSpec("Delete", Icons.Filled.Delete),
    )

/** Same-name icons used in two styles. If a pair matches, one vendored entry serves both. */
private val STYLE_VARIANTS: List<Triple<String, ImageVector, ImageVector>> =
    listOf(
        Triple("DragIndicator: Filled vs Rounded", Icons.Filled.DragIndicator, Icons.Rounded.DragIndicator),
        Triple("Close: Rounded vs Filled", Icons.Rounded.Close, Icons.Filled.Close),
        Triple("Delete: Rounded vs Filled", Icons.Rounded.Delete, Icons.Filled.Delete),
    )

private const val ATTRIBUTION = """/*
 * Icon path data derived from Material Icons (https://github.com/google/material-design-icons),
 * Copyright (C) Google LLC, licensed under the Apache License, Version 2.0.
 *
 * Generated from org.jetbrains.compose.material:material-icons-extended:1.7.3, which is deprecated
 * and no longer tracks Compose releases. Do not edit by hand.
 */"""

class IconDumpTest {
    @Test
    fun dumpIcons() {
        val outputDir = File("build/generated-icons").apply { mkdirs() }

        (TABLE_ICONS + SAMPLE_ICONS).forEach { it.source.requirePlainMaterialShape(it.propertyName) }

        File(outputDir, "TableIcons.kt").writeText(
            renderFile(
                packageName = "ua.wwind.table.icon",
                objectDeclaration = "public object TableIcons",
                kdoc = "/** Icons drawn by the table. Vendored so the library ships no icon dependency. */",
                icons = TABLE_ICONS,
            ),
        )
        File(outputDir, "SampleIcons.kt").writeText(
            renderFile(
                packageName = "ua.wwind.table.sample.icon",
                objectDeclaration = "internal object SampleIcons",
                kdoc = "/** Icons used only by the sample app. Deliberately not part of the library's public API. */",
                icons = SAMPLE_ICONS,
            ),
        )

        println("STYLE VARIANT REPORT")
        STYLE_VARIANTS.forEach { (label, a, b) ->
            val identical = a.pathNodes() == b.pathNodes() && a.autoMirror == b.autoMirror
            println("  $label -> ${if (identical) "IDENTICAL (collapse to one entry)" else "DIFFERENT (keep both)"}")
        }

        assertTrue(File(outputDir, "TableIcons.kt").length() > 0)
        assertTrue(File(outputDir, "SampleIcons.kt").length() > 0)
        println("Wrote ${outputDir.absolutePath}")
    }
}

/** Flattens the vector tree to its path commands. */
private fun ImageVector.pathNodes(): List<PathNode> = root.filterIsInstance<VectorPath>().flatMap { it.pathData }

/**
 * Fails loudly on anything the emitter cannot represent. A surprise must surface here, not as a
 * silently degraded icon after release.
 */
private fun ImageVector.requirePlainMaterialShape(label: String) {
    check(defaultWidth == 24.dp) { "$label: defaultWidth is $defaultWidth, expected 24.dp" }
    check(defaultHeight == 24.dp) { "$label: defaultHeight is $defaultHeight, expected 24.dp" }
    check(viewportWidth == 24f) { "$label: viewportWidth is $viewportWidth, expected 24f" }
    check(viewportHeight == 24f) { "$label: viewportHeight is $viewportHeight, expected 24f" }
    root.forEach { node ->
        check(node is VectorPath) { "$label: unsupported node ${node::class.simpleName}, expected VectorPath" }
        val fill = node.fill
        check(fill is SolidColor && fill.value == Color.Black) { "$label: fill is $fill, expected SolidColor(Black)" }
        check(node.fillAlpha == 1f) { "$label: fillAlpha is ${node.fillAlpha}, expected 1f" }
        // strokeLineWidth is not checked here: Material's generated XML sets it on some icons
        // (e.g. Rounded/Close) even though stroke is null, so it never paints. renderIcon()
        // reproduces it faithfully rather than assuming it is always 0f.
        check(node.stroke == null) { "$label: stroke is ${node.stroke}, expected null" }
        check(node.pathFillType == PathFillType.NonZero) { "$label: pathFillType is ${node.pathFillType}" }
        check(node.trimPathStart == 0f) { "$label: trimPathStart is ${node.trimPathStart}, expected 0f" }
        check(node.trimPathEnd == 1f) { "$label: trimPathEnd is ${node.trimPathEnd}, expected 1f" }
        check(node.trimPathOffset == 0f) { "$label: trimPathOffset is ${node.trimPathOffset}, expected 0f" }
    }
}

private fun renderFile(
    packageName: String,
    objectDeclaration: String,
    kdoc: String,
    icons: List<IconSpec>,
): String =
    buildString {
        appendLine(ATTRIBUTION)
        appendLine()
        appendLine("package $packageName")
        appendLine()
        appendLine("import androidx.compose.ui.graphics.Color")
        appendLine("import androidx.compose.ui.graphics.SolidColor")
        appendLine("import androidx.compose.ui.graphics.vector.ImageVector")
        appendLine("import androidx.compose.ui.graphics.vector.path")
        appendLine("import androidx.compose.ui.unit.dp")
        appendLine()
        appendLine(kdoc)
        appendLine("@Suppress(\"VariableNaming\", \"ktlint:standard:property-naming\")")
        appendLine("$objectDeclaration {")
        icons.forEachIndexed { index, spec ->
            if (index > 0) appendLine()
            append(renderIcon(spec))
        }
        appendLine("}")
    }

private fun renderIcon(spec: IconSpec): String =
    buildString {
        val vector = spec.source
        appendLine("    public val ${spec.propertyName}: ImageVector by lazy {")
        appendLine("        ImageVector.Builder(")
        appendLine("            name = \"${spec.propertyName}\",")
        appendLine("            defaultWidth = 24.dp,")
        appendLine("            defaultHeight = 24.dp,")
        appendLine("            viewportWidth = 24f,")
        appendLine("            viewportHeight = 24f,")
        if (vector.autoMirror) appendLine("            autoMirror = true,")
        appendLine("        ).apply {")
        vector.root.filterIsInstance<VectorPath>().forEach { path ->
            val pathParams =
                buildString {
                    append("fill = SolidColor(Color.Black)")
                    if (path.strokeLineWidth != 0f) append(", strokeLineWidth = ${f(path.strokeLineWidth)}")
                }
            appendLine("            path($pathParams) {")
            path.pathData.forEach { node -> appendLine("                ${node.toBuilderCall()}") }
            appendLine("            }")
        }
        appendLine("        }.build()")
        appendLine("    }")
    }

/** Float literal that round-trips: Kotlin's Float.toString() is the shortest exact representation. */
private fun f(value: Float): String = "${value}f"

/**
 * Exhaustive over the sealed [PathNode] hierarchy — no `else` branch, so a future Compose release
 * that adds a node type breaks the build here rather than emitting a lossy icon. That
 * exhaustiveness is also why this trips detekt's cyclomatic-complexity and method-length limits;
 * the branch count is the point, not an accident, so both are suppressed rather than restructured.
 */
@Suppress("CyclomaticComplexMethod", "LongMethod")
private fun PathNode.toBuilderCall(): String =
    when (this) {
        is PathNode.Close -> {
            "close()"
        }

        is PathNode.RelativeMoveTo -> {
            "moveToRelative(${f(dx)}, ${f(dy)})"
        }

        is PathNode.MoveTo -> {
            "moveTo(${f(x)}, ${f(y)})"
        }

        is PathNode.RelativeLineTo -> {
            "lineToRelative(${f(dx)}, ${f(dy)})"
        }

        is PathNode.LineTo -> {
            "lineTo(${f(x)}, ${f(y)})"
        }

        is PathNode.RelativeHorizontalTo -> {
            "horizontalLineToRelative(${f(dx)})"
        }

        is PathNode.HorizontalTo -> {
            "horizontalLineTo(${f(x)})"
        }

        is PathNode.RelativeVerticalTo -> {
            "verticalLineToRelative(${f(dy)})"
        }

        is PathNode.VerticalTo -> {
            "verticalLineTo(${f(y)})"
        }

        is PathNode.RelativeCurveTo -> {
            "curveToRelative(${f(dx1)}, ${f(dy1)}, ${f(dx2)}, ${f(dy2)}, ${f(dx3)}, ${f(dy3)})"
        }

        is PathNode.CurveTo -> {
            "curveTo(${f(x1)}, ${f(y1)}, ${f(x2)}, ${f(y2)}, ${f(x3)}, ${f(y3)})"
        }

        is PathNode.RelativeReflectiveCurveTo -> {
            "reflectiveCurveToRelative(${f(dx1)}, ${f(dy1)}, ${f(dx2)}, ${f(dy2)})"
        }

        is PathNode.ReflectiveCurveTo -> {
            "reflectiveCurveTo(${f(x1)}, ${f(y1)}, ${f(x2)}, ${f(y2)})"
        }

        is PathNode.RelativeQuadTo -> {
            "quadToRelative(${f(dx1)}, ${f(dy1)}, ${f(dx2)}, ${f(dy2)})"
        }

        is PathNode.QuadTo -> {
            "quadTo(${f(x1)}, ${f(y1)}, ${f(x2)}, ${f(y2)})"
        }

        is PathNode.RelativeReflectiveQuadTo -> {
            "reflectiveQuadToRelative(${f(dx)}, ${f(dy)})"
        }

        is PathNode.ReflectiveQuadTo -> {
            "reflectiveQuadTo(${f(x)}, ${f(y)})"
        }

        is PathNode.RelativeArcTo -> {
            "arcToRelative(${f(horizontalEllipseRadius)}, ${f(verticalEllipseRadius)}, ${f(theta)}, " +
                "$isMoreThanHalf, $isPositiveArc, ${f(arcStartDx)}, ${f(arcStartDy)})"
        }

        is PathNode.ArcTo -> {
            "arcTo(${f(horizontalEllipseRadius)}, ${f(verticalEllipseRadius)}, ${f(theta)}, " +
                "$isMoreThanHalf, $isPositiveArc, ${f(arcStartX)}, ${f(arcStartY)})"
        }
    }
