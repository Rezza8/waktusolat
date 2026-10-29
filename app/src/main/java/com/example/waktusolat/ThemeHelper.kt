package com.example.waktusolat

import android.graphics.Color

object ThemeHelper {
    fun isDarkTheme(themeIndex: Int): Boolean = true

    fun getPrimaryTextColor(themeIndex: Int): Int = Color.WHITE

    fun getSecondaryTextColor(themeIndex: Int): Int = Color.argb(180, 255, 255, 255)

    fun getTertiaryTextColor(themeIndex: Int): Int = Color.argb(120, 255, 255, 255)

    fun getCardBackgroundColor(themeIndex: Int): Int = 0x4D000000.toInt()

    fun getDividerColor(themeIndex: Int): Int = 0x4DFFFFFF.toInt()

    fun getAccentColor(themeIndex: Int): Int =
        ThemeConfig.themes[themeIndex].accent

    fun getLightAccentColor(themeIndex: Int): Int =
        ThemeConfig.themes[themeIndex].lightAccent
}
