#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
failures=()

for file in .gitignore .dockerignore; do
  [[ -f "$file" ]] || failures+=("missing $file")
done

for sensitive in '.env' '.env.aiven' '.env.local' 'src/main/resources/application-security.yaml'; do
  if git rev-parse --is-inside-work-tree >/dev/null 2>&1 && git ls-files --error-unmatch "$sensitive" >/dev/null 2>&1; then
    failures+=("sensitive file is tracked: $sensitive")
  fi
done

tracked_runtime="$(git ls-files | grep -E '(^|/)(target|migration-backups|\.idea|\.vscode)(/|$)|\.dump$' || true)"
[[ -z "$tracked_runtime" ]] || failures+=("runtime/local artefacts are tracked: ${tracked_runtime//$'\n'/, }")

for ignored in '.env' 'target/' 'migration-backups/' 'src/main/resources/application-security.yaml'; do
  grep -Fq "$ignored" .gitignore || failures+=(".gitignore must exclude $ignored")
done
for ignored in '.env' 'target' 'migration-backups' 'application-security.yaml'; do
  grep -Fq "$ignored" .dockerignore || failures+=(".dockerignore must exclude $ignored")
done

if ((${#failures[@]})); then
  printf 'Repository hygiene FAILED:\n' >&2
  printf ' - %s\n' "${failures[@]}" >&2
  exit 1
fi
printf 'Repository hygiene OK: secrets, local config, build outputs and migration backups are excluded from versioned/release surfaces.\n'
