package ua.wwind.table.sample.app.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.wwind.table.config.PinnedSide

/**
 * Settings sidebar that displays all table configuration options. Designed to be used as drawer
 * content in a ModalNavigationDrawer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSidebar(
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    useStripedRows: Boolean,
    onStripedRowsChange: (Boolean) -> Unit,
    showFastFilters: Boolean,
    onShowFastFiltersChange: (Boolean) -> Unit,
    enableDragToScroll: Boolean,
    onEnableDragToScrollChange: (Boolean) -> Unit,
    pinnedColumnsCount: Int,
    onPinnedColumnsCountChange: (Int) -> Unit,
    pinnedColumnsSide: PinnedSide,
    onPinnedColumnsSideChange: (PinnedSide) -> Unit,
    enableEditing: Boolean,
    onEnableEditingChange: (Boolean) -> Unit,
    enableSelectionMode: Boolean,
    onEnableSelectionModeChange: (Boolean) -> Unit,
    useCompactMode: Boolean,
    onCompactModeChange: (Boolean) -> Unit,
    showFooter: Boolean,
    onShowFooterChange: (Boolean) -> Unit,
    footerPinned: Boolean,
    onFooterPinnedChange: (Boolean) -> Unit,
    onConditionalFormattingClick: () -> Unit,
    onRecalculateAutoWidthsClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.width(360.dp).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close settings")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Scrollable content
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // Appearance Section
                SettingsSection(title = "Appearance") {
                    SettingSwitch(
                        label = "Dark theme",
                        checked = isDarkTheme,
                        onCheckedChange = onDarkThemeChange,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingSwitch(
                        label = "Striped rows",
                        checked = useStripedRows,
                        onCheckedChange = onStripedRowsChange,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingSwitch(
                        label = "Compact mode",
                        checked = useCompactMode,
                        onCheckedChange = onCompactModeChange,
                    )
                }

                // Table Behavior Section
                SettingsSection(title = "Table Behavior") {
                    SettingSwitch(
                        label = "Fast filters",
                        checked = showFastFilters,
                        onCheckedChange = onShowFastFiltersChange,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingSwitch(
                        label = "Drag to scroll",
                        checked = enableDragToScroll,
                        onCheckedChange = onEnableDragToScrollChange,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingSwitch(
                        label = "Cell editing",
                        checked = enableEditing,
                        onCheckedChange = onEnableEditingChange,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingSwitch(
                        label = "Selection mode",
                        checked = enableSelectionMode,
                        onCheckedChange = onEnableSelectionModeChange,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingSwitch(
                        label = "Show footer",
                        checked = showFooter,
                        onCheckedChange = onShowFooterChange,
                    )
                    AnimatedVisibility(
                        visible = showFooter,
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SettingSwitch(
                            label = "Pin footer",
                            checked = footerPinned,
                            onCheckedChange = onFooterPinnedChange,
                            modifier = Modifier.then(if (!showFooter) Modifier.alpha(0.5f) else Modifier),
                        )
                    }
                }

                // Columns Section
                SettingsSection(title = "Columns") {
                    // Pinned columns count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Pinned columns")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (pinnedColumnsCount > 0) {
                                        onPinnedColumnsCountChange(pinnedColumnsCount - 1)
                                    }
                                },
                            ) { Text("-") }
                            Text("$pinnedColumnsCount", modifier = Modifier.width(24.dp))
                            OutlinedButton(
                                onClick = { onPinnedColumnsCountChange(pinnedColumnsCount + 1) },
                            ) { Text("+") }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Pinned columns side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Pinned side")
                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = pinnedColumnsSide == PinnedSide.Left,
                                onClick = { onPinnedColumnsSideChange(PinnedSide.Left) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            ) { Text("Left") }
                            SegmentedButton(
                                selected = pinnedColumnsSide == PinnedSide.Right,
                                onClick = { onPinnedColumnsSideChange(PinnedSide.Right) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            ) { Text("Right") }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fit columns button
                    Button(
                        onClick = onRecalculateAutoWidthsClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Fit columns") }
                }

                // Advanced Section
                SettingsSection(title = "Advanced") {
                    Button(
                        onClick = onConditionalFormattingClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Conditional formatting") }
                }
            }
        }
    }
}

/** A switch control for settings with label. */
@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
