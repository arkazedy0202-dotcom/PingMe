package com.example.pingme.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class Adaptive3DPolicyTest {

    @Test
    fun strongDeviceUsesHighQuality() {
        val profile = Device3DProfile(
            lowRamDevice = false,
            sdkInt = 34,
            memoryClassMb = 512,
            totalRamMb = 8192,
            smallestWidthDp = 411,
            hardwareAccelerated = true,
            powerSaveMode = false,
            animatorsEnabled = true
        )

        val config = Adaptive3DPolicy.configure(profile)
        assertEquals(VisualQuality.HIGH, config.quality)
        assertEquals(60, config.maxFramesPerSecond)
        assertEquals(6, config.particleCount)
    }

    @Test
    fun balancedDeviceUsesMediumQuality() {
        val profile = Device3DProfile(
            lowRamDevice = false,
            sdkInt = 28,
            memoryClassMb = 192,
            totalRamMb = 4096,
            smallestWidthDp = 360,
            hardwareAccelerated = true,
            powerSaveMode = false,
            animatorsEnabled = true
        )

        val config = Adaptive3DPolicy.configure(profile)
        assertEquals(VisualQuality.MEDIUM, config.quality)
        assertEquals(30, config.maxFramesPerSecond)
        assertEquals(0, config.particleCount)

        val unsafeOverride = Adaptive3DPolicy.configure(profile, VisualQuality.HIGH)
        assertEquals(VisualQuality.MEDIUM, unsafeOverride.quality)
    }

    @Test
    fun lowRamOrPowerSaverUsesStaticLowQuality() {
        val profile = Device3DProfile(
            lowRamDevice = true,
            sdkInt = 34,
            memoryClassMb = 96,
            totalRamMb = 2048,
            smallestWidthDp = 320,
            hardwareAccelerated = true,
            powerSaveMode = true,
            animatorsEnabled = true
        )

        val config = Adaptive3DPolicy.configure(profile)
        assertEquals(VisualQuality.LOW, config.quality)
        assertFalse(config.motionAllowed)
        assertEquals(0, config.particleCount)
        assertEquals(0, config.maxFramesPerSecond)
    }
}
