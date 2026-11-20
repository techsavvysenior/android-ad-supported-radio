# 5. Ad units and SDK keys come from Gradle properties, demo values by default

Status: accepted

## Decision

App ID, ad units, the AppsFlyer dev key and the Meta app ID and client token are read from
`opendial.*` Gradle properties or `OPENDIAL_*` environment variables into `BuildConfig` and the
manifest. Blank ad values fall back to Google's demo app ID and demo units, one value at a time;
blank attribution keys turn that SDK off.

## Consequences

- A clone builds and runs with test ads and no accounts.
- Real values live in `~/.gradle/gradle.properties` or CI secrets, never in the repo.
- `AdUnitConfig.releaseProblems()` names what would be wrong with shipping the current config (demo
  IDs, malformed IDs). The app logs it on start in release builds.
