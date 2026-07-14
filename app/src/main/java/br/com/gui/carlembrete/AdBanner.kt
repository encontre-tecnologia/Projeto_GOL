package br.com.gui.carlembrete

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

private const val PRODUCTION_BANNER_ID = "ca-app-pub-1183660196592298/2547196302"
private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/9214589741"

fun shouldShowAdsForFreePlan(isPremium: Boolean): Boolean = BuildConfig.DEBUG || !isPremium

@Composable
fun FreePlanAdBanner(
    isPremium: Boolean,
    modifier: Modifier = Modifier
) {
    if (shouldShowAdsForFreePlan(isPremium)) {
        AdBanner(modifier = modifier)
    }
}

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    BoxWithConstraints(modifier = modifier) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(1)
        val adUnitId = remember { if (BuildConfig.DEBUG) TEST_BANNER_ID else PRODUCTION_BANNER_ID }
        val adSize = remember(widthDp) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
        }
        val adView = remember(adUnitId, adSize) {
            AdView(context).apply {
                this.adUnitId = adUnitId
                setAdSize(adSize)
                loadAd(AdRequest.Builder().build())
            }
        }

        AndroidView(
            factory = { adView },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
        )

        DisposableEffect(adView) {
            onDispose { adView.destroy() }
        }
    }
}
