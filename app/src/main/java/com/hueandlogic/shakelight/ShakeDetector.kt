package com.hueandlogic.shakelight

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Intent gesture: [SHAKES_NEEDED] hard jolts above [thresholdG] inside [WINDOW_MS] toggle the
 * light (same gesture turns it on and off). Walking, a pocket bump or a dropped phone gives one
 * or two spikes, not three hard ones in a row, so it does not trigger.
 *
 * Tuning knobs (all here):
 *  - SHAKES_NEEDED: how many hard shakes make the gesture.
 *  - WINDOW_MS: all shakes must land inside this time.
 *  - MIN_SHAKE_GAP_MS: spikes closer than this count as one shake (a single flick
 *    produces a forward and a rebound spike ~100-200 ms apart).
 *  - COOLDOWN_MS: ignore everything for this long after a trigger.
 *  - Force thresholds live in Prefs.LEVELS.
 */
class ShakeDetector(
    private val thresholdG: Float,
    private val onShake: () -> Unit,
) : SensorEventListener {

    companion object {
        const val SHAKES_NEEDED = 3
        const val WINDOW_MS = 1500L
        const val MIN_SHAKE_GAP_MS = 250L
        const val COOLDOWN_MS = 1500L
    }

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
        if (now - lastTriggerMs < COOLDOWN_MS) return
        if (peaks.isNotEmpty() && now - peaks.last() < MIN_SHAKE_GAP_MS) return // same shake

        peaks.addLast(now)
        while (peaks.isNotEmpty() && now - peaks.first() > WINDOW_MS) peaks.removeFirst()

        if (peaks.size >= SHAKES_NEEDED) {
            peaks.clear()
            lastTriggerMs = now
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
