package ua.wwind.table.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun systemPrefersReducedMotion(): Boolean = remember { prefersReducedMotion() }

// Guarded so a test run outside a browser, with no window, reads "no preference".
@OptIn(ExperimentalWasmJsInterop::class)
private fun prefersReducedMotion(): Boolean =
    js(
        """typeof window !== 'undefined' && !!window.matchMedia &&
            window.matchMedia('(prefers-reduced-motion: reduce)').matches""",
    )
