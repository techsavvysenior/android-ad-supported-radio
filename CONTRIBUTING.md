# Contributing

1. `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` must pass. CI runs the same.
2. A new behavior gets a row in [docs/use-cases.md](docs/use-cases.md) and a test whose name starts
   with its id, then `node scripts/traceability.mjs --write`.
3. Pacing, rewards, event names and start-up order belong in `:core`, where they can be tested
   without Android. `:app` only wires them to the SDKs.
4. Never commit real ad unit IDs, an AppsFlyer dev key or Meta credentials. Use
   `~/.gradle/gradle.properties`.
5. UI changes: the screenshot tests write `docs/screenshots`; CI commits them on main.
