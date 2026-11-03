# NOW

Updated 2026-10-22 from the repo as it sits. This is status, not history.

## Where things stand

- Android phone (`android/`) is the shipping app. `versionName` 1.0.40, `versionCode` 41 — APK built and delivered 2026-11-03 at the user's request (carries the in-main fixes after 1.0.39: category group header centring, company logos in Categories detail rows). Do not build or ship an APK unless the user asks.
- Android work through the recorded phases is in the archive. Still waiting on the user: the import rate-limiter, and moving the reference-only import animation into the app. Do not start those unasked.
- Windows (`Windows/` + `desktop-core/`) phases 00–27 are recorded complete. Phase 28 QA matrix and phase 29 checklist are drafted. Phase 30 release is blocked on signing and the Windows App SDK. The current shell is interim egui, not the final WinUI 3 app.
- Linux and macOS are backend crates only. No product UI. Do not start that UI from a backend task.
- iOS, iPadOS, and Android tablet scaffolds were deleted on 2026-10-20. Do not recreate them unless the user asks.

## Next

Wait for the user to name the platform and the task. Then read that platform's short `CONTEXT.md` and its `execution plan/<platform>/editor-rules.md`.

## Do not

- Read root `CONTEXT.md` to recover context.
- Touch `android/` unless the user asks.
- Claim an on-device, Linux desktop, or Mac launch from this Windows machine.
