package com.example.pingme.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.example.pingme.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A decorative, native 3D love field for premium dashboard surfaces.
 *
 * The view owns no app data and never consumes touch events. High quality uses
 * Camera-backed X/Y/Z rotation, depth-scaled hearts and sparkle trails. Medium
 * draws fewer, simpler hearts at a capped frame rate. Low and reduced-motion
 * environments render a composed static fallback without a continuous loop.
 */
class FloatingLoveFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val camera = Camera().apply { setLocation(0f, 0f, -12f) }
    private val cameraMatrix = Matrix()
    private val heartPath = Path().apply {
        moveTo(0f, 10f)
        cubicTo(-20f, -2f, -10f, -18f, 0f, -9f)
        cubicTo(10f, -18f, 20f, -2f, 0f, 10f)
        close()
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.rgb(103, 48, 68)
    }
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
    }
    private val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    // Deliberately fixed composition: no random allocation or visual reshuffle on redraw.
    private val xPositions = floatArrayOf(.08f, .18f, .29f, .41f, .54f, .67f, .79f, .91f, .13f, .34f, .59f, .74f, .86f, .47f)
    private val phaseOffsets = floatArrayOf(.05f, .47f, .81f, .22f, .64f, .92f, .36f, .72f, .58f, .13f, .88f, .31f, .69f, .43f)
    private val depthValues = floatArrayOf(.32f, .78f, .48f, .92f, .38f, .69f, .54f, .84f, .61f, .27f, .96f, .43f, .73f, .57f)
    private val driftDirections = floatArrayOf(1f, -1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f, -1f, -1f, 1f, -1f, 1f)

    private val requestedQuality: VisualQuality?
    private val adaptiveMotionEnabled: Boolean
    private var adaptiveConfig = staticFallbackConfig()
    private var animator: ValueAnimator? = null
    private var phase = 0f
    private var lastRenderedAtMs = 0L
    private var entrancePlayed = false

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.FloatingLoveFieldView, defStyleAttr, 0)
        requestedQuality = when (typedArray.getInt(R.styleable.FloatingLoveFieldView_adaptiveQuality, 0)) {
            1 -> VisualQuality.HIGH
            2 -> VisualQuality.MEDIUM
            3 -> VisualQuality.LOW
            else -> null
        }
        adaptiveMotionEnabled = typedArray.getBoolean(
            R.styleable.FloatingLoveFieldView_adaptiveMotionEnabled,
            true
        )
        typedArray.recycle()

        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val heartCount = when (adaptiveConfig.quality) {
            VisualQuality.HIGH -> HIGH_HEART_COUNT
            VisualQuality.MEDIUM -> MEDIUM_HEART_COUNT
            VisualQuality.LOW -> LOW_HEART_COUNT
        }

        for (index in 0 until heartCount) {
            drawFloatingHeart(canvas, index)
        }

        if (adaptiveConfig.quality != VisualQuality.LOW) {
            drawSparkleTrail(canvas, if (adaptiveConfig.quality == VisualQuality.HIGH) 8 else 3)
        }
    }

    private fun drawFloatingHeart(canvas: Canvas, index: Int) {
        val depth = depthValues[index]
        val travel = if (adaptiveConfig.motionAllowed) {
            (phase + phaseOffsets[index]) % 1f
        } else {
            STATIC_PHASES[index]
        }
        val orbitAngle = (phase * TWO_PI * (if (index % 2 == 0) 1f else -1f)) + index * .71f
        val drift = if (adaptiveConfig.motionAllowed) {
            sin(orbitAngle.toDouble()).toFloat() * width * (.012f + depth * .018f) * driftDirections[index]
        } else {
            0f
        }
        val x = width * xPositions[index] + drift
        val y = height * (1.10f - travel * 1.24f)
        val baseScale = density * (.29f + depth * .39f)
        val breathe = if (adaptiveConfig.motionAllowed) {
            1f + sin((orbitAngle * .72f).toDouble()).toFloat() * .055f
        } else {
            1f
        }
        val scale = baseScale * breathe
        val fadeAtEdges = edgeFade(travel)
        val alpha = (fadeAtEdges * (54f + depth * 130f)).toInt().coerceIn(0, 190)

        canvas.save()
        canvas.translate(x, y)

        if (adaptiveConfig.quality == VisualQuality.HIGH && adaptiveConfig.motionAllowed) {
            cameraMatrix.reset()
            camera.save()
            camera.rotateX(sin((orbitAngle * .74f).toDouble()).toFloat() * (8f + depth * 10f))
            camera.rotateY(cos((orbitAngle * .61f).toDouble()).toFloat() * (18f + depth * 24f))
            camera.rotateZ(sin((orbitAngle * .43f).toDouble()).toFloat() * 12f)
            camera.getMatrix(cameraMatrix)
            camera.restore()
            canvas.concat(cameraMatrix)
        } else if (adaptiveConfig.motionAllowed) {
            canvas.rotate(sin((orbitAngle * .55f).toDouble()).toFloat() * 9f)
        }

        canvas.scale(scale, scale)

        shadowPaint.alpha = (alpha * .24f).toInt()
        canvas.save()
        canvas.translate(1.8f + depth * 2.2f, 2.5f + depth * 2.8f)
        canvas.scale(1.08f, 1.08f)
        canvas.drawPath(heartPath, shadowPaint)
        canvas.restore()

        edgePaint.alpha = (alpha * .82f).toInt().coerceIn(0, 210)
        canvas.save()
        canvas.scale(1.055f, 1.055f)
        canvas.drawPath(heartPath, edgePaint)
        canvas.restore()

        facePaint.color = if (index % 3 == 0) ROSE else if (index % 3 == 1) BLUSH else MAUVE_ROSE
        facePaint.alpha = alpha
        canvas.drawPath(heartPath, facePaint)

        if (adaptiveConfig.quality == VisualQuality.HIGH) {
            highlightPaint.alpha = (alpha * .72f).toInt()
            highlightPaint.strokeWidth = .85f / scale.coerceAtLeast(.1f)
            canvas.drawLine(-5.5f, -7.5f, -1.5f, -9.2f, highlightPaint)
        }
        canvas.restore()
    }

    private fun drawSparkleTrail(canvas: Canvas, count: Int) {
        for (index in 0 until count) {
            val sourceIndex = (index * 2 + 1) % HIGH_HEART_COUNT
            val travel = (phase + phaseOffsets[sourceIndex] + .035f) % 1f
            val depth = depthValues[sourceIndex]
            val x = width * xPositions[sourceIndex] +
                cos((phase * TWO_PI + index).toDouble()).toFloat() * width * .012f
            val y = height * (1.10f - travel * 1.24f) + density * (10f + index % 3 * 5f)
            val pulse = (.5f + .5f * sin((phase * TWO_PI * 2f + index).toDouble()).toFloat())
            sparklePaint.alpha = (24f + pulse * 92f * depth).toInt().coerceIn(0, 116)
            canvas.drawCircle(x, y, density * (.7f + depth * .85f), sparklePaint)
        }
    }

    private fun edgeFade(travel: Float): Float {
        val fadeIn = (travel / .13f).coerceIn(0f, 1f)
        val fadeOut = ((1f - travel) / .18f).coerceIn(0f, 1f)
        return minOf(fadeIn, fadeOut)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyAdaptivePolicy()
        playEntrance()
        if (isShown) startAnimationIfAllowed()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE && isAttachedToWindow && isShown) {
            applyAdaptivePolicy()
            startAnimationIfAllowed()
        } else {
            stopAnimation()
        }
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible && isAttachedToWindow) {
            applyAdaptivePolicy()
            startAnimationIfAllowed()
        } else {
            stopAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        animate().cancel()
        stopAnimation()
        super.onDetachedFromWindow()
    }

    private fun applyAdaptivePolicy() {
        val profile = Adaptive3DPolicy.inspect(context, isHardwareAccelerated)
        adaptiveConfig = Adaptive3DPolicy.configure(profile, requestedQuality).let { config ->
            if (adaptiveMotionEnabled) config else config.copy(motionAllowed = false)
        }
        // The window already uses hardware-accelerated Canvas. A persistent layer would
        // cache this large animated surface only to invalidate it again on every frame.
        setLayerType(LAYER_TYPE_NONE, null)
        if (!adaptiveConfig.motionAllowed) {
            phase = 0f
            invalidate()
        }
    }

    private fun playEntrance() {
        if (entrancePlayed) return
        entrancePlayed = true
        if (!adaptiveConfig.motionAllowed) {
            alpha = 1f
            scaleX = 1f
            scaleY = 1f
            return
        }
        alpha = 0f
        scaleX = .96f
        scaleY = .96f
        animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(260L)
            .setDuration(720L)
            .setInterpolator(DecelerateInterpolator(1.6f))
            .start()
    }

    private fun startAnimationIfAllowed() {
        if (!adaptiveConfig.motionAllowed || adaptiveConfig.quality == VisualQuality.LOW || animator != null) return
        lastRenderedAtMs = 0L
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = if (adaptiveConfig.quality == VisualQuality.HIGH) 15_000L else 18_000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { valueAnimator ->
                val now = SystemClock.uptimeMillis()
                val fieldFrameRate = minOf(
                    adaptiveConfig.maxFramesPerSecond,
                    if (adaptiveConfig.quality == VisualQuality.HIGH) 45 else 24
                ).coerceAtLeast(1)
                val frameInterval = 1000L / fieldFrameRate
                if (now - lastRenderedAtMs >= frameInterval) {
                    phase = valueAnimator.animatedValue as Float
                    lastRenderedAtMs = now
                    invalidate()
                }
            }
            start()
        }
    }

    private fun stopAnimation() {
        animator?.cancel()
        animator = null
    }

    private companion object {
        const val HIGH_HEART_COUNT = 14
        const val MEDIUM_HEART_COUNT = 6
        const val LOW_HEART_COUNT = 2
        val TWO_PI = (PI * 2).toFloat()
        val STATIC_PHASES = floatArrayOf(.20f, .68f, .42f, .84f, .31f, .59f, .75f, .12f, .51f, .91f, .36f, .63f, .79f, .25f)
        val ROSE = Color.rgb(232, 139, 159)
        val BLUSH = Color.rgb(255, 218, 227)
        val MAUVE_ROSE = Color.rgb(199, 111, 137)

        fun staticFallbackConfig() = Adaptive3DConfig(
            quality = VisualQuality.MEDIUM,
            motionAllowed = false,
            maxFramesPerSecond = 0,
            particleCount = 0,
            floatAmplitudeDp = 0f,
            maxTiltX = 0f,
            maxTiltY = 0f,
            maxRotationZ = 0f,
            reflectionEnabled = false
        )
    }
}
