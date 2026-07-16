package com.lunaiptv.core.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    private const val KEY_LANGUAGE = "app_language"
    private const val PREFS_NAME = "lunaiptv_locale"

    fun applyLanguage(context: Context, langCode: String): Context {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(config)
    }

    fun applyLanguageToActivity(activity: Activity, langCode: String) {
        val prefs: SharedPreferences = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLang = prefs.getString(KEY_LANGUAGE, null)

        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = Configuration(activity.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        activity.resources.updateConfiguration(config, activity.resources.displayMetrics)

        if (savedLang != langCode) {
            prefs.edit().putString(KEY_LANGUAGE, langCode).apply()
            activity.recreate()
        }
    }
}
