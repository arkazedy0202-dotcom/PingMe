package com.example.pingme.ui

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager

enum class VisualQuality {
    HIGH,
    MEDIUM,
    LOW
}

data class Device3DProfile(
    val lowRamDevice: Boolean,
    val sdkInt: Int,
    val memoryClassMb: Int,
    val totalRamMb: Long,
    val smallestWidthDp: Int,
    val hardwareAccelerated: Boolean,
    val powerSaveMode: Boolean,
    val animatorsEnabled: Boolean
)

data class Adaptive3DConfig(
    val quality: VisualQuality,
    val motionAllowed: Boolean,
    val maxFramesPerSecond: Int,
    val particleCount: Int,
    val floatAmplitudeDp: Float,
    val maxTiltX: Float,
    val maxTiltY: Float,
    val maxRotationZ: Float,
    val reflectionEnabled: Boolean
)

/**
 * Resolves a conservative visual tier. Unknown device data intentionally lands on Medium.
 * This policy is UI-only and has no dependency on application state or backend data.
 */
object Adaptive3DPolicy {

    fun resolve(profile: Device3DProfile): VisualQuality {
        val knownLowMemory = profile.memoryClassMb in 1..127 || profile.totalRamMb in 1..2499
        val constrainedEnvironment = profile.lowRamDevice ||
            !profile.hardwareAccelerated ||
            profile.powerSaveMode ||
            !profile.animatorsEnabled ||
            profile.sdkInt < Build.VERSION_CODES.O ||
            profile.smallestWidthDp in 1..319 ||
            knownLowMemory

        if (constrainedEnvironment) return VisualQuality.LOW

        val enoughTotalRam = profile.totalRamMb == 0L || profile.totalRamMb >= 5500L
        val highCapability = profile.sdkInt >= Build.VERSION_CODES.Q &&
            profile.memoryClassMb >= 256 &&
            enoughTotalRam &&
            profile.smallestWidthDp >= 360

        return if (highCapability) VisualQuality.HIGH else VisualQuality.MEDIUM
    }

    fun inspect(context: Context, hardwareAccelerated: Boolean): Device3DProfile {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)
            val configuration = context.resources.configuration
            val smallestWidth = configuration.smallestScreenWidthDp.takeIf { it > 0 }
                ?: minOf(configuration.screenWidthDp, configuration.screenHeightDp)

            Device3DProfile(
                lowRamDevice = activityManager?.isLowRamDevice ?: false,
                sdkInt = Build.VERSION.SDK_INT,
                memoryClassMb = activityManager?.memoryClass ?: 192,
                totalRamMb = memoryInfo.totalMem.takeIf { it > 0L }?.div(1024L * 1024L) ?: 0L,
                smallestWidthDp = smallestWidth.takeIf { it > 0 } ?: 360,
                hardwareAccelerated = hardwareAccelerated,
                powerSaveMode = powerManager?.isPowerSaveMode ?: false,
                animatorsEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()
            )
        } catch (_: Throwable) {
            Device3DProfile(
                lowRamDevice = false,
                sdkInt = Build.VERSION_CODES.P,
                memoryClassMb = 192,
                totalRamMb = 4096L,
                smallestWidthDp = 360,
                hardwareAccelerated = hardwareAccelerated,
                powerSaveMode = false,
                animatorsEnabled = true
            )
        }
    }

    fun configure(profile: Device3DProfile, requestedQuality: VisualQuality? = null): Adaptive3DConfig {
        val detected = resolve(profile)
        val quality = when {
            detected == VisualQuality.LOW -> VisualQuality.LOW
            requestedQuality == null -> detected
            detected == VisualQuality.MEDIUM && requestedQuality == VisualQuality.HIGH -> VisualQuality.MEDIUM
            else -> requestedQuality
        }
        val motionAllowed = profile.hardwareAccelerated &&
            !profile.powerSaveMode &&
            profile.animatorsEnabled

        return when (quality) {
            VisualQuality.HIGH -> Adaptive3DConfig(
                quality = quality,
                motionAllowed = motionAllowed,
                maxFramesPerSecond = 60,
                particleCount = 6,
                floatAmplitudeDp = 6f,
                maxTiltX = 8f,
                maxTiltY = 10f,
                maxRotationZ = 1.6f,
                reflectionEnabled = true
            )
            VisualQuality.MEDIUM -> Adaptive3DConfig(
                quality = quality,
                motionAllowed = motionAllowed,
                maxFramesPerSecond = 30,
                particleCount = 0,
                floatAmplitudeDp = 3.5f,
                maxTiltX = 4f,
                maxTiltY = 5f,
                maxRotationZ = 0.65f,
                reflectionEnabled = false
            )
            VisualQuality.LOW -> Adaptive3DConfig(
                quality = quality,
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
}
