package com.davis.offerwall.web

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.davis.offerwall.ui.PointsRepository

/**
 * The JavaScript bridge.
 *
 * The offerwall HTML (served inside the WebView) calls
 * `AndroidOfferwall.onOfferCompleted(...)` when a user finishes an offer.
 * The native side credits the balance and can react (toast, analytics, backend sync).
 *
 * SECURITY:
 *  - `addJavascriptInterface` exposes these methods to ANY page loaded in the WebView.
 *    Only load offerwall URLs you trust, and keep the WebView from navigating to
 *    arbitrary third-party pages (see OfferwallWebViewClient.shouldOverrideUrlLoading).
 *  - Never pass secrets into the WebView. The app should not hold provider API secrets;
 *    those stay on your server.
 *
 * CONTRACT (match this in your offerwall HTML):
 *   window.AndroidOfferwall.onOfferCompleted('offer_id', rewardPoints, 'signature')
 *   window.AndroidOfferwall.onOfferwallReady()
 *   window.AndroidOfferwall.onOfferwallError('reason')
 */
class OfferwallBridge(
    private val webView: WebView,
    private val onPointsChanged: (newBalance: Int, reward: Int, offerId: String) -> Unit,
    private val onLog: (String) -> Unit = {}
) {

    /**
     * Called from JS when an offer is verified complete.
     * All arguments arrive as strings/numbers from JavaScript.
     *
     * @param offerId  provider offer id — for reconciliation/analytics.
     * @param reward   points to credit (server-validated in production!).
     * @param signature optional HMAC/verify token from your server. Empty in this skeleton.
     */
    @JavascriptInterface
    fun onOfferCompleted(offerId: String, reward: Int, signature: String?) {
        // ⚠️ PRODUCTION NOTE: never trust the client for the reward amount.
        // Verify offer completion server-side (provider callback -> your backend -> signed event)
        // and only then credit. The direct credit below is for local development only.
        val newBalance = PointsRepository.addPoints(reward)
        onLog("Offer completed: id=$offerId reward=$reward sign=${signature?.take(8)} balance=$newBalance")
        onPointsChanged(newBalance, reward, offerId)
    }

    /** Called by the offerwall once its UI has loaded and is interactive. */
    @JavascriptInterface
    fun onOfferwallReady() {
        onLog("Offerwall ready")
        // Push current balance into the page, if the offerwall wants to render it.
        val balance = PointsRepository.points.value
        webView.post {
            webView.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('nativeBalance',{detail:$balance}));",
                null
            )
        }
    }

    /** Called by the offerwall on a fatal error (network, auth). Surface it if useful. */
    @JavascriptInterface
    fun onOfferwallError(reason: String?) {
        onLog("Offerwall error: $reason")
    }
}
