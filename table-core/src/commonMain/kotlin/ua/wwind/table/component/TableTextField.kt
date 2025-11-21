package ua.wwind.table.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom text field with configurable content padding.
 * Uses BasicTextField with OutlinedTextFieldDefaults.DecorationBox under the hood.
 *
 * @param value the input text to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates the text
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
 * @param supportingText the optional supporting text to be displayed below the text field
 * @param isError indicates if the text field's current value is in error state
 * @param visualTransformation transforms the visual representation of the input value
 * @param keyboardOptions software keyboard options that contains configuration
 * @param keyboardActions when the input service emits an IME action, the corresponding callback is called
 * @param singleLine when true, this text field becomes a single horizontally scrolling text field
 * @param maxLines the maximum height in terms of maximum number of visible lines
 * @param minLines the minimum height in terms of minimum number of visible lines
 * @param interactionSource the [MutableInteractionSource] representing the stream of interactions for this text field
 * @param shape defines the shape of this text field's border
 * @param colors [TextFieldColors] that will be used to resolve the colors used for this text field in different states
 * @param contentPadding the padding applied to the inner text field. Use [TableTextFieldDefaults.contentPadding] for standard padding
 * or [TableTextFieldDefaults.reducedContentPadding] for compact appearance
 * @param showBorder when false, the border/outline of the text field will be hidden
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TableTextField(
    value: String,
    onValueChange: (String) -> Unit,
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
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    contentPadding: PaddingValues = TableTextFieldDefaults.contentPadding(),
    showBorder: Boolean = true,
) {
    // Merge text style with color from theme to ensure proper text visibility in dark mode
    val mergedTextStyle = textStyle.merge(
        TextStyle(color = MaterialTheme.colorScheme.onSurface)
    )

    // Configure cursor and text selection colors based on the current theme
    val cursorColor = MaterialTheme.colorScheme.primary
    val textSelectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colorScheme.primary,
        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    )

    // Apply transparent border colors when showBorder is false
    val effectiveColors = if (!showBorder) {
        colors.copy(
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
        )
    } else {
        colors
    }

    CompositionLocalProvider(LocalTextSelectionColors provides textSelectionColors) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(cursorColor),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
        ) { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = singleLine,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                isError = isError,
                label = label,
                placeholder = placeholder,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                prefix = prefix,
                suffix = suffix,
                supportingText = supportingText,
                colors = effectiveColors,
                contentPadding = contentPadding,
                container = {
                    OutlinedTextFieldDefaults.ContainerBox(
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = effectiveColors,
                        shape = shape,
                    )
                },
            )
        }
    }
}

/**
 * Contains default values used by [TableTextField].
 */
internal object TableTextFieldDefaults {
    /**
     * Standard content padding for [TableTextField].
     * Uses default Material3 padding values.
     */
    @Composable
    public fun contentPadding(
        start: Dp = 16.dp,
        top: Dp = 16.dp,
        end: Dp = 16.dp,
        bottom: Dp = 16.dp,
    ): PaddingValues =
        OutlinedTextFieldDefaults.contentPadding(
            start = start,
            top = top,
            end = end,
            bottom = bottom,
        )

    /**
     * Reduced content padding for [TableTextField] to achieve a more compact appearance.
     * Useful for dense layouts like filter rows.
     */
    @Composable
    public fun reducedContentPadding(
        start: Dp = 8.dp,
        top: Dp = 8.dp,
        end: Dp = 8.dp,
        bottom: Dp = 8.dp,
    ): PaddingValues =
        OutlinedTextFieldDefaults.contentPadding(
            start = start,
            top = top,
            end = end,
            bottom = bottom,
        )
}
