# Test plan

## Automated (CI, every push)

| Level | What | Command |
|---|---|---|
| Core (JVM) | Station filtering and queries, interstitial pacing, ad-free pass, event mapping, pending events, milestones, startup gate, ad unit config | `./gradlew :core:test` |
| App (Robolectric) | Event names against AppsFlyer and Meta constants; screenshots of the real screens in light and dark | `./gradlew :app:testDebugUnitTest` |
| Lint | Android lint, errors fail the build | `./gradlew :app:lintDebug` |
| Build | Debug APK with all SDKs and the AppLovin adapter on the classpath, legacy `play-services-ads` excluded | `./gradlew :app:assembleDebug` |
| Traceability | Every automated use case has at least one test | `node scripts/traceability.mjs --check` |

## Manual, on a phone, before a release

Use a real phone; fill, mediation and background audio behave differently on an emulator.

1. **UC-03 background playback.** Start a station, lock the phone, wait two minutes. Audio continues;
   the lock screen and notification show the station and pause/play works. Unplug wired headphones:
   playback pauses.
2. **UC-04 consent.** Clear app data, set the UMP debug geography to EEA, launch. The form shows
   before any ad request (check Logcat for `OpenDialAds`); decline, and confirm no ad loads and the
   radio still plays. Settings shows "Privacy choices".
3. **UC-05 banner.** With demo units, the home feed shows one labelled test banner between
   "Trending" and the list; none in the player.
4. **UC-06 interstitial.** Wait two minutes, switch stations three times: the third switch shows a
   test interstitial, the stream is paused under it and the new station starts after closing it.
   Switch three more times within five minutes: no ad.
5. **UC-07 rewarded.** Tap "Watch a short ad", close early: no pass. Watch to the end: the player
   shows "Ad-free · 60 min left", the banner disappears, and switches show no interstitial.
6. **UC-08 mediation.** With real units and an AppLovin mediation group, open Ad Inspector on a
   registered test device and confirm the AppLovin
   adapter initialized and appears in the waterfall or bidding results.
7. **Attribution.** With a dev key and Meta IDs set, check AppsFlyer's SDK integration test and
   Meta Events Manager's test events for `af_content_view` and `fb_mobile_content_view` after
   playing a station.
