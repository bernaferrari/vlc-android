#!/usr/bin/env bash
# Mirrors the fast GitHub Actions Web gate. Android and iOS release verification deliberately
# stay out of this feedback loop; their platform-specific checks are run before release.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST="$ROOT/webApp/build/dist/wasmJs/productionExecutable"

for arg in "$@"; do
  case "$arg" in
    --ci) ;;
    -h|--help)
      echo "Usage: $0 [--ci]"
      exit 0
      ;;
    *)
      echo "Unknown argument: $arg" >&2
      exit 1
      ;;
  esac
done

cd "$ROOT"

echo "==> Compiling VLC Web/Wasm tests"
./gradlew :shared:compileTestDevelopmentExecutableKotlinWasmJs \
  --no-daemon \
  --parallel \
  --build-cache \
  --configuration-cache \
  --console=plain

echo "==> Building VLC Web/Wasm distribution"
./gradlew :webApp:wasmJsBrowserDistribution \
  --no-daemon \
  --parallel \
  --build-cache \
  --configuration-cache \
  --console=plain

echo "==> Verifying VLC Web/Wasm distribution"
required_files=(index.html vlc-web.js)
for file in "${required_files[@]}"; do
  if [[ ! -f "$DIST/$file" ]]; then
    echo "Missing required Web artifact: $DIST/$file" >&2
    exit 1
  fi
done

wasm_count="$(find "$DIST" -maxdepth 1 -name '*.wasm' -type f | wc -l | tr -d ' ')"
if [[ "$wasm_count" -lt 1 ]]; then
  echo "Expected at least one WebAssembly artifact in $DIST" >&2
  exit 1
fi

if ! grep -q 'vlc-web.js' "$DIST/index.html"; then
  echo 'index.html does not load the VLC Web bundle' >&2
  exit 1
fi

echo "==> VLC Web/Wasm distribution is ready"
echo "    dist: $DIST"
echo "    wasm: $wasm_count file(s)"
