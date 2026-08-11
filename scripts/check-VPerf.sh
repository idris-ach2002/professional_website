#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
failures=()
require_file() { [[ -f "$1" ]] || failures+=("missing file: $1"); }
require_text() { grep -Fq "$2" "$1" || failures+=("missing contract '$2' in $1"); }

# VPerf is executed from a developer checkout as well as CI. Local runtime
# artefacts (.env, .git, target, migration-backups) are therefore allowed to
# exist on disk; what matters is that they are ignored and never tracked or
# sent in the Docker build context.
require_file .gitignore
require_file .dockerignore
for ignored in '.env' 'target/' 'migration-backups/'; do
  grep -Fq "$ignored" .gitignore || failures+=(".gitignore must exclude $ignored")
done
for ignored in '.env' 'target' 'migration-backups'; do
  grep -Fq "$ignored" .dockerignore || failures+=(".dockerignore must exclude $ignored")
done

if command -v git >/dev/null 2>&1 && git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  if git ls-files --error-unmatch .env >/dev/null 2>&1; then
    failures+=("real .env must never be tracked by git")
  fi
  tracked_runtime="$(git ls-files | grep -E '(^|/)(target|migration-backups)(/|$)' || true)"
  if [[ -n "$tracked_runtime" ]]; then
    failures+=("build/backup artefacts must never be tracked by git: ${tracked_runtime//$'\n'/, }")
  fi
fi

for file in \
  src/main/java/sorbonne/professional_website/concurrency/BackendConcurrencyConfig.java \
  src/main/java/sorbonne/professional_website/concurrency/BoundedVirtualThreadExecutor.java \
  src/main/java/sorbonne/professional_website/concurrency/ParallelWork.java \
  src/main/java/sorbonne/professional_website/analytics/service/AnalyticsIngestionPipeline.java \
  src/main/java/sorbonne/professional_website/cache/PublicPortfolioCacheConfig.java \
  src/main/java/sorbonne/professional_website/config/OpenApiConfig.java \
  src/test/java/sorbonne/professional_website/entity/PortfolioEntitiesTest.java \
  src/test/java/sorbonne/professional_website/integration/WebsiteVersionConcurrencyIntegrationTest.java; do
  require_file "$file"
done

require_text src/main/java/sorbonne/professional_website/entity/WebsiteVersion.java '@Version'
require_text src/main/java/sorbonne/professional_website/analytics/service/AnalyticsIngestionPipeline.java 'ArrayBlockingQueue'
require_text src/main/java/sorbonne/professional_website/service/WebsiteService.java '@Cacheable'
require_text src/main/java/sorbonne/professional_website/repository/OwnerRepository.java 'w.published = true'
require_text src/main/resources/db/migration/V6__v22_core_schema_and_constraints.sql 'uk_website_version_one_active_per_owner'
require_text src/main/resources/db/migration/V7__v22_concurrency_and_query_indexes.sql 'row_version'

if grep -R --include='*.java' -n 'FetchType.EAGER' src/main/java/sorbonne/professional_website/entity >/dev/null; then
  failures+=("EAGER entity association reintroduced")
fi
if grep -R --include='*.java' -n '\.parallelStream[[:space:]]*(' src/main/java >/dev/null; then
  failures+=("common ForkJoinPool parallelStream is forbidden; use bounded explicit executors")
fi
if grep -R --include='*.java' -n 'newCachedThreadPool\|newFixedThreadPool' src/main/java >/dev/null; then
  failures+=("unmanaged platform thread pool found in production code")
fi

migration_count="$(find src/main/resources/db/migration -maxdepth 1 -type f -name 'V*.sql' | wc -l | tr -d ' ')"
test_count="$(find src/test/java -type f -name '*Test.java' | wc -l | tr -d ' ')"
[[ "$migration_count" -ge 5 ]] || failures+=("expected >=5 forward Flyway migrations; found $migration_count")
[[ "$test_count" -ge 20 ]] || failures+=("expected >=20 backend test classes; found $test_count")

if ((${#failures[@]})); then
  printf 'VPerf backend contract FAILED:\n' >&2
  printf ' - %s\n' "${failures[@]}" >&2
  exit 1
fi

printf 'VPerf backend OK: %s Flyway migrations, %s test classes, bounded concurrency, async backpressure, cache, public filters, OpenAPI and observability contracts present.\n' "$migration_count" "$test_count"
