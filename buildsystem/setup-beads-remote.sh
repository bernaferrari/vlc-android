#!/usr/bin/env bash
# Configure bd (beads) Dolt remote for issue sync.
# Usage:
#   ./buildsystem/setup-beads-remote.sh
#   ./buildsystem/setup-beads-remote.sh '<dolt-remote-url>'
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if ! command -v bd >/dev/null 2>&1; then
  echo "error: bd CLI not found in PATH" >&2
  exit 1
fi

echo "== bd dolt status =="
bd dolt status || true

echo
echo "== current remotes =="
if bd dolt remote list 2>/dev/null | tee /tmp/bd-remotes.txt; then
  if grep -q 'origin' /tmp/bd-remotes.txt 2>/dev/null; then
    echo "origin already configured."
    exit 0
  fi
fi

URL="${1:-}"
if [[ -z "$URL" ]]; then
  echo
  echo "No Dolt remote configured."
  echo "Pass a URL to add origin:"
  echo "  $0 'https://doltremoteapi.dolthub.com/<org>/<db>'"
  echo
  echo "Docs: .beads/REMOTE.md"
  exit 2
fi

echo
echo "Adding dolt remote origin -> $URL"
bd dolt remote add origin "$URL"
bd dolt remote list
echo "Try: bd dolt push"
