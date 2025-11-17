@file:Suppress("MatchingDeclarationName")

package ua.wwind.table.format.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableList

/**
 * Represents a tab's content and its type for a UI component.
 *
 * @param E An enum type identifying the tab category.
 * @param T The content associated with the tab.
 */
@Immutable
public data class TabData<E, T>(
    val type: E,
    val data: T,
)

@Composable
@Suppress("LongParameterList")
public fun <E, T> FormatDialogTabRow(
    currentItem: E,
    onClick: (E) -> Unit,
    list: ImmutableList<TabData<E, T>>,
    modifier: Modifier = Modifier,
    isScrollable: Boolean = false,
    createTab: @Composable (TabData<E, T>, Boolean, () -> Unit) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Surface {
            val selectedIndex = list.indexOfFirst { it.type == currentItem }

            val tabContent: @Composable () -> Unit = {
                list.forEachIndexed { index, item ->
                    createTab(item, index == selectedIndex) {
                        onClick(item.type)
                    }
                }
            }

            if (isScrollable) {
                ScrollableTabRow(
                    selectedTabIndex = selectedIndex,
                    tabs = tabContent,
                    containerColor = Color.Transparent,
                )
            } else {
                TabRow(
                    selectedTabIndex = selectedIndex,
                    tabs = tabContent,
                    containerColor = Color.Transparent,
                )
            }
        }

        Box(modifier = modifier) {
            content()
        }
    }
}
