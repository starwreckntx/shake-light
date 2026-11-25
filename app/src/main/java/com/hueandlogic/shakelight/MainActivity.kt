package com.hueandlogic.shakelight

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView

/** One screen, built in code (no layout XML, no AndroidX). */
class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var startStop: Button
    private lateinit var torch: Torch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        torch = Torch(this)
        val pad = (20 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "Shake Light"
            textSize = 28f
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Shake the phone hard 3 times within about 1.5 seconds to turn the flashlight on or off. " +
                "Works with the app closed and the screen off while the notification is showing."
            textSize = 15f
            setPadding(0, pad / 2, 0, pad)
        })

        status = TextView(this).apply { textSize = 16f; setPadding(0, 0, 0, pad / 2) }
        root.addView(status)

        startStop = Button(this).apply {
            setOnClickListener {
                if (ShakeService.running) ShakeService.stop(this@MainActivity)
                else ShakeService.start(this@MainActivity)
                it.postDelayed({ refresh() }, 300)
            }
        }
        root.addView(startStop)

        root.addView(Button(this).apply {
            text = "Toggle flashlight now (test)"
            setOnClickListener { torch.toggle() }
        })

        root.addView(TextView(this).apply {
            text = "Shake strength needed"
            textSize = 16f
            setPadding(0, pad, 0, 0)
        })
        val group = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        val current = Prefs.sensitivity(this)
        Prefs.LEVELS.keys.forEachIndexed { i, name ->
            group.addView(RadioButton(this).apply {
                id = 1000 + i
                text = name
                isChecked = name == current
            })
        }
        group.setOnCheckedChangeListener { _, checkedId ->
            val name = Prefs.LEVELS.keys.elementAt(checkedId - 1000)
            Prefs.setSensitivity(this, name)
            if (ShakeService.running) ShakeService.start(this) // re-arm with new threshold
        }
        root.addView(group)

        root.addView(Button(this).apply {
            text = "Keep running with screen off (battery settings)"
            setOnClickListener { openBatterySettings() }
        })
        root.addView(TextView(this).apply {
            text = "Samsung tip: Settings > Apps > Shake Light > Battery > Unrestricted, " +
                "so the phone doesn't put it to sleep."
            textSize = 13f
            gravity = Gravity.START
            setPadding(0, pad / 2, 0, 0)
        })

        setContentView(root)

        if (torch.cameraId == null) {
            status.text = "This phone reports no flashlight."
        }
        askNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        if (torch.cameraId == null) return
        val on = ShakeService.running
        status.text = if (on) "Listening for shakes." else "Not listening."
        startStop.text = if (on) "Stop shake detection" else "Start shake detection"
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun openBatterySettings() {
        val pm = getSystemService(PowerManager::class.java)
        val intent = if (pm.isIgnoringBatteryOptimizations(packageName)) {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        } else {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        }
        try { startActivity(intent) } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }
}
