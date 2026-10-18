# Mailstack Linux — Context (Linux only)

Last updated: 2026-10-18

## Verified this session
| Check | Result | Evidence |
|-------|--------|----------|
| `cargo test --manifest-path Linux/Cargo.toml` / workspace | **PASS** 2 tests | Session output |
| `cargo run --manifest-path Linux/Cargo.toml` | **PASS** prints `ok` | Session output |
| GTK / libadwaita GUI | **BLOCKED** | **Biggest remaining item.** Decision: pin **GTK4 + libadwaita** when GUI work starts (not iced). Needs `libadwaita-1-dev` / pkg-config on the build host. |
| Distro packaging | **BLOCKED** | Release phase (`.deb` / Flatpak) |

## Phase status
0 done · 1–20 core wiring via CLI · 21–30 GUI + packaging blocked
