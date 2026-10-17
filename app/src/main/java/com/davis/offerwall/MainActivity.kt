package com.davis.offerwall

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.davis.offerwall.ads.AdIds
import com.davis.offerwall.databinding.ActivityMainBinding
import com.davis.offerwall.ui.PointsRepository
import com.davis.offerwall.web.OfferwallBridge
import com.davis.offerwall.web.OfferwallWebViewClient
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * The hybrid monetization main screen:
 *   header (balance)  ->  offerwall WebView  ->  bottom AdMob banner.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val numberFormat: NumberFormat = NumberFormat.getIntegerInstance(Locale.US)

    /**
     * OFFERWALL URL.
     *
     * Leave BLANK to run the bundled placeholder page (app/src/main/assets/offerwall_placeholder.html),
     * which demonstrates the JS <-> native bridge with zero external dependencies.
     *
     * ⚠️ REPLACE with your provider's publisher URL once your offerwall account is approved.
     * Most providers give you a URL like one of these (fill in YOUR app id / user id / signature):
     *
     *   Tapjoy  : https://offerwall.tapjoy.com/?publisher_user_id=USER_ID&pub_id=APP_ID&...   (also has an SDK)
     *   Ayet    : https://web.ayet.io/...?app_id=APP_ID&user_id=USER_ID                       (URL/iframe based)
     *   AdGem   : https://api.adgem.com/v1/wall?appid=APP_ID&playerid=USER_ID
     *   adjoe   : typically SDK-based (Playtime) — WebView path needs a provider-hosted web URL
     *
     * NOTE: provider accounts + keys are YOURS to obtain (Checklist step 5). No secrets belong in this file.
     */
    private val offerwallUrl: String = ""

    /** Hosts allowed to run INSIDE the WebView. Everything else opens in the system browser. */
    private val allowedHosts: Set<String> = buildSet {
        // Add your provider's offerwall host(s) here, e.g. "offerwall.tapjoy.com", "web.ayet.io", "api.adgem.com".
        // While running the bundled placeholder there is no remote host, so this can stay empty.
        offerwallUrl.takeIf { it.isNotBlank() }?.let {
            runCatching { android.net.Uri.parse(it).host }.getOrNull()?.let { host -> add(host) }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBalance()
        setupWebView()
        setupBanner()
        setupBackNavigation()

        binding.btnRetry.setOnClickListener { loadOfferwall() }
        loadOfferwall()
    }

    // ── Header: observe the points balance ────────────────────────────────────
    private fun setupBalance() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                PointsRepository.points.collect { points ->
                    binding.tvBalance.text = getString(
                        R.string.balance_format,
                        numberFormat.format(points)
                    )
                }
            }
        }
    }

    // ── Center: offerwall WebView + JS bridge ─────────────────────────────────
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val web = binding.webView

        with(web.settings) {
            javaScriptEnabled = true               // offerwall pages need JS
            domStorageEnabled = true               // localStorage for provider walls
            loadsImagesAutomatically = true
            setSupportMultipleWindows(false)
            mediaPlaybackRequiresUserGesture = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            // Identify the host app to the offerwall; harmless and sometimes requested by publishers.
            userAgentString = "$userAgentString ${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"
        }

        // Some offerwalls rely on third-party cookies for attribution.
        // Needed for many providers; review your provider + privacy policy before release.
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)

        // The native side the offerwall calls (see OfferwallBridge for the JS contract).
        web.addJavascriptInterface(
            OfferwallBridge(
                webView = web,
                onPointsChanged = { newBalance, reward, offerId ->
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            getString(R.string.reward_credited, numberFormat.format(reward)),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    // TODO(analytics): log offerId + reward to your analytics/backend for reconciliation.
                },
                onLog = { android.util.Log.d("Offerwall", it) }
            ),
            BRIDGE_NAME
        )

        web.webViewClient = OfferwallWebViewClient(
            context = this,
            allowedHosts = allowedHosts,
            onPageStarted = {
                binding.progressBar.visibility = android.view.View.VISIBLE
                binding.errorContainer.visibility = android.view.View.GONE
            },
            onPageFinished = { binding.progressBar.visibility = android.view.View.GONE },
            onError = { message ->
                binding.progressBar.visibility = android.view.View.GONE
                binding.tvError.text = message
                binding.errorContainer.visibility = android.view.View.VISIBLE
            }
        )

        // Hardening: never let a page swap out our WebViewClient/bridge.
        web.webChromeClient = android.webkit.WebChromeClient()
    }

    private fun loadOfferwall() {
        binding.errorContainer.visibility = android.view.View.GONE
        if (offerwallUrl.isBlank()) {
            binding.webView.loadUrl("file:///android_asset/offerwall_placeholder.html")
        } else {
            binding.webView.loadUrl(offerwallUrl)
        }
    }

    // ── Bottom: AdMob banner ──────────────────────────────────────────────────
    private fun setupBanner() {
        binding.adView.adUnitId = AdIds.BANNER_HOME   // test id today; swap in AdIds.kt before release
        binding.adView.adListener = object : com.google.android.gms.ads.AdListener() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                // Banner failure must never break the app; log and move on.
                android.util.Log.w("OfferwallAd", "Banner failed: ${error.message}")
            }
        }
        binding.adView.loadAd(AdRequest.Builder().build())
    }

    // ── Back navigation: let the WebView consume back first ───────────────────
    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onDestroy() {
        binding.adView.destroy()
        binding.webView.apply {
            removeJavascriptInterface(BRIDGE_NAME)
            stopLoading()
            destroy()
        }
        super.onDestroy()
    }

    companion object {
        /** Name the offerwall JS uses: window.<BRIDGE_NAME>.onOfferCompleted(...) */
        const val BRIDGE_NAME = "AndroidOfferwall"
    }
}
