# doorDuck contributor guide

Before changing QR timing, refresh, retries, readiness, or widget visibility, read `docs/HOW_DOOR_DUCK_WORKS.md`.

- Keep doorDuck focused on obtaining and displaying one QR pass.
- Keep user preferences in the platform settings store.
- Keep built-in timing parameters in `DoorDuckBehavior.kt`.
- Do not add profiles, configuration import/export, revision history, or a second settings store without an explicit product requirement.
- Trace every policy change to its Android/iOS call sites and cover concrete times in tests.
- Never place credentials, endpoints, user identifiers, or QR data in documentation or logs.

Verify Kotlin changes with `./gradlew :app:testDebugUnitTest`. Run `./gradlew :app:lintDebug` for Android UI, worker, manifest, or resource changes.
