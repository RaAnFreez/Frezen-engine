// BallPredictor.kt — Ball trajectory, reflection, dan prediction engine
// Implementasi physics 8 Ball Pool:
//   1. Ghost ball method untuk cue-target collision
//   2. Reflection off cushion (max 3 bounce)
//   3. Pocket prediction dari angle
//   4. ESP line dari cue ke semua target ball

package com.frezen.hub.cheat

import android.graphics.PointF
import kotlin.math.*

class BallPredictor {

    companion object {
        const val MAX_REFLECTIONS = 3      // max pantulan dari cushion
        const val MAX_PATH_STEPS  = 300    // max step simulasi
        const val FRICTION        = 0.985f // koefisien gesekan per step
        const val MIN_VELOCITY    = 0.5f   // stop kalau velocity < ini
    }

    private val geo = TableGeometry

    // ----------------------------------------------------------------
    // Hitung ESP line: dari cue ball ke semua target ball
    // Return: map dari ballId ke list titik jalur
    // ----------------------------------------------------------------
    fun calculateEspLines(
        cueBall: BallState,
        targets: List<BallState>
    ): Map<Int, List<PointF>> {
        val result = mutableMapOf<Int, List<PointF>>()

        for (target in targets) {
            if (target.isCueBall || target.isPocketed) continue

            // Ghost ball position — titik kontak cue dengan target
            val contactDir = normalizeVec(PointF(
                target.position.x - cueBall.position.x,
                target.position.y - cueBall.position.y
            ))
            val ghostPos = PointF(
                target.position.x - contactDir.x * (geo.ballRadius * 2),
                target.position.y - contactDir.y * (geo.ballRadius * 2)
            )

            // ESP line = cue ball → ghost ball → (terus ke cushion)
            val line = mutableListOf(cueBall.position, ghostPos)

            // Extend line ke cushion/pocket
            val extended = extendToCushionOrPocket(ghostPos, contactDir)
            line.addAll(extended)

            result[target.id] = line
        }

        return result
    }

    // ----------------------------------------------------------------
    // Full prediction untuk shot tertentu:
    // Dari arah cue stick, hitung:
    // 1. Jalur cue ball (dengan pantulan)
    // 2. Jalur target ball setelah kontak
    // 3. Ghost ball position
    // 4. Pocket prediction
    // ----------------------------------------------------------------
    fun predictShot(
        cueBall: BallState,
        target: BallState,
        cueDirection: PointF   // unit vector arah tembak cue stick
    ): PredictionResult {
        val contactDir = normalizeVec(PointF(
            target.position.x - cueBall.position.x,
            target.position.y - cueBall.position.y
        ))

        // Ghost ball position
        val ghostPos = PointF(
            target.position.x - contactDir.x * (geo.ballRadius * 2),
            target.position.y - contactDir.y * (geo.ballRadius * 2)
        )

        // Sudut tumbukan (cut angle)
        val cutAngle = acos(
            (cueDirection.x * contactDir.x + cueDirection.y * contactDir.y)
                .coerceIn(-1f, 1f)
        )

        // Velocity transfer:
        // Target ball bergerak searah contactDir
        // Cue ball deflect ke arah perpendicular dari contactDir
        val targetVel = PointF(
            contactDir.x * cos(cutAngle),
            contactDir.y * cos(cutAngle)
        )

        // Perpendicular component untuk cue ball setelah kontak
        val perpDir = PointF(-contactDir.y, contactDir.x)
        val cuePostVel = PointF(
            perpDir.x * sin(cutAngle),
            perpDir.y * sin(cutAngle)
        )

        // Simulasi jalur cue ball setelah kontak
        val cuePath = simulatePath(ghostPos, cuePostVel)

        // Simulasi jalur target ball
        val targetPath = simulatePath(target.position, targetVel)

        // Cek pocket untuk target ball
        val pocketTarget = findNearestPocketOnPath(targetPath)

        // Entry angle ke pocket
        val entryAngle = if (pocketTarget != null && targetPath.size >= 2) {
            val lastDir = PointF(
                pocketTarget.x - targetPath[targetPath.size-2].x,
                pocketTarget.y - targetPath[targetPath.size-2].y
            )
            atan2(lastDir.y, lastDir.x) * 180f / PI.toFloat()
        } else 0f

        // Reflections dari cushion dalam cuePath
        val reflections = findReflectionPoints(cuePath)

        return PredictionResult(
            cuePath      = cuePath,
            targetPath   = targetPath,
            reflections  = reflections,
            ghostBallPos = ghostPos,
            entryAngle   = entryAngle,
            exitAngle    = cutAngle * 180f / PI.toFloat(),
            pocketTarget = pocketTarget
        )
    }

