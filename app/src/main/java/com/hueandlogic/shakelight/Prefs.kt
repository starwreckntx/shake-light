package com.hueandlogic.shakelight

import android.content.Context

object Prefs {
    private const val FILE = "shakelight"
    private const val KEY_SENS = "sensitivity"

    /** Force per shake (peak g) for each level. Raised for the 3-hard-shake gesture.
     *  Walking peaks around 1.3-1.8 g; a deliberate hard shake runs 3-5 g. */
    val LEVELS = linkedMapOf("Gentle" to 2.6f, "Normal" to 3.2f, "Firm" to 3.8f)

    fun sensitivity(ctx: Context): String =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_SENS, "Normal") ?: "Normal"

    fun setSensitivity(ctx: Context, name: String) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY_SENS, name).apply()

    fun thresholdG(ctx: Context): Float = LEVELS[sensitivity(ctx)] ?: 3.2f
}
