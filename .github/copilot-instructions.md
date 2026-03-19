# Project Copilot Instructions

This repository uses Spring Boot 3.x with Java 21 and Hexagonal Architecture.

## Architectural Boundaries
- Keep dependencies flowing inward: infrastructure -> application -> domain.
- Domain code must remain framework-agnostic (no Spring, JPA, or web annotations).
- Controllers and persistence entities are infrastructure concerns; never leak them into domain.
- Use ports in application and adapters in infrastructure.

## API and Contracts
- Never expose JPA entities in REST responses; use request/response DTOs.
- Use validation on request DTOs and return RFC 7807 style errors.
- Prefer consistent versioned paths: /api/v1, /api/v2.

## Security Baseline
- Use deny-by-default authorization.
- Keep secrets out of source code and config files; use environment variables.
- Apply least privilege to roles and endpoint access.

## Persistence Baseline
- Prefer explicit table/column/index mapping.
- Use Flyway for schema changes; never mutate schema manually in runtime.
- Default to LAZY for relations unless there is a clear and documented reason.

## Testing Baseline
- Unit tests: JUnit 5 + Mockito, strict Arrange-Act-Assert.
- Use integration tests for repository and web slices where needed.
- Keep minimum coverage threshold at 40%.

## Observability Baseline
- Enable Actuator and expose only required endpoints.
- Use structured logs and include correlation identifiers.
- Add metrics for key business flows.

## CI Quality Baseline
- Integrate SonarCloud/SonarQube quality gates.
- Block merges on critical vulnerabilities and failed quality gates.

## Context Instructions
Copilot should also load contextual files from `.github/instructions/*.instructions.md` and apply the most specific matching rules first.

## HU Routing
When the user asks to implement a User Story (examples: "implementa HU-XXX", "implement HU-XXX", "implement user story" or when HU content is pasted), route the task to the custom agent `hu-implement` defined at `.github/agents/hu-implement.agent.md`.
Do not start coding immediately: the agent must first produce Phase 0 plan and wait for explicit user approval.
