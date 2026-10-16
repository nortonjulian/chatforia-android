package com.chatforia.android.auth

import android.content.Context
import java.net.URI
import java.util.Locale

/** Keeps a first-touch creator code until signup, for at most 30 days. */
class CreatorReferralStore(
    context: Context,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "creator_referral",
        Context.MODE_PRIVATE
    )

    fun capture(url: String): String? {
        val code = codeFromLink(url) ?: return null
        if (currentCode() == null) {
            preferences.edit()
                .putString("code", code)
                .putLong("captured_at", nowMillis())
                .apply()
        }
        return currentCode()
    }

    fun currentCode(): String? {
        val code = preferences.getString("code", null) ?: return null
        val age = nowMillis() - preferences.getLong("captured_at", 0L)
        if (age < 0 || age >= THIRTY_DAYS_MILLIS) {
            clear()
            return null
        }
        return code
    }

    fun clear() {
        preferences.edit().remove("code").remove("captured_at").apply()
    }

    companion object {
        private const val THIRTY_DAYS_MILLIS = 30L * 24 * 60 * 60 * 1000
        private val CODE = Regex("[A-Z0-9_-]{3,40}")

        fun codeFromLink(link: String): String? {
            val uri = try { URI(link) } catch (_: Exception) { return null }
            val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return null
            val host = uri.host?.lowercase(Locale.ROOT) ?: return null
            val path = uri.path ?: return null
            val allowed = when (scheme) {
                "https" -> host == "chatforia.com" || host == "www.chatforia.com"
                "chatforia" -> host == "ref"
                else -> false
            }
            if (!allowed) return null

            val raw = when {
                scheme == "https" && path.startsWith("/ref/") ->
                    path.removePrefix("/ref/").takeIf { !it.contains('/') }
                scheme == "https" && path == "/register" ->
                    uri.rawQuery?.split('&')?.firstNotNullOfOrNull { part ->
                        part.split('=', limit = 2).takeIf { it.size == 2 && it[0] == "ref" }?.get(1)
                    }
                scheme == "chatforia" && path.startsWith("/") ->
                    path.removePrefix("/").takeIf { !it.contains('/') }
                else -> null
            } ?: return null

            val code = raw.uppercase(Locale.ROOT)
            return code.takeIf { CODE.matches(it) }
        }
    }
}
