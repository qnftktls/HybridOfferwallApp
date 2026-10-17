package com.davis.offerwall

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.davis.offerwall.ads.AdIds

/**
 * Initializes the Google Mobile Ads SDK once, off the main thread.
 *
 * Initialization is intentionally deferred out of the Activity so the first frame
 * is not blocked (see the OPTIMIZE_INITIALIZATION manifest flag).
 */
class OfferwallApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // In development, request test ads explicitly so impressions never hit a live account.
        // REMOVE / guard this block before a production release (Checklist step 4).
        if (AdIds.USE_TEST_ADS && BuildConfig.DEBUG) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    // Optional: add your device id here to always get test ads while coding.
                    // .setTestDeviceIds(listOf("YOUR_TEST_DEVICE_ID"))
                    .build()
            )
        }

        // Initialize the Ads SDK on a background thread to keep startup smooth.
        Thread {
            MobileAds.initialize(this) {
                // SDK ready. Banner loading in MainActivity can safely continue.
            }
        }.start()
    }
}
