// SettingsActivity.kt — Pengaturan overlay
package com.frezen.hub.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.frezen.hub.R
import com.frezen.hub.service.OverlayService
import com.frezen.hub.utils.PrefManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: PrefManager

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_settings)
        prefs = PrefManager(this)

        val switchEsp      = findViewById<Switch>(R.id.switch_esp)
        val switchBallLine = findViewById<Switch>(R.id.switch_ball_line)
        val switchPred     = findViewById<Switch>(R.id.switch_prediction)
        val sliderAlpha    = findViewById<SeekBar>(R.id.slider_alpha)
        val btnBack        = findViewById<ImageButton>(R.id.btn_back)

        switchEsp.isChecked      = prefs.espEnabled
        switchBallLine.isChecked = prefs.ballLineEnabled
        switchPred.isChecked     = prefs.predictionEnabled
        sliderAlpha.progress     = prefs.overlayAlpha

        fun notifyService() {
            sendBroadcast(Intent(OverlayService.ACTION_UPDATE_SETTINGS))
        }

        switchEsp.setOnCheckedChangeListener      { _, v -> prefs.espEnabled      = v; notifyService() }
        switchBallLine.setOnCheckedChangeListener { _, v -> prefs.ballLineEnabled = v; notifyService() }
        switchPred.setOnCheckedChangeListener     { _, v -> prefs.predictionEnabled = v; notifyService() }
        sliderAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) { prefs.overlayAlpha = p; notifyService() }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        btnBack.setOnClickListener { finish() }
    }
}
