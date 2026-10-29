package com.example.waktusolat

data class ThemeColors(
    val name: String,
    val bg: Int,
    val card: Int,
    val accent: Int,
    val lightAccent: Int,
    val fab: Int,
    val gradientColors: IntArray? = null
)

object ThemeConfig {
    val themes = listOf(
        ThemeColors("Midnight", 0xFF667eea.toInt(), 0xFF4D000000.toInt(), 0xFF00D4AA.toInt(), 0xFF00F5C8.toInt(), 0xFF00FFD4.toInt(), intArrayOf(0xFF667eea.toInt(), 0xFF764ba2.toInt())),
        ThemeColors("Ocean", 0xFF4facfe.toInt(), 0xFF4D000000.toInt(), 0xFF00B4D8.toInt(), 0xFF00D4FF.toInt(), 0xFF00E5FF.toInt(), intArrayOf(0xFF4facfe.toInt(), 0xFF00f2fe.toInt())),
        ThemeColors("Royal", 0xFFa18cd1.toInt(), 0xFF4D000000.toInt(), 0xFFA855F7.toInt(), 0xFFC084FC.toInt(), 0xFFD8B4FE.toInt(), intArrayOf(0xFFa18cd1.toInt(), 0xFFfbc2eb.toInt())),
        ThemeColors("Forest", 0xFF84fab0.toInt(), 0xFF4D000000.toInt(), 0xFF10B981.toInt(), 0xFF34D399.toInt(), 0xFF6EE7B7.toInt(), intArrayOf(0xFF84fab0.toInt(), 0xFF8fd3f4.toInt())),
        ThemeColors("Ember", 0xFFfa709a.toInt(), 0xFF4D000000.toInt(), 0xFFF97316.toInt(), 0xFFFB923C.toInt(), 0xFFFDBA74.toInt(), intArrayOf(0xFFfa709a.toInt(), 0xFFfee140.toInt())),
        ThemeColors("Crimson", 0xFFff9a9e.toInt(), 0xFF4D000000.toInt(), 0xFFEF4444.toInt(), 0xFFF87171.toInt(), 0xFFFCA5A5.toInt(), intArrayOf(0xFFff9a9e.toInt(), 0xFFfecfef.toInt())),
        ThemeColors("Pelangi", 0xFF1A0033.toInt(), 0xFF4D000000.toInt(), 0xFFFFC107.toInt(), 0xFFFFD54F.toInt(), 0xFFFF6F00.toInt())
    )

    val rainbowGradient = intArrayOf(
        0xFFFFAB91.toInt(), 0xFFFFCC80.toInt(), 0xFFFFF59D.toInt(),
        0xFFA5D6A7.toInt(), 0xFF90CAF9.toInt(), 0xFFCE93D8.toInt(), 0xFFF48FB1.toInt()
    )

    const val CARD_ALPHA = 30
    const val FAB_ALPHA = 0x66

    const val RAINBOW_INDEX = 6
    const val PREFS_KEY = "theme_prefs"
    const val PREFS_THEME_INDEX = "theme_index"
}
