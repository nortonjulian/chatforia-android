package com.chatforia.android.ads

import android.app.Activity
import android.os.SystemClock
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class InterstitialAdManager(
    private val activity: Activity
) {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var lastShownAt = 0L
    private var showAfterLoad = false

    private val minimumGapMillis = 3 * 60 * 1000L

    fun load(showWhenReady: Boolean = false) {
        if (showWhenReady) {
            showAfterLoad = true
        }

        if (isLoading) return

        val readyAd = interstitialAd
        if (readyAd != null) {
            if (showWhenReady) {
                showIfReady()
            }
            return
        }

        isLoading = true

        val request = AdRequest.Builder().build()

        InterstitialAd.load(
            activity,
            AdMobConfig.interstitialAdUnitId,
            request,
            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false

                    ad.fullScreenContentCallback =
                        object : FullScreenContentCallback() {

                            override fun onAdShowedFullScreenContent() {
                                lastShownAt = SystemClock.elapsedRealtime()
                            }

                            override fun onAdDismissedFullScreenContent() {
                                interstitialAd = null
                                showAfterLoad = false
                                load()
                            }

                            override fun onAdFailedToShowFullScreenContent(
                                adError: AdError
                            ) {
                                interstitialAd = null
                                showAfterLoad = false
                                load()
                            }
                        }

                    if (showAfterLoad) {
                        showAfterLoad = false
                        showIfReady()
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    showAfterLoad = false
                }
            }
        )
    }

    fun showIfReady() {
        val now = SystemClock.elapsedRealtime()

        if (now - lastShownAt < minimumGapMillis) return

        val ad = interstitialAd

        if (ad == null) {
            load(showWhenReady = true)
            return
        }

        ad.show(activity)
    }
}