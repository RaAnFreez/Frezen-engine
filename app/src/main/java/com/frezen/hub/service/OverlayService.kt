// OverlayService.kt — Frezen Hub Overlay Service
// Render ESP line, ball trajectory, dan prediction di atas 8 Ball Pool
// TYPE_APPLICATION_OVERLAY — tidak butuh root

package com.frezen.hub.service

import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import androidx.core.app.NotificationCompat
import com.frezen.hub.R
import com.frezen.hub.overlay.EspOverlayView
import com.frezen.hub.ui.MainActivity
import com.frezen.hub.utils.PrefManager

class OverlayService : Service() {

    companion object {
        const val ACTION_UPDATE_SETTINGS = "com.frezen.hub.UPDATE_SETTINGS"
        var isRunning = false
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: EspOverlayView
    private lateinit var prefs: PrefManager

    private val settingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == ACTION_UPDATE_SETTINGS) {
                overlayView.updateSettings(prefs)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        prefs     = PrefManager(this)

        setupNotification()
        createOverlay()
        registerReceiver(settingsReceiver, IntentFilter(ACTION_UPDATE_SETTINGS))
    }

    private fun setupNotification() {
        val channelId = "frezen_hub_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId, "Frezen Hub",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }

        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Frezen Hub")
            .setContentText("ESP aktif — 8 Ball Pool")
            .setSmallIcon(R.drawable.ic_logo)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        startForeground(1, notif)
    }

    private fun createOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        overlayView = EspOverlayView(this, prefs)
        windowManager.addView(overlayView, params)
    }

    override fun onDestroy() {
        isRunning = false
        unregisterReceiver(settingsReceiver)
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
