package com.lunaiptv.ui.theme

enum class AnimationLevel(val label: String) {
    FULL("On"), OFF("Off");

    fun scale(durationMs: Int): Int = when (this) {
        FULL -> durationMs
        OFF -> 0
    }
}
