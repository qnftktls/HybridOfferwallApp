package com.davis.offerwall.web

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Keeps the WebView scoped to your offerwall, and sends everything else to the
 * system browser instead of letting third-party pages run inside the app.
 *
 * This matters because the JS bridge is exposed to whatever page is loaded.
 * Allow-listing the offerwall host(s) is the cheap, effective hardening step.
 */
class OfferwallWebViewClient(
    private val context: Context,
    private val allowedHosts: Set<String>,
    private val onPageStarted: ((String) -> Unit)? = null,
    private val onPageFinished: ((String) -> Unit)? = null,
    private val onError: ((String) -> Unit)? = null
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        val url = request?.url?.toString() ?: return false
        val host = request.url.host ?: ""

        return if (allowedHosts.any { host == it || host.endsWith(".$it") }) {
            // Internal offerwall navigation: let the WebView handle it.
            false
        } else {
            // External link (offer terms, privacy policy, provider page): open outside.
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }.onFailure { onError?.invoke("No app can open: $url") }
            true // we handled it
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        url?.let { onPageStarted?.invoke(it) }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        url?.let { onPageFinished?.invoke(it) }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: android.webkit.WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            onError?.invoke("Page failed to load (${error?.errorCode})")
        }
    }
}
