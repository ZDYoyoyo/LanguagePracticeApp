package io.github.zdyoyoyo.englishpractice.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import io.github.zdyoyoyo.englishpractice.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AdMob 廣告管理：
 * 1. 先用 UMP（User Messaging Platform）取得使用者同意（歐盟 / 英國等地區法規要求），同意後才初始化廣告
 * 2. 套用兒童相關設定（內容分級上限、是否兒童導向）
 * 3. 預先載入插頁廣告與獎勵廣告，需要時才顯示
 */
class AdsManager(
    private val appContext: Context,
    private val clock: () -> Long,
) {
    private val initialized = AtomicBoolean(false)
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null

    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()
    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    // 插頁廣告頻率計數（只存在記憶體：重開 App 會重算，對使用者更友善）
    private var totalSessions = 0
    private var sessionsSinceLastAd = 0
    private var lastAdShownAt = 0L

    /** 在 MainActivity.onCreate 呼叫 */
    fun gatherConsentAndInit(activity: Activity) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(BuildConfig.ADS_CHILD_DIRECTED)
            .build()
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(activity, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                if (error != null) Log.w(TAG, "Consent form: ${error.message}")
                updatePrivacyStatus(consent)
                if (consent.canRequestAds()) initAds()
            }
        }, { error ->
            Log.w(TAG, "Consent info: ${error.message}")
            if (consent.canRequestAds()) initAds()
        })
        // 之前已同意過就不必等待，直接初始化
        if (consent.canRequestAds()) initAds()
    }

    private fun updatePrivacyStatus(consent: ConsentInformation) {
        _privacyOptionsRequired.value =
            consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    /** 設定頁的「廣告隱私選項」：讓使用者隨時修改同意設定 */
    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) Log.w(TAG, "Privacy options: ${error.message}")
        }
    }

    private fun initAds() {
        if (!initialized.compareAndSet(false, true)) return
        val childTag = if (BuildConfig.ADS_CHILD_DIRECTED) RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
        else RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        val underAge = if (BuildConfig.ADS_CHILD_DIRECTED) RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
        else RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(childTag)
                .setTagForUnderAgeOfConsent(underAge)
                .setMaxAdContentRating(maxRating())
                .build()
        )
        MobileAds.initialize(appContext) {
            loadInterstitial()
            loadRewarded()
        }
    }

    private fun maxRating() = when (BuildConfig.ADS_MAX_CONTENT_RATING) {
        "PG" -> RequestConfiguration.MAX_AD_CONTENT_RATING_PG
        "T" -> RequestConfiguration.MAX_AD_CONTENT_RATING_T
        "MA" -> RequestConfiguration.MAX_AD_CONTENT_RATING_MA
        else -> RequestConfiguration.MAX_AD_CONTENT_RATING_G
    }

    private fun loadInterstitial() {
        InterstitialAd.load(appContext, BuildConfig.ADMOB_INTERSTITIAL_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    Log.d(TAG, "Interstitial failed: ${error.message}")
                }
            })
    }

    private fun loadRewarded() {
        RewardedAd.load(appContext, BuildConfig.ADMOB_REWARDED_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewarded = ad
                    _rewardedReady.value = true
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewarded = null
                    _rewardedReady.value = false
                    Log.d(TAG, "Rewarded failed: ${error.message}")
                }
            })
    }

    /** 完成一輪題目後呼叫；由 [AdPolicy] 決定這次要不要顯示插頁廣告 */
    fun onSessionCompleted(activity: Activity, questionsInSession: Int) {
        totalSessions++
        sessionsSinceLastAd++
        val ad = interstitial ?: return
        val show = AdPolicy.shouldShowInterstitial(
            totalSessions, sessionsSinceLastAd, lastAdShownAt, clock(), questionsInSession,
        )
        if (!show) return
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = loadInterstitial()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = loadInterstitial()
        }
        interstitial = null
        sessionsSinceLastAd = 0
        lastAdShownAt = clock()
        ad.show(activity)
    }

    /**
     * 使用者「自己選擇」看獎勵廣告。看完才會呼叫 onReward。
     * @return false 代表廣告還沒準備好
     */
    fun showRewarded(activity: Activity, onReward: () -> Unit): Boolean {
        val ad = rewarded ?: return false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = loadRewarded()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = loadRewarded()
        }
        rewarded = null
        _rewardedReady.value = false
        ad.show(activity) { onReward() }
        return true
    }

    companion object {
        private const val TAG = "AdsManager"
    }
}
