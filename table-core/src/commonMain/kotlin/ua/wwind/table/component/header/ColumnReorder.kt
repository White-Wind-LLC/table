package ua.wwind.table.component.header

internal fun <C> computeReorderMove(
    fromIndex: Int,
    toIndex: Int,
    fullOrder: List<C>,
    visibleKeys: List<C>,
): Pair<Int, Int>? {
    if (fromIndex !in visibleKeys.indices) return null

    val fromKey = visibleKeys[fromIndex]
    val fromIndexInFull = fullOrder.indexOf(fromKey)
    if (fromIndexInFull == -1) return null

    val fullAfterRemoval = fullOrder.toMutableList().apply { removeAt(fromIndexInFull) }
    val visibleAfterRemoval = fullAfterRemoval.filter { it in visibleKeys.toSet() }

    val targetIndexInFull =
        if (toIndex >= visibleAfterRemoval.size) {
            val lastVisibleKey = visibleAfterRemoval.lastOrNull()
            if (lastVisibleKey == null) fullOrder.size else fullOrder.indexOf(lastVisibleKey) + 1
        } else {
            val targetBeforeKey = visibleAfterRemoval[toIndex]
            fullOrder.indexOf(targetBeforeKey)
        }

    return fromIndexInFull to targetIndexInFull
}
