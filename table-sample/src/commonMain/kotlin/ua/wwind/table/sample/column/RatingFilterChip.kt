package ua.wwind.table.sample.column

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import ua.wwind.table.TableFilterChipNode
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.sample.icon.SampleIcons

private const val MAX_RATING = 5

/**
 * The Rating column's chip label. An "equals N" filter shows as five stars, and a click on a star switches
 * the filter to that rating at once; a click elsewhere on the chip still opens the panel.
 * Any other rating filter keeps the default label.
 */
@Composable
internal fun <C> RatingFilterChip(node: TableFilterChipNode.Condition<C>) {
    val rating =
        (node.state.values?.singleOrNull() as? Int)
            ?.takeIf { node.state.constraint == FilterConstraint.EQUALS }
    if (rating == null) {
        Text(node.label)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("${node.title}:")
        Row(Modifier.padding(start = 4.dp).selectableGroup()) {
            for (stars in 1..MAX_RATING) {
                // Filled up to the rating, outlined after it, so the value does not rely on color alone.
                val icon = if (stars <= rating) SampleIcons.Star else SampleIcons.StarBorder
                Icon(
                    painter = painterResource(icon),
                    contentDescription = "$stars of $MAX_RATING stars",
                    modifier =
                        Modifier
                            .selectable(
                                selected = stars == rating,
                                interactionSource = null,
                                indication = ripple(bounded = false, radius = 12.dp),
                                role = Role.RadioButton,
                                onClick = {
                                    node.onChange(TableFilterState(FilterConstraint.EQUALS, listOf(stars)))
                                },
                            ).padding(2.dp)
                            .size(18.dp),
                )
            }
        }
    }
}
