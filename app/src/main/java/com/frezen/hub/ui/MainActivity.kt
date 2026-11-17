// MainActivity.kt — Frezen Hub v1.0 — Snake Engine style
package com.frezen.hub.ui

import android.app.AlertDialog
import android.content.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
import androidx.viewpager2.widget.ViewPager2
import com.frezen.hub.R
import com.frezen.hub.model.SupportedGame
import com.frezen.hub.service.OverlayService
import com.frezen.hub.utils.GameManager
import com.frezen.hub.utils.PrefManager

class MainActivity : AppCompatActivity() {
    companion object { private const val REQ_OVERLAY = 1001 }

    private lateinit var prefs: PrefManager
    private lateinit var gameAdapter: GameCardAdapter
    private lateinit var bannerAdapter: BannerAdapter
    private lateinit var vpBanner: ViewPager2
    private lateinit var tvGameCount: TextView
    private lateinit var tvUserId: TextView
    private lateinit var btnSubscription: Button
    private lateinit var rvGames: RecyclerView
    private lateinit var indicatorLayout: LinearLayout
    private lateinit var navHome: LinearLayout
    private lateinit var navShop: LinearLayout
    private lateinit var navChat: LinearLayout
    private lateinit var navSettings: LinearLayout

    private val handler = Handler(Looper.getMainLooper())
    private var bannerRunnable: Runnable? = null
    private var pendingGame: SupportedGame? = null

    private val bannerItems = listOf(
        BannerItem(R.drawable.banner_8bp, "Best Cheat Engine ·", "8 Ball Pool"),
        BannerItem(R.drawable.banner_8bp, "Real-time ESP Lines", "Predict Every Shot"),
        BannerItem(R.drawable.banner_8bp, "Ball Trajectory", "3-Cushion Prediction")
    )

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_main)
        prefs = PrefManager(this)
        bindViews(); setupBanner(); setupGames()
        setupBottomNav(); setupSubscriptionButton(); generateUserId()
    }

    override fun onResume() { super.onResume(); refreshGameStatus() }

    private fun bindViews() {
        vpBanner        = findViewById(R.id.vp_banner)
        tvGameCount     = findViewById(R.id.tv_game_count)
        tvUserId        = findViewById(R.id.tv_user_id)
        btnSubscription = findViewById(R.id.btn_subscription)
        rvGames         = findViewById(R.id.rv_games)
        indicatorLayout = findViewById(R.id.banner_indicator)
        navHome         = findViewById(R.id.nav_home)
        navShop         = findViewById(R.id.nav_shop)
        navChat         = findViewById(R.id.nav_chat)
        navSettings     = findViewById(R.id.nav_settings)
    }

    private fun setupBanner() {
        bannerAdapter = BannerAdapter(this, bannerItems)
        vpBanner.adapter = bannerAdapter
        // Dots
        indicatorLayout.removeAllViews()
        bannerItems.forEachIndexed { i, _ ->
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(if(i==0) 20 else 12, if(i==0) 20 else 12).apply { marginEnd = 6 }
                setBackgroundResource(if(i==0) R.drawable.dot_active else R.drawable.dot_inactive)
            }
            indicatorLayout.addView(dot)
        }
        vpBanner.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(pos: Int) { updateDots(pos) }
        })
        bannerRunnable = object : Runnable {
            override fun run() {
                vpBanner.setCurrentItem((vpBanner.currentItem + 1) % bannerItems.size, true)
                handler.postDelayed(this, 3000)
            }
        }
        handler.postDelayed(bannerRunnable!!, 3000)
    }

    private fun updateDots(pos: Int) {
        for (i in 0 until indicatorLayout.childCount) {
            val d = indicatorLayout.getChildAt(i)
            val active = i == pos
            d.layoutParams = (d.layoutParams as LinearLayout.LayoutParams).apply {
                width = if (active) 20 else 12; height = if (active) 20 else 12
            }
            d.setBackgroundResource(if (active) R.drawable.dot_active else R.drawable.dot_inactive)
        }
    }

    private fun setupGames() {
        val games = GameManager.getGamesWithStatus(this)
        tvGameCount.text = games.size.toString()
        gameAdapter = GameCardAdapter(this, games) { onGameCardClick(it) }
        rvGames.layoutManager = GridLayoutManager(this, 3)
        rvGames.adapter = gameAdapter
        rvGames.isNestedScrollingEnabled = false
    }

    private fun refreshGameStatus() {
        gameAdapter.updateGames(GameManager.getGamesWithStatus(this))
    }

    private fun onGameCardClick(game: SupportedGame) {
        if (!game.isInstalled) {
            AlertDialog.Builder(this)
                .setTitle("Game Belum Ter-install")
                .setMessage("${game.name} belum ter-install.\n\nInstall dari Google Play Store untuk menggunakan fitur ini.")
                .setPositiveButton("Buka Play Store") { _, _ -> GameManager.openPlayStore(this, game) }
                .setNegativeButton("Nanti", null).show()
        } else {
            pendingGame = game
            if (!Settings.canDrawOverlays(this)) {
                AlertDialog.Builder(this)
                    .setTitle("Izin Diperlukan")
                    .setMessage("Aktifkan 'Tampil di atas aplikasi lain' untuk ESP overlay.")
                    .setPositiveButton("Buka Pengaturan") { _, _ ->
                        startActivityForResult(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                            REQ_OVERLAY
                        )
                    }
                    .setNegativeButton("Batal", null).show()
            } else {
                startOverlayAndGame(game)
            }
        }
    }

    private fun startOverlayAndGame(game: SupportedGame) {
        val svc = Intent(this, OverlayService::class.java).apply {
            putExtra("game_id", game.id)
            putExtra("game_package", game.packageName)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc)
        else startService(svc)
        handler.postDelayed({ GameManager.launchGame(this, game.packageName) }, 500)
        Toast.makeText(this, "Overlay aktif — meluncurkan ${game.name}", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == REQ_OVERLAY && Settings.canDrawOverlays(this)) {
            pendingGame?.let { startOverlayAndGame(it) }
            pendingGame = null
        }
    }

    private fun setupSubscriptionButton() {
        btnSubscription.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Subscription")
                .setMessage("Status: LIFETIME\nSemua fitur aktif tanpa batas waktu.")
                .setPositiveButton("OK", null).show()
        }
    }

    private fun generateUserId() {
        if (prefs.userId.isEmpty()) prefs.userId = (10000..99999).random().toString()
        tvUserId.text = prefs.userId
    }

    private fun setupBottomNav() {
        setNavActive(navHome)
        navHome.setOnClickListener { setNavActive(navHome) }
        navShop.setOnClickListener { setNavActive(navShop); Toast.makeText(this, "Shop — coming soon", Toast.LENGTH_SHORT).show() }
        navChat.setOnClickListener { setNavActive(navChat); Toast.makeText(this, "Community — coming soon", Toast.LENGTH_SHORT).show() }
        navSettings.setOnClickListener {
            setNavActive(navSettings)
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setNavActive(active: LinearLayout) {
        listOf(navHome, navShop, navChat, navSettings).forEach { nav ->
            val isActive = nav == active
            (nav.getChildAt(0) as? ImageView)?.alpha = if (isActive) 1f else 0.4f
            (nav.getChildAt(1) as? TextView)?.setTextColor(
                if (isActive) 0xFF6EE7B7.toInt() else 0xFF55555F.toInt()
            )
        }
    }

    override fun onDestroy() {
        bannerRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }
}
