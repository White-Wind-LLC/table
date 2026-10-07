package ua.wwind.table.sample.app.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.wwind.table.sample.icon.SampleIcons

@Composable
fun AppToolbar(
    onSettingsClick: () -> Unit,
    pagingDemo: Boolean,
    onPagingDemoChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Table Sample",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = pagingDemo,
                onClick = { onPagingDemoChange(!pagingDemo) },
                label = { Text("Paging demo") },
            )
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = SampleIcons.Settings,
                    contentDescription = "Open settings",
                )
            }
        }
    }
}
