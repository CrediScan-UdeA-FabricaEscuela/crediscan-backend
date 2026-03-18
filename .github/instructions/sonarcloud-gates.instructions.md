---
applyTo: "build.gradle.kts,settings.gradle.kts,sonar-project.properties,.github/workflows/**/*.yml"
---
# SonarCloud Quality Gate Rules

## Mandatory thresholds
- Unit test coverage >= 40%.
- Technical debt <= 2 days.
- Major/Critical/Blocker issues: none on new code.
- Critical vulnerabilities: 0.

## Build integration
- Configure JaCoCo XML reports for Sonar ingestion.
- Fail CI when quality gate fails.
- Keep test and source paths explicit in scanner configuration.

## Complexity policy
- Reduce cyclomatic/cognitive complexity through decomposition and guard clauses.
- Avoid suppressing findings without documented justification.

## Anti-patterns
- No blanket suppressions (`NOSONAR`) on new code.
- No bypassing quality gate in main branch pipelines.
