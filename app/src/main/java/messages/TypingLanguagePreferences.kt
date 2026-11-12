package com.chatforia.android.messages

import android.content.Context
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import com.chatforia.android.chats.ConversationDto
import com.chatforia.android.auth.LANGUAGES

/** Device-local keyboard hints. These do not change the app or translation language. */
object TypingLanguagePreferences {
    private const val PREFS = "chatforia_typing_language_v1"
    private val supportedCodes = LANGUAGES.associate { it.code.lowercase(java.util.Locale.ROOT) to it.code }

    private fun canonicalCode(value: String?): String? = value
        ?.trim()
        ?.replace('_', '-')
        ?.lowercase(java.util.Locale.ROOT)
        ?.let(supportedCodes::get)

    private fun key(userId: Int?, conversation: ConversationDto): String? {
        val id = conversation.id ?: return null
        if (userId == null || userId <= 0 || id <= 0) return null
        return "$userId:${conversation.kind}:$id"
    }

    fun read(context: Context, userId: Int?, conversation: ConversationDto): String? {
        val key = key(userId, conversation) ?: return null
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key, null)
            .let(::canonicalCode)
    }

    fun save(context: Context, userId: Int?, conversation: ConversationDto, languageTag: String?) {
        val key = key(userId, conversation) ?: return
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (languageTag == null) editor.remove(key)
        else {
            val code = canonicalCode(languageTag) ?: return
            editor.putString(key, code)
        }
        editor.apply()
    }

    fun hintLocales(languageTag: String?): LocaleList? {
        val code = canonicalCode(languageTag) ?: return null
        return when (code) {
            // Preserve specified scripts/dialects and match the app's Bokmål
            // mapping. Include legacy region/generic tags for compatible IMEs.
            "zh-CN" -> LocaleList(Locale("zh-Hans-CN"), Locale("zh-CN"))
            "zh-TW" -> LocaleList(Locale("zh-Hant-TW"), Locale("zh-TW"))
            "ku" -> LocaleList(Locale("kmr"), Locale("ku"))
            "no" -> LocaleList(Locale("nb"), Locale("no"))
            else -> LocaleList(Locale(code))
        }
    }
}
