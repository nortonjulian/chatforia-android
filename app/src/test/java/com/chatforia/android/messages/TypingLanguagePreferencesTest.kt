package com.chatforia.android.messages

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.chatforia.android.chats.ConversationDto
import com.chatforia.android.auth.AppLocaleManager
import com.chatforia.android.auth.LANGUAGES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
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

    @Test fun everySupportedChoicePersistsAndProducesARepresentableImeTag() {
        assertEquals(147, LANGUAGES.size)
        assertEquals(147, LANGUAGES.map { it.code }.distinct().size)
        LANGUAGES.forEach { language ->
            TypingLanguagePreferences.save(context, 501, chat(1), language.code)
            assertEquals(language.code, TypingLanguagePreferences.read(context, 501, chat(1)))
            assertTrue(language.code, TypingLanguagePreferences.localizedName(context, language).isNotBlank())
            val hint = TypingLanguagePreferences.hintLocales(language.code)
            assertNotNull(language.code, hint)
            assertTrue(language.code, hint!!.all { it.toLanguageTag() != "und" })
        }
    }

    @Test fun writingSystemsAndScriptRegionalVariantsKeepTheirIntendedTags() {
        val expected = mapOf(
            "es" to "es", "ru" to "ru", "ar" to "ar", "hi" to "hi",
            "ja" to "ja", "ko" to "ko", "am" to "am", "he" to "he",
            "th" to "th", "mni-Mtei" to "mni-Mtei", "prs" to "prs",
            "zh-CN" to "zh-Hans-CN", "zh-TW" to "zh-Hant-TW",
            "ku" to "kmr", "no" to "nb", "pt" to "pt"
        )
        expected.forEach { (choice, nativeTag) ->
            assertEquals(choice, nativeTag,
                TypingLanguagePreferences.hintLocales(choice)!![0].toLanguageTag())
        }
        assertEquals("zh-CN", TypingLanguagePreferences.hintLocales("zh-CN")!![1].toLanguageTag())
        assertEquals("zh-TW", TypingLanguagePreferences.hintLocales("zh-TW")!![1].toLanguageTag())
    }

    @Test fun caseNormalizationAndUnsupportedCodesFallBackWithoutChangingSelection() {
        TypingLanguagePreferences.save(context, 601, chat(1), "ZH_cn")
        assertEquals("zh-CN", TypingLanguagePreferences.read(context, 601, chat(1)))
        assertEquals("mni-Mtei", TypingLanguagePreferences.hintLocales("MNI_mtei")!![0].toLanguageTag())
        TypingLanguagePreferences.save(context, 601, chat(1), "zz")
        assertEquals("zh-CN", TypingLanguagePreferences.read(context, 601, chat(1)))
        assertNull(TypingLanguagePreferences.hintLocales("zz"))
        context.getSharedPreferences("chatforia_typing_language_v1", Context.MODE_PRIVATE)
            .edit().putString("601:chat:2", "zz").commit()
        assertNull(TypingLanguagePreferences.read(context, 601, chat(2)))
    }

    @Test fun keyboardMenuUsesLocalizedNamesAndPortugueseHintStaysGeneric() {
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("es")) }
        val spanishContext = context.createConfigurationContext(config)
        val portuguese = LANGUAGES.first { it.code == "pt" }
        val kurmanji = LANGUAGES.first { it.code == "ku" }
        assertEquals("Portugués", TypingLanguagePreferences.localizedName(spanishContext, portuguese))
        assertEquals("Kurdo (Kurmanji)", TypingLanguagePreferences.localizedName(spanishContext, kurmanji))
        assertEquals("pt", TypingLanguagePreferences.hintLocales("pt")!![0].toLanguageTag())
        assertNull(TypingLanguagePreferences.hintLocales("pt-BR"))
        assertEquals("Sugerencia de idioma de escritura. Es posible que el teclado ignore esta preferencia.",
            spanishContext.getString(com.chatforia.android.R.string.android_chat_thread_typing_language))
    }

    @Test fun keyboardCopyIsLocalizedForEverySupportedAppLanguage() {
        val englishHint = "Typing language hint. Your keyboard may ignore this preference."
        val englishSystem = "System keyboard (no hint)"
        val failures = mutableListOf<String>()
        LANGUAGES.filter { it.code != "en" }.forEach { language ->
            AppLocaleManager.saveLanguage(context, language.code)
            val localized = AppLocaleManager.wrapContext(context)
            val hint = localized.getString(com.chatforia.android.R.string.android_chat_thread_typing_language)
            val system = localized.getString(com.chatforia.android.R.string.android_chat_thread_system_keyboard)
            if (hint.equals(englishHint, ignoreCase = true)) failures += "${language.code}: typing hint"
            if (system.equals(englishSystem, ignoreCase = true)) failures += "${language.code}: system option"
        }
        assertTrue("English fallback: $failures", failures.isEmpty())
    }
}
