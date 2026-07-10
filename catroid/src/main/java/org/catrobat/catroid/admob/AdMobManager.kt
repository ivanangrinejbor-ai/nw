package org.catrobat.catroid.admob

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.eventids.EventId
import org.catrobat.catroid.stage.StageActivity

object AdMobManager {
    private const val TAG = "AdMobManager"

    var isTestMode = false
    var appId: String? = null
    var bannerUnitId: String? = null
    var interstitialUnitId: String? = null
    var rewardedUnitId: String? = null
    var appOpenUnitId: String? = null

    var isInitialized = false
    var isBannerLoaded = false
    var isInterstitialLoaded = false
    var isRewardedLoaded = false
    var isAppOpenLoaded = false

    var lastErrorCode: Int = 0
    var lastErrorMessage: String = ""

    var bannerPosition: BannerPosition = BannerPosition.TOP

    private var adView: AdView? = null
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var isSdkInitializing = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val eventCallbacks = mutableListOf<(EventId) -> Unit>()

    enum class BannerPosition {
        TOP, BOTTOM
    }

    fun addEventCallback(callback: (EventId) -> Unit) {
        eventCallbacks.add(callback)
    }

    fun removeEventCallback(callback: (EventId) -> Unit) {
        eventCallbacks.remove(callback)
    }

    private fun fireEvent(eventType: Int) {
        val eventId = EventId(eventType)
        for (callback in eventCallbacks) {
            try {
                callback(eventId)
            } catch (e: Exception) {
                Log.e(TAG, "Event callback error", e)
            }
        }
        val stage = StageActivity.activeStageActivity?.get()
        if (stage != null) {
            stage.broadcastEventToAllSprites(eventId)
        }
    }

