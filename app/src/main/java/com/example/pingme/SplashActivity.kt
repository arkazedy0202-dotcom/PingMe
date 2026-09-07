package com.example.pingme

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.ivLogo)
        val tagline = findViewById<TextView>(R.id.tvTagline)
        val subTagline = findViewById<TextView>(R.id.tvSubTagline)

        val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
        val motionEnabled = powerManager?.isPowerSaveMode != true &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled())

        val gentleInterpolator = DecelerateInterpolator(1.2f)

        if (motionEnabled) {
            logo.translationY = 42f
            logo.scaleX = 0.92f
            logo.scaleY = 0.92f
            logo.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(900)
                .setInterpolator(gentleInterpolator)
                .start()

            tagline.translationY = 20f
            tagline.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .setStartDelay(260)
                .setInterpolator(gentleInterpolator)
                .start()

            subTagline.translationY = 14f
            subTagline.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(650)
                .setStartDelay(430)
                .setInterpolator(gentleInterpolator)
                .start()
        } else {
            logo.alpha = 1f
            tagline.alpha = 1f
            subTagline.alpha = 1f
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // Transisi pergantian layar (fade) bawaan Android
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, if (motionEnabled) 1800L else 450L)
    }
}
