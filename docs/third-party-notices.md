# Android third-party notices

Profile → Legal & Support → Open Source Licenses launches Google's in-app notices activity. The existing row style and other legal links are preserved.

The pinned Google OSS plugin (0.13.0) processes dependency license metadata during the build. The matching documented reader dependency is play-services-oss-licenses 17.5.3, using its v2 activity. These versions and the Kotlin DSL resolution strategy follow https://developers.google.com/android/guides/opensource (reviewed November 2, 2026).

## Coverage limits

This integration is not a completed license inventory. Google documents that ordinary Maven POM entries contribute license names/links; its own transitive OSS notices may include full text. A working in-app screen is not proof that every copyright/NOTICE file is present or available offline. Audit the actual generated release output, and add missing attribution/full text from the corresponding artifacts or upstream releases. Separately review Twilio, JNA/LazySodium native binaries, fonts/assets, and SDK-specific notices. Do not present proprietary SDKs as open source based on their wrappers.

## Validation required before merge

1. In the normal Android checkout with its existing local SDK/signing configuration, run `./gradlew :app:assembleDebug :app:assembleRelease --no-daemon`. Do not change or share signing secrets to run the review.
2. Confirm plugin compatibility with this repository's AGP 9.2.1 / Gradle 9.4.1 and that generated license resources are packaged for both variants. Inspect build warnings and dependency coverage.
3. Install the debug build. Open Profile → Legal & Support → Open Source Licenses, open several library entries, and return to Profile. Check large text and dark mode.
4. Repeat without network access. Record entries that depend on a web link; supplement full text/attribution as required.
5. Confirm Privacy, Terms, SMS Policy, and Contact Support still open their original destinations.

Environment limitation: Gradle failed before project configuration because services.gradle.org was unreachable while downloading Gradle 9.4.1. No dependency resolution, Kotlin compilation, APK generation, instrumented tests, or completeness claim is made. XML parsing and git diff --check passed. Keep the PR draft until the build and coverage checks above are complete.
