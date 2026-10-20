package ua.wwind.table.filter.component.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

/**
 * What a filter panel's form shares with whoever shows the panel: the edit still waiting on the auto-apply
 * debounce, which [applyPending] applies before the panel closes, and the form's value field, which
 * [focusValueField] focuses.
 */
internal class FilterPanelSession {
    private val pending = mutableListOf<() -> Unit>()
    internal val valueField = FocusRequester()
    internal var hasValueField = false

    /** Applies the edits the form has not applied yet; call it before the panel leaves the composition. */
    fun applyPending() {
        pending.toList().forEach { it() }
    }

    /** Focuses the form's value field; false when the form shows none. */
    fun focusValueField(): Boolean = hasValueField && valueField.requestFocus()

    internal fun register(apply: () -> Unit) {
        pending += apply
    }

    internal fun unregister(apply: () -> Unit) {
        pending -= apply
    }
}

/** The session of the filter panel around a form; null for a form outside one, as a fast filter. */
internal val LocalFilterPanelSession: ProvidableCompositionLocal<FilterPanelSession?> =
    staticCompositionLocalOf { null }

/**
 * Registers [apply], which applies an edit still waiting on the debounce, with the enclosing panel's session.
 * Does nothing without auto-apply, where an edit waits for Apply instead.
 */
@Composable
internal fun RegisterPendingFilterEdit(
    autoApply: Boolean,
    apply: () -> Unit,
) {
    val session = LocalFilterPanelSession.current
    val currentApply by rememberUpdatedState(apply)
    DisposableEffect(session, autoApply) {
        if (session == null || !autoApply) return@DisposableEffect onDispose {}
        val entry = { currentApply() }
        session.register(entry)
        onDispose { session.unregister(entry) }
    }
}

/** The modifier of a form's value field, which the enclosing panel focuses first. */
@Composable
internal fun rememberFilterValueFieldModifier(): Modifier {
    val session = LocalFilterPanelSession.current
    DisposableEffect(session) {
        session?.hasValueField = true
        onDispose { session?.hasValueField = false }
    }
    return if (session != null) Modifier.focusRequester(session.valueField) else Modifier
}
