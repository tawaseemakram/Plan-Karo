package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.TealNeon
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

object AdManager {
    private const val TAG = "AdManager"

    // Production AdMob Ad Unit IDs
    const val AD_UNIT_BANNER_OPTIONS = "ca-app-pub-7340151781341221/2088878109"
    const val AD_UNIT_INTERSTITIAL_TASK_ENTRY = "ca-app-pub-7340151781341221/5114321687"
    const val AD_UNIT_EMPTY_STATE_TASK = "ca-app-pub-7340151781341221/2088878109"

    const val INTERSTITIAL_COOLDOWN_MS = 90_000L

    private val isMobileAdsInitCalled = AtomicBoolean(false)
    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading: Boolean = false
    private var isInterstitialShowing: Boolean = false

    @Volatile
    private var lastInterstitialShownAtMs: Long = 0L

    fun canShowInterstitialNow(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (isInterstitialShowing) return false
        val lastShown = lastInterstitialShownAtMs
        return lastShown == 0L || (nowMs - lastShown) >= INTERSTITIAL_COOLDOWN_MS
    }

    internal fun recordInterstitialShown(nowMs: Long = System.currentTimeMillis()) {
        lastInterstitialShownAtMs = nowMs
    }

    internal fun resetInterstitialCooldown() {
        lastInterstitialShownAtMs = 0L
        isInterstitialShowing = false
    }

    /**
     * Requests Google UMP GDPR consent where required (EEA/UK/CH) before initializing MobileAds.
     */
    fun initializeWithConsent(activity: Activity) {
        val appContext = activity.applicationContext
        try {
            val params = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

            if (consentInformation.canRequestAds()) {
                initialize(appContext)
            }

            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        if (formError != null) {
                            Log.w(TAG, "UMP consent form warning: ${formError.message}")
                        }
                        if (consentInformation.canRequestAds()) {
                            initialize(appContext)
                        }
                    }
                },
                { requestConsentError ->
                    Log.w(TAG, "UMP consent info update failed: ${requestConsentError.message}")
                    if (consentInformation.canRequestAds()) {
                        initialize(appContext)
                    }
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "UMP consent check fallback to initialize: ${e.message}")
            initialize(appContext)
        }
    }

    fun initialize(context: Context) {
        if (isMobileAdsInitCalled.getAndSet(true)) return
        try {
            MobileAds.initialize(context.applicationContext) { status ->
                Log.d(TAG, "AdMob MobileAds initialized: $status")
                preloadInterstitial(context.applicationContext)
            }
        } catch (e: Exception) {
            isMobileAdsInitCalled.set(false)
            Log.e(TAG, "Error initializing AdMob", e)
        }
    }

    fun preloadInterstitial(context: Context) {
        if (interstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true

        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            AD_UNIT_INTERSTITIAL_TASK_ENTRY,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d(TAG, "Task Entry Interstitial Ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                    Log.w(TAG, "Task Entry Interstitial Ad load failed: ${error.message}")
                }
            }
        )
    }

    /**
     * Shows Interstitial Ad after task/note/habit/routine entry, respecting a 90-second cooldown
     * to prevent excessive ad frequency and maintain Google AdMob policy compliance.
     */
    fun showInterstitialAfterTaskEntry(activity: Activity, onDismissed: () -> Unit = {}) {
        if (!canShowInterstitialNow()) {
            Log.d(TAG, "Skipping interstitial ad due to 90s cooldown")
            preloadInterstitial(activity)
            onDismissed()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            isInterstitialShowing = true
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    isInterstitialShowing = false
                    interstitialAd = null
                    recordInterstitialShown()
                    preloadInterstitial(activity)
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    isInterstitialShowing = false
                    interstitialAd = null
                    preloadInterstitial(activity)
                    onDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    recordInterstitialShown()
                    Log.d(TAG, "Showing task entry interstitial ad")
                }
            }
            ad.show(activity)
        } else {
            preloadInterstitial(activity)
            onDismissed()
        }
    }
}

/**
 * Standard Banner Ad for screens / options
 */
@Composable
fun ScreenOptionBannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = AdManager.AD_UNIT_BANNER_OPTIONS
) {
    var adLoaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var adViewRef by remember { mutableStateOf<AdView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { adViewRef?.destroy() }
            adViewRef = null
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("screen_option_banner_ad"),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, TealNeon.copy(alpha = 0.25f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = TealNeon.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Ad",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TealNeon,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sponsored",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx ->
                        AdView(ctx).apply {
                            setAdSize(AdSize.BANNER)
                            this.adUnitId = adUnitId
                            adListener = object : AdListener() {
                                override fun onAdLoaded() {
                                    super.onAdLoaded()
                                    adLoaded = true
                                    loadError = null
                                }

                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    super.onAdFailedToLoad(error)
                                    adLoaded = false
                                    loadError = error.message
                                }
                            }
                            loadAd(AdRequest.Builder().build())
                        }.also { adViewRef = it }
                    }
                )

                if (!adLoaded && loadError == null) {
                    Text(
                        text = "Loading Ad…",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Banner Ad shown in the empty task state with generous spacing away from interactive controls.
 */
@Composable
fun AddFirstTaskBottomAd(
    modifier: Modifier = Modifier,
    adUnitId: String = AdManager.AD_UNIT_EMPTY_STATE_TASK
) {
    var adLoaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var adViewRef by remember { mutableStateOf<AdView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { adViewRef?.destroy() }
            adViewRef = null
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 8.dp, end = 8.dp)
            .testTag("add_first_task_ad"),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, TealNeon.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = TealNeon.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Ad",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TealNeon,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sponsored",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx ->
                        AdView(ctx).apply {
                            setAdSize(AdSize.BANNER)
                            this.adUnitId = adUnitId
                            adListener = object : AdListener() {
                                override fun onAdLoaded() {
                                    super.onAdLoaded()
                                    adLoaded = true
                                    loadError = null
                                }

                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    super.onAdFailedToLoad(error)
                                    adLoaded = false
                                    loadError = error.message
                                }
                            }
                            loadAd(AdRequest.Builder().build())
                        }.also { adViewRef = it }
                    }
                )

                if (!adLoaded && loadError == null) {
                    Text(
                        text = "Loading Ad…",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}