    // ----------------------------------------------------------------
    // Hitung prediksi pantulan dari posisi dan velocity awal
    // Simulasi step-by-step dengan friction dan cushion bounce
    // ----------------------------------------------------------------
    fun calculateReflectionPath(
        startPos: PointF,
        startVel: PointF,
        maxBounces: Int = MAX_REFLECTIONS
    ): ReflectionPath {
        val path = mutableListOf(startPos)
        var pos  = PointF(startPos.x, startPos.y)
        var vel  = PointF(startVel.x, startVel.y)
        var bounces = 0

        for (step in 0 until MAX_PATH_STEPS) {
            // Apply friction
            vel = PointF(vel.x * FRICTION, vel.y * FRICTION)

            // Stop kalau terlalu lambat
            val speed = sqrt(vel.x*vel.x + vel.y*vel.y)
            if (speed < MIN_VELOCITY) break

            val nextX = pos.x + vel.x
            val nextY = pos.y + vel.y

            // Cek cushion collision
            var reflected = false
            var newVel    = vel

            if (nextX <= geo.cushionLeft + geo.ballRadius) {
                newVel = geo.reflectVelocity(vel, TableGeometry.Side.LEFT)
                pos = PointF(geo.cushionLeft + geo.ballRadius, nextY)
                reflected = true; bounces++
            } else if (nextX >= geo.cushionRight - geo.ballRadius) {
                newVel = geo.reflectVelocity(vel, TableGeometry.Side.RIGHT)
                pos = PointF(geo.cushionRight - geo.ballRadius, nextY)
                reflected = true; bounces++
            } else if (nextY <= geo.cushionTop + geo.ballRadius) {
                newVel = geo.reflectVelocity(vel, TableGeometry.Side.TOP)
                pos = PointF(nextX, geo.cushionTop + geo.ballRadius)
                reflected = true; bounces++
            } else if (nextY >= geo.cushionBottom - geo.ballRadius) {
                newVel = geo.reflectVelocity(vel, TableGeometry.Side.BOTTOM)
                pos = PointF(nextX, geo.cushionBottom - geo.ballRadius)
                reflected = true; bounces++
            }

            if (reflected) {
                vel = newVel
                path.add(PointF(pos.x, pos.y))
                if (bounces >= maxBounces) break
            } else {
                pos = PointF(nextX, nextY)
            }

            // Cek pocket
            val pocketIdx = geo.isNearPocket(pos)
            if (pocketIdx >= 0) {
                path.add(geo.pockets[pocketIdx])
                return ReflectionPath(
                    points       = path,
                    pocketTarget = geo.pockets[pocketIdx],
                    confidence   = 1f - bounces * 0.2f
                )
            }

            // Sample path setiap beberapa step untuk reduce points
            if (step % 8 == 0) path.add(PointF(pos.x, pos.y))
        }

        path.add(PointF(pos.x, pos.y))
        return ReflectionPath(path, null, 0.3f)
    }

    // ----------------------------------------------------------------
    // Simulasi path sederhana untuk shot prediction
    // ----------------------------------------------------------------
    private fun simulatePath(startPos: PointF, startVel: PointF): List<PointF> {
        val speed = sqrt(startVel.x*startVel.x + startVel.y*startVel.y)
        // Normalize dan kasih speed yang realistis
        val initSpeed = 12f
        val path = calculateReflectionPath(
            startPos,
            if (speed > 0.001f)
                PointF(startVel.x/speed * initSpeed, startVel.y/speed * initSpeed)
            else
                startVel
        )
        return path.points
    }

    // ----------------------------------------------------------------
    // Extend line dari titik ke arah tertentu sampai cushion/pocket
    // ----------------------------------------------------------------
    private fun extendToCushionOrPocket(
        from: PointF,
        direction: PointF,
        maxLength: Float = 2000f
    ): List<PointF> {
        val dir  = normalizeVec(direction)
        var dist = 0f
        val step = 4f
        val result = mutableListOf<PointF>()

        while (dist < maxLength) {
            dist += step
            val p = PointF(from.x + dir.x * dist, from.y + dir.y * dist)

            // Cek pocket
            val pocketIdx = geo.isNearPocket(p)
            if (pocketIdx >= 0) {
                result.add(geo.pockets[pocketIdx])
                break
            }

            // Cek cushion
            if (p.x <= geo.cushionLeft  || p.x >= geo.cushionRight ||
                p.y <= geo.cushionTop   || p.y >= geo.cushionBottom) {
                result.add(p)
                break
            }
        }

        return result
    }

    // ----------------------------------------------------------------
    // Cari titik pantulan dari path
    // ----------------------------------------------------------------
    private fun findReflectionPoints(path: List<PointF>): List<PointF> {
        val reflections = mutableListOf<PointF>()
        for (i in 1 until path.size - 1) {
            val p = path[i]
            if (abs(p.x - geo.cushionLeft)   < 5f ||
                abs(p.x - geo.cushionRight)   < 5f ||
                abs(p.y - geo.cushionTop)     < 5f ||
                abs(p.y - geo.cushionBottom)  < 5f) {
                reflections.add(p)
            }
        }
        return reflections
    }

    // ----------------------------------------------------------------
    // Cari pocket terdekat dari path
    // ----------------------------------------------------------------
    private fun findNearestPocketOnPath(path: List<PointF>): PointF? {
        for (p in path) {
            val idx = geo.isNearPocket(p)
            if (idx >= 0) return geo.pockets[idx]
        }
        return null
    }

    private fun normalizeVec(v: PointF): PointF {
        val len = sqrt(v.x*v.x + v.y*v.y)
        return if (len < 0.001f) PointF(1f, 0f) else PointF(v.x/len, v.y/len)
    }
}
