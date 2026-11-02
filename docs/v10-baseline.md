# v10 baseline and migration

Source of truth: `mj-modified-v10-project-and-signing.zip` (private archival backup) and signed `mj-launcher-justicia-h1-multisession-afk-10.modified.apk`.

Verified source archive contains:
- `mj-work/patches/rebuild_v10.py`
- `mj-work/patches/patch_v10.py`
- `mj-work/source/RendererCatalog.java`, `SessionNativeAfk.java`, and native AFK JVM agent
- `mj-work/renderer-native-manifest-v10.json`
- pinned v9 APK and native release assets (private backup only)

### Existing v10 behavior
20 independent sessions; per-session heap config; resume a live JVM without relog; renderer snapshot at Play and strict no-auto-replacement policy; native AFK Java agent with visual frame suppression / OpenAL volume muting while app hidden.

### Known defects / constraints
- Minecraft 1.13 and 1.14 fail during window initialization on *both official MJ and modified v10*. No fix proven yet.
- 0 FPS only refers to presentation. Background client ticks, networking, chunk processing and server-side AFK rules still apply.
- Android OS can kill sessions under memory pressure or power restrictions.
- v10 signed APK exceeds GitHub's 100 MiB per-file git upload limit; do **not** encode/reupload secrets/keystores to work around it.

### v1.11 development policy
Keep v10 as the base and implement independent AFK control per process, snapshot/state observability, memory warning, crash log sharing and safe resume. Never silently relaunch a stopped JVM as a substitute for resume.
