package com.example.pingme.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.example.pingme.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Native adaptive crystal orb used by the dashboard hero.
 *
 * High renders layered crystal, animated reflection, three orbit rings and six Canvas
 * particles. Medium removes expensive layers and caps redraws near 30 fps. Low is a
 * static two-layer premium fallback. No application state or backend data enters here.
 */
class PremiumOrbView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val reflectionPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 232
        style = Paint.Style.FILL
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(38, 78, 60, 67)
    }
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.15f * density
    }
    private val orbitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }

    private val heartPath = Path()
    private val clipPath = Path()
    private val orbitRect = RectF()
    private val shadowRect = RectF()
    private val reflectionRect = RectF()

    private val particleAngles = floatArrayOf(0.18f, 1.12f, 2.08f, 3.16f, 4.25f, 5.32f)
    private val particleRadiusFactors = floatArrayOf(1.22f, 1.43f, 1.31f, 1.52f, 1.26f, 1.39f)
    private val particleSizes = floatArrayOf(1.8f, 1.2f, 1.5f, 1.1f, 1.6f, 1.25f)

    private var phase = 0f
    private var lastRenderedAtMs = 0L
    private var floatAnimator: ValueAnimator? = null
    private var entrancePlayed = false
    private var orbRadius = 0f
    private var centerXValue = 0f
    private var centerYValue = 0f
    private var touchParallaxX = 0f
    private var touchParallaxY = 0f

    private var coreGradient: Shader? = null
    private var shellGradient: Shader? = null
    private var innerGradient: Shader? = null
    private var rimGradient: Shader? = null
    private var highlightGradient: Shader? = null
    private var glowGradient: Shader? = null
    private var reflectionGradient: Shader? = null

    private val requestedQuality: VisualQuality?
    private val adaptiveMotionEnabled: Boolean
    private var adaptiveConfig = mediumStaticConfig()

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.PremiumOrbView, defStyleAttr, 0)
        requestedQuality = when (typedArray.getInt(R.styleable.PremiumOrbView_adaptiveQuality, 0)) {
            1 -> VisualQuality.HIGH
            2 -> VisualQuality.MEDIUM
            3 -> VisualQuality.LOW
            else -> null
        }
        adaptiveMotionEnabled = typedArray.getBoolean(
            R.styleable.PremiumOrbView_adaptiveMotionEnabled,
            true
        )
        typedArray.recycle()

        isClickable = true
        isFocusable = true
        cameraDistance = 9000f * density
        contentDescription = context.getString(R.string.dashboard_orb_description)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerXValue = w / 2f
        centerYValue = h / 2f
        orbRadius = minOf(w, h) * 0.33f

        coreGradient = RadialGradient(
            centerXValue - orbRadius * .34f,
            centerYValue - orbRadius * .42f,
            orbRadius * 1.46f,
            intArrayOf(
                Color.rgb(255, 252, 253),
                ContextCompat.getColor(context, R.color.pingme_pink_soft),
                ContextCompat.getColor(context, R.color.pingme_pink),
                ContextCompat.getColor(context, R.color.pingme_pink_deep)
            ),
            floatArrayOf(0f, .30f, .70f, 1f),
            Shader.TileMode.CLAMP
        )
        shellGradient = RadialGradient(
            centerXValue - orbRadius * .28f,
            centerYValue - orbRadius * .35f,
            orbRadius * 1.58f,
            intArrayOf(
                Color.argb(142, 255, 255, 255),
                Color.argb(82, 247, 216, 225),
                Color.argb(12, 215, 100, 128),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, .36f, .72f, 1f),
            Shader.TileMode.CLAMP
        )
        innerGradient = RadialGradient(
            centerXValue - orbRadius * .20f,
            centerYValue - orbRadius * .28f,
            orbRadius * .88f,
            intArrayOf(
                Color.argb(125, 255, 255, 255),
                Color.argb(30, 255, 237, 242),
                Color.argb(12, 142, 105, 116)
            ),
            floatArrayOf(0f, .58f, 1f),
            Shader.TileMode.CLAMP
        )
        rimGradient = LinearGradient(
            centerXValue - orbRadius,
            centerYValue - orbRadius,
            centerXValue + orbRadius,
            centerYValue + orbRadius,
            intArrayOf(
                Color.argb(245, 255, 255, 255),
                Color.argb(62, 255, 255, 255),
                Color.argb(205, 255, 255, 255)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        highlightGradient = RadialGradient(
            centerXValue - orbRadius * .38f,
            centerYValue - orbRadius * .48f,
            orbRadius * .52f,
            Color.argb(220, 255, 255, 255),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        glowGradient = RadialGradient(
            centerXValue,
            centerYValue,
            orbRadius * 1.58f,
            intArrayOf(
                Color.argb(76, 215, 100, 128),
                Color.argb(28, 247, 216, 225),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, .58f, 1f),
            Shader.TileMode.CLAMP
        )
        reflectionGradient = LinearGradient(
            centerXValue - orbRadius,
            centerYValue - orbRadius,
            centerXValue + orbRadius,
            centerYValue + orbRadius,
            intArrayOf(Color.TRANSPARENT, Color.argb(128, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, .5f, 1f),
            Shader.TileMode.CLAMP
        )

        clipPath.rewind()
        clipPath.addCircle(centerXValue, centerYValue, orbRadius, Path.Direction.CW)
        reflectionRect.set(
            centerXValue - orbRadius * .72f,
            centerYValue - orbRadius * .95f,
            centerXValue - orbRadius * .30f,
            centerYValue + orbRadius * .80f
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (orbRadius <= 0f) return

        val sinPhase = sin(phase.toDouble()).toFloat()
        val cosPhase = cos(phase.toDouble()).toFloat()
        val floatOffset = sinPhase * adaptiveConfig.floatAmplitudeDp * density
        val cx = centerXValue + touchParallaxX
        val cy = centerYValue + floatOffset + touchParallaxY
        val localDx = cx - centerXValue
        val localDy = cy - centerYValue

        glowPaint.shader = glowGradient
        glowPaint.alpha = when (adaptiveConfig.quality) {
            VisualQuality.HIGH -> (176 + sinPhase * 24f).toInt().coerceIn(120, 205)
            VisualQuality.MEDIUM -> 116
            VisualQuality.LOW -> 72
        }
        canvas.save()
        canvas.translate(localDx, localDy)
        canvas.drawCircle(centerXValue, centerYValue, orbRadius * 1.54f, glowPaint)
        canvas.restore()

        val shadowShift = touchParallaxX * .45f
        shadowRect.set(
            cx - orbRadius * .68f + shadowShift,
            cy + orbRadius * 1.16f,
            cx + orbRadius * .68f + shadowShift,
            cy + orbRadius * 1.38f
        )
        shadowPaint.alpha = when (adaptiveConfig.quality) {
            VisualQuality.HIGH -> 52
            VisualQuality.MEDIUM -> 38
            VisualQuality.LOW -> 26
        }
        canvas.drawOval(shadowRect, shadowPaint)

        if (adaptiveConfig.quality == VisualQuality.HIGH) {
            shellPaint.shader = shellGradient
            shellPaint.alpha = 220
            canvas.drawCircle(cx, cy, orbRadius * (1.10f + sinPhase * .012f), shellPaint)
        }

        corePaint.shader = coreGradient
        canvas.drawCircle(cx, cy, orbRadius, corePaint)

        if (adaptiveConfig.quality != VisualQuality.LOW) {
            innerPaint.shader = innerGradient
            innerPaint.alpha = if (adaptiveConfig.quality == VisualQuality.HIGH) 170 else 110
            canvas.drawCircle(cx - orbRadius * .03f, cy - orbRadius * .02f, orbRadius * .74f, innerPaint)
        }

        rimPaint.shader = rimGradient
        rimPaint.alpha = 235
        canvas.drawCircle(cx, cy, orbRadius - rimPaint.strokeWidth, rimPaint)

        drawOrbitRings(canvas, cx, cy)

        highlightPaint.shader = highlightGradient
        highlightPaint.alpha = if (adaptiveConfig.quality == VisualQuality.LOW) 155 else 230
        canvas.save()
        canvas.translate(localDx + cosPhase * density * 1.8f, localDy + sinPhase * density)
        canvas.drawCircle(
            centerXValue - orbRadius * .18f,
            centerYValue - orbRadius * .24f,
            orbRadius * .52f,
            highlightPaint
        )
        canvas.restore()

        if (adaptiveConfig.reflectionEnabled) {
            reflectionPaint.shader = reflectionGradient
            reflectionPaint.alpha = (102 + cosPhase * 26f).toInt().coerceIn(70, 132)
            canvas.save()
            canvas.translate(localDx + sinPhase * orbRadius * .18f, localDy)
            canvas.clipPath(clipPath)
            canvas.rotate(-16f, centerXValue, centerYValue)
            canvas.drawRoundRect(reflectionRect, orbRadius * .20f, orbRadius * .20f, reflectionPaint)
            canvas.restore()
        }

        drawHeart(canvas, cx, cy + orbRadius * .02f, orbRadius * .23f)

        if (adaptiveConfig.particleCount > 0) {
            drawParticles(canvas, cx, cy)
        }
    }

    private fun drawOrbitRings(canvas: Canvas, cx: Float, cy: Float) {
        val phaseDegrees = phase * 180f / PI.toFloat()

        orbitPaint.color = Color.argb(126, 255, 255, 255)
        orbitRect.set(cx - orbRadius * .96f, cy - orbRadius * .23f, cx + orbRadius * .96f, cy + orbRadius * .23f)
        canvas.save()
        canvas.rotate(if (adaptiveConfig.quality == VisualQuality.LOW) -8f else phaseDegrees * .20f - 8f, cx, cy)
        canvas.drawOval(orbitRect, orbitPaint)
        canvas.restore()

        if (adaptiveConfig.quality == VisualQuality.LOW) return

        orbitPaint.color = Color.argb(98, 195, 161, 94)
        orbitRect.set(cx - orbRadius * 1.08f, cy - orbRadius * .13f, cx + orbRadius * 1.08f, cy + orbRadius * .13f)
        canvas.save()
        canvas.rotate(-phaseDegrees * .14f + 17f, cx, cy)
        canvas.drawOval(orbitRect, orbitPaint)
        canvas.restore()

        if (adaptiveConfig.quality == VisualQuality.HIGH) {
            orbitPaint.color = Color.argb(72, 142, 105, 116)
            orbitRect.set(cx - orbRadius * .72f, cy - orbRadius * .51f, cx + orbRadius * .72f, cy + orbRadius * .51f)
            canvas.save()
            canvas.rotate(phaseDegrees * .11f + 54f, cx, cy)
            canvas.drawOval(orbitRect, orbitPaint)
            canvas.restore()
        }
    }

    private fun drawParticles(canvas: Canvas, cx: Float, cy: Float) {
        for (index in 0 until adaptiveConfig.particleCount.coerceAtMost(particleAngles.size)) {
            val angle = phase * (.32f + index * .025f) + particleAngles[index]
            val orbitRadius = orbRadius * particleRadiusFactors[index]
            val px = cx + cos(angle.toDouble()).toFloat() * orbitRadius
            val py = cy + sin(angle.toDouble()).toFloat() * orbitRadius * .58f
            particlePaint.alpha = 74 + (index % 3) * 32
            canvas.drawCircle(px, py, particleSizes[index] * density, particlePaint)
        }
    }

    private fun drawHeart(canvas: Canvas, x: Float, y: Float, size: Float) {
        heartPath.rewind()
        heartPath.moveTo(x, y + size * .72f)
        heartPath.cubicTo(x - size * 1.35f, y - size * .05f, x - size * .65f, y - size, x, y - size * .42f)
        heartPath.cubicTo(x + size * .65f, y - size, x + size * 1.35f, y - size * .05f, x, y + size * .72f)
        heartPath.close()
        canvas.drawPath(heartPath, heartPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> applyTouchTilt(event)
            MotionEvent.ACTION_UP -> {
                performClick()
                settleToRest()
            }
            MotionEvent.ACTION_CANCEL -> settleToRest()
        }
        return true
    }

    private fun applyTouchTilt(event: MotionEvent) {
        animate().cancel()
        scaleX = .975f
        scaleY = .975f

        if (adaptiveConfig.quality == VisualQuality.LOW || !adaptiveConfig.motionAllowed) {
            translationZ = 1f * density
            return
        }

        val normalizedX = ((event.x / width.coerceAtLeast(1)) - .5f) * 2f
        val normalizedY = ((event.y / height.coerceAtLeast(1)) - .5f) * 2f
        rotationY = normalizedX * adaptiveConfig.maxTiltY
        rotationX = -normalizedY * adaptiveConfig.maxTiltX
        touchParallaxX = normalizedX * density * if (adaptiveConfig.quality == VisualQuality.HIGH) 3.5f else 1.8f
        touchParallaxY = normalizedY * density * if (adaptiveConfig.quality == VisualQuality.HIGH) 2.5f else 1.2f
        translationZ = (if (adaptiveConfig.quality == VisualQuality.HIGH) 4f else 2f) * density
        invalidate()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun settleToRest() {
        touchParallaxX = 0f
        touchParallaxY = 0f
        if (!adaptiveConfig.motionAllowed) {
            rotationX = 0f
            rotationY = 0f
            scaleX = 1f
            scaleY = 1f
            translationZ = 0f
            invalidate()
            return
        }

        animate()
            .rotationX(0f)
            .rotationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .translationX(0f)
            .translationY(0f)
            .translationZ(0f)
            .setDuration(360L)
            .setInterpolator(DecelerateInterpolator(2f))
            .withEndAction { invalidate() }
            .start()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyAdaptivePolicy()
        playEntrance()
        startFloatingIfAllowed()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE && isAttachedToWindow) {
            applyAdaptivePolicy()
            startFloatingIfAllowed()
        } else {
            stopFloating()
        }
    }

    override fun onDetachedFromWindow() {
        animate().cancel()
        stopFloating()
        super.onDetachedFromWindow()
    }

    private fun applyAdaptivePolicy() {
        val profile = Adaptive3DPolicy.inspect(context, isHardwareAccelerated)
        adaptiveConfig = Adaptive3DPolicy.configure(profile, requestedQuality).let { config ->
            if (adaptiveMotionEnabled) config else config.copy(motionAllowed = false)
        }
        elevation = when (adaptiveConfig.quality) {
            VisualQuality.HIGH -> 14f * density
            VisualQuality.MEDIUM -> 10f * density
            VisualQuality.LOW -> 6f * density
        }
        setLayerType(
            if (adaptiveConfig.motionAllowed) LAYER_TYPE_HARDWARE else LAYER_TYPE_NONE,
            null
        )
        contentDescription = context.getString(
            when (adaptiveConfig.quality) {
                VisualQuality.HIGH -> R.string.dashboard_orb_description_high
                VisualQuality.MEDIUM -> R.string.dashboard_orb_description_medium
                VisualQuality.LOW -> R.string.dashboard_orb_description_low
            }
        )
        if (!adaptiveConfig.motionAllowed) {
            phase = 0f
            rotation = 0f
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
            translationY = 0f
            return
        }

        alpha = 0f
        scaleX = .82f
        scaleY = .82f
        translationY = 10f * density
        animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setStartDelay(180L)
            .setDuration(if (adaptiveConfig.quality == VisualQuality.HIGH) 680L else 520L)
            .setInterpolator(DecelerateInterpolator(1.7f))
            .start()
    }

    private fun startFloatingIfAllowed() {
        if (!adaptiveConfig.motionAllowed || adaptiveConfig.quality == VisualQuality.LOW || floatAnimator != null) return
        lastRenderedAtMs = 0L
        floatAnimator = ValueAnimator.ofFloat(0f, (PI * 2).toFloat()).apply {
            duration = if (adaptiveConfig.quality == VisualQuality.HIGH) 10_500L else 14_000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val now = SystemClock.uptimeMillis()
                val frameInterval = 1000L / adaptiveConfig.maxFramesPerSecond.coerceAtLeast(1)
                if (now - lastRenderedAtMs >= frameInterval) {
                    phase = animator.animatedValue as Float
                    rotation = sin(phase.toDouble()).toFloat() * adaptiveConfig.maxRotationZ
                    lastRenderedAtMs = now
                    invalidate()
                }
            }
            start()
        }
    }

    private fun stopFloating() {
        floatAnimator?.cancel()
        floatAnimator = null
    }

    private companion object {
        fun mediumStaticConfig() = Adaptive3DConfig(
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
