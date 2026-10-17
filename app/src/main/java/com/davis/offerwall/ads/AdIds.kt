package com.davis.offerwall.ads

/**
 * Single source of truth for every ad unit id used by the app.
 *
 * ⚠️ REPLACE THESE WITH YOUR REAL ADMOB IDs (Checklist step 2 & 3 in README.md).
 *
 * The constants below are Google's OFFICIAL TEST ids. They always fill and are safe
 * to ship in a debug build, but they earn NOTHING. You must swap them before release.
 *
 * Where to get yours: https://admob.google.com -> Apps -> Ad units.
 *   - App ID            : looks like `ca-app-pub-################~##########` (tilde ~)
 *   - Banner unit id    : looks like `ca-app-pub-################/##########` (slash /)
 *
 * The App ID is wired in app/build.gradle.kts (manifestPlaceholders["admobAppId"]).
 * Banner / interstitial / rewarded ids are wired right here.
 */
object AdIds {

    /** Bottom home banner — Google official test id. REPLACE before release. */
    const val BANNER_HOME: String = "ca-app-pub-3940256099942544/6300978111"

    /** Reserved for the next screens (offer detail). Google official test id. REPLACE before release. */
    const val INTERSTITIAL_OFFER_DETAIL: String = "ca-app-pub-3940256099942544/1033173712"

    /** Optional rewarded ad ("watch to double reward"). Google official test id. REPLACE before release. */
    const val REWARDED_BONUS: String = "ca-app-pub-3940256099942544/5224354917"

    /**
     * While developing, keep this `true` so you never accidentally tap a live ad.
     * The AdMob SDK logs a warning if you request test ads on a non-test device.
     */
    const val USE_TEST_ADS: Boolean = true
}
