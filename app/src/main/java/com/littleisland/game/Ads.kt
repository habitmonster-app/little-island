package com.littleisland.game

import android.app.Activity
import android.widget.LinearLayout
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * 광고: 앱 화면 맨 아래 배너 + 보고 싶을 때만 보는 보상 광고.
 * 배경화면(홈 화면)에는 절대 광고를 넣지 않는다.
 * 지금 ID는 모두 구글이 주는 테스트 ID다. 출시 전에 대열천재님의 AdMob ID로 바꾼다.
 */
object Ads {
    private const val BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val REWARD_ID = "ca-app-pub-3940256099942544/5224354917"
    private var reward: RewardedAd? = null
    private var loading = false

    fun init(a: Activity) {
        try { MobileAds.initialize(a) { loadReward(a) } } catch (e: Throwable) { }
    }

    /** 게임 화면 아래에 붙일 배너 (실패하면 null) */
    fun banner(a: Activity): AdView? = try {
        AdView(a).apply {
            setAdSize(AdSize.BANNER); adUnitId = BANNER_ID
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            loadAd(AdRequest.Builder().build())
        }
    } catch (e: Throwable) { null }

    private fun loadReward(a: Activity) {
        if (loading || reward != null) return
        loading = true
        try {
            RewardedAd.load(a, REWARD_ID, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { reward = ad; loading = false }
                override fun onAdFailedToLoad(e: LoadAdError) { reward = null; loading = false }
            })
        } catch (e: Throwable) { loading = false }
    }

    /** 보상 광고를 보여 주고, 끝까지 보면 onReward. 광고가 아직 없으면 false */
    fun showReward(a: Activity, onReward: () -> Unit): Boolean {
        val ad = reward ?: run { loadReward(a); return false }
        reward = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { loadReward(a) }
        }
        ad.show(a) { onReward() }
        return true
    }
}
