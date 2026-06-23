package com.chatforia.android.ads

import com.chatforia.android.BuildConfig

object AdMobConfig {

    val bannerAdUnitId: String
        get() = BuildConfig.ADMOB_BANNER_AD_UNIT_ID

    val interstitialAdUnitId: String
        get() = BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID
}