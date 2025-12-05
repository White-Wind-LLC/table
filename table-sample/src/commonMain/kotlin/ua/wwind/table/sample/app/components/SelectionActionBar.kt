package ua.wwind.table.sample.app.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquid

/**
 * Floating action bar shown at the bottom when items are selected.
 * Displays the count of selected items and provides delete/clear actions.
 * Features a Liquid Glass effect powered by the Liquid library with GPU-accelerated shaders.
 *
 * @param selectedCount Number of selected items to display
 * @param onDeleteClick Callback when delete button is clicked
 * @param onClearSelection Callback when clear selection button is clicked
 * @param liquidState State for the Liquid Glass effect (must be shared with liquefiable content)
 * @param modifier Modifier for the composable
 */
@Composable
fun SelectionActionBar(
    selectedCount: Int,
    onDeleteClick: () -> Unit,
    onClearSelection: () -> Unit,
    liquidState: LiquidState,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = selectedCount > 0,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier,
    ) {
        // Liquid Glass effect: GPU-accelerated shader distortion with semi-transparent background
        Surface(
            modifier = Modifier.liquid(liquidState),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            shape = RoundedCornerShape(24.dp),
            border =
                BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                ),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            ) {
                IconButton(onClick = onClearSelection) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear selection",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDeleteClick,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text("Delete")
                }
            }
        }
    }
}
