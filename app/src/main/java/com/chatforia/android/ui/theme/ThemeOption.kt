package com.chatforia.android.ui.theme

data class ThemeOption(
    val code: String,
    val name: String,
    val requiredPlan: String
)

object AppThemes {
    val all = listOf(
        ThemeOption("dawn", "Dawn", "FREE"),
        ThemeOption("midnight", "Midnight", "FREE"),
        ThemeOption("amoled", "AMOLED", "PREMIUM"),
        ThemeOption("aurora", "Aurora", "PREMIUM"),
        ThemeOption("neon", "Neon", "PREMIUM"),
        ThemeOption("sunset", "Sunset", "PREMIUM"),
        ThemeOption("solarized", "Solarized", "PREMIUM"),
        ThemeOption("velvet", "Velvet", "PREMIUM")
    )

    fun nameFor(code: String?): String {
        return all.firstOrNull { it.code == code }?.name ?: "Dawn"
    }

    fun canAccess(code: String, plan: String?): Boolean {
        val option = all.firstOrNull { it.code == code } ?: return false
        val normalizedPlan = plan?.uppercase() ?: "FREE"

        return option.requiredPlan == "FREE" ||
                normalizedPlan == "PREMIUM"
    }

    fun resolvedThemeForPlan(
        code: String?,
        plan: String?
    ): String {
        val fallback = "dawn"
        val resolvedCode = code?.lowercase() ?: fallback

        return if (canAccess(resolvedCode, plan)) {
            resolvedCode
        } else {
            fallback
        }
    }
}