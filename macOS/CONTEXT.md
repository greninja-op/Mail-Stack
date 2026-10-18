# Mailstack macOS — Context (macOS only)

Last updated: 2026-10-18

## Verified this session
| Check | Result | Evidence |
|-------|--------|----------|
| `cargo test -p mailstack-desktop-core` | **PASS** 18 tests | Session cargo output |
| Xcode / Swift compile / MacStoreTests | **BLOCKED** | No Mac. Unblock: Mac + XcodeGen + Xcode |
| UniFFI bridge to desktop-core | **BLOCKED** | Biggest remaining item — next step: generate UniFFI bindings on Mac and replace `MacStore` demo mirror |
| Live OAuth / notarization | **BLOCKED** | Secrets + Apple Developer ID |

## Phase status
0 done · 1–15 SwiftUI scaffold · 16–30 checklists · UniFFI pending
