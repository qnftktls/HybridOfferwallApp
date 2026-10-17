# Hybrid Offerwall App — AdMob banner + WebView offerwall (Android / Kotlin)

A working, minimal **hybrid monetization home screen** in Android native (Kotlin):

```
┌───────────────────────────────────────────┐
│  Rewards Hub                    1,250 pts  │  ← header (app name + live balance)
├───────────────────────────────────────────┤
│                                            │
│            OFFERWALL (WebView)             │  ← scrollable offer list
│            provider HTML + JS bridge       │
│                                            │
├───────────────────────────────────────────┤
│            AdMob BANNER (test id)          │  ← fixed bottom banner
└───────────────────────────────────────────┘
```

Runs out of the box with **Google's official test ad units** and a **bundled
placeholder offerwall page** — no accounts required to see it work. Swap in your
real IDs and provider URL following the checklist below.

> Stack note: this is the recommended **Kotlin native** build. If you'd rather ship
> **Flutter** or **React Native**, the same three-part structure (header / WebView
> offerwall / bottom banner) maps 1:1 — say the word and this gets rewritten for
> that stack.

---

## 1. Project structure

```
HybridOfferwallApp/
├── settings.gradle.kts                 # module wiring + repositories
├── build.gradle.kts                    # top-level plugin versions
├── gradle.properties                   # AndroidX / Kotlin flags
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts                # deps, SDK levels, AdMob App ID placeholder
    ├── proguard-rules.pro              # keeps Ads SDK + JS bridge under R8
    └── src/main/
        ├── AndroidManifest.xml         # INTERNET, AdMob App ID meta-data
        ├── assets/
        │   └── offerwall_placeholder.html    # demo offerwall (shows the bridge)
        ├── java/com/davis/offerwall/
        │   ├── OfferwallApp.kt          # Application: initializes the Ads SDK
        │   ├── MainActivity.kt          # header + WebView + banner wiring
        │   ├── ads/AdIds.kt             # ⚠️  ALL ad unit ids live here
        │   ├── ui/PointsRepository.kt   # balance state (StateFlow) — swap for your backend
        │   └── web/
        │       ├── OfferwallBridge.kt        # @JavascriptInterface callbacks
        │       └── OfferwallWebViewClient.kt # host allow-list + error handling
        └── res/
            ├── layout/activity_main.xml
            ├── values/            (strings, colors, dimens, themes)
            ├── values-night/colors.xml      # dark palette
            ├── drawable/          (pill background, icons)
            ├── mipmap-anydpi*/     (launcher icon)
            └── xml/               (network security, backup rules)
```

## 2. Build & run

**Requirements:** Android Studio Ladybug+ (or JDK 17 + Android SDK 35), Android SDK
Platform 35, an emulator/device on API 24+.

```bash
# From the project root (HybridOfferwallApp/)
./gradlew :app:assembleDebug        # build a debug APK
./gradlew :app:installDebug         # install on a connected device/emulator
```
Or: Android Studio → **Open** the `HybridOfferwallApp` folder → select an emulator →
**Run ▶**.

On first run you'll see the placeholder offerwall. Tap any offer card or the
"Simulate: complete top offer" button — the header balance updates via the native
bridge, proving the WebView ↔ Kotlin path works end to end.

---

## 3. Architecture & data flow

```
Offerwall HTML  ──window.AndroidOfferwall.onOfferCompleted(id, pts, sig)──▶  OfferwallBridge
                                                                                  │
                                                          PointsRepository.addPoints(pts)
                                                                                  │
                                       StateFlow<Int> collected in MainActivity ──▶ header balance
```

**Header** — `MainActivity.setupBalance()` observes `PointsRepository.points` and
formats it with `NumberFormat`. Replace the in-memory repository with Room /
DataStore / your backend API without touching the UI.

**Center** — the WebView loads either the bundled placeholder (`file:///android_asset/…`)
or your provider URL (`MainActivity.offerwallUrl`). Only hosts in `allowedHosts`
stay inside the app; every other link opens in the system browser (safety, since
the JS bridge is exposed to whatever page loads).

**Bottom** — an `AdView` anchored to the parent bottom, loaded with
`AdIds.BANNER_HOME`.

### JS ↔ native bridge contract

