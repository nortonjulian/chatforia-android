package com.chatforia.android.ads

import com.chatforia.android.auth.UserDto

fun UserDto.shouldShowAds(): Boolean {
    if (isAdmin == true) return false
    if (isPremium == true) return false

    val normalizedPlan =
        plan
            ?.trim()
            ?.lowercase()
            ?.replace("-", "_")
            ?.replace(" ", "_")

    val paidPlans =
        setOf(
            "plus",
            "premium",
            "premium_monthly",
            "premium_annual",
            "premiummonthly",
            "premiumannual"
        )

    return normalizedPlan !in paidPlans
}

fun resolveAdsEnabled(
    isAdmin: Boolean?,
    backendAdsEnabled: Boolean?,
    fallbackPlanAdsEnabled: Boolean
): Boolean {
    if (isAdmin == true) {
        return false
    }

    return backendAdsEnabled ?: fallbackPlanAdsEnabled
}

fun UserDto.resolveAdsEnabled(
    backendAdsEnabled: Boolean?
): Boolean =
    resolveAdsEnabled(
        isAdmin = isAdmin,
        backendAdsEnabled = backendAdsEnabled,
        fallbackPlanAdsEnabled = shouldShowAds()
    )
