package org.catrobat.catroid.desktop.admob

object DesktopAdMobManager {

    fun isInitialized(): Boolean = true
    fun isTestMode(): Boolean = true
    fun isBannerLoaded(): Boolean = false
    fun isInterstitialLoaded(): Boolean = false
    fun isRewardedLoaded(): Boolean = false
    fun isAppOpenLoaded(): Boolean = false

    fun loadBanner(unitId: String) {}
    fun showBanner() {}
    fun hideBanner() {}
    fun destroyBanner() {}

    fun loadInterstitial(unitId: String) {}
    fun showInterstitial() {}

    fun loadRewarded(unitId: String) {}
    fun showRewarded(onReward: () -> Unit = {}) {
        onReward()
    }
}
