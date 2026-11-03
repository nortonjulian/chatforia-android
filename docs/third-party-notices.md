# Android third-party notices

Profile → Legal & Support now offers Technology Across Chatforia (the shared website overview of web, backend, iOS and Android) and This App’s Licenses (the native reader). The shared overview does not replace this app’s bundled notices.

Google OSS plugin 0.13.0 still generates the raw notice resources. The reader parses metadata offsets as UTF-8 byte ranges, validates bounds and preserves text. The pinned 17.1.0 dependency remains in the inventory; its stock activities are no longer launched. No SDK level, signing or version changes.

## Build and device evidence

The previous classic viewer compiled and installed on the user's emulator. Release notice generation succeeded. Uploaded release resources contained 281 entries / 1,260,963 text bytes, with all ranges in bounds; 179 entries contained only URLs. The debug generator produced a 126-byte Debug License Info placeholder. This is why a normal debug build did not display the release inventory.

The previous viewer displayed the release list after a QA-only resource substitution, but had status-bar overlap and stock styling. This revision replaces that UI. It has not yet been compiled or exercised on Android: the editing environment has no Android SDK. XML and whitespace checks pass. Keep this PR draft pending the checks below.

## Reproducible emulator UI QA

Run from this branch's checkout:

```sh
./gradlew :app:assembleDebug -I scripts/license-reader-qa.init.gradle --no-configuration-cache --no-daemon
```

This opt-in script lets the debug generator finish, then copies freshly generated release notices into its output before packaging. Do not exclude debugOssLicensesTask: AGP consumes its task-backed resource provider. This is a UI preview of release notices, not verification of the debug inventory. Normal builds do not use the substitution.

Install the resulting debug APK on the chosen test emulator, then open the row through Profile (do not launch a different Android Studio project). Verify list scrolling, an embedded-text detail, a URL-only detail, back navigation/list position, rotation, large text, a dark Chatforia theme, and airplane-mode reading of embedded text. Check status/navigation icons and camera-cutout clearance. Verify other legal links retain their destinations.

Before merge, build and inspect the actual release artifact using the existing local signing configuration; verify resources and the same UI flows. Never share signing secrets.

## Coverage limits

This is not a completed license inventory. Many Maven POM entries provide license links only. A populated list does not prove copyright/NOTICE completeness or offline availability. Review exact shipped artifacts and supplement missing attribution, including native binaries (Twilio, JNA/LazySodium), fonts/assets and proprietary SDK notices. Do not label a proprietary SDK open source based on its wrapper.
