package com.example.pingme.ui

import android.content.Context
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout

/** A self-contained, accessibility-aware dashboard header entrance. */
class AdaptiveHeaderLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var entrancePlayed = false

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (entrancePlayed) return
        entrancePlayed = true

        val profile = Adaptive3DPolicy.inspect(context, isHardwareAccelerated)
        val config = Adaptive3DPolicy.configure(profile)
        if (!config.motionAllowed) {
            alpha = 1f
            translationY = 0f
            return
        }

        alpha = 0f
        translationY = -12f * resources.displayMetrics.density
        animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(if (config.quality == VisualQuality.LOW) 220L else 420L)
            .setInterpolator(DecelerateInterpolator(1.8f))
            .start()
    }
}
