package com.hueandlogic.shakelight

import android.content.Context

object Prefs {
    private const val FILE = "shakelight"
    private const val KEY_SENS = "sensitivity"

    /** Sensitivity levels -> peak g-force needed. Lower g = easier to trigger. */
    val LEVELS = linkedMapOf("Gentle" to 2.0f, "Normal" to 2.6f, "Firm" to 3.2f)

    fun sensitivity(ctx: Context): String =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_SENS, "Normal") ?: "Normal"

    fun setSensitivity(ctx: Context, name: String) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY_SENS, name).apply()

    fun thresholdG(ctx: Context): Float = LEVELS[sensitivity(ctx)] ?: 2.6f
}
