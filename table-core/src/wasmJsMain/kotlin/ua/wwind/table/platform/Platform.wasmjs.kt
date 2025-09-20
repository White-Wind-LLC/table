package ua.wwind.table.platform

import kotlinx.browser.window

public actual fun getPlatform(): Platform = Platform.WASM

public actual fun isMobileBrowser(): Boolean {
    val userAgentRegex =
        Regex(
            "Mobi|Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini",
            RegexOption.IGNORE_CASE,
        )
    val userAgentCheck = userAgentRegex.containsMatchIn(window.navigator.userAgent)
    val hasTouchSupport = window.navigator.maxTouchPoints > 0
    return userAgentCheck || hasTouchSupport
}
