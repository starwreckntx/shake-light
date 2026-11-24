package com.hueandlogic.shakelight

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Fires onShake() after [peaksNeeded] acceleration peaks above [thresholdG] land
 * inside [windowMs]. Two quick jolts are needed, so a bump in a pocket or a dropped
 * phone on a table does not flip the light. A [cooldownMs] debounce follows each trigger.
 */
class ShakeDetector(
    private val thresholdG: Float,
    private val onShake: () -> Unit,
    private val peaksNeeded: Int = 2,
    private val windowMs: Long = 800,
    private val minPeakGapMs: Long = 120,
    private val cooldownMs: Long = 1200,
) : SensorEventListener {

    private val peaks = ArrayDeque<Long>()
    private var lastTriggerMs = 0L

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val gx = event.values[0] / SensorManager.GRAVITY_EARTH
        val gy = event.values[1] / SensorManager.GRAVITY_EARTH
        val gz = event.values[2] / SensorManager.GRAVITY_EARTH
        val g = sqrt(gx * gx + gy * gy + gz * gz)
        if (g < thresholdG) return

        val now = event.timestamp / 1_000_000L // ns -> ms (monotonic sensor clock)
        if (now - lastTriggerMs < cooldownMs) return
        if (peaks.isNotEmpty() && now - peaks.last() < minPeakGapMs) return // same jolt

        peaks.addLast(now)
        while (peaks.isNotEmpty() && now - peaks.first() > windowMs) peaks.removeFirst()

        if (peaks.size >= peaksNeeded) {
            peaks.clear()
            lastTriggerMs = now
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
