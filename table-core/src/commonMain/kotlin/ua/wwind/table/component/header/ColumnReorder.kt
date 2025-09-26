package ua.wwind.table.component.header

internal fun <C> computeReorderMove(
    fromIndex: Int,
    toIndex: Int,
    leadingOffset: Int,
    fullOrder: List<C>,
    visibleKeys: List<C>,
): Pair<Int, Int>? {
    val fromVisibleIndex = fromIndex - leadingOffset
    val toVisibleIndex = toIndex - leadingOffset

    if (fromVisibleIndex !in visibleKeys.indices) return null

    val fromKey = visibleKeys[fromVisibleIndex]
    val fromIndexInFull = fullOrder.indexOf(fromKey)
    if (fromIndexInFull == -1) return null

    val fullAfterRemoval = fullOrder.toMutableList().apply { removeAt(fromIndexInFull) }
    val visibleAfterRemoval = fullAfterRemoval.filter { it in visibleKeys.toSet() }

    val targetIndexInFull =
        if (toVisibleIndex >= visibleAfterRemoval.size) {
            val lastVisibleKey = visibleAfterRemoval.lastOrNull()
            if (lastVisibleKey == null) fullOrder.size else fullOrder.indexOf(lastVisibleKey) + 1
        } else {
            val targetBeforeKey = visibleAfterRemoval[toVisibleIndex]
            fullOrder.indexOf(targetBeforeKey)
        }

    return fromIndexInFull to targetIndexInFull
}
