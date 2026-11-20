# Ads and events

## Where ads appear

| Format | Where | When | Never |
|---|---|---|---|
| Inline adaptive banner | One slot in the home feed, between "Trending" and the station list, under an "Advertisement" label | Once the SDK is ready and an ad has loaded; the slot takes no space until then | In the player, next to playback controls, or during an ad-free pass |
| Interstitial | Full screen, between two stations | On a station switch, if [`InterstitialPolicy`](../core/src/main/kotlin/com/example/opendial/core/ads/InterstitialPolicy.kt) allows it | In the first 2 minutes of a session, less than 5 minutes or 3 switches after the last one, during an ad-free pass, or mid-stream |
| Rewarded | Full screen, from the player's "Watch a short ad for 1 hour ad-free" button | Only when the listener asks for it | Granted on close; the hour is only added when the SDK reports the reward |

The stream pauses while a full-screen ad is on screen and the next station (or the same one,
after a rewarded ad) starts when it closes. Radio is the product; an ad talking over it would
be the fastest way to lose the listener.

## Networks

- **Google Mobile Ads Next-Gen SDK** (`ads-mobile-sdk` 1.7.0). One SDK for AdMob and Ad Manager.
  Set `opendial.ads.network=admanager` to run on Google's Ad Manager demo units instead of the
  AdMob ones, or put real unit IDs from either network in the `opendial.ads.*Unit` properties.
- **AppLovin through AdMob mediation** (`com.google.ads.mediation:applovin` 13.7.0.0, verified by
  Google with AppLovin SDK 13.7.0). The adapter needs no code and no SDK key in the manifest;
  AppLovin is added to the mediation group (bidding or waterfall) in the AdMob console, and AppLovin
  must be on the consent ad-partner lists there. Consent from the UMP form reaches AppLovin through
  the adapter.

## Events

All events go through [`EventMapping`](../core/src/main/kotlin/com/example/opendial/core/analytics/AnalyticsEvent.kt),
so AppsFlyer and Meta always receive the same thing under their own standard names.

| App event | AppsFlyer | Meta | Parameters |
|---|---|---|---|
| Station played | `af_content_view` | `fb_mobile_content_view` | content id, content type `radio_station`, genre (when picked from a genre) |
| Favorite added | `af_add_to_wishlist` | `fb_mobile_add_to_wishlist` | content id, content type |
| Ad shown | `af_ad_view` | `AdImpression` | ad format (banner, interstitial, rewarded) |
| Listening milestone | `listen_milestone` | `listen_milestone` | minutes (10, 30, 60) |
| Ad-free pass granted | `ad_free_pass_granted` | `ad_free_pass_granted` | minutes |

Installs are attributed by the SDKs themselves once started. Nothing in the events identifies a
person: no search text, no free-form input. An app test pins these names to the SDKs' own
constants (`AFInAppEventType`, `AppEventsConstants`).

## Start order

```
launch
  |-- AppsFlyer init (every cold start; SDK 7 does not start by itself)
  |-- UMP: request consent info, show the form if required
  v
consent resolved ---- canRequestAds = false --> nothing starts this session; the radio still plays
  |
  | canRequestAds = true
  v
StartupGate --> Mobile Ads init (background thread), then preload interstitial and rewarded
            --> Meta SDK: app ID and token set, initialized, auto-logging and ad ID turned on
            --> AppsFlyer start(), as soon as its session-ready callback has also arrived
            --> events held so far are sent, in order
```

The decisions in that diagram live in [`StartupGate`](../core/src/main/kotlin/com/example/opendial/core/startup/StartupGate.kt)
and are unit tested for every order the signals can arrive in.
