# Synthetic Android forwarding UI QA — 2026-11-10

A separate read-only arm64 emulator ran on `emulator-5556` in airplane mode. Existing `emulator-5554` was not touched. The instrumentation test mounted the real Compose `ForwardingSettingsView` with an in-memory fake store and no real provider effects.

The UI test observed account A's text destination, toggled SMS forwarding off, saved the disabled state while retaining its destination, and switched to account B. B's call destination appeared and A's text destination was absent. Instrumentation reported **OK (1 test)**. The disposable emulator was stopped afterward.

The Android unit tests from the first pass passed **3/3** and were not repeated because the production forwarding code did not change in this follow-up. Offline `assembleDebug` and `assembleDebugAndroidTest` passed. Two existing Compose smoke-test call sites were updated for current required parameters so the instrumentation APK could compile; no test assertion was removed.

This is an automated emulator UI observation. Interactive phone/email edits, save/reopen across process restart, a deployed settings endpoint, physical devices, and real carrier forwarding remain unverified. No production or Stage 5/6 recovery work was performed.
