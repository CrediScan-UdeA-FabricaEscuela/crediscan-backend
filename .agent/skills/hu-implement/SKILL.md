---
name: hu-implement
description: >
  Orchestrator that takes a User Story (HU) and implements it end-to-end by selecting
  and sequencing the project's existing skills in hexagonal inside-out order.
  Trigger: "implementá la HU-XXX", "implement HU-XXX", "implement user story", or when user pastes HU content.
license: Apache-2.0
metadata:
  author: gentleman-programming
  version: "1.0"
---

## When to Use

- User says "implementá la HU-XXX" or "implement HU-XXX"
- User pastes a User Story with ID, Como/Quiero/Para, Criterios de Aceptación
- User says "implement user story" followed by HU content

---

## Input Format

Expect a HU with these fields (any subset is valid — infer what is missing):

```
ID: HU-XXX
Como: <rol>
Quiero: <acción>
Para: <valor de negocio>

Criterios de Aceptación (CA):
- CA-1: ...
- CA-2: ...

Reglas de Negocio (RN):
- RN-1: ...

Notas Técnicas:
- ...

Dependencias: HU-YYY, HU-ZZZ (or "ninguna")
```

---

## Critical Patterns

### You are an ORCHESTRATOR — not an executor

- NEVER write code inline. Delegate each phase to a sub-agent.
- Your job: plan, sequence, synthesize results, track state.
- One HU at a time. Never start phase N+1 without completing phase N.

### Inside-out hexagonal order is MANDATORY

Domain → Application → Infrastructure → Security → Testing → Observability

Inverting this order produces broken dependency graphs. Enforce it every time.

### Plan first. Always wait for user approval before writing any code.

---

## Phase 0: Analysis (ALWAYS before any code)

**Actions:**

1. Parse the HU: extract ID, actor, goal, CAs, RNs, technical notes, dependencies.
2. Identify the target domain module:

   | Keyword in HU | Module |
   |---------------|--------|
   | applicant, solicitante, cliente | `applicant` |
   | financial, ingreso, deuda, historial | `financialdata` |
   | score, puntuación, modelo | `scoring` |
   | evaluación, decisión, aprobación | `evaluation` |
   | reporte, informe, dashboard | `reporting` |
   | none of the above | `shared` |

3. Select only the skills actually needed. Default selection:

   | Skill | Include when |
   |-------|-------------|
   | `spring-hexagonal` | Always (domain + application layers) |
   | `spring-persistence` | HU touches DB (entities, queries, migrations) |
   | `spring-api-design` | HU exposes REST endpoints |
   | `spring-security` | HU has role/permission requirements |
   | `spring-testing` | Always (at least unit tests per CA) |
   | `spring-observability` | HU involves critical business events |
   | `spring-docker-deploy` | HU changes infra/deployment config |
   | `sonarcloud-gates` | HU closes a quality gate gap |

4. Check dependencies: if HU depends on HU-YYY that is not yet implemented, note which ports/classes need to be stubbed and mark them as `// TODO: depends on HU-YYY`.

5. **Present the following plan to the user and STOP. Wait for explicit approval before proceeding.**

```
## Plan: HU-XXX — <title>

**Module:** co.udea.codefactory.creditscoring.<module>

**Skills to load (in order):**
1. spring-hexagonal
2. spring-persistence (if needed)
3. spring-api-design (if needed)
4. spring-security (if needed)
5. spring-testing
6. spring-observability (if needed)

**CAs to implement:** CA-1, CA-2, ...
**RNs to enforce:** RN-1, ...
**Dependencies to stub:** HU-YYY → <what gets stubbed>

**Implementation phases:**
- Phase 1: Domain — Entities, VOs, port interfaces, domain exceptions
- Phase 2: Application — Use case service, DTOs, MapStruct mapper
- Phase 3: Infrastructure — JPA adapter, Flyway migration, REST controller
- Phase 4: Security — @PreAuthorize, RBAC rules (if required)
- Phase 5: Testing — Unit (AAA), integration (Testcontainers), Gherkin from CAs
- Phase 6: Observability — Structured logging, metrics (if critical events)

Proceed? (yes / adjust plan)
```

---

## Implementation Phases

### Before EVERY phase: read the relevant SKILL.md

Path pattern: `.agent/skills/{skill-name}/SKILL.md`

Load the skill and apply ALL its patterns. Never write code for a phase without reading its skill first.

---

### Phase 1 — Domain Layer

**Skills:** `spring-hexagonal`, `spring-persistence`

