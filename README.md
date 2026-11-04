# MJ Launcher Modified — Justicia Hotfix 1

## Current release: v1.12.0

**Android package:** `git.artdeell.mjlaunch.modified`  
**Data directory:** `Android/data/git.artdeell.mjlaunch.modified/files/`  
**Version code:** `556`  
**Version name:** `justicia-h1-v1.12.0-stable`

The v1.12.0 build uses the exact Minecraft compatibility engine from v1.11.2, which the device owner reported successfully launched **Minecraft 1.13 and 1.14** on Android 10. The launcher keeps the 20-session controller, per-session background AFK settings, renderer lock and 11 renderer catalog choices.

**APK SHA-256:** `376d655804db8842d6b2de3b9df17e845079aa5b430e730e3ab3d5a079240e08`

See [release notes](docs/releases/v1.12.0.md) for the changelog, installation, signing and validation limitations.

### Source structure

- `v10/`: historic reproducible patch sources for the baseline.
- `src/afk/`, `smali/`, `patches/`: v1.11–v1.12 modifications.
- `tests/monitor-compat/`: monitor-cache recovery tests using authentic Minecraft 1.13 and 1.14 client-bytecode in an ephemeral runner.
- `scripts/prepare-v112.py`: create an unsigned v1.12 package from a known-good v1.11.2 binary.
- `docs/`: compatibility research and version history.

The v1.12 APK exceeds GitHub's 100 MB single-file Git upload limit. Its download is distributed separately from the source repo. No private signing keys, passwords, or Minecraft account data are in this repository.

**Testing scope:** 1.13 and 1.14 were reported working. Other point releases and background AFK/network longevity are not yet fully verified; Android may terminate processes under memory pressure.
