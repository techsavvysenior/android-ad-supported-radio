# Use cases

Every behavior the app promises, with an id. Kotlin tests carry the id in their name, and
`node scripts/traceability.mjs --check` (run in CI) fails if an automated use case has no test.
The generated matrix is in [traceability.md](traceability.md).

| Id | Use case | How it is verified |
|---|---|---|
| UC-01 | Browse real stations by genre, showing only streams that passed their last check and use HTTPS | automated |
| UC-02 | Search stations by name, and list what is popular in the listener's country without asking for location | automated |
| UC-03 | Keep playing with the screen off, with notification, lock-screen and headset controls | manual |
| UC-04 | Ask for consent first; no ad or attribution SDK starts before the consent step allows it | automated |
| UC-05 | Show one inline adaptive banner in the home feed, labelled as an ad, never inside the player | manual |
| UC-06 | Show an interstitial only when switching stations: not in the first 2 minutes, 5 minutes and 3 switches apart, with the stream paused during the ad | automated |
| UC-07 | A finished rewarded ad buys 60 minutes with no banners or interstitials, stacking up to 3 hours | automated |
| UC-08 | AppLovin fills through AdMob mediation when Google's demand doesn't | manual |
| UC-09 | Send plays, favorites and ad views to AppsFlyer and Meta as their standard events, with no free text or personal data | automated |
| UC-10 | Hold events logged before an SDK starts and send them in order once it does, with a bounded buffer | automated |
| UC-11 | Report listening milestones at 10, 30 and 60 minutes, once each per session | automated |
| UC-12 | Run on Google's demo ad units and without attribution when nothing is configured; flag demo IDs in a release | automated |
| UC-13 | Spread requests across the directory's mirrors and count a click when a station starts, as the directory asks | automated |

Manual cases are covered by the release checklist in [test-plan.md](test-plan.md), because they
need a real phone, live ad serving or the AdMob console.
