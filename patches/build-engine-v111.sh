#!/usr/bin/env bash
set -euo pipefail
# Usage: bash patches/build-engine-v111.sh /path/to/asm-deps ./build/afk-engine-v111.jar
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEPS="${1:?Pass path to asm.jar, asm-tree.jar, asm-commons.jar and ASM-LICENSE.txt}"
OUT="${2:-$ROOT/build/afk-engine-v111.jar}"
WORK="$ROOT/build/afk-v111"
mkdir -p "$WORK/classes" "$WORK/relocate" "$(dirname "$OUT")"
CP="$DEPS/asm.jar:$DEPS/asm-tree.jar:$DEPS/asm-commons.jar"
javac -source 8 -target 8 -Xlint:none -cp "$CP" -d "$WORK/classes" \
  "$ROOT/src/afk/git/artdeell/mojo/afk/Agent.java" \
  "$ROOT/src/afk/git/artdeell/mojo/afk/Bridge.java" \
  "$ROOT/src/afk/git/artdeell/mojo/afk/EngineTransformer.java" \
  "$ROOT/src/afk/git/artdeell/mojo/afk/AfkPolicyV111.java"
jar cf "$WORK/agent-raw.jar" -C "$WORK/classes" .
javac -source 8 -target 8 -Xlint:none -cp "$CP" -d "$WORK/relocate" \
  "$ROOT/src/afk/git/artdeell/mojo/afk/Relocate.java"
java -cp "$WORK/relocate:$CP" Relocate "$OUT" "$WORK/agent-raw.jar" "$DEPS/asm.jar" "$DEPS/asm-tree.jar"
if [ -f "$DEPS/ASM-LICENSE.txt" ]; then
  jar uf "$OUT" -C "$DEPS" ASM-LICENSE.txt
fi
printf 'Engine built (no signing key): %s\n' "$OUT"