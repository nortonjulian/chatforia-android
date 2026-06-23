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

    private val minimumGapMillis = 3 * 60 * 1000L

    fun load() {
        if (isLoading || interstitialAd != null) return

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
                                load()
                            }

                            override fun onAdFailedToShowFullScreenContent(
                                adError: AdError
                            ) {
                                interstitialAd = null
                                load()
                            }
                        }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    fun showIfReady() {
        val now = SystemClock.elapsedRealtime()

        if (now - lastShownAt < minimumGapMillis) return

        val ad = interstitialAd

        if (ad == null) {
            load()
            return
        }

        ad.show(activity)
    }
}