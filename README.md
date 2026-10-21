# Mailstack

Mailstack is a private mail organizer. Gmail stays the source of truth. The apps add a local layer for classification, search, priority, companies, and action suggestions. Nothing in this repository sends mail, and remote images inside a message are not fetched.

The Android phone app is the product that has been carried through a real release. The other folders are desktop and tablet clients at different stages. Read [CONTEXT.md](CONTEXT.md) before changing the project. It is the continuity log for decisions, versions, and rules.

Repository: [greninja-op/Mail-Stack](https://github.com/greninja-op/Mail-Stack)

## What each platform is today

| Platform | Folder | Language | Status |
| --- | --- | --- | --- |
| Android phone | `android/` | Kotlin, Jetpack Compose | Shipping app. Application id `com.greninjaop.mailorganizer`. Version name `1.0.39`, version code `40`. |
| Android tablet | `AndroidTablet/` | Kotlin, Jetpack Compose | Wide-screen UI over a local demo mailbox. Not wired to the phone database or to live Gmail. |
| iPhone | `iOS/` | Swift, SwiftUI | Phone UI over a local demo mailbox. Requires Xcode. Not built on Windows. |
| iPad | `iPadOS/` | Swift, SwiftUI | Split-view UI over a local demo mailbox. Requires Xcode. Not built on Windows. |
| Windows | `Windows/` | Rust, egui | Interim desktop window on the shared Rust core. Packaging for the Microsoft Store is not finished. |
| Linux | `Linux/` | Rust | Backend adapters only. There is no Linux window. The earlier UI was removed because it was not good enough. |
| macOS | `macOS/` | Rust | Backend adapters only. SwiftUI has not been started. The earlier UI was removed because it was not good enough. |
| Shared desktop core | `desktop-core/` | Rust | Platform-neutral contracts used by Windows, Linux, and macOS. |

Phone business data lives in the Android app. There is no Kotlin Multiplatform module in this repository. Desktop clients share `desktop-core` instead of copying those rules into Swift or a second Kotlin tree.

## Product shape

The phone destinations are Home, Mail, Categories, Companies, and Actions. Mail folders include the inbox, Primary, Promotional, Social, Spam, and Starred. A wide Android window moves those destinations onto a leading rail and places the message list beside the open message. iPad uses a sidebar with the same split. A narrow window falls back to one column.

Windows uses a three-pane mail window. Its palette is separate from the phone: pastel blue `#4285F4` / `#8AB4F8` on canvas `#F7F9FC` (dark `#151A22`). The phone, iPhone, iPad, and Android tablet stay on the violet identity `#5238E8` / `#B9A8FF`, with light background `#F6F3FF` and dark background `#131120`.

Linux and macOS do not have that UI yet. Their context files say so on purpose: [Linux/CONTEXT.md](Linux/CONTEXT.md) and [macOS/CONTEXT.md](macOS/CONTEXT.md).

## Android phone

The phone module is the reference implementation.

- Sign-in is Google OAuth in the system browser, with PKCE. One sign-in is intended to cover Gmail read, Calendar events, and Tasks. `gmail.modify` stays behind a separate confirmation. The redirect scheme is `com.greninjaop.mailorganizer`.
- Local storage is Room (schema version 10) plus DataStore. Account id is a foreign-key boundary. Tokens are not columns in the mail tables.
- Sync pages through the Gmail API, keeps a cursor, and paces requests. A demo or fixture account does not sync.
- On-device work covers classification, company detection, priority, full-text search, rules and corrections, dates and deadlines, conversation state, action suggestions, privacy tools, and an optional AI fallback that is off unless the user turns it on.
- HTML from mail is sanitized. JavaScript is not executed. Remote images are not downloaded.

Build from `android/` with JDK 17 and Android SDK 35:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Point the SDK at the machine with `ANDROID_HOME` or a gitignored `local.properties`. Do not commit keystores, OAuth client secrets, or API keys.

## Shared desktop core

`desktop-core` is a Rust 2021 library (`#![forbid(unsafe_code)]`). It holds the contracts the desktop apps are supposed to share: accounts, OAuth URL and PKCE construction, a Gmail API trait, sync progress, mailbox models, classification, search query building, rules, HTML sanitizing, privacy redaction, and an AI router that defaults to off.

It does not open a SQLite file and it does not contain an HTTP client. Persistence in the crate is in memory. A real Gmail call, a real keychain, and a real database file are platform work that is not finished. `MemoryCredentialStore` is for tests.

The Cargo workspace at the repository root contains `desktop-core` and `Windows`. Linux and macOS are separate workspaces so they are not mixed into the Windows build.

```bash
cargo test -p desktop-core --lib
cargo test --manifest-path Linux/Cargo.toml
cargo test --manifest-path macOS/Cargo.toml
```

On this Windows machine those Linux and macOS suites passed (17 tests and 11 tests). That run does not launch a Linux desktop or a Mac app. Linux secret service, macOS Keychain, and the system browser all return “not available” here rather than pretending a secret was saved or a login finished.

## Windows

The Windows app crate is `mailstack-windows`. The window is egui / eframe 0.29, not WinUI. WinUI 3 and MSIX remain the packaging path and are blocked on signing. See [Windows/packaging/RELEASE_STATUS.md](Windows/packaging/RELEASE_STATUS.md).

The toolchain file [Windows/rust-toolchain.toml](Windows/rust-toolchain.toml) pins stable Rust and `x86_64-pc-windows-msvc`. Linking needs Visual Studio Build Tools with the C++ workload and the Windows SDK. Without a live OAuth client id the window shows demo mail, and it labels that state as demo.

```powershell
cargo run -p mailstack-windows
```

Hierarchy and spacing for that window followed the rules in [greninja-op/DesignSoul](https://github.com/greninja-op/DesignSoul). DesignSoul is a design reference. It is not a dependency.

## iPhone, iPad, and Android tablet

These three clients copy the phone’s information architecture and violet tokens. Their mail is a local demo set. They do not open the phone’s Room database and they do not complete Google sign-in.

| Client | Toolchain | Identity |
| --- | --- | --- |
| iPhone | Xcode 16, iOS 17, SwiftUI, device family iPhone | `com.greninjaop.mailstack` |
| iPad | Xcode 16, iOS 17, SwiftUI, device family iPad | `com.greninjaop.mailstack.ipad` |
| Android tablet | Android Gradle Plugin 8.13.2, Kotlin 2.3.21, Compose BOM 2026.06.01, minSdk 26 | `com.greninjaop.mailstack.tablet` |

The tablet module does not ship a Gradle wrapper. Open it from Android Studio, or run Gradle with a local installation and `ANDROID_HOME` set. Dynamic color is off so the violet brand does not follow the wallpaper.

iPhone and iPad projects use Xcode’s synchronized folders. There is no Xcode on the Windows machine used for this repository, so those projects have not been compiled here.

## Tech stack

Versions below are the ones pinned in the tree.

| Area | Choice | Where it is declared |
| --- | --- | --- |
| Phone language | Kotlin 2.3.21 | `android/gradle/libs.versions.toml` |
| Phone UI | Jetpack Compose, Material 3, Compose BOM 2026.06.01 | same catalog |
| Android Gradle Plugin | 8.13.2 | same catalog |
| Phone compile / min SDK | 35 / 26 | `android/app/build.gradle.kts` |
| Phone database | Room 2.8.5, schema 10 | catalog and `AppDatabase.kt` |
| Phone preferences | DataStore 1.2.1 | catalog |
| Navigation and lifecycle | Navigation 2.9.8, Lifecycle 2.10.0 | catalog |
| Coroutines | 1.11.0 | catalog |
| Gmail access | Google API Client and the Gmail API service (`google-api-services-gmail`) | catalog |
| Token storage on Android | AndroidX Security Crypto | catalog |
| Phone tests | JUnit 4.13.2, MockK, Turbine, Robolectric 4.17 | catalog |
| Desktop language | Rust, edition 2021 | root `Cargo.toml` |
| Desktop serialization and errors | serde, thiserror | root `Cargo.toml` |
| PKCE hashing | sha2, base64 | root `Cargo.toml` |
| Windows UI | egui and eframe 0.29 | `Windows/Cargo.toml` |
| Windows OS calls | `windows` crate 0.58 | `Windows/Cargo.toml` |
| Apple UI | SwiftUI on iOS 17 | `iOS/` and `iPadOS/` Xcode projects |

Third-party code is pulled from Google Maven, Maven Central, and crates.io by those manifests. This repository does not vendor those libraries.

## Repository map

```
android/            Phone app (source of truth)
AndroidTablet/      Tablet Compose UI
iOS/                iPhone SwiftUI app
iPadOS/             iPad SwiftUI app
Windows/            Windows egui app
desktop-core/       Shared Rust contracts
Linux/              Linux backend adapters and context
macOS/              macOS backend adapters and context
execution plan/     Phase plans per platform
CONTEXT.md          Continuity log
```

Plans live under `execution plan/<platform>/`. A plan file is not evidence that the phase is implemented. Where a plan and the current phone UI disagree, the phone UI wins.

## Privacy and limits

- Mail HTML is untrusted. It is sanitized before display.
- Logs are not a place for bodies, tokens, or passwords.
- AI output cannot send, delete, or downgrade a security classification. The desktop AI router ships disabled.
- Company logos on the phone follow a fixed pipeline: a bundled logo, then a catalog asset, then a favicon lookup, then a letter. People stay on letter avatars. A favicon lookup sends the sender domain to Google. The desktop and tablet demos use letter circles only.
- Do not commit `.env` files, `local.properties`, keystores, or OAuth client secrets.

## License

The desktop Cargo workspace is marked Proprietary. This repository does not include a separate license file. Do not assume an open-source grant.
