package com.chatforia.android.auth

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import java.util.Locale

object AppLocaleManager {
    private const val PREFS = "chatforia_locale"
    private const val KEY_LANGUAGE = "language"

    fun saveLanguage(context: Context, languageCode: String?) {
        val code = languageCode?.takeIf { it.isNotBlank() } ?: "en"

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, code)
            .commit()
    }

    fun readLanguage(context: Context): String {
        val code = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "en") ?: "en"

        return code
    }

    fun wrapContext(context: Context): Context {
        val code = readLanguage(context)
        val tag = toAndroidLanguageTag(code)
        val locale = Locale.forLanguageTag(tag)

        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }

    private fun toAndroidLanguageTag(code: String): String {
        return when (code.lowercase()) {
            "zh-cn", "zh-hans" -> "zh-Hans"
            "zh-tw", "zh-hant" -> "zh-Hant"
            "pt-br" -> "pt-BR"
            "pt-pt" -> "pt-PT"
            "fil" -> "tl"
            "no" -> "nb"
            "mni-mtei" -> "mni"
            "prs" -> "fa-AF"
            else -> code
        }
    }
}