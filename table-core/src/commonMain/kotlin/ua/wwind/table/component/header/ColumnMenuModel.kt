package ua.wwind.table.component.header

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.ui.unit.LayoutDirection
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.ColumnMenuDefaults.Ids
import ua.wwind.table.component.ColumnMenuDefaults.Sections
import ua.wwind.table.component.ColumnMenuItem
import ua.wwind.table.component.ColumnMenuItemId
import ua.wwind.table.component.ColumnMenuSection
import ua.wwind.table.config.PinnedSide
import ua.wwind.table.config.isInteractionLockByRowReorderEnabled
import ua.wwind.table.data.SortOrder
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.isActive
import ua.wwind.table.icon.TableIcons
import ua.wwind.table.isPhysicallyLeft
import ua.wwind.table.state.ColumnWidthAction
import ua.wwind.table.state.TableColumnsState
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

/** Where a column menu opens: a column header, or the sticky header of a group. */
internal enum class ColumnMenuContext { Header, GroupHeader }

/** A menu item before localisation: labels and reasons are still [UiString] keys. */
@Immutable
internal data class ColumnMenuEntry(
    val id: ColumnMenuItemId,
    val label: UiString,
    val icon: DrawableResource?,
    val enabled: Boolean = true,
    val disabledReason: UiString? = null,
    val checked: Boolean = false,
    /** Appended to the label as " (n)". */
    val count: Int? = null,
    val onClick: () -> Unit,
)

@Immutable
internal data class ColumnMenuEntrySection(
    val id: String,
    val entries: List<ColumnMenuEntry>,
)

/**
 * The default menu for [spec]. Items the column can never do are left out; items the current state
 * blocks stay in, disabled, with a reason.
 *
 * [hasWidthOverride] and [canAutoFit] default to reading the column state. A caller that wants to
 * recompose only when they flip, not on every width change of a resize drag, passes derived values.
 */
internal fun <C> columnMenuModel(
    spec: ColumnSpec<*, C, *>,
    state: TableState<C>,
    context: ColumnMenuContext,
    onOpenFilter: () -> Unit,
    layoutDirection: LayoutDirection,
    hasWidthOverride: Boolean = spec.key in state.columns.widths,
    canAutoFit: Boolean = spec.key in state.columns.contentMaxWidths,
): List<ColumnMenuEntrySection> =
    when (context) {
        ColumnMenuContext.Header -> {
            listOfNotNull(
                sortSection(spec, state),
                filterSection(spec, state, onOpenFilter),
                layoutSection(spec, state, hasWidthOverride, canAutoFit, layoutDirection),
                groupSection(spec, state),
                visibilitySection(spec, state),
            )
        }

        ColumnMenuContext.GroupHeader -> {
            listOfNotNull(sortSection(spec, state), groupSection(spec, state))
        }
    }

private fun <C> sortSection(
    spec: ColumnSpec<*, C, *>,
    state: TableState<C>,
): ColumnMenuEntrySection? {
    if (!spec.sortable) return null
    val reason = UiString.ColumnMenuReasonRowReorder.takeIf { state.settings.isInteractionLockByRowReorderEnabled }
    val current = state.sort?.takeIf { it.column == spec.key }?.order
    val entries =
        buildList {
            add(
                ColumnMenuEntry(
                    id = Ids.SortAscending,
                    label = UiString.ColumnMenuSortAscending,
                    icon = TableIcons.ArrowUpward,
                    enabled = reason == null,
                    disabledReason = reason,
                    checked = current == SortOrder.ASCENDING,
                ) { state.setSort(spec.key, SortOrder.ASCENDING) },
            )
            add(
                ColumnMenuEntry(
                    id = Ids.SortDescending,
                    label = UiString.ColumnMenuSortDescending,
                    icon = TableIcons.ArrowDownward,
                    enabled = reason == null,
                    disabledReason = reason,
                    checked = current == SortOrder.DESCENDING,
                ) { state.setSort(spec.key, SortOrder.DESCENDING) },
            )
            if (current != null) {
                add(
                    ColumnMenuEntry(
                        id = Ids.ClearSort,
                        label = UiString.ColumnMenuClearSort,
                        icon = TableIcons.Close,
                        enabled = reason == null,
                        disabledReason = reason,
                    ) { state.clearSort() },
                )
            }
        }
    return ColumnMenuEntrySection(Sections.Sort, entries)
}

