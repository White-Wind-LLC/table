package ua.wwind.table.platform

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.updateAndFetch

/**
 * Builds a value once per locale tag. Lock-free copy-on-write map: a few locales are read on every
 * recomposition and new ones are rare. Two threads may build the same entry; the first one stored wins.
 */
@OptIn(ExperimentalAtomicApi::class)
internal class LocaleCache<T : Any>(
    private val create: (String) -> T,
) {
    private val entries = AtomicReference(emptyMap<String, T>())

    operator fun get(tag: String): T = entries.load()[tag] ?: store(tag, create(tag))

    private fun store(
        tag: String,
        value: T,
    ): T = entries.updateAndFetch { it.putIfAbsent(tag, value) }.getValue(tag)

    private fun Map<String, T>.putIfAbsent(
        tag: String,
        value: T,
    ): Map<String, T> = if (tag in this) this else this + (tag to value)
}
