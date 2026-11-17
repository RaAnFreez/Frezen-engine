package com.frezen.hub.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Auto-start overlay setelah reboot kalau sebelumnya aktif
            val prefs = context.getSharedPreferences("frezen_hub_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("auto_start", false)) {
                val svc = Intent(context, OverlayService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(svc)
                } else {
                    context.startService(svc)
                }
            }
        }
    }
}
