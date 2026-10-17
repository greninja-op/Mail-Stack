# Integration logos — base64 parts (2026-10-17)

The two Google product icons the Integrations rows show (user-supplied,
on a white circular backing) live here as base64 split parts, NOT as raw
PNGs: the GitHub push path corrupts raw binaries (double-base64), so raw
binaries are never pushed. The raw PNGs live in the build working tree
(`android/app/src/main/res/drawable-nodpi/ic_calendar_logo.png` and
`ic_tasks_logo.png`) and in the built APK.

## Reassembly

From this directory:

```sh
cat ic_calendar_logo.png.b64.part* | base64 -d > ic_calendar_logo.png
cat ic_tasks_logo.png.b64.part* | base64 -d > ic_tasks_logo.png
```

Verify (must match):

- `ic_calendar_logo.png` — SHA-256
  `936fa24eab99cfe64547a707b6d7c4a8cf804637707db518f0281635099fdc95`
- `ic_tasks_logo.png` — SHA-256
  `8c2d863682de357a853dcdd6908fd9353233b163ba3af9d12a58c80dcc61bd3c`

## Wiring

`CompanyLogoResolver.bundledRes` maps `calendar.google.com` →
`R.drawable.ic_calendar_logo` and `tasks.google.com` →
`R.drawable.ic_tasks_logo`, so the Integrations rows resolve the real
product icons from the bundled path instead of falling through to the
Google "G" favicon. `CompanyLogoResolverTest` covers both mappings.
