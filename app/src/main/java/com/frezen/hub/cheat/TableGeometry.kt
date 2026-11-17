// TableGeometry.kt — Geometri meja 8 Ball Pool
// Koordinat relatif terhadap screen, di-detect otomatis saat runtime
// Pocket positions: 6 pocket (4 sudut + 2 tengah)

package com.frezen.hub.cheat

import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.*

object TableGeometry {

    // Bounds meja — di-set saat overlay mulai
    // Default proporsi 8BP standard (2:1 ratio)
    var tableBounds = RectF(0f, 0f, 1080f, 1920f)
    var screenWidth  = 1080f
    var screenHeight = 1920f

    // Radius bola dalam pixel (diestimasi dari screenshot)
    var ballRadius = 28f

    // Pocket radius (lebih besar dari ball — toleransi masuk)
    var pocketRadius = 42f

    // Ketebalan cushion dari tepi meja
    var cushionThickness = 36f

    // ----------------------------------------------------------------
    // 6 Pocket positions
    // 0=TopLeft, 1=TopMid, 2=TopRight
    // 3=BotLeft, 4=BotMid, 5=BotRight
    // ----------------------------------------------------------------
    val pockets: Array<PointF> get() {
        val l = tableBounds.left   + cushionThickness
        val r = tableBounds.right  - cushionThickness
        val t = tableBounds.top    + cushionThickness
        val b = tableBounds.bottom - cushionThickness
        val mx = tableBounds.centerX()

        return arrayOf(
            PointF(l,  t),   // 0 TopLeft
            PointF(mx, t),   // 1 TopMid
            PointF(r,  t),   // 2 TopRight
            PointF(l,  b),   // 3 BotLeft
            PointF(mx, b),   // 4 BotMid
            PointF(r,  b)    // 5 BotRight
        )
    }

    // ----------------------------------------------------------------
    // Cushion boundaries — 4 sisi
    // ----------------------------------------------------------------
    val cushionLeft:   Float get() = tableBounds.left   + cushionThickness
    val cushionRight:  Float get() = tableBounds.right  - cushionThickness
    val cushionTop:    Float get() = tableBounds.top    + cushionThickness
    val cushionBottom: Float get() = tableBounds.bottom - cushionThickness

    // ----------------------------------------------------------------
    // Reflect vector off cushion
    // ----------------------------------------------------------------
    fun reflectVelocity(vel: PointF, hitSide: Side): PointF = when (hitSide) {
        Side.LEFT, Side.RIGHT -> PointF(-vel.x, vel.y)
        Side.TOP, Side.BOTTOM -> PointF(vel.x, -vel.y)
    }

    enum class Side { LEFT, RIGHT, TOP, BOTTOM }

    // Cek apakah titik di dalam batas meja (dengan margin ball radius)
    fun isInsideTable(p: PointF, margin: Float = ballRadius): Boolean =
        p.x >= cushionLeft  + margin &&
        p.x <= cushionRight - margin &&
        p.y >= cushionTop   + margin &&
        p.y <= cushionBottom - margin

    // Cek apakah bola masuk pocket
    fun isNearPocket(p: PointF): Int {
        for ((i, pocket) in pockets.withIndex()) {
            val dx = p.x - pocket.x
            val dy = p.y - pocket.y
            if (sqrt(dx*dx + dy*dy) <= pocketRadius) return i
        }
        return -1
    }

    // Jarak antara dua titik
    fun dist(a: PointF, b: PointF): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx*dx + dy*dy)
    }

    // Normalize vector
    fun normalize(v: PointF): PointF {
        val len = sqrt(v.x*v.x + v.y*v.y)
        return if (len < 0.001f) PointF(0f, 0f) else PointF(v.x/len, v.y/len)
    }

    // Auto-detect table bounds dari screen size
    fun autoDetect(w: Int, h: Int) {
        screenWidth  = w.toFloat()
        screenHeight = h.toFloat()

        // 8BP standard: meja occupy ~80% lebar, mulai dari ~15% dari atas
        val tableW = w * 0.82f
        val tableH = tableW * 2.0f  // ratio 1:2
        val left   = (w - tableW) / 2f
        val top    = h * 0.12f

        tableBounds = RectF(left, top, left + tableW, top + tableH)
        ballRadius  = tableW * 0.026f
        pocketRadius = ballRadius * 1.5f
        cushionThickness = tableW * 0.033f
    }
}
