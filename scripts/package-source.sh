#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
./scripts/check-repository-hygiene.sh
out="${1:-target/professional_website-source.zip}"
mkdir -p "$(dirname "$out")"
if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "package-source requires a Git checkout so only tracked files can be exported." >&2
  exit 1
fi
git archive --format=zip --output="$out" HEAD
printf 'Safe source archive created: %s\n' "$out"
