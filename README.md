# MJLauncher Modified — Justicia H1 (v10 baseline)

**Current verified baseline: v10**, versionName `justicia-h1-multisession-afk-10-modified`, versionCode `552`.

- Application ID: `git.artdeell.mjlaunch.modified`
- Data: `Android/data/git.artdeell.mjlaunch.modified/files/`
- 20 independent Android game sessions (A–T internally, 1–20 in UI)
- Built-in background AFK Java agent (OpenGL presentation/audio throttling; no Fabric required)
- 11 available renderer catalog entries, including hardware-limited entries, with **renderer choice pinned** for a running instance
- Android 32/64 support according to bundled ABI libraries

## v10 APK identity

SHA-256: `52d7dcfb6090f2fe3cf079cbda93ba558f13c8fc78bbe21d491939a314fcd828`

The v10 APK is **124 MB**, too large for GitHub's regular 100 MB file limit. It is preserved in the user's private ChatGPT Library/project backup, not this Git tree. For a reproducible binary rebuild, use the private v10 backup containing the pinned signed v9 input, decompilation/patch scripts and renderer APK dependencies.

**Security:** Keep `signing/*.p12`, `store-password.txt`, account tokens and APK private userdata out of this repository, Actions artifacts and logs.

## Development

- v10 is the **base**. Do not mix the previous v13 experiment into the v10 lineage.
- v1.11 development continues in `feature/v1.11-session-afk`.
- The 1.13–1.14 initialization crash is also reproducible on official MJ; it is not considered resolved by v10 or v1.11.
- Android actual-device startup, networking and AFK survivability require device testing before a stable release.

Legacy v13 experiment removed from the active repository tree; historical commits retain it.
