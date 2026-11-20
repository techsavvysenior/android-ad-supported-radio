# 4. A rewarded ad buys an ad-free hour

Status: accepted

## Context

Rewarded ads need a reward the listener wants. In a free radio app that is fewer ads.

## Decision

Finishing a rewarded ad grants 60 minutes with no banner and no interstitials. Rewards stack, up to
3 hours ahead, so heavy listeners can bank time but the app never gives away unlimited ad-free
listening. The pass is one timestamp in DataStore, so it survives restarts. It is granted only on
the SDK's reward callback, never on close.

## Consequences

- The offer is phrased as what the listener gets, sits under the player controls, and is hidden
  once the pass is full.
- Server-side verification is not used; the reward has no resale value, so client-side is enough.
  An app selling a real-money reward should add SSV.