**Read:** `.agent/skills/spring-hexagonal/SKILL.md`

**Produces (under `src/main/java/co/udea/codefactory/creditscoring/<module>/domain/`):**

```
domain/
├── model/
│   ├── <Entity>.java          # Pure Java, zero framework imports
│   └── <ValueObject>.java     # Immutable, validation in constructor
├── port/
│   ├── in/
│   │   └── <UseCase>Port.java # Input port interface
│   └── out/
│       └── <Repository>Port.java  # Output port interface
└── exception/
    └── <Domain>Exception.java  # Extends RuntimeException
```

**Rules:**
- Entities: no JPA annotations, no Spring annotations, no Lombok (use records or plain Java)
- Value Objects: immutable, validate in constructor, throw domain exception on invalid input
- Port interfaces: only domain types in signatures — never JPA entities or Spring types
- Domain exceptions: carry meaningful message and error code

---

### Phase 2 — Application Layer

**Skills:** `spring-hexagonal`

**Read:** `.agent/skills/spring-hexagonal/SKILL.md`

**Produces (under `src/main/java/co/udea/codefactory/creditscoring/<module>/application/`):**

```
application/
├── usecase/
│   └── <UseCase>Service.java  # Implements input port, orchestrates output ports
├── dto/
│   ├── <Entity>Request.java
│   └── <Entity>Response.java
└── mapper/
    └── <Entity>Mapper.java    # MapStruct interface
```

**Rules:**
- Service implements the input port interface from domain
- Service depends ONLY on output port interfaces — never on JPA repositories directly
- DTOs are plain records or classes — no domain types exposed
- MapStruct mapper declared as interface; Spring component via `@Mapper(componentModel = "spring")`
- One use case class per use case — no god services

---

### Phase 3 — Infrastructure Layer

**Skills:** `spring-persistence`, `spring-api-design`

**Read:** `.agent/skills/spring-persistence/SKILL.md`, then `.agent/skills/spring-api-design/SKILL.md`

**Produces:**

```
infrastructure/
├── persistence/
│   ├── entity/
│   │   └── <Entity>JpaEntity.java     # @Entity, @Table, auditing
│   ├── repository/
│   │   └── <Entity>JpaRepository.java # extends JpaRepository
│   └── adapter/
│       └── <Entity>PersistenceAdapter.java  # implements output port
├── web/
│   └── <Entity>Controller.java        # @RestController, HATEOAS
└── migration/
    └── (Flyway SQL goes in resources/db/migration/)
```

**Flyway versioning rule:** `V{major}_{minor}__<description>.sql`
- Check existing migrations to pick the next version number.

**REST controller rules (from spring-api-design skill):**
- `@RestController`, `@RequestMapping("/api/v1/<resource>")`
- Return `EntityModel<>` or `CollectionModel<>` (HATEOAS)
- Use `@Valid` on request bodies
- Error responses follow `ApiError` standard (RFC 7807 / project format)

**JPA entity rules (from spring-persistence skill):**
- Separate JPA entity from domain model — never expose JPA entity through API
- `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy` via `JpaAuditingConfig`
- Optimistic locking with `@Version` on mutable entities

---

### Phase 4 — Security (if required)

**Skills:** `spring-security`

**Read:** `.agent/skills/spring-security/SKILL.md`

**Produces:** `@PreAuthorize` annotations on controller methods, role/permission constants if new ones needed.

**Rules:**
- Map each CA that mentions a role to a `@PreAuthorize("hasRole('ROLE_X')")` or `hasAuthority`
- Never bypass security for "testing convenience" — stub tokens instead
- Document every permission decision with a comment: `// RN-X: only ADMIN can delete`

---

### Phase 5 — Testing

**Skills:** `spring-testing`

**Read:** `.agent/skills/spring-testing/SKILL.md`

**Produces (under `src/test/`):**

```
test/
├── unit/
│   └── <module>/
│       ├── domain/
│       │   └── <Entity>Test.java           # Pure unit, no Spring context
│       └── application/
│           └── <UseCase>ServiceTest.java   # Mocked ports
└── integration/
    └── <module>/
        ├── <Entity>RepositoryIT.java       # Testcontainers PostgreSQL
        └── <Entity>ControllerIT.java       # @SpringBootTest
```

**Gherkin (if CAs are behavior-rich):**
```
src/test/resources/features/<module>/
└── <hu-id>.feature
```

**Naming rules:**
- Method: `should_<expected>_when_<condition>()` (snake_case)
- Gherkin: one Scenario per CA, one Scenario per RN edge case
- AAA: Arrange / Act / Assert with blank lines between sections

