# Mailstack "Breeze" backgrounds

Full-screen background artwork for the glassmorphism redesign (2026-10-13).
User-supplied, used exactly as provided — never recreated or approximated.

| File | Mode | Source |
|---|---|---|
| `light_mode_background.jpg.b64` | Light | Pastel blue / white / mint soft waves |
| `dark_mode_background.jpg.b64` | Dark | Deep navy / blue-green soft waves |

## Why base64?

The GitHub push API stores file content as text and corrupts raw binaries.
These files hold the exact JPEG bytes as base64 text, so they land in the
repo byte-exact. The app build uses the decoded JPEGs at
`android/app/src/main/res/drawable-nodpi/breeze_background_light.jpg` and
`breeze_background_dark.jpg` (same bytes, kept in the working tree like the
other binary drawables).

## Recover the JPEGs

```bash
base64 -d light_mode_background.jpg.b64 > breeze_background_light.jpg
base64 -d dark_mode_background.jpg.b64 > breeze_background_dark.jpg
```

Verify with md5sum: light `c161172ee39cb8e7199e6d5ea862a0f9`,
dark `280e32a31be5ea1618c2af65d0d67870`.
