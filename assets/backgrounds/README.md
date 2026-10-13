# Mailstack "Breeze" backgrounds

Full-screen background artwork for the glassmorphism redesign (2026-10-13).
User-supplied, used exactly as provided — never recreated or approximated.

| File | Mode | Source |
|---|---|---|
| `light_mode_background.jpg.b64` | Light | Pastel blue / white / mint soft waves |
| `dark_mode_background.jpg.b64` | Dark | Deep navy / blue-green soft waves |
| `splash_light.jpg.b64.part1..3` | Light | Launch splash card ("Mail Stack" + envelope) |
| `splash_dark.jpg.b64.part1..4` | Dark | Launch splash card, dark version |

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
Splash: light `829bb8a43ce2261f3a009a51230cf710`,
dark `8df0046f79af8ea2337a72e22578a965`.
The splash base64 is split into <100KB `.partN` files because the push
CLI caps a single argument at ~128KB. Reassemble with:
```bash
cat splash_light.jpg.b64.part* | base64 -d > splash_light.jpg
cat splash_dark.jpg.b64.part* | base64 -d > splash_dark.jpg
```

## Splash cards (2026-10-14)

The splash JPEGs are the user's final cards (941×1672) mirror-extended
top/bottom to 941×2512 (9:24) so they fill any phone screen with
`ContentScale.Crop` — no stretching, the logo/text artwork is untouched.
Extension method: the top/bottom 280-row bands are flipped vertically
(the flipped inner edge equals the original edge row, so there is no
seam) and stretched to 420px per side.
