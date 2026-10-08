package ua.wwind.table.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

@Composable
internal actual fun systemPrefersReducedMotion(): Boolean = remember { UIAccessibilityIsReduceMotionEnabled() }
