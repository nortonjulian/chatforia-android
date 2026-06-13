package com.chatforia.android.auth

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object AppLocaleManager {
    fun applyLanguage(
        context: Context,
        languageCode: String?
    ) {
        val code = languageCode
            ?.takeIf { it.isNotBlank() }
            ?: "en"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager =
                context.getSystemService(LocaleManager::class.java)

            localeManager.applicationLocales =
                LocaleList.forLanguageTags(toAndroidLanguageTag(code))
        } else {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(toAndroidLanguageTag(code))
            )
        }
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