package ua.wwind.table.platform

public enum class Platform {
    ANDROID,
    IOS,
    JS,
    WASM,
    JVM,
}

public fun Platform.isAndroid(): Boolean = this == Platform.ANDROID

public fun Platform.isIos(): Boolean = this == Platform.IOS

public fun Platform.isJs(): Boolean = this == Platform.JS

public fun Platform.isWasm(): Boolean = this == Platform.WASM

public fun Platform.isJvm(): Boolean = this == Platform.JVM

public fun Platform.isMobile(): Boolean = this.isAndroid() || this.isIos() || isMobileBrowser()

public fun Platform.isNonMobile(): Boolean = !isMobile()

public expect fun getPlatform(): Platform

public expect fun isMobileBrowser(): Boolean
