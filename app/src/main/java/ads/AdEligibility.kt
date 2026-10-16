package com.chatforia.android.ads

import com.chatforia.android.auth.isPaidPlan
import com.chatforia.android.auth.UserDto

fun UserDto.shouldShowAds(): Boolean =
    !isPaidPlan()

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