| Direction | Call | Meaning |
|-----------|------|---------|
| JS → native | `AndroidOfferwall.onOfferCompleted(offerId, reward, signature)` | Credit reward for a finished offer |
| JS → native | `AndroidOfferwall.onOfferwallReady()` | Page is interactive; native replies with current balance |
| JS → native | `AndroidOfferwall.onOfferwallError(reason)` | Report a fatal offerwall error |
| native → JS | `window.dispatchEvent(new CustomEvent('nativeBalance', {detail: n}))` | Push balance into the page |

The bundled `offerwall_placeholder.html` demonstrates all four calls. Point your
provider's page (or your own wrapper page) at the same `window.AndroidOfferwall`
object to go live.

---

## 4. Checklist — replace before a production release

- [ ] **1. Application ID / package.** Rename `com.davis.offerwall` (package + `namespace` +
  `applicationId`) to your own if desired.
- [ ] **2. AdMob App ID.** Edit `app/build.gradle.kts` →
  `defaultConfig.manifestPlaceholders["admobAppId"] = "YOUR_APP_ID"` (format `ca-app-pub-…~…`).
- [ ] **3. Ad unit IDs.** Edit `app/src/main/java/com/davis/offerwall/ads/AdIds.kt` →
  `BANNER_HOME`, and (when used) `INTERSTITIAL_OFFER_DETAIL`, `REWARDED_BONUS`
  (format `ca-app-pub-…/…`).
- [ ] **4. Turn off test-ads mode.** In `AdIds.kt` set `USE_TEST_ADS = false` (and remove/guard
  the test configuration block in `OfferwallApp.kt`).
- [ ] **5. Offerwall provider.** Get a publisher account + app ID from your chosen
  provider (Tapjoy / adjoe / Ayet / AdGem / …). This step is **yours** — it needs
  your business identity, app store listing, and payment details. Then:
  - set `MainActivity.offerwallUrl` to the publisher URL they give you (with your
    app id / user id / signature), and
  - add the provider's host to `allowedHosts`.
  - If the provider is **SDK-based** (adjoe Playtime, some Tapjoy products), also
    add their SDK dependency and call `showOfferwall()` from a button; the WebView
    path in this project is for URL/iframe-style walls.
- [ ] **6. Server-side reward verification.** Never credit points from the client in
  production. Wire provider postbacks → your backend → a signed event the app
  consumes. (`OfferwallBridge.onOfferCompleted` marks exactly where to verify.)
- [ ] **7. Privacy & compliance.** Add a privacy policy link, consent handling for
  EEA/UK (Google UMP / GDPR), COPPA/age gate if relevant, and an ATT-style
  disclosure on iOS later. Third-party cookies are enabled in
  `MainActivity.setupWebView()` — review with your provider.
- [ ] **8. Branding.** Update `res/values/strings.xml` (`app_name`), palette in
  `res/values/colors.xml` + `res/values-night/colors.xml`, and launcher icons.

## 5. Extending to the next screens

The structure is intentionally ready for these additions:

**Offer detail screen** — add `OfferDetailActivity` + `activity_offer_detail.xml`.
Pass the offer id from the WebView via the bridge (extend `OfferwallBridge` with
`openOfferDetail(String offerId)`), and deep-link into the detail screen:

```kotlin
// in OfferwallBridge
@JavascriptInterface
fun openOfferDetail(offerId: String) {
    // navigate on the UI thread
    onOpenOfferDetail(offerId)
}
```
Use `AdIds.INTERSTITIAL_OFFER_DETAIL` for a full-screen ad on entry (respect
frequency caps), and show the reward + provider terms.

**Reward history screen** — create a `RewardTransaction` Room entity and turn
`PointsRepository` into a repository backed by Room/your API. Add a
`RewardHistoryActivity` with a `RecyclerView` (list item = offer title, points,
date, status). The header balance can then link to it.

Because the balance already flows through a single `StateFlow`, both screens stay
consistent automatically — no rework of the home screen.

---

## 6. Assumptions & limitations (stated up front)

1. **Kotlin native** was chosen as the primary stack; Flutter/RN rewrite offered on request.
2. The offerwall is a **WebView placeholder**; real provider integration needs your
   publisher account and (for some providers) their SDK. Accounts and keys are yours.
3. Rewards are **credited locally** just to demonstrate the bridge — production
   requires server-side verification (checklist step 6).
4. AdMob uses **official test IDs**, so no revenue is generated until you replace them.
5. Dark/light theme follows the system setting automatically (`Theme.Material3.DayNight`);
   the WebView placeholder also adapts via `prefers-color-scheme`.
6. Accessibility baseline: ≥48dp touch targets, content descriptions, scalable sp text,
   high-contrast palette. A full accessibility audit is recommended before release.
