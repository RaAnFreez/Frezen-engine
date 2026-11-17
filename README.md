# Frezen Hub — 8 Ball Pool ESP Cheat Injector

Overlay cheat injector untuk 8 Ball Pool dan game lainnya.
Berjalan di atas game via TYPE_APPLICATION_OVERLAY — tidak butuh root.

---

## Fitur

| Fitur | Deskripsi |
|-------|-----------|
| **ESP Line** | Garis tembak dari cue ball ke semua target ball real-time |
| **Pantulan Bola** | Jalur pantulan dari cushion (max 3 bounce) dengan titik pantulan |
| **Prediksi Arah** | Ghost ball position + trajectory cue & target setelah tumbukan |
| **Game Detection** | Auto detect game ter-install, redirect ke Play Store kalau belum |
| **Snake Engine Style UI** | Banner slideshow, supported games grid, subscription button, bottom nav |

---

## Supported Games

- 8 Ball Pool (`com.miniclip.eightballpool`)
- Carrom Pool (`com.miniclip.carrompool`)
- Soccer Stars (`com.miniclip.soccerstars`)

**Logic:**
- Game belum install → warning icon + tap → Google Play Store
- Game sudah install → tap → overlay aktif otomatis + launch game

---

## Cara Build

### Prerequisites
- Android Studio / Flutter SDK
- Java 17+
- Android SDK API 28+

### Build Debug APK
```bash
# Di folder FrezenHub
./gradlew assembleDebug

# APK output:
# app/build/outputs/apk/debug/app-debug.apk
```

### Build Release APK
```bash
# Buat keystore dulu
keytool -genkey -v -keystore frezen.keystore -alias frezenkey -keyalg RSA -keysize 2048 -validity 36500

# Build release
./gradlew assembleRelease

# APK output:
# app/build/outputs/apk/release/app-release.apk
```

---

## Struktur Project

```
app/src/main/
├── java/com/frezen/hub/
│   ├── ui/
│   │   ├── MainActivity.kt         ← Main UI Snake Engine style
│   │   ├── SettingsActivity.kt     ← Settings overlay
│   │   ├── BannerAdapter.kt        ← ViewPager2 banner slideshow
│   │   └── GameCardAdapter.kt      ← RecyclerView game grid
│   ├── service/
│   │   ├── OverlayService.kt       ← Foreground service overlay
│   │   └── BootReceiver.kt         ← Boot receiver
│   ├── overlay/
│   │   └── EspOverlayView.kt       ← Canvas ESP drawing
│   ├── cheat/
│   │   ├── BallPredictor.kt        ← Physics + trajectory engine
│   │   ├── BallTracer.kt           ← Real-time ball detection
│   │   ├── BallState.kt            ← Data model bola
│   │   └── TableGeometry.kt        ← Geometri meja + pocket
│   ├── model/
│   │   └── SupportedGame.kt        ← Data model game
│   └── utils/
│       ├── GameManager.kt          ← Game install detection + launch
│       └── PrefManager.kt          ← SharedPreferences wrapper
├── res/
│   ├── layout/
│   │   ├── activity_main.xml       ← Snake Engine style main UI
│   │   ├── activity_settings.xml   ← Settings UI
│   │   ├── item_game_card.xml      ← Game card di grid
│   │   └── item_banner.xml         ← Banner slide item
│   └── drawable/                   ← Semua icons + backgrounds
└── AndroidManifest.xml
```

---

## Cara Pakai

1. Install APK ke HP
2. Buka Frezen Hub
3. Tap game yang sudah ter-install (8 Ball Pool)
4. Grant izin overlay kalau diminta
5. App akan minimize dan launch game
6. Overlay ESP akan muncul di atas game secara otomatis
7. Buka Frezen Hub lagi untuk ubah settings

---

## Troubleshooting

| Masalah | Solusi |
|---------|--------|
| Overlay tidak muncul | Settings → Apps → Frezen Hub → "Tampil di atas aplikasi lain" → Izinkan |
| Game icon grayscale + ⚠️ | Game belum ter-install, tap untuk ke Play Store |
| ESP tidak akurat | Ball detection pakai color detection — akurasi tergantung kondisi layar |