    fun enableTestMode() {
        isTestMode = true
        val testDeviceIds = listOf(AdRequest.DEVICE_ID_EMULATOR)
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        )
    }


    fun initialize(activity: Activity) {
        if (isInitialized || isSdkInitializing) return
        isSdkInitializing = true
        try {
            MobileAds.initialize(activity) { status ->
                isSdkInitializing = false
                isInitialized = true
                mainHandler.post {
                    if (status.adapterStatusMap.values.any { it.initializationState == com.google.android.gms.ads.initialization.AdapterStatus.State.READY }) {
                        Log.d(TAG, "AdMob initialized successfully")
                        fireEvent(EventId.ADMOB_INITIALIZED)
                    } else {
                        Log.w(TAG, "AdMob initialization incomplete")
                        fireEvent(EventId.ADMOB_INIT_FAILED)
                    }
                }
            }
        } catch (e: Exception) {
            isSdkInitializing = false
            lastErrorCode = -1
            lastErrorMessage = e.message ?: "Unknown error"
            Log.e(TAG, "AdMob initialization failed", e)
            mainHandler.post { fireEvent(EventId.ADMOB_INIT_FAILED) }
        }
    }



    fun loadBanner(activity: Activity) {
        mainHandler.post {
            val unitId = bannerUnitId ?: return@post
            destroyBanner()
            try {
                val request = AdRequest.Builder().build()
                adView = AdView(activity).apply {
                    adUnitId = unitId
                    setAdSize(AdSize.BANNER)
                    loadAd(request)
                    setAdListener(object : com.google.android.gms.ads.AdListener() {
                        override fun onAdLoaded() {
                            isBannerLoaded = true
                            Log.d(TAG, "Banner loaded")
                            fireEvent(EventId.ADMOB_BANNER_LOADED)
                        }
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            isBannerLoaded = false
                            lastErrorCode = loadAdError.code
                            lastErrorMessage = loadAdError.message ?: ""
                            Log.e(TAG, "Banner failed: $lastErrorMessage")
                            fireEvent(EventId.ADMOB_BANNER_FAILED)
                        }
                        override fun onAdOpened() {
                            Log.d(TAG, "Banner shown")
                            fireEvent(EventId.ADMOB_BANNER_SHOWN)
                        }
                        override fun onAdClosed() {
                            Log.d(TAG, "Banner hidden")
                            fireEvent(EventId.ADMOB_BANNER_HIDDEN)
                        }
                    })
                }
            } catch (e: Exception) {
                lastErrorCode = -2
                lastErrorMessage = e.message ?: ""
                Log.e(TAG, "Banner load error", e)
            }
        }
    }

    fun showBanner(activity: Activity) {
        mainHandler.post {
            val view = adView ?: return@post
            try {
                val rootView = activity.window.decorView.findViewById<android.view.ViewGroup>(android.R.id.content)
                if (view.parent == null) {
                    val params = android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.gravity = when (bannerPosition) {
                        BannerPosition.TOP -> android.view.Gravity.TOP
                        BannerPosition.BOTTOM -> android.view.Gravity.BOTTOM
                    }
                    rootView.addView(view, params)
                }
                view.visibility = android.view.View.VISIBLE
            } catch (e: Exception) {
                Log.e(TAG, "Show banner error", e)
            }
        }
    }

    fun hideBanner() {
        mainHandler.post {
            adView?.visibility = android.view.View.GONE
        }
    }

    fun destroyBanner() {
        mainHandler.post {
            adView?.destroy()
            adView = null
            isBannerLoaded = false
        }
    }


    fun loadInterstitial(activity: Activity) {
        mainHandler.post {
            val unitId = interstitialUnitId ?: return@post
            try {
                val request = AdRequest.Builder().build()
                InterstitialAd.load(activity, unitId, request, object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isInterstitialLoaded = true
                        Log.d(TAG, "Interstitial loaded")
                        fireEvent(EventId.ADMOB_INTERSTITIAL_LOADED)
                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                interstitialAd = null
                                isInterstitialLoaded = false
                                Log.d(TAG, "Interstitial closed")
                                fireEvent(EventId.ADMOB_INTERSTITIAL_CLOSED)
                            }
                            override fun onAdShowedFullScreenContent() {
                                Log.d(TAG, "Interstitial shown")
                                fireEvent(EventId.ADMOB_INTERSTITIAL_SHOWN)
                            }
                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                lastErrorCode = adError.code
                                lastErrorMessage = adError.message ?: ""
                                Log.e(TAG, "Interstitial show failed: $lastErrorMessage")
                            }
                        }
                    }
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isInterstitialLoaded = false
                        lastErrorCode = loadAdError.code
                        lastErrorMessage = loadAdError.message ?: ""
                        Log.e(TAG, "Interstitial load failed: $lastErrorMessage")
                        fireEvent(EventId.ADMOB_INTERSTITIAL_FAILED)
                    }
                })
            } catch (e: Exception) {
                lastErrorCode = -3
                lastErrorMessage = e.message ?: ""
                Log.e(TAG, "Interstitial load error", e)
            }
        }
    }

    fun showInterstitial(activity: Activity) {
        mainHandler.post {
            val ad = interstitialAd ?: return@post
            try {
                ad.show(activity)
            } catch (e: Exception) {
                Log.e(TAG, "Show interstitial error", e)
            }
        }
    }


    fun loadRewarded(activity: Activity) {
        mainHandler.post {
            val unitId = rewardedUnitId ?: return@post
            try {
                val request = AdRequest.Builder().build()
                RewardedAd.load(activity, unitId, request, object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        isRewardedLoaded = true
                        Log.d(TAG, "Rewarded loaded")
                        fireEvent(EventId.ADMOB_REWARDED_LOADED)
                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                rewardedAd = null
                                isRewardedLoaded = false
                                Log.d(TAG, "Rewarded closed")
                                fireEvent(EventId.ADMOB_REWARDED_CLOSED)
                            }
                            override fun onAdShowedFullScreenContent() {
                                Log.d(TAG, "Rewarded shown")
                                fireEvent(EventId.ADMOB_REWARDED_SHOWN)
                            }
                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                lastErrorCode = adError.code
                                lastErrorMessage = adError.message ?: ""
                                Log.e(TAG, "Rewarded show failed: $lastErrorMessage")
                            }
                        }
                    }
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isRewardedLoaded = false
                        lastErrorCode = loadAdError.code
                        lastErrorMessage = loadAdError.message ?: ""
                        Log.e(TAG, "Rewarded load failed: $lastErrorMessage")
                        fireEvent(EventId.ADMOB_REWARDED_FAILED)
                    }
                })
            } catch (e: Exception) {
                lastErrorCode = -4
                lastErrorMessage = e.message ?: ""
                Log.e(TAG, "Rewarded load error", e)
            }
        }
    }

    fun showRewarded(activity: Activity) {
        mainHandler.post {
            val ad = rewardedAd ?: return@post
            try {
                ad.show(activity) { rewardItem ->
                    Log.d(TAG, "Reward earned: ${rewardItem.amount} ${rewardItem.type}")
                    fireEvent(EventId.ADMOB_REWARDED_REWARD)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Show rewarded error", e)
            }
        }
    }


    fun loadAppOpen(activity: Activity) {
        mainHandler.post {
            val unitId = appOpenUnitId ?: return@post
            try {
                val request = AdRequest.Builder().build()
                AppOpenAd.load(activity, unitId, request, AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                    object : AppOpenAd.AppOpenAdLoadCallback() {
                        override fun onAdLoaded(ad: AppOpenAd) {
                            appOpenAd = ad
                            isAppOpenLoaded = true
                            Log.d(TAG, "App Open loaded")
                            fireEvent(EventId.ADMOB_APP_OPEN_LOADED)
                            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    appOpenAd = null
                                    isAppOpenLoaded = false
                                    Log.d(TAG, "App Open closed")
                                    fireEvent(EventId.ADMOB_APP_OPEN_CLOSED)
                                }
                                override fun onAdShowedFullScreenContent() {
                                    Log.d(TAG, "App Open shown")
                                    fireEvent(EventId.ADMOB_APP_OPEN_SHOWN)
                                }
                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    lastErrorCode = adError.code
                                    lastErrorMessage = adError.message ?: ""
                                    Log.e(TAG, "App Open show failed: $lastErrorMessage")
                                }
                            }
                        }
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            isAppOpenLoaded = false
                            lastErrorCode = loadAdError.code
                            lastErrorMessage = loadAdError.message ?: ""
                            Log.e(TAG, "App Open load failed: $lastErrorMessage")
                        }
                    })
            } catch (e: Exception) {
                lastErrorCode = -5
                lastErrorMessage = e.message ?: ""
                Log.e(TAG, "App Open load error", e)
            }
        }
    }

    fun showAppOpen(activity: Activity) {
        mainHandler.post {
            val ad = appOpenAd ?: return@post
            try {
                ad.show(activity)
            } catch (e: Exception) {
                Log.e(TAG, "Show App Open error", e)
            }
        }
    }

    fun isGooglePlayServicesAvailable(activity: Activity): Boolean {
        return try {
            val result = com.google.android.gms.common.GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(activity)
            result == com.google.android.gms.common.ConnectionResult.SUCCESS
        } catch (e: Exception) {
            false
        }
    }

    fun reset() {
        mainHandler.post {
            adView?.destroy()
            adView = null
            interstitialAd = null
            rewardedAd = null
            appOpenAd = null
            isInitialized = false
            isBannerLoaded = false
            isInterstitialLoaded = false
            isRewardedLoaded = false
            isAppOpenLoaded = false
            lastErrorCode = 0
            lastErrorMessage = ""
            isSdkInitializing = false
        }
    }
}