private fun <C> filterSection(
    spec: ColumnSpec<*, C, *>,
    state: TableState<C>,
    onOpenFilter: () -> Unit,
): ColumnMenuEntrySection? {
    val filter = spec.filter
    if (filter == null || filter is TableFilterType.DisabledTableFilter) return null
    val entries =
        buildList {
            add(
                ColumnMenuEntry(
                    Ids.OpenFilter,
                    UiString.ColumnMenuOpenFilter,
                    TableIcons.FilterAltOutlined,
                    onClick = onOpenFilter,
                ),
            )
            if (state.filters[spec.key]?.isActive() == true) {
                add(
                    ColumnMenuEntry(Ids.ClearFilter, UiString.ColumnMenuClearFilter, TableIcons.Close) {
                        state.setFilter<Any?>(spec.key, null)
                    },
                )
            }
        }
    return ColumnMenuEntrySection(Sections.Filter, entries)
}

private fun <C> layoutSection(
    spec: ColumnSpec<*, C, *>,
    state: TableState<C>,
    hasWidthOverride: Boolean,
    canFit: Boolean,
    layoutDirection: LayoutDirection,
): ColumnMenuEntrySection {
    val columns = state.columns
    val key = spec.key
    val entries =
        buildList {
            add(pinEntry(columns, key, state.settings.pinnedColumnsSide, layoutDirection))
            add(moveEntry(columns, key, -1, layoutDirection))
            add(moveEntry(columns, key, 1, layoutDirection))
            if (spec.resizable) {
                add(
                    ColumnMenuEntry(
                        id = Ids.AutoFit,
                        label = UiString.ColumnMenuAutoFit,
                        icon = TableIcons.SettingsEthernet,
                        enabled = canFit,
                        disabledReason = UiString.ColumnMenuReasonNothingToFit.takeUnless { canFit },
                    ) { columns.fitToContent(key) },
                )
                val canReset = hasWidthOverride
                add(
                    ColumnMenuEntry(
                        id = Ids.ResetWidth,
                        label = UiString.ColumnMenuResetWidth,
                        icon = TableIcons.SettingsBackupRestore,
                        enabled = canReset,
                        disabledReason = UiString.ColumnMenuReasonDefaultWidth.takeUnless { canReset },
                    ) { columns.resize(key, ColumnWidthAction.Reset) },
                )
            }
        }
    return ColumnMenuEntrySection(Sections.Layout, entries)
}

private fun <C> pinEntry(
    columns: TableColumnsState<C>,
    key: C,
    side: PinnedSide,
    layoutDirection: LayoutDirection,
): ColumnMenuEntry {
    if (columns.isPinned(key)) {
        return ColumnMenuEntry(Ids.Unpin, UiString.ColumnMenuUnpin, TableIcons.PushPinOutlined) { columns.unpin(key) }
    }
    val canPin = columns.canPin(key)
    val pinsLeft = layoutDirection.isPhysicallyLeft(towardStart = side == PinnedSide.Start)
    return ColumnMenuEntry(
        id = Ids.Pin,
        label = if (pinsLeft) UiString.ColumnMenuPinLeft else UiString.ColumnMenuPinRight,
        icon = TableIcons.PushPin,
        enabled = canPin,
        disabledReason = UiString.ColumnMenuReasonLastUnpinned.takeUnless { canPin },
    ) { columns.pin(key) }
}

private fun <C> moveEntry(
    columns: TableColumnsState<C>,
    key: C,
    delta: Int,
    layoutDirection: LayoutDirection,
): ColumnMenuEntry {
    val enabled = columns.canMoveBy(key, delta)
    val towardStart = delta < 0
    return ColumnMenuEntry(
        id = if (towardStart) Ids.MoveToStart else Ids.MoveToEnd,
        // The label names the physical side; the auto-mirrored arrow already points there.
        label =
            if (layoutDirection.isPhysicallyLeft(towardStart)) {
                UiString.ColumnMenuMoveLeft
            } else {
                UiString.ColumnMenuMoveRight
            },
        icon = if (towardStart) TableIcons.KeyboardArrowLeft else TableIcons.KeyboardArrowRight,
        enabled = enabled,
        disabledReason = if (enabled) null else columns.moveBlockedReason(key, delta),
    ) { columns.moveBy(key, delta) }
}

