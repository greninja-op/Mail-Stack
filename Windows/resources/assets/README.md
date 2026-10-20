# Resource pipeline (Phase 01)

Static assets for the Windows app (bundled company logos, import-flow marks, etc.)
land here. Build steps in later phases will copy selected files into the MSIX
layout under `Assets/`.

## Pipeline notes

1. Source assets live under `Windows/resources/`.
2. Debug/release cargo profiles do not embed secrets.
3. MSIX packaging (see `Windows/packaging/msix/`) maps these into the package.
4. Favicon / remote logo fetches are **runtime** (logo pipeline) — not build-time bundling of arbitrary web content.

## Placeholders

- `mailstack-mark.svg` — deferred (use user-supplied mark when provided)
- `gmail-mark.svg` — deferred (import conduit left mark; user-supplied / licensed only)
