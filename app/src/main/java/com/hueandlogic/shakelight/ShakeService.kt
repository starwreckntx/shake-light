package com.hueandlogic.shakelight

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Foreground service: keeps the accelerometer listener alive while the app is in the
 * background or the screen is off. A partial wake lock keeps the CPU awake so sensor
 * events keep arriving on Samsung devices with the screen off.
 */
class ShakeService : Service() {

    companion object {
        const val ACTION_START = "com.hueandlogic.shakelight.START"
        const val ACTION_STOP = "com.hueandlogic.shakelight.STOP"
        const val ACTION_TOGGLE = "com.hueandlogic.shakelight.TOGGLE"
        private const val CHANNEL = "shake_light"
        private const val NOTIF_ID = 1
        @Volatile var running = false
            private set

        fun start(ctx: Context) {
            val i = Intent(ctx, ShakeService::class.java).setAction(ACTION_START)
            ctx.startForegroundService(i)
        }

        fun stop(ctx: Context) {
            ctx.startService(Intent(ctx, ShakeService::class.java).setAction(ACTION_STOP))
        }
    }

    private lateinit var torch: Torch
    private lateinit var sensors: SensorManager
    private var detector: ShakeDetector? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        torch = Torch(this)
        torch.start()
        sensors = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                shutdown()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE -> {
                if (!running) goForeground() // keep the FGS contract if started via notification
                torch.toggle()
                buzz()
                return START_STICKY
            }
            else -> goForeground()
        }
        return START_STICKY
    }

    private fun goForeground() {
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
        if (running) {
            // Restart the listener so a changed sensitivity takes effect.
            detector?.let { sensors.unregisterListener(it) }
        }
        val accel = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val d = ShakeDetector(Prefs.thresholdG(this), onShake = {
            torch.toggle()
            buzz()
        })
        detector = d
        if (accel != null) {
            sensors.registerListener(d, accel, SensorManager.SENSOR_DELAY_GAME)
        }
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ShakeLight::sensor").apply {
                setReferenceCounted(false)
                acquire()
            }
        }
        running = true
    }

    private fun shutdown() {
        detector?.let { sensors.unregisterListener(it) }
        detector = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        running = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        detector?.let { sensors.unregisterListener(it) }
        wakeLock?.let { if (it.isHeld) it.release() }
        torch.stop()
        running = false
        super.onDestroy()
    }

    /** Short haptic tick so you can feel that the shake registered, even in a pocket. */
    private fun buzz() {
        val vib: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        try {
            vib?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: SecurityException) {
            // Haptics are a nicety; never let them break the toggle.
        }
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CHANNEL, "Shake Light", NotificationManager.IMPORTANCE_LOW)
        ch.description = "Shows while shake-to-flashlight is listening"
        ch.setShowBadge(false)
        nm.createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), piFlags
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, ShakeService::class.java).setAction(ACTION_STOP), piFlags
        )
        val toggle = PendingIntent.getService(
            this, 2, Intent(this, ShakeService::class.java).setAction(ACTION_TOGGLE), piFlags
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Shake Light is on")
            .setContentText("Shake hard 3 times to toggle the flashlight (${Prefs.sensitivity(this)})")
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Toggle light", toggle).build())
            .addAction(Notification.Action.Builder(null, "Stop", stop).build())
            .build()
    }
}