private fun <C> TableColumnsState<C>.moveBlockedReason(
    key: C,
    delta: Int,
): UiString {
    val keys = visibleKeys()
    val target = keys.indexOf(key) + delta
    return when {
        target < 0 -> UiString.ColumnMenuReasonFirst
        target > keys.lastIndex -> UiString.ColumnMenuReasonLast
        else -> UiString.ColumnMenuReasonPinnedEdge
    }
}

private fun <C> groupSection(
    spec: ColumnSpec<*, C, *>,
    state: TableState<C>,
): ColumnMenuEntrySection {
    if (state.groupBy == spec.key) {
        return ColumnMenuEntrySection(
            Sections.Group,
            listOf(ColumnMenuEntry(Ids.Ungroup, UiString.Ungroup, TableIcons.TableRows) { state.groupBy(null) }),
        )
    }
    // Activating groupBy would silently suppress visible row blocks; a disabled item with a reason
    // surfaces that instead of letting the blocks vanish on a menu click.
    val reason =
        when {
            state.settings.isInteractionLockByRowReorderEnabled -> UiString.ColumnMenuReasonRowReorder
            state.rowBlocksNonEmpty -> UiString.ColumnMenuReasonRowBlocks
            else -> null
        }
    val entry =
        ColumnMenuEntry(
            id = Ids.GroupBy,
            label = UiString.GroupBy,
            icon = TableIcons.TableRows,
            enabled = reason == null,
            disabledReason = reason,
        ) {
            state.groupBy(spec.key)
            if (spec.sortable && state.sort?.column != spec.key) state.setSort(spec.key)
        }
    return ColumnMenuEntrySection(Sections.Group, listOf(entry))
}

private fun <C> visibilitySection(
    spec: ColumnSpec<*, C, *>,
    state: TableState<C>,
): ColumnMenuEntrySection {
    val columns = state.columns
    val canHide = columns.canHide(spec.key)
    val entries =
        buildList {
            add(
                ColumnMenuEntry(
                    id = Ids.Hide,
                    label = UiString.ColumnMenuHide,
                    icon = TableIcons.VisibilityOff,
                    enabled = canHide,
                    disabledReason = UiString.ColumnMenuReasonLastVisible.takeUnless { canHide },
                ) { columns.hide(spec.key) },
            )
            if (columns.hidden.isNotEmpty()) {
                add(
                    ColumnMenuEntry(
                        id = Ids.ShowHidden,
                        label = UiString.ColumnMenuShowHidden,
                        icon = TableIcons.Visibility,
                        count = columns.hidden.size,
                    ) { columns.showAll() },
                )
            }
        }
    return ColumnMenuEntrySection(Sections.Visibility, entries)
}

/**
 * Localises the model into the public menu types. Each entry is keyed by its ids, so an item that
 * appears or disappears does not hand its neighbours' icon painters to the wrong entry (on web a
 * rebuilt resource painter is blank until it loads again).
 */
@Composable
internal fun List<ColumnMenuEntrySection>.resolve(strings: StringProvider): List<ColumnMenuSection> =
    map { section ->
        ColumnMenuSection(
            id = section.id,
            items =
                section.entries.map { entry ->
                    key(section.id, entry.id) {
                        val label = strings.get(entry.label)
                        ColumnMenuItem(
                            id = entry.id,
                            label = if (entry.count != null) "$label (${entry.count})" else label,
                            icon = entry.icon?.let { painterResource(it) },
                            enabled = entry.enabled,
                            disabledReason = entry.disabledReason?.let { strings.get(it) },
                            checked = entry.checked,
                            onClick = entry.onClick,
                        )
                    }
                },
        )
    }
