package ua.wwind.table.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import ua.wwind.table.strings.UiString
import ua.wwind.table.strings.currentStrings

/**
 * Table cell text field with error message displayed in a persistent tooltip.
 * When [errorMessage] is not empty, a tooltip with the error will be shown.
 * The tooltip remains visible until the user clicks the dismiss button.
 *
 * @param value the input text to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates the text
 * @param errorMessage the error message to display in the tooltip. Empty string means no error
 * @param modifier the [Modifier] to be applied to this text field
 * @param enabled controls the enabled state of this text field
 * @param readOnly controls the editable state of the text field
 * @param textStyle the style to be applied to the input text
 * @param label the optional label to be displayed inside the text field container
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and the input text is empty
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field container
 * @param prefix the optional prefix to be displayed before the input text in the text field
 * @param suffix the optional suffix to be displayed after the input text in the text field
 * @param visualTransformation transforms the visual representation of the input value
 * @param keyboardOptions software keyboard options that contains configuration
 * @param keyboardActions when the input service emits an IME action, the corresponding callback is called
 * @param singleLine when true, this text field becomes a single horizontally scrolling text field
 * @param maxLines the maximum height in terms of maximum number of visible lines
 * @param minLines the minimum height in terms of minimum number of visible lines
 * @param interactionSource the [MutableInteractionSource] representing the stream of interactions for this text field
 * @param shape defines the shape of this text field's border
 * @param colors [TextFieldColors] that will be used to resolve the colors used for this text field in different states
 * @param contentPadding the padding applied to the inner text field
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun TableCellTextFieldWithTooltipError(
    value: String,
    onValueChange: (String) -> Unit,
    errorMessage: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = RectangleShape,
    colors: TextFieldColors = TextFieldDefaults.colors(),
    contentPadding: PaddingValues = TableTextFieldDefaults.reducedContentPadding(),
) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val hasError = errorMessage.isNotEmpty()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val strings = currentStrings()

    // Show tooltip immediately when error appears
    LaunchedEffect(hasError) {
        if (hasError) {
            tooltipState.show()
        } else {
            tooltipState.dismiss()
        }
    }

    // Show tooltip when field gains focus with error
    LaunchedEffect(isFocused) {
        if (isFocused && hasError) {
            tooltipState.show()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = {
                if (hasError) {
                    RichTooltip(
                        colors =
                            TooltipDefaults.richTooltipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                        action = {
                            TextButton(onClick = { tooltipState.dismiss() }) {
                                Text(strings.get(UiString.TooltipDismiss))
                            }
                        },
                    ) {
                        Text(errorMessage)
                    }
                }
            },
            state = tooltipState,
            enableUserInput = false,
            hasAction = true,
            focusable = true,
        ) {
            TableCellTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                readOnly = readOnly,
                textStyle = textStyle,
                label = label,
                placeholder = placeholder,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                prefix = prefix,
                suffix = suffix,
                isError = hasError,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = singleLine,
                maxLines = maxLines,
                minLines = minLines,
                interactionSource = interactionSource,
                shape = shape,
                colors = colors,
                contentPadding = contentPadding,
            )
        }
    }
}
