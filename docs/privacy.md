# Privacy notes

## What happens before consent

Nothing that serves ads or measures installs runs:

- The Meta SDK is installed but inert: `AutoInitEnabled`, `AutoLogAppEventsEnabled`,
  `AdvertiserIDCollectionEnabled`, `AutoLogMetaDataEnabled` and `AutoLogProductImprovementEnabled`
  are all `false` in the manifest, and the app ID is only set in code after consent.
- AppsFlyer is initialized (SDK 7 requires that on every cold start) but not started.
- The Mobile Ads SDK is not initialized and no ad is requested.
- Events logged in that window wait in memory (at most 50) and are dropped if consent never comes.

## Consent

Google's User Messaging Platform shows the consent form where it is required and writes the IAB
TCF strings to shared preferences. The Mobile Ads SDK and the AppLovin adapter read them, and
AppsFlyer reads them because `enableTCFDataCollection(true)` is set on every launch (it is not
persisted in SDK 7). Where the form applies, Settings offers "Privacy choices" to change it, as
Google requires.

## What is stored on the phone

- Favorite stations (name, stream URL, logo URL, tags, format), so the library works offline.
- The time the current ad-free pass ends.

Nothing else. There is no account and no backend of our own.

## What leaves the phone

- Requests to the Radio Browser directory: genre or name searches and one click count per started
  station, with a descriptive User-Agent. No identifiers.
- The audio stream request to the station's own server.
- After consent: ad requests to Google (and AppLovin through mediation), and attribution and
  events to AppsFlyer and Meta as listed in [ads-and-events.md](ads-and-events.md).

## Known gaps

- No way inside the app to delete data held by AppsFlyer or Meta; that goes through their own
  deletion processes.
- The country used for "Popular in ..." comes from the phone's locale, not location. It can be
  wrong for travellers; that is the trade for not asking for location.
- `AppLovinPrivacySettings.setDoNotSell(true)` is not wired to a US-state opt-out screen. Google
  recommends calling it before initializing the SDK when that opt-out applies.
