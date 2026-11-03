# Minecraft 1.13 / 1.14 startup NPE — investigation results

Date: 2026-11-03. Tested on official Mojang 1.13 and 1.14 client JARs via an ephemeral GitHub Actions runner. No game JAR or disassembled classes are stored in Git.

## Confirmed call chain

| Version | Constructor failure | Lookup | Missing object |
| --- | --- | --- | --- |
| 1.13 | `cfs.<init>`, SourceFile line 90 | `glfwGetPrimaryMonitor() -> cfr.a(long)` | `cfs.e` of type `cfj` |
| 1.14 | `cub.<init>`, SourceFile line 78 | `glfwGetPrimaryMonitor() -> cty.a(long)` | `cub.e` of type `ctu` |

Both constructors read a primitive boolean field from the monitor object at the exact crash line. An NPE there requires the monitor object to be null; it is not caused by the boolean field itself.

The corresponding monitor-manager getter calls `Map.get(Long)` with no fallback:
- 1.13: `cfr.a(long)`
- 1.14: `cty.a(long)`

Both manager constructors register `glfwSetMonitorCallback`, call `glfwGetMonitors`, and populate their monitor map. Their connect callback stores a monitor wrapper in that map. The on-device failure is therefore consistent with primary monitor absence from the map, but *why* it is absent is not yet confirmed.

## What must be logged next

Before creating a window, log `glfwInit`, `glfwGetMonitors` (null, count, pointer handles), `glfwGetPrimaryMonitor` (pointer handle), and the manager lookup result. The existing v1.11.1 diagnostic only observes primary monitor and video mode; the supplied crash reports do not include diagnostic lines.

## Correct repair design (not yet shipped)

1. On Android, initialize GLFW before enumerating the virtual monitor, verify positive width/height and consistent handle between enumeration and primary selection.
2. If primary handle is nonzero but absent from Minecraft 1.13/1.14's cached monitor map, register a monitor wrapper through the manager's existing connected-monitor code path, then retry the lookup. Restrict any transformation to versions verified by bytecode shape, not obfuscated class names alone.
3. If the handle is zero, diagnose native GLFW initialization/surface readiness and DO NOT invent an OpenGL context or fake pointer.
4. Keep renderer selection pinned, separate session IDs, and AFK behavior unchanged.
5. Validate representative Minecraft versions 1.13, 1.13.2, 1.14, 1.14.4, plus regression 1.12.2 / 1.15.2, on actual Android hardware.

## Status

Root cause localized to a null Java monitor wrapper (manager lookup). Native reason and working Android fix are not verified. v1.11.1 APK is **not** a fix for 1.13–1.14. Issue also reproduced in official MJ base.
