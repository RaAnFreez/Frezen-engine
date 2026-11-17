// BallTracer.kt — Real-time ball position detection
// Method: MediaProjection screen capture → bitmap → color cluster detection
// Fallback: manual tap input kalau projection tidak tersedia

package com.frezen.hub.cheat

import android.graphics.*
import kotlin.math.*

class BallTracer {

    companion object {
        // 8 Ball Pool ball color signatures (HSV ranges)
        // Format: (hueMin, hueMax, satMin, satMax, valMin, valMax)
        private val BALL_COLORS = mapOf(
            0  to floatArrayOf(0f, 360f, 0f, 0.1f, 0.9f, 1.0f),  // cue: white
            1  to floatArrayOf(45f, 65f,  0.8f, 1.0f, 0.8f, 1.0f), // 1: yellow
            2  to floatArrayOf(200f, 240f, 0.8f, 1.0f, 0.5f, 0.9f), // 2: blue
            3  to floatArrayOf(0f,  15f,  0.9f, 1.0f, 0.7f, 1.0f), // 3: red
            4  to floatArrayOf(270f, 300f, 0.7f, 1.0f, 0.4f, 0.7f), // 4: purple
            5  to floatArrayOf(10f,  25f,  0.9f, 1.0f, 0.6f, 0.9f), // 5: orange
            6  to floatArrayOf(90f,  150f, 0.5f, 1.0f, 0.4f, 0.8f), // 6: green
            7  to floatArrayOf(0f,   8f,  0.9f, 1.0f, 0.4f, 0.7f), // 7: maroon
            8  to floatArrayOf(0f,  360f, 0f, 0.1f, 0.0f, 0.1f),   // 8: black
        )
    }

    private val balls = mutableMapOf<Int, BallState>()
    private val prevPositions = mutableMapOf<Int, PointF>()
    private var frameCount = 0L

    // ----------------------------------------------------------------
    // Update dari bitmap (screen capture frame)
    // ----------------------------------------------------------------
    fun updateFromBitmap(bitmap: Bitmap): List<BallState> {
        frameCount++
        val geo = TableGeometry

        // Crop ke area meja saja untuk performa
        val tableLeft = geo.cushionLeft.toInt().coerceAtLeast(0)
        val tableTop  = geo.cushionTop.toInt().coerceAtLeast(0)
        val tableW    = (geo.cushionRight - geo.cushionLeft).toInt().coerceAtMost(bitmap.width  - tableLeft)
        val tableH    = (geo.cushionBottom - geo.cushionTop).toInt().coerceAtMost(bitmap.height - tableTop)

        if (tableW <= 0 || tableH <= 0) return balls.values.toList()

        val tableCrop = Bitmap.createBitmap(bitmap, tableLeft, tableTop, tableW, tableH)

        // Scan untuk setiap ball color
        val detected = mutableListOf<BallState>()
        for ((ballId, colorRange) in BALL_COLORS) {
            val center = findBallCenter(tableCrop, colorRange, geo.ballRadius)
            if (center != null) {
                // Adjust koordinat ke screen space
                val screenPos = PointF(
                    center.x + tableLeft,
                    center.y + tableTop
                )

                // Hitung velocity dari prev position
                val prev = prevPositions[ballId]
                val vel  = if (prev != null)
                    PointF(screenPos.x - prev.x, screenPos.y - prev.y)
                else
                    PointF(0f, 0f)

                prevPositions[ballId] = screenPos

                val state = BallState(
                    id        = ballId,
                    position  = screenPos,
                    velocity  = vel,
                    radius    = geo.ballRadius,
                    isCueBall = ballId == 0,
                    isPocketed = false
                )
                balls[ballId] = state
                detected.add(state)
            }
        }

        tableCrop.recycle()
        return detected
    }

    // ----------------------------------------------------------------
    // Find ball center via color blob detection
    // Scan dalam grid, cari cluster pixel yang match color range
    // ----------------------------------------------------------------
    private fun findBallCenter(
        bitmap: Bitmap,
        colorRange: FloatArray,
        expectedRadius: Float
    ): PointF? {
        val w = bitmap.width
        val h = bitmap.height
        val step = 4  // scan setiap 4 pixel untuk performa

        var sumX = 0f; var sumY = 0f; var count = 0

        for (y in 0 until h step step) {
            for (x in 0 until w step step) {
                val pixel = bitmap.getPixel(x, y)
                if (matchesColor(pixel, colorRange)) {
                    sumX += x; sumY += y; count++
                }
            }
        }

        if (count < 3) return null  // too few pixels

        val cx = sumX / count
        val cy = sumY / count

        // Verifikasi: hitung radius dari centroid
        var maxR = 0f
        for (y in 0 until h step step) {
            for (x in 0 until w step step) {
                if (matchesColor(bitmap.getPixel(x, y), colorRange)) {
                    val dx = x - cx; val dy = y - cy
                    val r = sqrt(dx*dx + dy*dy)
                    if (r > maxR) maxR = r
                }
            }
        }

        // Reject kalau radius jauh dari expected
        val tolerance = 0.6f
        if (maxR < expectedRadius * tolerance || maxR > expectedRadius * (2f - tolerance)) {
            return null
        }

        return PointF(cx, cy)
    }

    // ----------------------------------------------------------------
    // Match pixel warna ke color range (HSV)
    // ----------------------------------------------------------------
    private fun matchesColor(pixel: Int, range: FloatArray): Boolean {
        val r = Color.red(pixel)   / 255f
        val g = Color.green(pixel) / 255f
        val b = Color.blue(pixel)  / 255f

        val hsv = FloatArray(3)
        Color.RGBToHSV(Color.red(pixel), Color.green(pixel), Color.blue(pixel), hsv)

        val (hueMin, hueMax, satMin, satMax, valMin, valMax) = range

        return hsv[0] in hueMin..hueMax &&
               hsv[1] in satMin..satMax &&
               hsv[2] in valMin..valMax
    }

    private operator fun FloatArray.component1() = this[0]
    private operator fun FloatArray.component2() = this[1]
    private operator fun FloatArray.component3() = this[2]
    private operator fun FloatArray.component4() = this[3]
    private operator fun FloatArray.component5() = this[4]
    private operator fun FloatArray.component6() = this[5]

    // ----------------------------------------------------------------
    // Manual override — set ball position dari tap user
    // Dipakai sebagai fallback kalau screen capture tidak available
    // ----------------------------------------------------------------
    fun setManualBallPosition(ballId: Int, screenPos: PointF) {
        val prev = prevPositions[ballId]
        val vel  = if (prev != null)
            PointF(screenPos.x - prev.x, screenPos.y - prev.y)
        else
            PointF(0f, 0f)

        prevPositions[ballId] = screenPos
        balls[ballId] = BallState(
            id        = ballId,
            position  = screenPos,
            velocity  = vel,
            radius    = TableGeometry.ballRadius,
            isCueBall = ballId == 0
        )
    }

    fun getCurrentBalls(): List<BallState> = balls.values.toList()

    fun getCueBall(): BallState? = balls[0]

    fun clearAll() {
        balls.clear()
        prevPositions.clear()
    }
}
