package ua.wwind.table.component.body

import androidx.compose.ui.Alignment

internal fun Alignment.Horizontal.toCellContentAlignment(): Alignment =
    when (this) {
        Alignment.Start -> Alignment.CenterStart
        Alignment.CenterHorizontally -> Alignment.Center
        Alignment.End -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }
