#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
./scripts/check-repository-hygiene.sh
./scripts/check-backend-architecture.sh
failures=()
require_file() { [[ -f "$1" ]] || failures+=("missing file: $1"); }
require_text() { grep -Fq "$2" "$1" || failures+=("missing contract '$2' in $1"); }

for file in \
  src/main/java/sorbonne/professional_website/concurrency/BackendConcurrencyConfig.java \
  src/main/java/sorbonne/professional_website/concurrency/BoundedVirtualThreadExecutor.java \
  src/main/java/sorbonne/professional_website/analytics/service/AnalyticsIngestionPipeline.java \
  src/main/java/sorbonne/professional_website/cache/PublicPortfolioCacheConfig.java \
  src/main/java/sorbonne/professional_website/config/OpenApiConfig.java \
  src/test/java/sorbonne/professional_website/integration/WebsiteVersionConcurrencyIntegrationTest.java \
  src/test/java/sorbonne/professional_website/concurrency/HttpEntityTagTest.java; do
  require_file "$file"
done

require_text src/main/java/sorbonne/professional_website/analytics/service/AnalyticsIngestionPipeline.java 'ArrayBlockingQueue'
require_text src/main/java/sorbonne/professional_website/service/WebsiteService.java '@Cacheable'
require_text src/main/java/sorbonne/professional_website/repository/OwnerRepository.java 'w.published = true'
require_text src/main/resources/db/migration/V6__v22_core_schema_and_constraints.sql 'uk_website_version_one_active_per_owner'
require_text src/main/resources/db/migration/V7__v22_concurrency_and_query_indexes.sql 'row_version'
require_text src/main/resources/db/migration/V8__admin_optimistic_concurrency.sql 'content_revision'

if grep -R --include='*.java' -n 'FetchType.EAGER' src/main/java/sorbonne/professional_website/entity >/dev/null; then
  failures+=("EAGER entity association reintroduced")
fi

migration_count="$(find src/main/resources/db/migration -maxdepth 1 -type f -name 'V*.sql' | wc -l | tr -d ' ')"
test_count="$(find src/test/java -type f -name '*Test.java' | wc -l | tr -d ' ')"
[[ "$migration_count" -ge 6 ]] || failures+=("expected >=6 forward Flyway migrations; found $migration_count")
[[ "$test_count" -ge 24 ]] || failures+=("expected >=24 backend test classes; found $test_count")

if ((${#failures[@]})); then
  printf 'VPerf backend contract FAILED:\n' >&2
  printf ' - %s\n' "${failures[@]}" >&2
  exit 1
fi
printf 'VPerf backend OK: %s Flyway migrations, %s test classes, versioned writes, bounded concurrency, async backpressure, cache, retention, OpenAPI and observability contracts present.\n' "$migration_count" "$test_count"
