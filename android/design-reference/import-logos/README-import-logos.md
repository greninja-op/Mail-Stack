# Import-flow logos (base64 split parts)

The Import conduit (Gmail -> Mailstack) uses two real product logos that
cannot be pushed as raw binaries via the GitHub API (double-base64
corruption). This directory holds their base64 split parts, NOT the raw
PNGs. The raw PNGs live in the build working tree + the APK at
`app/src/main/res/drawable-nodpi/`.

## Assets

1. `ic_gmail_logo.png` — the real Gmail "M" (user-supplied 2026-10-17,
   gradient, transparent background). Replaces the old flat vector
   `ic_gmail_logo.xml` (deleted). SHA-256:
   `3c0cabe4f4acc1d80f505687c57ea619cd43a48fda2231bcd69b91e54ef549d6`
   Parts: `ic_gmail_logo.png.b64.part00` .. `part03`.

2. `ic_mailstack_logo.png` — the real Mailstack app logo
   (user-supplied, circular mark). Replaces the Canvas-drawn illustration
   in the Import conduit. SHA-256:
   `5f880885a85c8540796c653c374a2605aaa3da0e1a5bb549aabeb85fe455deb5`
   Parts: `ic_mailstack_logo.png.b64.part00` .. `part04`.

## Reassembly

Concatenate the parts in order, then base64-decode, writing to
`../../app/src/main/res/drawable-nodpi/`:

    cat ic_gmail_logo.png.b64.part* | base64 -d > ../../app/src/main/res/drawable-nodpi/ic_gmail_logo.png
    cat ic_mailstack_logo.png.b64.part* | base64 -d > ../../app/src/main/res/drawable-nodpi/ic_mailstack_logo.png

Verify with `sha256sum` against the hashes above. Do NOT place the
`.b64.part*` files under `res/` — AAPT rejects non-.png/.xml names
there and breaks the build.
