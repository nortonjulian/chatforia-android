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
    private var adsEnabled = false
    private var lastShownAt = 0L

    private val minimumGapMillis = 15 * 60 * 1000L

    private var smsThreadExitCount = 0
    private val showEverySmsThreadExits = 10

    /**
     * Call whenever the signed-in user's plan or ad eligibility changes.
     */
    fun setAdsEnabled(enabled: Boolean) {
        if (adsEnabled == enabled) {
            return
        }

        adsEnabled = enabled

        if (!enabled) {
            // Drop any previously loaded Free-plan ad.
            interstitialAd = null
            smsThreadExitCount = 0
            return
        }

        load()
    }

    fun recordSmsThreadExitAndMaybeShow() {
        if (!adsEnabled) {
            return
        }

        smsThreadExitCount += 1

        if (
            smsThreadExitCount %
            showEverySmsThreadExits != 0
        ) {
            load()
            return
        }

        showIfReady()
    }

    fun load() {
        if (!adsEnabled) {
            return
        }

        if (isLoading || interstitialAd != null) {
            return
        }

        isLoading = true

        val request =
            AdRequest.Builder()
                .build()

        InterstitialAd.load(
            activity,
            AdMobConfig.interstitialAdUnitId,
            request,
            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(
                    ad: InterstitialAd
                ) {
                    isLoading = false

                    /*
                     * The user's plan may have changed while
                     * the network request was in progress.
                     */
                    if (!adsEnabled) {
                        interstitialAd = null
                        return
                    }

                    interstitialAd = ad

                    ad.fullScreenContentCallback =
                        object :
                            FullScreenContentCallback() {

                            override fun onAdShowedFullScreenContent() {
                                lastShownAt =
                                    SystemClock.elapsedRealtime()
                            }

                            override fun onAdDismissedFullScreenContent() {
                                interstitialAd = null

                                if (adsEnabled) {
                                    load()
                                }
                            }

                            override fun onAdFailedToShowFullScreenContent(
                                adError: AdError
                            ) {
                                interstitialAd = null

                                if (adsEnabled) {
                                    load()
                                }
                            }
                        }
                }

                override fun onAdFailedToLoad(
                    error: LoadAdError
                ) {
                    interstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    fun showIfReady() {
        if (!adsEnabled) {
            return
        }

        val now =
            SystemClock.elapsedRealtime()

        if (
            now - lastShownAt <
            minimumGapMillis
        ) {
            return
        }

        val ad = interstitialAd

        if (ad == null) {
            /*
             * Load for a future eligible checkpoint,
             * but never display automatically later.
             */
            load()
            return
        }

        /*
         * Clear the stored reference before showing so
         * this ad cannot accidentally be reused.
         */
        interstitialAd = null
        ad.show(activity)
    }

    fun clear() {
        adsEnabled = false
        interstitialAd = null
        isLoading = false
        smsThreadExitCount = 0
    }
}