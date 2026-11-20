# 2. Nothing that serves ads or measures installs starts before consent

Status: accepted

## Context

Three SDKs want to start early: Mobile Ads, AppsFlyer and Meta. Each has its own way to delay,
AppsFlyer SDK 7 adds a session-ready callback that must also have fired, and the signals arrive in
any order on any thread. Getting this wrong is the most common way an ad-supported app ends up
collecting identifiers before consent.

## Decision

A small pure class, `StartupGate`, takes the two signals (consent resolved, AppsFlyer session
ready) and returns which SDKs may start now, each at most once. The app only executes what it
returns. Meta is held back by manifest flags until then; events wait in a bounded buffer.

## Consequences

- Every arrival order is unit tested on the JVM.
- If consent says ads may not be requested, nothing starts for the session and the radio still works.
- Consent from the previous session is honored at launch, as Google's samples do, so returning
  listeners don't wait for the form round-trip.
