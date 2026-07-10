package tv.own.owntv.core.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    private const val KEY_LANGUAGE = "app_language"

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
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = Configuration(activity.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
    }
}
