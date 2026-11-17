// BallState.kt — Data model untuk posisi dan vektor bola
package com.frezen.hub.cheat

import android.graphics.PointF

data class BallState(
    val id: Int,            // 0 = cue ball, 1-15 = object balls
    val position: PointF,   // screen coordinates
    val velocity: PointF,   // pixel/frame velocity
    val radius: Float,      // screen radius
    val isCueBall: Boolean,
    val isPocketed: Boolean = false
)

data class ReflectionPath(
    val points: List<PointF>,   // titik-titik jalur pantulan
    val pocketTarget: PointF?,  // pocket yang dituju (null kalau tidak langsung)
    val confidence: Float       // 0.0 - 1.0
)

data class PredictionResult(
    val cuePath: List<PointF>,          // jalur cue ball
    val targetPath: List<PointF>,       // jalur target ball setelah ditabrak
    val reflections: List<PointF>,      // titik pantulan di cushion
    val ghostBallPos: PointF?,          // ghost ball position (contact point)
    val entryAngle: Float,              // sudut masuk ke pocket (degrees)
    val exitAngle: Float,               // sudut keluar setelah kontak
    val pocketTarget: PointF?           // koordinat pocket yang bisa dicapai
)
