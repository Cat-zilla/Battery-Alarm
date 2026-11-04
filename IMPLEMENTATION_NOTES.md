# Redesign and verification notes

This is an incremental, **unverified development snapshot**, not a release-ready build.

## Changes
- Neutral light/dark Material palette; dashboard battery status placed before preview, and neutral alarm accents.
- Both threshold sliders now have one-percent increment/decrement buttons and a larger value. Existing DataStore keys are unchanged; new writes are constrained to the existing UI ranges (max 10–100%, low 5–50%).
- Full-screen alarm shows live battery percentage, trigger reason and a prominent stop control. Lock-screen and service intent handling were retained. Alarm type is assigned before registering the sticky battery receiver so a low-battery alert is not treated as a max alert during initial registration.
- Separate monochrome adaptive and legacy launcher icons; Quick Settings vector is a single lightning bolt. Existing tile behavior is unchanged.
- Corrected app-start service restoration to include a low-alarm-only configuration, and cancel stale preview timeout jobs when previews are stopped or replaced.
- Restored the missing Unix Gradle wrapper script and missing release ProGuard rules file.

## Build
1. Install Android Studio with JDK 17 (the Android Gradle Plugin 8.2.2 requirement), Android SDK Platform 34 and the corresponding build tools.
2. Open this directory as a project and allow Gradle sync to download the declared dependencies.
3. Run `./gradlew testDebugUnitTest assembleDebug assembleRelease lint` (Windows: `gradlew.bat testDebugUnitTest assembleDebug assembleRelease lint`). An installable release APK requires your own signing configuration; never add signing secrets to the project.

## Verification and limitations
- Attempted the combined Gradle command above. It failed during dependency resolution with Maven TLS handshake failures before compilation. This environment also has no Android SDK or emulator. No build, lint, unit test, UI test, or physical-device test passed or was completed.
- The reference ZIP contained a split-APK installer archive with a single obfuscated `base.apk`; its archive contents were inspected, but its UI could not be run or reliably reconstructed here. Visual matching to the reference is **not verified** and this is not a claim of a close match.
- Existing service, notifications, DataStore, playback, TTS, setup, sharing and tile code are largely retained; their behavior is not verified. In particular the upstream project has no boot receiver despite its README's boot-persistence claim. It also uses persisted enable flags rather than an independently verified running-service state, and tile toggling is inconsistent when only the low alarm is enabled. Existing asynchronous alarm playback and preview use shared device audio/vibration resources and require real-device race testing. These gaps are not solved by this snapshot.
- Validate full-screen intents, locked-screen behavior, alarm cancellation on unplug, audio/TTS, vibration, background-start restrictions, rapid preview interactions, accessibility, light/dark rendering and reboot behavior on supported devices before shipping. No changes add INTERNET permission or networking dependencies.
