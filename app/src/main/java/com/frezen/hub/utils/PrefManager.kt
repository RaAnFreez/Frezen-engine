// PrefManager.kt — SharedPreferences wrapper untuk settings overlay
package com.frezen.hub.utils

import android.content.Context
import android.content.SharedPreferences

class PrefManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("frezen_hub_prefs", Context.MODE_PRIVATE)

    var espEnabled: Boolean
        get() = prefs.getBoolean("esp_enabled", true)
        set(v) = prefs.edit().putBoolean("esp_enabled", v).apply()

    var ballLineEnabled: Boolean
        get() = prefs.getBoolean("ball_line", true)
        set(v) = prefs.edit().putBoolean("ball_line", v).apply()

    var predictionEnabled: Boolean
        get() = prefs.getBoolean("prediction", true)
        set(v) = prefs.edit().putBoolean("prediction", v).apply()

    var overlayAlpha: Int
        get() = prefs.getInt("overlay_alpha", 80)
        set(v) = prefs.edit().putInt("overlay_alpha", v).apply()

    var userId: String
        get() = prefs.getString("user_id", "") ?: ""
        set(v) = prefs.edit().putString("user_id", v).apply()
}
