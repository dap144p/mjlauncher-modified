# MJ Launcher Modified v1.11 PREVIEW (based on v10)

Application ID: git.artdeell.mjlaunch.modified
VersionName: justicia-h1-v1.11-modified
VersionCode: 553
Signer: same private v6-v10 signer (private key NOT included)

Current APK patch:
- Pinned 11-renderer catalog and 20 independent slots carried unchanged from v10.
- Built-in game Java agent updated: AFK per-slot flags, global AFK master, live on/off without JVM restart.
- Long-press slot: Resume existing JVM, toggle AFK per slot, Stop one service with confirmation, edit heap, or instructions to restart manually.
- Existing v10 0 presentation FPS target and OpenAL mute in background kept unchanged.
- JVM engine/source and Android smali source shipped in this package.

Not yet implemented in this preview:
- One-tap automatic Restart. Restart currently deliberately requires Stop followed by manually starting slot.
- Full Starting/Running/Background/Crashed UI; status can still be seen in v10 logs/OS.
- Crash log sharing from slot menu.
- Proactive low-RAM warning.
- Reliable 1 FPS fallback mode: only v10 0 FPS target exists currently.
- Proof that a session remains online in background under Android memory pressure.
- Fix for original upstream Minecraft 1.13.x/1.14.x crashes.

Risk: this APK is a test preview, not a stable release. Back up app data first. Do not uninstall app if preserving data is critical. Android may reject an APK signed by a different keystore.

Local checks: APK ZIP CRC PASS, v1/v2 signature RSA/certificate continuity PASS, package/version via apktool decode PASS; all native libs/assets (other than AFK engine JAR) unchanged, classes.dex main identical to v10, new classes2.dex assembles. Host-JVM policy tests PASS. Android installation and Minecraft startup NOT tested.

Keep signing p12 and password private. GitHub is source-only, not a source of APK signing credentials.