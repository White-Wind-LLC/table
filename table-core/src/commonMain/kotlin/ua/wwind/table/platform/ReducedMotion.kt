package ua.wwind.table.platform

import androidx.compose.runtime.Composable

/** Whether the user asked the platform for reduced motion; false where there is no such setting. */
@Composable
internal expect fun systemPrefersReducedMotion(): Boolean
