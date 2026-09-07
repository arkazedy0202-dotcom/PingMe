package com.example.pingme.ui

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar

/** Animates only real progress values supplied by the existing HomeFragment. */
class AdaptiveProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.progressBarStyleHorizontal
) : ProgressBar(context, attrs, defStyleAttr) {

    private var progressAnimator: ValueAnimator? = null
    private var adaptiveConfig: Adaptive3DConfig? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        adaptiveConfig = Adaptive3DPolicy.configure(
            Adaptive3DPolicy.inspect(context, isHardwareAccelerated)
        )
    }

    override fun setProgress(progress: Int) {
        val config = adaptiveConfig
        if (!isAttachedToWindow || config?.motionAllowed != true || progress == super.getProgress()) {
            progressAnimator?.cancel()
            super.setProgress(progress)
            return
        }

        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofInt(super.getProgress(), progress).apply {
            duration = if (config.quality == VisualQuality.HIGH) 620L else 420L
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener { super@AdaptiveProgressBar.setProgress(it.animatedValue as Int) }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        progressAnimator?.cancel()
        progressAnimator = null
        adaptiveConfig = null
        super.onDetachedFromWindow()
    }
}
