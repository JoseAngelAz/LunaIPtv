package com.lunaiptv.phone.di

import android.content.Context
import android.content.SharedPreferences

class PhoneEngineStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("phone_vod_engine", Context.MODE_PRIVATE)

    enum class Engine { EXO, MPV }

    fun getEngineForUrl(url: String): Engine {
        val pinned = prefs.getString(url, null)
        return when (pinned) {
            "mpv" -> Engine.MPV
            "exo" -> Engine.EXO
            else -> Engine.MPV
        }
    }

    fun pinEngine(url: String, engine: Engine) {
        prefs.edit().putString(url, engine.name.lowercase()).apply()
    }

    fun clearPin(url: String) {
        prefs.edit().remove(url).apply()
    }
}
