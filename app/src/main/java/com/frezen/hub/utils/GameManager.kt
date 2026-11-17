// GameManager.kt — Detect installed games dan manage overlay per game
package com.frezen.hub.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.frezen.hub.R
import com.frezen.hub.model.SupportedGame

object GameManager {

    // Semua game yang didukung
    val SUPPORTED_GAMES = listOf(
        SupportedGame(
            id           = "8bp",
            name         = "8 Ball Pool",
            packageName  = "com.miniclip.eightballpool",
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.miniclip.eightballpool",
            iconResId    = R.drawable.ic_game_8bp,
            bannerResId  = R.drawable.banner_8bp
        ),
        SupportedGame(
            id           = "carrom",
            name         = "Carrom Pool",
            packageName  = "com.miniclip.carrompool",
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.miniclip.carrompool",
            iconResId    = R.drawable.ic_game_carrom,
            bannerResId  = R.drawable.banner_8bp   // reuse
        ),
        SupportedGame(
            id           = "soccer",
            name         = "Soccer Stars",
            packageName  = "com.miniclip.soccerstars",
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.miniclip.soccerstars",
            iconResId    = R.drawable.ic_game_soccer,
            bannerResId  = R.drawable.banner_8bp   // reuse
        )
    )

    // Cek apakah game ter-install di device
    fun isInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    // Return list games dengan status isInstalled yang update
    fun getGamesWithStatus(context: Context): List<SupportedGame> {
        return SUPPORTED_GAMES.map { game ->
            game.copy(isInstalled = isInstalled(context, game.packageName))
        }
    }

    // Buka Play Store untuk game yang belum di-install
    fun openPlayStore(context: Context, game: SupportedGame) {
        try {
            // Coba buka Play Store app langsung
            val marketIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=${game.packageName}")
            )
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(marketIntent)
        } catch (e: Exception) {
            // Fallback ke browser
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(game.playStoreUrl)
            )
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(webIntent)
        }
    }

    // Launch game yang sudah ter-install
    fun launchGame(context: Context, packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
