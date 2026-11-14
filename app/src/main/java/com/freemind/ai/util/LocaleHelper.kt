package com.freemind.ai.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/** The languages the app is translated into. [tag] is a BCP-47 tag; null follows the system. */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    ARABIC("ar"),
    FRENCH("fr"),
}

private const val PREFS = "freemind_locale"
private const val KEY_LANGUAGE = "language"

/**
 * Per-app language. On Android 13+ the system owns this (it is also what the system's
 * own "App languages" screen edits), so it is delegated to [LocaleManager]. Earlier
 * versions have no such API, so the choice is kept in SharedPreferences - read
 * synchronously, because it is needed in `attachBaseContext` before anything else
 * runs - and applied by wrapping the context's configuration.
 */
object LocaleHelper {

    /** The language currently chosen, [AppLanguage.SYSTEM] if none. */
    fun current(context: Context): AppLanguage {
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales?.takeIf { !it.isEmpty }?.get(0)?.language
        } else {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, null)
        }
        return AppLanguage.entries.firstOrNull { it.tag != null && it.tag == tag } ?: AppLanguage.SYSTEM
    }

    /** Saves the choice. The caller recreates the activity (pre-13) - the system does it on 13+. */
    fun set(context: Context, language: AppLanguage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (language.tag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(language.tag)
        } else {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
                if (language.tag == null) remove(KEY_LANGUAGE) else putString(KEY_LANGUAGE, language.tag)
            }.apply()
        }
    }

    /**
     * A context whose resources use the chosen language. A no-op on Android 13+, where the
     * system already applies it, and when the language is "follow the system".
     */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, null) ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(config)
    }
}
