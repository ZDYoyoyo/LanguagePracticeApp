package io.github.zdyoyoyo.englishpractice.ads

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.ads.mediation.admob.AdMobAdapter
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
 * 1. 先用 UMP（User Messaging Platform）取得使用者同意（歐盟 / 英國等地區法規要求），再初始化廣告
 *    - 若同意流程本身出錯（例如 AdMob 後台還沒設定同意訊息），改用「非個人化廣告」繼續，不會卡住
 * 2. 套用兒童相關設定（內容分級上限、是否兒童導向）
 * 3. 預先載入插頁廣告與獎勵廣告；載入失敗會自動重試，並把原因顯示在設定頁
 */
class AdsManager(
    private val appContext: Context,
    private val clock: () -> Long,
) {
    private val initialized = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var rewardedRetries = 0
    private var interstitialRetries = 0
    /** 同意流程失敗時只請求非個人化廣告 */
    private var nonPersonalizedOnly = false

    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()
    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()
    /** 給設定頁顯示的狀態（方便排查「一直載入中」） */
    private val _status = MutableStateFlow("等待初始化")
    val status: StateFlow<String> = _status.asStateFlow()

    // 插頁廣告頻率計數（只存在記憶體：重開 App 會重算，對使用者更友善）
    private var totalSessions = 0
    private var sessionsSinceLastAd = 0
    private var lastAdShownAt = 0L

    /** 在 MainActivity.onCreate 呼叫 */
    fun gatherConsentAndInit(activity: Activity) {
        _status.value = "取得廣告同意設定中…"
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(BuildConfig.ADS_CHILD_DIRECTED)
            .build()
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(activity, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                if (error != null) Log.w(TAG, "Consent form: ${error.message}")
                updatePrivacyStatus(consent)
                if (consent.canRequestAds()) initAds() else fallbackInit("使用者未同意")
            }
        }, { error ->
            Log.w(TAG, "Consent info: ${error.errorCode} ${error.message}")
            if (consent.canRequestAds()) initAds() else fallbackInit("同意設定讀取失敗（${error.errorCode}）")
        })
        // 之前已同意過就不必等待，直接初始化
        if (consent.canRequestAds()) initAds()
    }

    /** 無法取得同意時：只請求非個人化廣告，避免整個廣告功能卡住 */
    private fun fallbackInit(reason: String) {
        Log.w(TAG, "Fallback to non-personalized ads: $reason")
        nonPersonalizedOnly = true
        initAds()
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
        _status.value = "廣告初始化中…"
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
            // 回呼可能不在主執行緒，廣告載入要在主執行緒
            handler.post {
                loadInterstitial()
                loadRewarded()
            }
        }
    }

    private fun maxRating() = when (BuildConfig.ADS_MAX_CONTENT_RATING) {
        "PG" -> RequestConfiguration.MAX_AD_CONTENT_RATING_PG
        "T" -> RequestConfiguration.MAX_AD_CONTENT_RATING_T
        "MA" -> RequestConfiguration.MAX_AD_CONTENT_RATING_MA
        else -> RequestConfiguration.MAX_AD_CONTENT_RATING_G
    }

    private fun request(): AdRequest {
        val builder = AdRequest.Builder()
        if (nonPersonalizedOnly) {
            builder.addNetworkExtrasBundle(AdMobAdapter::class.java, Bundle().apply { putString("npa", "1") })
        }
        return builder.build()
    }

    /** 失敗後等一下再試：30 秒、1 分、2 分…最多 5 分鐘 */
    private fun retryDelayMs(attempt: Int) = (30_000L shl attempt.coerceAtMost(3)).coerceAtMost(300_000L)

    private fun loadInterstitial() {
        InterstitialAd.load(appContext, BuildConfig.ADMOB_INTERSTITIAL_ID, request(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    interstitialRetries = 0
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    Log.d(TAG, "Interstitial failed: ${error.code} ${error.message}")
                    handler.postDelayed(::loadInterstitial, retryDelayMs(interstitialRetries++))
                }
            })
    }

    private fun loadRewarded() {
        _status.value = "獎勵廣告載入中…"
        RewardedAd.load(appContext, BuildConfig.ADMOB_REWARDED_ID, request(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewarded = ad
                    rewardedRetries = 0
                    _rewardedReady.value = true
                    _status.value = "廣告已就緒" + if (nonPersonalizedOnly) "（非個人化）" else ""
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewarded = null
                    _rewardedReady.value = false
                    _status.value = "廣告載入失敗（錯誤 ${error.code}：${describe(error.code)}），稍後自動重試"
                    Log.d(TAG, "Rewarded failed: ${error.code} ${error.message}")
                    handler.postDelayed(::loadRewarded, retryDelayMs(rewardedRetries++))
                }
            })
    }

    /** 設定頁的「重新載入」 */
    fun retryNow() {
        if (!initialized.get()) {
            fallbackInit("手動重試")
            return
        }
        handler.removeCallbacksAndMessages(null)
        rewardedRetries = 0
        interstitialRetries = 0
        if (rewarded == null) loadRewarded()
        if (interstitial == null) loadInterstitial()
    }

    private fun describe(code: Int) = when (code) {
        AdRequest.ERROR_CODE_NETWORK_ERROR -> "網路錯誤，若有使用擋廣告的 DNS / App 請暫時關閉"
        AdRequest.ERROR_CODE_NO_FILL -> "目前沒有可顯示的廣告"
        AdRequest.ERROR_CODE_INVALID_REQUEST -> "廣告設定有誤"
        AdRequest.ERROR_CODE_INTERNAL_ERROR -> "內部錯誤"
        AdRequest.ERROR_CODE_APP_ID_MISSING -> "缺少 App ID"
        else -> "其他錯誤"
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