**Coverage target:** every CA must have at least one test; every RN edge case must have a negative test.

---

### Phase 6 — Observability (if critical business events)

**Skills:** `spring-observability`

**Read:** `.agent/skills/spring-observability/SKILL.md`

**Produces:** Structured log statements at domain service boundaries, Micrometer counter/timer for business events.

**When to add:**
- Credit score calculated → emit metric `scoring.calculated` with tags `{module, result}`
- Evaluation decision made → log at INFO with structured fields `{hu_id, applicant_id, decision}`
- Any irreversible state change → log at INFO before + after

---

## Quality Checklist

Run mentally before declaring a phase done:

### Domain
- [ ] Zero framework imports (`javax.*`, `org.springframework.*`, `jakarta.persistence.*`)
- [ ] All VOs validate input and throw domain exceptions
- [ ] Port interfaces use only domain types

### Application
- [ ] Service depends only on port interfaces (not JPA repos)
- [ ] DTOs used at all in/out boundaries — domain model never returned directly
- [ ] MapStruct mapper declared as interface with `componentModel = "spring"`

### Infrastructure
- [ ] JPA entity is separate from domain model
- [ ] Controller returns HATEOAS links
- [ ] Error responses use `ApiError` structure
- [ ] Flyway migration version does not conflict with existing files

### Security
- [ ] Every endpoint with a role requirement has `@PreAuthorize`
- [ ] No hardcoded credentials or tokens

### Testing
- [ ] Every CA has at least one test scenario
- [ ] Every RN has a negative/edge-case test
- [ ] Integration tests use Testcontainers (no H2)
- [ ] AAA structure visible with blank lines

### General
- [ ] Dependencies flow inward only: infra → app → domain
- [ ] No circular dependencies between modules
- [ ] Stub classes for unimplemented HU dependencies marked with `// TODO: HU-XXX`

---

## Output Summary

After all phases complete, present:

```
## HU-XXX Implementation Complete

### Files Created
**Domain**
- src/main/java/co/udea/codefactory/creditscoring/<module>/domain/model/<Entity>.java
- ...

**Application**
- src/main/java/co/udea/codefactory/creditscoring/<module>/application/usecase/<UseCase>Service.java
- ...

**Infrastructure**
- src/main/java/co/udea/codefactory/creditscoring/<module>/infrastructure/persistence/...
- src/main/java/co/udea/codefactory/creditscoring/<module>/infrastructure/web/...
- src/main/resources/db/migration/V<X>_<Y>__<description>.sql

**Tests**
- src/test/java/.../<Entity>Test.java
- src/test/resources/features/<module>/<hu-id>.feature (if Gherkin)

### CA/RN Coverage
| ID | Type | Test | Status |
|----|------|------|--------|
| CA-1 | Acceptance | `should_X_when_Y` | Covered |
| RN-1 | Business rule | `should_reject_when_Z` | Covered |

### TODOs
- [ ] HU-YYY: stub `<PortInterface>` needs real impl when HU-YYY is done

### Suggested Next HU
Based on dependencies: HU-YYY (required to remove stubs)
```

---

## Key Rules (Summary)

1. **Plan first** — Phase 0 is mandatory. Never skip it.
2. **Wait for approval** — STOP after presenting the plan. Do not proceed until user says yes.
3. **Read skill before each phase** — no exceptions. Load `.agent/skills/{name}/SKILL.md` before writing code for that phase.
4. **Inside-out order** — Domain → App → Infra → Security → Tests → Observability.
5. **One HU at a time** — do not mix concerns from multiple HUs.
6. **Stub missing deps** — if HU depends on unimplemented HU, stub the output port and mark `// TODO`.
7. **Save decisions to engram** — after implementation, call `mem_save` with project `demo-repository`, capturing architecture decisions, non-obvious patterns discovered, and any gotchas.

---

## Base Package Reference

```
co.udea.codefactory.creditscoring
├── applicant/
├── financialdata/
├── scoring/
├── evaluation/
├── reporting/
└── shared/
```

Each module follows:
```
<module>/
├── domain/
│   ├── model/
│   ├── port/
│   │   ├── in/
│   │   └── out/
│   └── exception/
├── application/
│   ├── usecase/
│   ├── dto/
│   └── mapper/
└── infrastructure/
    ├── persistence/
    │   ├── entity/
    │   ├── repository/
    │   └── adapter/
    └── web/
```
