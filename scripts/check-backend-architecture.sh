#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
failures=()
require_file() { [[ -f "$1" ]] || failures+=("missing file: $1"); }
require_text() { grep -Fq "$2" "$1" || failures+=("missing contract '$2' in $1"); }
forbid_path() { [[ ! -e "$1" ]] || failures+=("retired path must stay absent: $1"); }

for file in \
  src/main/java/sorbonne/professional_website/concurrency/HttpEntityTag.java \
  src/main/java/sorbonne/professional_website/service/PortfolioHealthEvaluator.java \
  src/main/java/sorbonne/professional_website/service/PortfolioBackupCodec.java \
  src/main/java/sorbonne/professional_website/service/WebsiteVersionCloner.java \
  src/main/java/sorbonne/professional_website/persistence/StringListJsonConverter.java \
  src/main/java/sorbonne/professional_website/security/SensitiveResponseNoStoreFilter.java \
  src/main/java/sorbonne/professional_website/analytics/service/AnalyticsRetentionJob.java \
  src/main/resources/db/migration/V8__admin_optimistic_concurrency.sql \
  src/main/resources/db/migration/V9__project_case_study_contract.sql; do
  require_file "$file"
done

for retired in \
  src/main/java/sorbonne/professional_website/concurrency/ParallelWork.java \
  src/main/java/sorbonne/professional_website/controller/ProjectController.java \
  src/main/java/sorbonne/professional_website/controller/ProfileController.java \
  src/main/java/sorbonne/professional_website/controller/TimelineController.java \
  src/main/java/sorbonne/professional_website/controller/ExperienceController.java \
  src/main/java/sorbonne/professional_website/service/ProjectService.java \
  src/main/java/sorbonne/professional_website/service/ProfileService.java \
  src/main/java/sorbonne/professional_website/service/TimelineService.java \
  src/main/java/sorbonne/professional_website/service/ExperienceService.java \
  src/main/java/sorbonne/professional_website/engineering/controller/PortfolioIntelligenceController.java \
  src/main/java/sorbonne/professional_website/engineering/service/PortfolioIntelligenceService.java \
  src/main/java/sorbonne/professional_website/engineering/dto/PortfolioIntelligenceQueryRequest.java \
  src/main/java/sorbonne/professional_website/engineering/dto/PortfolioIntelligenceResponse.java \
  src/test/java/sorbonne/professional_website/engineering/PortfolioIntelligenceServiceTest.java; do
  forbid_path "$retired"
done

require_text src/main/java/sorbonne/professional_website/entity/Owner.java '@Version'
require_text src/main/java/sorbonne/professional_website/entity/WebsiteVersion.java 'contentRevision'
require_text src/main/java/sorbonne/professional_website/entity/Project.java 'caseStudyTechnicalChoices'
require_text src/main/java/sorbonne/professional_website/entity/Project.java 'StringListJsonConverter.class'
require_text src/main/java/sorbonne/professional_website/entity/enumerations/ProjectLinkType.java 'ARCHITECTURE'
require_text src/main/java/sorbonne/professional_website/dto/request/ProjectRequestDTO.java 'ProjectCaseStudyRequestDTO caseStudy'
require_text src/main/java/sorbonne/professional_website/dto/response/ProjectResponseDTO.java 'ProjectCaseStudyResponseDTO caseStudy'
require_text src/main/java/sorbonne/professional_website/controller/WebsiteVersionAdminController.java 'If-Match'
require_text src/main/java/sorbonne/professional_website/controller/OwnerController.java 'If-Match'
require_text src/main/java/sorbonne/professional_website/security/WebSecurityConfig.java '"If-Match"'
require_text src/main/java/sorbonne/professional_website/security/WebSecurityConfig.java '"ETag"'
require_text Dockerfile 'USER 10001:10001'
require_text Dockerfile 'ENTRYPOINT ["java", "-jar", "/app/app.jar"]'
require_text docker-compose.yml 'no-new-privileges:true'
require_file .github/dependabot.yml
require_text src/main/resources/application.yaml 'retention-days:'
require_text src/main/resources/application.yaml 'percentiles-histogram:'
require_text src/main/resources/application.yaml 'requestId:%X{requestId:-}'
require_file src/test/java/sorbonne/professional_website/controller/WebsiteVersionAdminControllerTest.java
require_file src/test/java/sorbonne/professional_website/controller/OwnerControllerTest.java
require_text pom.xml '<minimum>0.40</minimum>'

if grep -R --include='*.java' -n '\.parallelStream[[:space:]]*(' src/main/java >/dev/null; then
  failures+=("common ForkJoinPool parallelStream is forbidden")
fi
if grep -R --include='*.java' -n 'newCachedThreadPool\|newFixedThreadPool' src/main/java >/dev/null; then
  failures+=("unmanaged platform thread pool found in production code")
fi
if grep -R --include='*.java' -n '@RequestMapping("/api/\(projects\|profiles\|timelines\|experiences\)' src/main/java >/dev/null; then
  failures+=("unversioned content mutation API was reintroduced")
fi

service_lines=$(wc -l < src/main/java/sorbonne/professional_website/service/WebsiteVersionService.java)
if (( service_lines > 500 )); then
  failures+=("WebsiteVersionService must remain under 500 lines after responsibility extraction; got ${service_lines}")
fi

if ((${#failures[@]})); then
  printf 'Backend architecture FAILED:\n' >&2
  printf ' - %s\n' "${failures[@]}" >&2
  exit 1
fi
printf 'Backend architecture OK: one versioned content write model, optimistic HTTP preconditions, bounded async lanes, retention, HTTP latency telemetry, no-store admin responses and no retired CRUD/concurrency surface.\n'
