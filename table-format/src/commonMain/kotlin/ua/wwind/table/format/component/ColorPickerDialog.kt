package ua.wwind.table.format.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import ua.wwind.table.format.scrollbar.VerticalScrollbarRenderer
import ua.wwind.table.format.scrollbar.VerticalScrollbarState
import ua.wwind.table.format.toColor
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
internal fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onChooseColor: (Color) -> Unit,
    strings: StringProvider,
    scrollbarRenderer: VerticalScrollbarRenderer? = null,
) {
    var color by remember { mutableStateOf(initialColor) }
    val controller = rememberColorPickerController()
    LaunchedEffect(Unit) {
        controller.selectByColor(color, false)
        controller.setBrightness(1f, false)
        controller.setAlpha(1f, false)
    }
    val scrollState = rememberScrollState()
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                Surface(shape = MaterialTheme.shapes.large) {
                    Column(
                        modifier = Modifier.padding(16.dp).verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = strings.get(UiString.FormatChooseColor),
                                    modifier = Modifier.weight(1f).padding(start = 32.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                IconButton(
                                    onClick = {
                                        onChooseColor(color)
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(
                                onClick = {
                                    controller.selectByColor(Color.Unspecified, false)
                                    controller.setBrightness(1f, false)
                                    controller.setAlpha(1f, false)
                                    color = Color.Unspecified
                                },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FormatColorReset,
                                        contentDescription = null,
                                    )
                                    Text(text = strings.get(UiString.FormatResetColor))
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                items(items = ColorPalette.entries, key = { it.name }) { item ->
                                    val paletteColor = item.value.toColor()
                                    val isCurrentColor = color == paletteColor
                                    Spacer(
                                        modifier =
                                            Modifier
                                                .size(40.dp)
                                                .clip(MaterialTheme.shapes.large)
                                                .then(
                                                    if (isCurrentColor) {
                                                        Modifier.border(
                                                            border =
                                                                BorderStroke(
                                                                    width = 2.dp,
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                ),
                                                            shape = MaterialTheme.shapes.large,
                                                        )
                                                    } else {
                                                        Modifier
                                                    },
                                                ).background(paletteColor)
                                                .clickable {
                                                    color = paletteColor
                                                    controller.selectByColor(paletteColor, true)
                                                },
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider()
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            HsvColorPicker(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(250.dp),
                                controller = controller,
                                onColorChanged = { colorEnvelope: ColorEnvelope ->
                                    if (colorEnvelope.fromUser) {
                                        color = colorEnvelope.color
                                    }
                                },
                            )
                            AlphaSlider(
                                modifier = Modifier.fillMaxWidth().height(35.dp),
                                controller = controller,
                            )
                            BrightnessSlider(
                                modifier = Modifier.fillMaxWidth().height(35.dp),
                                controller = controller,
                            )
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .height(50.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(color),
                            )
                        }
                    }
                }
                scrollbarRenderer?.Render(
                    modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight(),
                    state = VerticalScrollbarState.Scroll(scrollState),
                )
            }
        },
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false,
            ),
    )
}

private enum class ColorPalette(
    val value: Int,
) {
    DEEP_BLUE(0xFF1E3A8A.toInt()),
    SKY_BLUE(0xFF3B82F6.toInt()),
    EMERALD_GREEN(0xFF10B981.toInt()),
    LIME_GREEN(0xFF84CC16.toInt()),
    SUNSET_ORANGE(0xFFF97316.toInt()),
    CHERRY_RED(0xFFDC2626.toInt()),
    PURPLE_HAZE(0xFF9333EA.toInt()),
    ROSE_PINK(0xFFEC4899.toInt()),
    STEEL_GRAY(0xFF64748B.toInt()),
    GOLDEN_AMBER(0xFFFACC15.toInt()),
}
