# Changelog

## v1.12.0 — 2026-11-04
- Promoted the working 1.13 and 1.14 monitor-cache compatibility patch from v1.11.2.
- Release versionName `justicia-h1-v1.12.0-stable`, versionCode 556.
- Kept existing package, signature, renderer, 20 session controls, AFK engine and stored data.
- No runtime/renderer changes compared to v1.11.2.

## v1.11.2 — compatibility preview
- Injected monitor-manager recovery for Minecraft 1.13/1.14, where GLFW primary monitor existed but the game's Java monitor cache lookup returned null.
- Original Minecraft 1.13 and 1.14 transformations verified in GitHub CI; user later confirmed launch success.

## v1.11.1 — AFK hotfix preview
- Background lifecycle callback and OpenAL source mute/restore patch.
- GLFW diagnostic instrumentation.

## v1.11 — multi-session/AFK preview
- Independent AFK controls per session, session quick controls, kept 20 sessions and renderer lock.

## v10 — baseline
- Renderer catalog, pinned renderer, session manager and native AFK agent.
