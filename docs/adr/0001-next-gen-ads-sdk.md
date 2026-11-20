# 1. Use the Google Mobile Ads Next-Gen SDK, not play-services-ads

Status: accepted, November 2026

## Context

In July 2026 Google made the GMA Next-Gen SDK (`com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk`)
the preferred Android SDK and called `play-services-ads` legacy. Google's schedule: 25.x is
deprecated on June 30, 2027 (ads still serve, no support) and sunset on June 30, 2028 (ads may stop
serving and Play can block releases).

## Decision

Build on the Next-Gen SDK 1.7.0 and exclude `play-services-ads` and `play-services-ads-lite` from
every configuration, so a transitive dependency can't bring the legacy SDK back.

## Consequences

- One SDK covers AdMob and Ad Manager; the network is chosen by the ad unit ID.
- Initialization takes an `InitializationConfig` with the app ID and must run off the main thread.
- Ad callbacks arrive on a background thread. Everything that touches UI state is posted to the
  main scope (see `Ads.kt`).
- Mediation adapters must be versions verified for Next-Gen; AppLovin 13.7.0.0 is.
- Most production apps are still on the legacy SDK. The mapping matters for migration work:
  `AdView` and `AdRequest` keep their names, `InterstitialAd.load` takes the request without a
  context, `FullScreenContentCallback` becomes `InterstitialAdEventCallback`/`RewardedAdEventCallback`,
  and `BannerAdRequest` carries the size.
