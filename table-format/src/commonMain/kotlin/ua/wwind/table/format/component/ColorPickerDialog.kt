package ua.wwind.table.format.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.ColorPickerController
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import org.jetbrains.compose.resources.painterResource
import ua.wwind.table.format.scrollbar.VerticalScrollbarRenderer
import ua.wwind.table.format.scrollbar.VerticalScrollbarState
import ua.wwind.table.format.toColor
import ua.wwind.table.icon.TableIcons
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString
import kotlin.math.roundToInt

private const val SWATCHES_PER_ROW = 5
private const val LIGHT_SWATCH_LUMINANCE = 0.4f
private const val RATIO_DECIMALS = 10.0

/**
 * Picks a format color. [onChooseColor] receives [Color.Unspecified] when the color was reset.
 *
 * [pairedColor] is the color this one is read against: the background when picking the text color, the text
 * color when picking the background ([editsBackground]). When set, the preview shows sample text in both
 * colors and a warning appears while their contrast is under [MIN_TEXT_CONTRAST].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "LongParameterList")
@Composable
internal fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onChooseColor: (Color) -> Unit,
    strings: StringProvider,
    pairedColor: Color? = null,
    editsBackground: Boolean = false,
    scrollbarRenderer: VerticalScrollbarRenderer? = null,
) {
    var color by remember { mutableStateOf(initialColor) }
    var hexText by remember { mutableStateOf(initialColor.toHex()) }
    val controller = rememberColorPickerController()
    LaunchedEffect(Unit) {
        controller.selectByColor(color, false)
        controller.setBrightness(1f, false)
        controller.setAlpha(1f, false)
    }

    // Swatches and the hex field move the wheel without reporting back (fromUser = false); only drags on
    // the wheel and sliders update the color from the controller.
    fun select(newColor: Color) {
        color = newColor
        hexText = newColor.toHex()
        controller.selectByColor(newColor, false)
    }
    val scrollState = rememberScrollState()
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(16.dp).widthIn(max = 420.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp) {
            Box {
                Column(
                    modifier = Modifier.verticalScroll(scrollState).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = strings.get(UiString.FormatChooseColor),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Button(
                        onClick = {
                            select(Color.Unspecified)
                            controller.setBrightness(1f, false)
                            controller.setAlpha(1f, false)
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = spacedBy(8.dp),
                        ) {
                            Icon(
                                painter = painterResource(TableIcons.FormatColorReset),
                                contentDescription = null,
                            )
                            Text(text = strings.get(UiString.FormatResetColor))
                        }
                    }
                    ColorSwatches(selected = color, onSelect = ::select, strings = strings)
                    HorizontalDivider()
                    ColorWheel(
                        controller = controller,
                        onColorChange = { newColor ->
                            color = newColor
                            hexText = newColor.toHex()
                        },
                    )
                    val hexError = hexText.isNotBlank() && parseHexColor(hexText) == null
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { text ->
                            hexText = text
                            parseHexColor(text)?.let {
                                color = it
                                controller.selectByColor(it, false)
                            }
                        },
                        label = { Text(strings.get(UiString.FormatColorHex)) },
                        placeholder = { Text("#RRGGBB") },
                        isError = hexError,
                        supportingText =
                            if (hexError) {
                                { Text(strings.get(UiString.FormatColorHexInvalid)) }
                            } else {
                                null
                            },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ColorPreview(
                        color = color,
                        pairedColor = pairedColor,
                        editsBackground = editsBackground,
                        strings = strings,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(strings.get(UiString.FormatColorCancel))
                        }
                        TextButton(onClick = { onChooseColor(color) }) {
                            Text(strings.get(UiString.FormatColorConfirm))
                        }
                    }
                }
                scrollbarRenderer?.Render(
                    modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight(),
                    state = VerticalScrollbarState.Scroll(scrollState),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSwatches(
    selected: Color,
    onSelect: (Color) -> Unit,
    strings: StringProvider,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = SWATCHES_PER_ROW,
    ) {
        ColorPalette.entries.forEach { item ->
            val swatchColor = item.value.toColor()
            val isSelected = selected == swatchColor
            val name = strings.get(item.uiString)
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(swatchColor)
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                    shape = MaterialTheme.shapes.large,
                                )
                            } else {
                                Modifier
                            },
                        ).selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onSelect(swatchColor) },
                        ).semantics { contentDescription = name },
            ) {
                if (isSelected) {
                    Icon(
                        painter = painterResource(TableIcons.Check),
                        contentDescription = null,
                        tint = if (swatchColor.luminance() > LIGHT_SWATCH_LUMINANCE) Color.Black else Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorWheel(
    controller: ColorPickerController,
    onColorChange: (Color) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HsvColorPicker(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            controller = controller,
            onColorChanged = { colorEnvelope: ColorEnvelope ->
                if (colorEnvelope.fromUser) onColorChange(colorEnvelope.color)
            },
        )
        AlphaSlider(
            modifier = Modifier.fillMaxWidth().height(32.dp),
            controller = controller,
        )
        BrightnessSlider(
            modifier = Modifier.fillMaxWidth().height(32.dp),
            controller = controller,
        )
    }
}

@Composable
private fun ColorPreview(
    color: Color,
    pairedColor: Color?,
    editsBackground: Boolean,
    strings: StringProvider,
) {
    val paired = pairedColor?.takeIf { it.isSpecified }
    val background = if (editsBackground || paired == null) color else paired
    val content = if (editsBackground) paired else color.takeIf { paired != null }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                    .background(background),
        ) {
            if (content != null && content.isSpecified) {
                Text(text = "Aa Bb 123", color = content, style = MaterialTheme.typography.titleMedium)
            }
        }
        if (paired != null && color.isSpecified) {
            val ratio = if (editsBackground) contrastRatio(paired, color) else contrastRatio(color, paired)
            if (ratio < MIN_TEXT_CONTRAST) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(TableIcons.ErrorOutline),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = strings.get(UiString.FormatColorLowContrast),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${strings.formatNumber((ratio * RATIO_DECIMALS).roundToInt() / RATIO_DECIMALS)}:1",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private enum class ColorPalette(
    val value: Int,
    val uiString: UiString,
) {
    DEEP_BLUE(0xFF1E3A8A.toInt(), UiString.FormatColorDeepBlue),
    SKY_BLUE(0xFF3B82F6.toInt(), UiString.FormatColorSkyBlue),
    EMERALD_GREEN(0xFF10B981.toInt(), UiString.FormatColorEmeraldGreen),
    LIME_GREEN(0xFF84CC16.toInt(), UiString.FormatColorLimeGreen),
    SUNSET_ORANGE(0xFFF97316.toInt(), UiString.FormatColorSunsetOrange),
    CHERRY_RED(0xFFDC2626.toInt(), UiString.FormatColorCherryRed),
    PURPLE_HAZE(0xFF9333EA.toInt(), UiString.FormatColorPurpleHaze),
    ROSE_PINK(0xFFEC4899.toInt(), UiString.FormatColorRosePink),
    STEEL_GRAY(0xFF64748B.toInt(), UiString.FormatColorSteelGray),
    GOLDEN_AMBER(0xFFFACC15.toInt(), UiString.FormatColorGoldenAmber),
}
