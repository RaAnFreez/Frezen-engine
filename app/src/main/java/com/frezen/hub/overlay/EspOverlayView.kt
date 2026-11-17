// EspOverlayView.kt — Main canvas overlay view
// Render: ESP lines, ball circles, reflection path, pocket prediction, ghost ball
// Jalan di TYPE_APPLICATION_OVERLAY di atas 8 Ball Pool

package com.frezen.hub.overlay

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.View
import com.frezen.hub.cheat.*
import com.frezen.hub.utils.PrefManager
import kotlin.math.*

class EspOverlayView(context: Context, private var prefs: PrefManager) : View(context) {

    // ----------------------------------------------------------------
    // Paints
    // ----------------------------------------------------------------
    private val espLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style     = Paint.Style.STROKE
        strokeWidth = 2.5f
        color     = Color.argb(200, 0, 255, 136)  // hijau mint
        pathEffect = DashPathEffect(floatArrayOf(12f, 6f), 0f)
    }

    private val trajectoryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style     = Paint.Style.STROKE
        strokeWidth = 3f
        color     = Color.argb(220, 255, 220, 0)  // kuning
    }

    private val reflectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style     = Paint.Style.STROKE
        strokeWidth = 2f
        color     = Color.argb(180, 0, 180, 255)  // biru
        pathEffect = DashPathEffect(floatArrayOf(8f, 4f), 0f)
    }

    private val ghostBallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(160, 255, 255, 255)
    }

    private val ghostBallFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(40, 255, 255, 255)
    }

    private val ballEspPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style     = Paint.Style.STROKE
        strokeWidth = 1.5f
        color     = Color.argb(180, 0, 255, 136)
    }

    private val pocketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style     = Paint.Style.STROKE
        strokeWidth = 3f
        color     = Color.argb(220, 255, 80, 80)  // merah
    }

    private val pocketFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(60, 255, 80, 80)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = Color.WHITE
        textSize  = 28f
        typeface  = Typeface.DEFAULT_BOLD
        setShadowLayer(4f, 1f, 1f, Color.BLACK)
    }

    private val reflectionDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(220, 255, 100, 100)
    }

    // ----------------------------------------------------------------
    // Data
    // ----------------------------------------------------------------
    private val tracer    = BallTracer()
    private val predictor = BallPredictor()
    private var balls     = listOf<BallState>()
    private var espLines  = mapOf<Int, List<PointF>>()
    private var prediction: PredictionResult? = null
    private var reflectionPath: ReflectionPath? = null

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateData()
            handler.postDelayed(this, 50) // 20 FPS update
        }
    }

    // Ball colors untuk render circle
    private val ballColors = mapOf(
        0  to Color.argb(255, 255, 255, 255),
        1  to Color.argb(255, 255, 220, 0  ),
        2  to Color.argb(255, 30,  100, 220),
        3  to Color.argb(255, 220, 30,  30 ),
        4  to Color.argb(255, 140, 30,  180),
        5  to Color.argb(255, 255, 140, 0  ),
        6  to Color.argb(255, 30,  160, 50 ),
        7  to Color.argb(255, 140, 50,  30 ),
        8  to Color.argb(255, 20,  20,  20 ),
    )

    init {
        TableGeometry.autoDetect(
            context.resources.displayMetrics.widthPixels,
            context.resources.displayMetrics.heightPixels
        )

        // Demo mode: inject fake ball positions untuk testing
        injectDemoBalls()

        handler.post(updateRunnable)
    }

    // ----------------------------------------------------------------
    // Inject demo ball positions (saat screen capture tidak ada)
    // User bisa tap untuk set posisi bola secara manual
    // ----------------------------------------------------------------
    private fun injectDemoBalls() {
        val geo = TableGeometry
        val cx  = geo.tableBounds.centerX()
        val cy  = geo.tableBounds.centerY()
        val r   = geo.ballRadius * 3

        // Cue ball
        tracer.setManualBallPosition(0, PointF(cx, cy + r * 4))
        // Target balls dalam formation
        tracer.setManualBallPosition(1, PointF(cx,         cy))
        tracer.setManualBallPosition(2, PointF(cx - r,     cy + r))
        tracer.setManualBallPosition(3, PointF(cx + r,     cy + r))
        tracer.setManualBallPosition(4, PointF(cx - r*2,   cy + r*2))
        tracer.setManualBallPosition(5, PointF(cx,         cy + r*2))
        tracer.setManualBallPosition(6, PointF(cx + r*2,   cy + r*2))
        tracer.setManualBallPosition(8, PointF(cx,         cy + r))

        balls = tracer.getCurrentBalls()
    }

    private fun updateData() {
        balls = tracer.getCurrentBalls()

        val cueBall = tracer.getCueBall() ?: return
        val targets = balls.filter { !it.isCueBall && !it.isPocketed }

        if (prefs.espEnabled) {
            espLines = predictor.calculateEspLines(cueBall, targets)
        }

        if (prefs.predictionEnabled && targets.isNotEmpty()) {
            val nearest = targets.minByOrNull { geo.dist(cueBall.position, it.position) }
            if (nearest != null) {
                // Arah tembak = dari cue ke target terdekat
                val dir = PointF(
                    nearest.position.x - cueBall.position.x,
                    nearest.position.y - cueBall.position.y
                )
                prediction = predictor.predictShot(cueBall, nearest, normalizeVec(dir))
            }
        }

        if (prefs.ballLineEnabled) {
            val cueBall2 = tracer.getCueBall()
            if (cueBall2 != null && cueBall2.velocity.let { sqrt(it.x*it.x + it.y*it.y) } > 0.3f) {
                reflectionPath = predictor.calculateReflectionPath(
                    cueBall2.position,
                    PointF(cueBall2.velocity.x * 3f, cueBall2.velocity.y * 3f)
                )
            }
        }

        // Alpha dari prefs
        val a = (prefs.overlayAlpha * 2.55f).toInt().coerceIn(0, 255)
        espLinePaint.alpha      = (200 * a / 255)
        trajectoryPaint.alpha   = (220 * a / 255)
        reflectionPaint.alpha   = (180 * a / 255)

        invalidate()
    }

    // ----------------------------------------------------------------
    // Draw
    // ----------------------------------------------------------------
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val geo = TableGeometry

        // Draw table outline (debug — bisa di-hide)
        // drawTableOutline(canvas, geo)

        // Draw pocket indicators
        drawPockets(canvas, geo)

        // Draw ESP lines (cue → target → extend)
        if (prefs.espEnabled) {
            drawEspLines(canvas)
        }

        // Draw reflection path (ball sedang bergerak)
        if (prefs.ballLineEnabled) {
            drawReflectionPath(canvas)
        }

        // Draw prediction (ghost ball + trajectory)
        if (prefs.predictionEnabled) {
            drawPrediction(canvas)
        }

        // Draw ball circles + labels
        drawBalls(canvas)
    }

    private fun drawPockets(canvas: Canvas, geo: TableGeometry) {
        for (pocket in geo.pockets) {
            canvas.drawCircle(pocket.x, pocket.y, geo.pocketRadius, pocketFillPaint)
            canvas.drawCircle(pocket.x, pocket.y, geo.pocketRadius, pocketPaint)
        }
    }

    private fun drawEspLines(canvas: Canvas) {
        for ((ballId, line) in espLines) {
            if (line.size < 2) continue

            val path = Path()
            path.moveTo(line[0].x, line[0].y)
            for (i in 1 until line.size) {
                path.lineTo(line[i].x, line[i].y)
            }

            // Warna per ball type
            val color = ballColors[ballId] ?: Color.GREEN
            espLinePaint.color = Color.argb(
                180,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            )
            canvas.drawPath(path, espLinePaint)
        }
    }

    private fun drawReflectionPath(canvas: Canvas) {
        val rf = reflectionPath ?: return
        if (rf.points.size < 2) return

        val path = Path()
        path.moveTo(rf.points[0].x, rf.points[0].y)
        for (i in 1 until rf.points.size) {
            path.lineTo(rf.points[i].x, rf.points[i].y)
        }
        canvas.drawPath(path, reflectionPaint)

        // Dot di titik pantulan
        for (i in 1 until rf.points.size - 1) {
            val p = rf.points[i]
            if (abs(p.x - TableGeometry.cushionLeft)   < 8f ||
                abs(p.x - TableGeometry.cushionRight)   < 8f ||
                abs(p.y - TableGeometry.cushionTop)     < 8f ||
                abs(p.y - TableGeometry.cushionBottom)  < 8f) {
                canvas.drawCircle(p.x, p.y, 6f, reflectionDotPaint)
            }
        }

        // Label pocket target
        rf.pocketTarget?.let { pt ->
            canvas.drawCircle(pt.x, pt.y, TableGeometry.pocketRadius * 1.3f, pocketPaint)
        }
    }

    private fun drawPrediction(canvas: Canvas) {
        val pred = prediction ?: return

        // Ghost ball
        pred.ghostBallPos?.let { ghost ->
            canvas.drawCircle(ghost.x, ghost.y, TableGeometry.ballRadius, ghostBallFillPaint)
            canvas.drawCircle(ghost.x, ghost.y, TableGeometry.ballRadius, ghostBallPaint)
        }

        // Cue path setelah kontak
        if (pred.cuePath.size >= 2) {
            val cuePath = Path()
            cuePath.moveTo(pred.cuePath[0].x, pred.cuePath[0].y)
            for (i in 1 until pred.cuePath.size) {
                cuePath.lineTo(pred.cuePath[i].x, pred.cuePath[i].y)
            }
            canvas.drawPath(cuePath, reflectionPaint)
        }

        // Target ball path
        if (pred.targetPath.size >= 2) {
            val targetPath = Path()
            targetPath.moveTo(pred.targetPath[0].x, pred.targetPath[0].y)
            for (i in 1 until pred.targetPath.size) {
                targetPath.lineTo(pred.targetPath[i].x, pred.targetPath[i].y)
            }
            canvas.drawPath(targetPath, trajectoryPaint)
        }

        // Reflection dots
        for (rp in pred.reflections) {
            canvas.drawCircle(rp.x, rp.y, 7f, reflectionDotPaint)
        }

        // Pocket target highlight
        pred.pocketTarget?.let { pt ->
            canvas.drawCircle(pt.x, pt.y, TableGeometry.pocketRadius * 1.5f, pocketPaint)
            textPaint.textSize = 22f
            val angle = "%.0f°".format(pred.entryAngle)
            canvas.drawText(angle, pt.x + 12f, pt.y - 12f, textPaint)
        }

        // Cut angle label
        pred.ghostBallPos?.let { ghost ->
            textPaint.textSize = 24f
            val cut = "Cut: %.0f°".format(pred.exitAngle)
            canvas.drawText(cut, ghost.x + TableGeometry.ballRadius + 8f, ghost.y, textPaint)
        }
    }

    private fun drawBalls(canvas: Canvas) {
        for (ball in balls) {
            if (ball.isPocketed) continue

            val color = ballColors[ball.id] ?: Color.WHITE
            ballEspPaint.color = color

            // Outer glow circle
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1f
                this.color  = Color.argb(80, Color.red(color), Color.green(color), Color.blue(color))
            }
            canvas.drawCircle(ball.position.x, ball.position.y,
                ball.radius * 1.6f, glowPaint)

            // Ball outline
            canvas.drawCircle(ball.position.x, ball.position.y,
                ball.radius, ballEspPaint)

            // Label ball ID
            textPaint.textSize = ball.radius * 0.9f
            textPaint.textAlign = Paint.Align.CENTER
            val label = if (ball.isCueBall) "C" else ball.id.toString()
            canvas.drawText(
                label,
                ball.position.x,
                ball.position.y + ball.radius * 0.35f,
                textPaint
            )
            textPaint.textAlign = Paint.Align.LEFT

            // Velocity vector (kalau bola bergerak)
            val speed = sqrt(ball.velocity.x.pow(2) + ball.velocity.y.pow(2))
            if (speed > 0.5f) {
                val velScale = 20f
                val endX = ball.position.x + ball.velocity.x * velScale
                val endY = ball.position.y + ball.velocity.y * velScale
                val velPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                    this.color = Color.argb(180, 255, 255, 0)
                }
                canvas.drawLine(ball.position.x, ball.position.y, endX, endY, velPaint)
            }
        }
    }

    private val geo get() = TableGeometry

    private fun normalizeVec(v: PointF): PointF {
        val len = sqrt(v.x*v.x + v.y*v.y)
        return if (len < 0.001f) PointF(1f, 0f) else PointF(v.x/len, v.y/len)
    }

    private fun geo.dist(a: PointF, b: PointF): Float {
        val dx = a.x - b.x; val dy = a.y - b.y
        return sqrt(dx*dx + dy*dy)
    }

    fun updateSettings(prefs: PrefManager) {
        this.prefs = prefs
        invalidate()
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(updateRunnable)
        super.onDetachedFromWindow()
    }
}
