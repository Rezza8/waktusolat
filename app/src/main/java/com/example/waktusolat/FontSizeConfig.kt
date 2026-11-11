package com.example.waktusolat

import android.content.Context
import android.content.res.Configuration

object FontSizeConfig {

    const val PREFS_NAME = "prefs"
    const val PREF_INDEX = "font_scale_index"
    const val DEFAULT_INDEX = 2

    val scales = floatArrayOf(0.8f, 0.9f, 1f, 1.15f, 1.3f)
    private val labels = arrayOf("Kecil", "Sederhana", "Biasa", "Besar", "Sangat Besar")

    fun savedIndex(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(PREF_INDEX, DEFAULT_INDEX)
            .coerceIn(0, scales.size - 1)

    fun label(index: Int): String {
        val safe = index.coerceIn(0, scales.size - 1)
        val percent = Math.round(scales[safe] * 100)
        return "${labels[safe]} ($percent%)"
    }

    /** Rebuilds the context with the saved font scale so every sp value follows it. */
    fun wrap(base: Context): Context {
        val config = Configuration(base.resources.configuration)
        config.fontScale = scales[savedIndex(base)]
        return base.createConfigurationContext(config)
    }
}
