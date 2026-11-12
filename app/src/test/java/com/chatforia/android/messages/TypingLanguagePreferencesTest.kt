package com.chatforia.android.messages

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.chatforia.android.chats.ConversationDto
import com.chatforia.android.auth.AppLocaleManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TypingLanguagePreferencesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun chat(id: Int?) = ConversationDto(kind = "chat", id = id, title = "Test")
    private fun sms(id: Int?) = ConversationDto(kind = "sms", id = id, title = "Test")

    @Test fun preferenceIsSeparateByAccountTransportAndConversation() {
        TypingLanguagePreferences.save(context, 101, chat(42), "es")
        assertEquals("es", TypingLanguagePreferences.read(context, 101, chat(42)))
        assertNull(TypingLanguagePreferences.read(context, 102, chat(42)))
        assertNull(TypingLanguagePreferences.read(context, 101, chat(43)))
        assertNull(TypingLanguagePreferences.read(context, 101, sms(42)))

        TypingLanguagePreferences.save(context, 101, sms(42), "ja")
        assertEquals("ja", TypingLanguagePreferences.read(context, 101, sms(42)))
        assertEquals("es", TypingLanguagePreferences.read(context, 101, chat(42)))
    }

    @Test fun manualSystemOverrideClearsHintWithoutChangingOtherConversations() {
        TypingLanguagePreferences.save(context, 201, chat(51), "fr")
        TypingLanguagePreferences.save(context, 201, chat(52), "de")
        assertEquals("fr", TypingLanguagePreferences.hintLocales(
            TypingLanguagePreferences.read(context, 201, chat(51)))?.get(0)?.toLanguageTag())
        TypingLanguagePreferences.save(context, 201, chat(51), null)
        assertNull(TypingLanguagePreferences.read(context, 201, chat(51)))
        assertNull(TypingLanguagePreferences.hintLocales(null))
        assertEquals("de", TypingLanguagePreferences.read(context, 201, chat(52)))
    }

    @Test fun unsavedThreadsAndInvalidTagsNeverIssueHint() {
        TypingLanguagePreferences.save(context, 301, chat(null), "es")
        TypingLanguagePreferences.save(context, null, chat(1), "es")
        TypingLanguagePreferences.save(context, 301, chat(1), "not a locale")
        assertNull(TypingLanguagePreferences.read(context, 301, chat(null)))
        assertNull(TypingLanguagePreferences.read(context, null, chat(1)))
        assertNull(TypingLanguagePreferences.read(context, 301, chat(1)))
        assertNull(TypingLanguagePreferences.hintLocales("not a locale"))
    }

    @Test fun typingHintDoesNotChangeAppLanguage() {
        AppLocaleManager.saveLanguage(context, "fr")
        TypingLanguagePreferences.save(context, 401, chat(9), "ja")
        assertEquals("fr", AppLocaleManager.readLanguage(context))
        assertEquals("ja", TypingLanguagePreferences.read(context, 401, chat(9)))
    }
}
