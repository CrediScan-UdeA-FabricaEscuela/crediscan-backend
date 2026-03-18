# Copilot Instructions Validation Checklist

Use this checklist to verify that contextual instructions in `.github/instructions` are actually being applied by GitHub Copilot.

## 1. Baseline checks
- Confirm `.github/copilot-instructions.md` exists and describes project-level constraints.
- Confirm every file in `.github/instructions/*.instructions.md` has valid YAML frontmatter with `applyTo`.
- Confirm each `applyTo` points to real paths in this repo.

## 2. Targeted activation tests

For each instruction file, open one matching file and ask Copilot Chat to perform a focused change.

### spring-hexagonal.instructions.md
- Open any Java file under `src/main/java/co/udea/codefactory/creditscoring`.
- Prompt: "Refactor this flow to respect hexagonal boundaries."
- Expected: Copilot keeps domain framework-agnostic and uses ports/adapters.

### spring-api-design.instructions.md
- Open a `*Controller.java` or a DTO in `application/dto`.
- Prompt: "Create endpoint and request/response contracts for this use case."
- Expected: DTOs, validation, versioned path style, RFC 7807 error handling guidance.

### spring-persistence.instructions.md
- Open a persistence adapter or repository file.
- Prompt: "Add a query and mapping improvements for persistence."
- Expected: Explicit mappings, LAZY defaults, Flyway-first schema changes.

### spring-security.instructions.md
- Open security config/JWT/auth file.
- Prompt: "Harden auth config for production."
- Expected: deny-by-default, strict token validation, no hardcoded secrets.

### spring-testing.instructions.md
- Open a test file in `src/test`.
- Prompt: "Add tests for this scenario."
- Expected: AAA structure, JUnit5 + Mockito style, focused slice/full-test choice.

### spring-observability.instructions.md
- Open `application.yml` or `logback-spring.xml`.
- Prompt: "Improve observability for this service."
- Expected: structured logs, correlation IDs, metrics/health emphasis, secret masking.

### spring-docker-deploy.instructions.md
- Open Docker/compose/workflow/k8s file.
- Prompt: "Improve deployment hardening."
- Expected: multi-stage, non-root, env var usage, no inline secrets.

### sonarcloud-gates.instructions.md
- Open `build.gradle.kts` or `.github/workflows/*.yml`.
- Prompt: "Add SonarCloud quality gate integration."
- Expected: JaCoCo XML path and quality gate enforcement behavior.

## 3. False-positive checks
- Open an unrelated file (e.g. `README.md`) and verify Copilot does not inject domain-specific instruction rules.
- Open a non-security Java file and verify security-specific constraints are not over-applied.

## 4. Regression checks after edits
- When updating `applyTo`, repeat one activation test and one false-positive test.
- Keep prompts short and task-specific to observe instruction effects clearly.

## 5. Troubleshooting
- If an instruction is not applied, first verify exact path matching in `applyTo`.
- If multiple instructions conflict, narrow globs and keep each file domain-specific.
- Prefer repository-relative paths and avoid overly broad `**/*.java` unless intentional.
