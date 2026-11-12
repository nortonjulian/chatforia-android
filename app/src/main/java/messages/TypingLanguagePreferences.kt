package com.chatforia.android.messages

import android.content.Context
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import com.chatforia.android.chats.ConversationDto

/** Device-local keyboard hints. These do not change the app or translation language. */
object TypingLanguagePreferences {
    private const val PREFS = "chatforia_typing_language_v1"

    private fun key(userId: Int?, conversation: ConversationDto): String? {
        val id = conversation.id ?: return null
        if (userId == null || userId <= 0 || id <= 0) return null
        return "$userId:${conversation.kind}:$id"
    }

    fun read(context: Context, userId: Int?, conversation: ConversationDto): String? {
        val key = key(userId, conversation) ?: return null
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key, null)
            ?.takeIf(::isValidLanguageTag)
    }

    fun save(context: Context, userId: Int?, conversation: ConversationDto, languageTag: String?) {
        val key = key(userId, conversation) ?: return
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (languageTag == null) editor.remove(key)
        else if (isValidLanguageTag(languageTag)) editor.putString(key, languageTag)
        else return
        editor.apply()
    }

    fun hintLocales(languageTag: String?): LocaleList? =
        languageTag?.takeIf(::isValidLanguageTag)?.let { LocaleList(Locale(it)) }

    private fun isValidLanguageTag(value: String): Boolean =
        value.matches(Regex("[A-Za-z]{2,3}(-[A-Za-z]{2,8}){0,2}"))
}
