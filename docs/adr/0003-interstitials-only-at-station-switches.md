# 3. Interstitials only at station switches, with pacing in pure code

Status: accepted

## Context

A radio app has no levels or screens to finish; the only natural break is the listener choosing
another station. Interstitials anywhere else interrupt audio people are actively listening to.

## Decision

`InterstitialPolicy` allows an interstitial only on a station switch, and only when the session is
at least 2 minutes old, the last interstitial was at least 5 minutes ago, there have been at least
3 switches since, and no ad-free pass is running. The pacing resets only when an ad actually
showed, so a missed fill doesn't cost the listener a free stretch. The stream pauses under the ad.

## Consequences

- At most one interstitial per 5 minutes, and none for listeners who stay on a station.
- The numbers are in one `Config` and covered by tests, so tuning them is a one-line change.
