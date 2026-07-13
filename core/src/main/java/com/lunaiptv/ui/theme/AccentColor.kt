package com.lunaiptv.ui.theme

/**
 * Material You-style accent presets. Colors stored as ARGB Int to avoid compose-ui dependency in core.
 *
 * NOTE: In Kotlin, hex literals >= 0x80000000 are Long. Since all ARGB colors with full alpha
 * (0xFF______) exceed Int.MAX_VALUE (0x7FFFFFFF), every literal needs .toInt().
 */
enum class AccentColor(
    val label: String,
    val primaryDark: Int,
    val onPrimaryDark: Int,
    val primaryContainerDark: Int,
    val onPrimaryContainerDark: Int,
    val primaryLight: Int,
    val onPrimaryLight: Int,
    val primaryContainerLight: Int,
    val onPrimaryContainerLight: Int,
) {
    TEAL(
        "Teal",
        primaryDark = 0xFF52DBC8.toInt(), onPrimaryDark = 0xFF003730.toInt(),
        primaryContainerDark = 0xFF004F46.toInt(), onPrimaryContainerDark = 0xFF6FF8E4.toInt(),
        primaryLight = 0xFF006B5E.toInt(), onPrimaryLight = 0xFFFFFFFF.toInt(),
        primaryContainerLight = 0xFF6FF8E4.toInt(), onPrimaryContainerLight = 0xFF00201B.toInt(),
    ),
    BLUE(
        "Blue",
        primaryDark = 0xFF6FB0FF.toInt(), onPrimaryDark = 0xFF00315C.toInt(),
        primaryContainerDark = 0xFF134A7C.toInt(), onPrimaryContainerDark = 0xFFD3E4FF.toInt(),
        primaryLight = 0xFF1565C0.toInt(), onPrimaryLight = 0xFFFFFFFF.toInt(),
        primaryContainerLight = 0xFFD6E3FF.toInt(), onPrimaryContainerLight = 0xFF001C3A.toInt(),
    ),
    VIOLET(
        "Violet",
        primaryDark = 0xFFCBBEFF.toInt(), onPrimaryDark = 0xFF312170.toInt(),
        primaryContainerDark = 0xFF483A88.toInt(), onPrimaryContainerDark = 0xFFE7DEFF.toInt(),
        primaryLight = 0xFF5B45C9.toInt(), onPrimaryLight = 0xFFFFFFFF.toInt(),
        primaryContainerLight = 0xFFE5DEFF.toInt(), onPrimaryContainerLight = 0xFF190066.toInt(),
    ),
    GREEN(
        "Green",
        primaryDark = 0xFF6FDB94.toInt(), onPrimaryDark = 0xFF00391C.toInt(),
        primaryContainerDark = 0xFF1F5135.toInt(), onPrimaryContainerDark = 0xFF8BF8AF.toInt(),
        primaryLight = 0xFF1B6B3F.toInt(), onPrimaryLight = 0xFFFFFFFF.toInt(),
        primaryContainerLight = 0xFFA6F2C0.toInt(), onPrimaryContainerLight = 0xFF00210F.toInt(),
    ),
    AMBER(
        "Amber",
        primaryDark = 0xFFFFB95C.toInt(), onPrimaryDark = 0xFF452B00.toInt(),
        primaryContainerDark = 0xFF624000.toInt(), onPrimaryContainerDark = 0xFFFFDDB3.toInt(),
        primaryLight = 0xFF8A5100.toInt(), onPrimaryLight = 0xFFFFFFFF.toInt(),
        primaryContainerLight = 0xFFFFDDB3.toInt(), onPrimaryContainerLight = 0xFF2C1600.toInt(),
    ),
    ROSE(
        "Rose",
        primaryDark = 0xFFFFB1C8.toInt(), onPrimaryDark = 0xFF621234.toInt(),
        primaryContainerDark = 0xFF7D2A4A.toInt(), onPrimaryContainerDark = 0xFFFFD9E5.toInt(),
        primaryLight = 0xFF9C4066.toInt(), onPrimaryLight = 0xFFFFFFFF.toInt(),
        primaryContainerLight = 0xFFFFD9E5.toInt(), onPrimaryContainerLight = 0xFF3E0021.toInt(),
    ),
    CRIMSON(
        "Crimson",
        primaryDark = 0xFFFFB4AB.toInt(), onPrimaryDark = 0xFF690005.toInt(),
        primaryContainerDark = 0xFF93000A.toInt(), onPrimaryContainerDark = 0xFFFFDAD6.toInt(),
        primaryLight = 0xFFBA1A1A.toInt(), onPrimaryLight = 0xFFFFFFFF.toInt(),
        primaryContainerLight = 0xFFFFDAD6.toInt(), onPrimaryContainerLight = 0xFF410002.toInt(),
    ),
    INDIGO(
        "Indigo",
        primaryDark = 0xFFBEC6FF.toInt(), onPrimaryDark = 0xFF271B6B.toInt(),
        primaryContainerDark = 0xFF3D3382.toInt(), onPrimaryContainerDark = 0xFFDADEFF.toInt(),
        primaryLight = 0xFF5B57A8.toInt(), onPrimaryLight = 0xFFFFFFFF.toInt(),
        primaryContainerLight = 0xFFE3DEFF.toInt(), onPrimaryContainerLight = 0xFF170062.toInt(),
    ),
    LIME(
        "Lime",
        primaryDark = 0xFFA6D46A.toInt(), onPrimaryDark = 0xFF1F3607.toInt(),
        primaryContainerDark = 0xFF364E1C.toInt(), onPrimaryContainerDark = 0xFFC1EB84.toInt(),
        primaryLight = 0xFF4D6B23.toInt(), onPrimaryLight = 0xFFFFFFFF.toInt(),
        primaryContainerLight = 0xFFC1EB84.toInt(), onPrimaryContainerLight = 0xFF0D2000.toInt(),
    ),
    ORANGE(
        "Orange",
        primaryDark = 0xFFFFB68C.toInt(), onPrimaryDark = 0xFF4F2600.toInt(),
        primaryContainerDark = 0xFF6E3A00.toInt(), onPrimaryContainerDark = 0xFFFFDCBE.toInt(),
        primaryLight = 0xFF8C4D00.toInt(), onPrimaryLight = 0xFFFFFFFF.toInt(),
        primaryContainerLight = 0xFFFFDCBE.toInt(), onPrimaryContainerLight = 0xFF2E1500.toInt(),
    );

    fun primary(isDark: Boolean) = if (isDark) primaryDark else primaryLight
    fun onPrimary(isDark: Boolean) = if (isDark) onPrimaryDark else onPrimaryLight
    fun primaryContainer(isDark: Boolean) = if (isDark) primaryContainerDark else primaryContainerLight
    fun onPrimaryContainer(isDark: Boolean) = if (isDark) onPrimaryContainerDark else onPrimaryContainerLight
}
