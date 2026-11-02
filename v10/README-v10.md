# MJLauncher Modified v10 — renderer catalog and pinned selection

Package `git.artdeell.mjlaunch.modified`, versionCode **552**, non-debug, signed with the existing v6–v9 key. It can update those modified builds directly. This remains separate from Cylox and official MJLauncher.

## Renderer catalog

Both the global renderer preference and each instance's renderer selector show all **11 integrated options**, with capability/installation labels instead of silently hiding unavailable entries:

1. Holy GL4ES
2. Holy GL4ES + ANGLE
3. LTW
4. LTW + ANGLE
5. MobileGlues 2.0.0
6. MobileGlues 2.0.0 + ANGLE
7. Zink (bundled Mesa)
8. Legacy Zink (Mesa 23.0.4)
9. Freedreno (Adreno only)
10. Mesa Desktop (Chromebook)
11. External Mesa (requires a compatible `git.mojo.mesa` plugin)

MobileGlues and ANGLE libraries are bundled for armeabi-v7a, arm64-v8a, x86 and x86_64. Legacy Zink is bundled for ARM 32/64 only, as provided by upstream. ANGLE/Vulkan, Android, GLES and device-specific restrictions remain meaningful; selecting a marked option does not make unsupported hardware compatible. VirGL and renderer projects designed for other launchers are not integrated or claimed to work. MJ's current base has no compatible VirGL server/renderer adapter.

The old global ANGLE checkbox is hidden; ANGLE is an explicit renderer variant, so a global flag cannot quietly change the backend of a saved instance selection. Native GL4ES/LTW entries use the native GLES driver, while their ANGLE entries name the bundled ANGLE libraries explicitly. MobileGlues has a private per-game-process config/cache/log folder; ANGLE is forced on or off using the renderer's documented enum rather than auto-detection. These are launcher renderers, not Minecraft mods.

## Selection remains pinned

The selected renderer is captured when Play is pressed and passed through the existing per-session launch snapshot. Subsequent changes to the selected instance/global default do not change that pending session's renderer. Active sessions keep their already running game process.

The original version/mod compatibility checks no longer rewrite `instance.renderer`, save an automatic replacement, or call `setCurrentRenderer` with another backend. If a known incompatibility requires a different renderer, launch reports the issue and leaves the user's selection unchanged. Renderer initialization no longer falls back to GL4ES. Unknown IDs fail instead of silently using a default. Runtime replacement with a different renderer tag is blocked.

An explicit instance renderer takes precedence. If the instance is set to use the global default, that default is resolved and frozen for the launch when Play is pressed. Changing a renderer manually applies to a new game start, not a live JVM.

## Native binaries and provenance

Sources and release asset SHA-256 values are pinned in `renderer-sources-v10.json`. Download inputs are the official MobileGL-Dev/MobileGlues-release V2.0.0, MojoLauncher/AnglePlugin rel, and MojoLauncher/ZinkPlugin rel releases. The MobileGlues release's signing certificate matches the fingerprint published by its maintainers. GitHub asset digests were checked when present. The upstream ANGLE/Zink containers have debug filenames; only their native libraries are included, and the resulting launcher APK is non-debug.

Original v9 libraries and bundled assets are byte-for-byte preserved. The 18 additional libraries are listed in `renderer-native-manifest-v10.json`. Legacy Zink's dependency names are changed with equal-length ELF string replacements: `libgallium_dri.so` becomes `libgallium_old.so`, and `libdrm.so` becomes `libdrz.so`. This isolates legacy Mesa from the modern versions in the same application; executable code is unchanged. The changed SONAME/NEEDED chains were inspected. Licenses and source pointers are in the APK's license assets and backup inputs.

## Validation and limits

45 JVM checks passed with `-Xverify:all` using Android/native substitutes. They cover catalog identity/labels, missing capabilities, GL4ES aliases, native/ANGLE pinning despite global preferences, rejected replacement, initialization failure without fallback, MobileGlues's exact ANGLE enum and per-process settings paths, and legacy dependency availability.

The actual output APK was fully disassembled and checked for the catalog hooks, strict constructor/init policy, absence of launch-time renderer persistence/replacement, renderer snapshot extras and preserved AFK hooks. All 20 session groups, signer continuity, APK signature schemes v1/v2/v3, non-debug identity, 16 KiB ZIP alignment, DEX checksums and native hashes passed.

**No Android phone, embedded JVM, live Minecraft game, shader pack, or native EGL/Vulkan driver initialization was tested.** This is a device-test candidate. The capability labels are preliminary checks, not proof that every backend works on a GPU. Several added upstream libraries have 4 KiB ELF LOAD alignment; ZIP alignment does not make them compatible with all 16 KiB-page devices. The user's Redmi 9C/Android 10/32-bit/PowerVR target has not been tested directly. No guarantee of speed or shader compatibility is made.

The v9 AFK engine, physical/virtual mouse separation, 20 numbered sessions, running usernames, configured-memory labels, and resume-without-restarting behavior are retained.

## Rebuild and private signing

The pinned input is `input/mj-launcher-justicia-h1-multisession-afk-9.modified.apk`. The backup includes original renderer release APK containers under `mj-work/renderer-input/`, native provenance, source, tests, compiled helper smali, patches, tools, validation, and the existing private signing key. Keep this backup private.

Use Java 17 and Android build-tools 36.0.0 at `mj-work/tools/sdk/android-16/`. Official Linux archive: `https://dl.google.com/android/repository/build-tools_r36_linux.zip`. Run `python3 mj-work/patches/rebuild_v10.py`. The script disassembles complete raw DEX buffers, applies only v10 changes to the pinned v9 APK, includes the verified renderer libraries, compiles/tests the helper, removes raw DEX copies before assembling, signs with the preserved key, and verifies output bytes.

Reuse `signing/mj-modified-v6.p12`, alias `mj-modified`, and private `signing/store-password.txt`; do not generate another key. Previous source files are retained for maintenance, but v10 rebuild does not need to reapply v8/v9 patches.