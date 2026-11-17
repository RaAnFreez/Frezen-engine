// SupportedGame.kt — Data model game yang didukung Frezen Hub
package com.frezen.hub.model

data class SupportedGame(
    val id: String,
    val name: String,
    val packageName: String,     // package name di Play Store
    val playStoreUrl: String,    // URL Google Play
    val iconResId: Int,          // resource icon
    val bannerResId: Int,        // resource banner
    val isInstalled: Boolean = false
)
