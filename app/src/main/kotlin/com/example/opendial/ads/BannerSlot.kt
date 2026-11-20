package com.example.opendial.ads

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.opendial.R
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

/**
 * An inline adaptive banner for the home feed, sized to the screen width. It takes no space until
 * an ad has loaded, so a missed fill never leaves an empty box in the feed. [onImpression] runs on
 * the SDK's background thread.
 */
@Composable
fun BannerSlot(adUnitId: String, sdkReady: Boolean, onImpression: () -> Unit, modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) return
    val context = LocalContext.current
    val windowWidthPx = LocalWindowInfo.current.containerSize.width
    val widthDp = with(LocalDensity.current) { windowWidthPx.toDp().value.toInt() } - 32
    val adSize = remember(widthDp) { AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(context, widthDp) }
    val adView = remember { AdView(context) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(sdkReady, adUnitId, adSize) {
        if (!sdkReady) return@LaunchedEffect
        adView.loadAd(
            BannerAdRequest.Builder(adUnitId, adSize).build(),
            object : AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) {
                    ad.adEventCallback = object : BannerAdEventCallback {
                        override fun onAdImpression() = onImpression()
                    }
                    loaded = true
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.w("OpenDialAds", "Banner failed to load: $adError")
                }
            },
        )
    }
    DisposableEffect(adView) { onDispose { adView.destroy() } }

    // The AdView stays in the tree from the first frame (it has to, to load); the labelled frame
    // around it only appears once there is an ad to show.
    Column(modifier.fillMaxWidth()) {
        if (loaded) {
            Text(
                stringResource(R.string.advertisement).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, bottom = 6.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (loaded) Modifier.wrapContentHeight() else Modifier.height(0.dp)),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(factory = { adView })
        }
    }
}
