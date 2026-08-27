# Zeron Android

Native Android viewport (Kotlin + Compose, minSdk 26, phone portrait). No engine on-device — joins the same workspace/chat2 rooms via edge.

## Toolchain

- JDK 17
- Android SDK 34+ (compileSdk 36), NDK 26+ (for `zeron-loro-android` via UniFFI)
- Rust 1.85+ with targets `aarch64-linux-android`, `x86_64-linux-android`
- Gradle wrapper (`./gradlew`)

No secrets in repo; no absolute local paths.

## Commands (wrapper only)

```sh
cd apps/android
./gradlew :app:assembleDebug      # arm64 debug APK (also bundles x86_64 for emulator in debug)
./gradlew :app:assembleRelease    # arm64-only internal APK (Task 2.3)
./gradlew :app:testDebugUnitTest  # unit tests (no credentials, no network)
./gradlew lint                    # static analysis (Task 2.2; warnings not globally disabled)
./gradlew :app:connectedDebugAndroidTest  # instrumented + native smoke (x86_64 emulator)
```

Format check (if ktlint added): `./gradlew ktlintCheck` — documented but not required for green CI until pinned.

## ABI policy (Task 2.3)

- Release/internal APK: `arm64-v8a` only.
- Debug/emulator: `arm64-v8a` + `x86_64` (CI builds x86_64 artifact for emulator; APK for distribution stays arm64-only).
- `armeabi-v7a`/`x86` never packaged. Verify: `unzip -l app/build/outputs/apk/*/*.apk | grep lib/`.

## Native build (Task 2.5)

- Single entry point `NativeLoader.loadOnce()` (`System.loadLibrary("zeron_loro_android")`).
- Gradle invokes `cargo ndk` (or consumes prebuilt `jniLibs` if `ANDROID_NDK_HOME` missing — failure message names the missing toolchain).
- Reproducible: `crates/zeron-loro-android` pinned to `loro = 1.13`. No checked-in binaries.

## Dev edge

- Production config points to `https://edge.zeron.sh` (or env-provided base URL).
- Dev mode: `AUTH_MODE=dev` edge, bearer `user@org`, debug/test BuildConfig only. Never in release.

## Architecture guardrails

See `docs/android-protocol-contract.md` + `docs/android-loro-api.md`. Kotlin never does CRDT; all Loro via `ZeronLoroDoc`.
