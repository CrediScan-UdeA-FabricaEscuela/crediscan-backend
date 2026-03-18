# Architecture Document — Credit Risk Scoring Engine

> **Project**: Credit Risk Scoring Engine
> **Course**: CodeF@ctory Advanced — Universidad de Antioquia
> **Stack**: Java 21, Spring Boot 3.4, PostgreSQL 16, Hexagonal Architecture
> **Last updated**: see git log

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Architectural Style](#2-architectural-style)
3. [Module Decomposition](#3-module-decomposition)
4. [Persistence Layer](#4-persistence-layer)
5. [Security](#5-security)
6. [Error Handling](#6-error-handling)
7. [API Design and Versioning](#7-api-design-and-versioning)
8. [Testing Strategy](#8-testing-strategy)
9. [Deployment](#9-deployment)
10. [Cross-Cutting Concerns](#10-cross-cutting-concerns)
11. [C4 Diagrams](#11-c4-diagrams)
12. [Architecture Decision Records](#12-architecture-decision-records)

---

## 1. System Overview

El **Credit Risk Scoring Engine** es una API REST que calcula el score de riesgo crediticio de un solicitante. El sistema:

- Recibe solicitudes de evaluación del **Analista de Crédito**.
- Carga datos financieros del solicitante (ingresados manualmente o consultados a bureaux externos).
- Aplica un modelo de scoring configurable (variables, pesos, reglas de cálculo).
- Genera una **evaluación de riesgo** con clasificación (AAA, AA, A, BBB, BB, B, CCC, D) y una **decisión de crédito** (Aprobado / Rechazado / Revisión manual).
- Persiste el historial de evaluaciones para auditoría y reportes.
- Expone métricas operativas para Prometheus/Grafana.

El sistema está diseñado para ser **desplegado en Render** (producción) y ejecutarse localmente con **Docker Compose**.

---

## 2. Architectural Style

### Hexagonal Architecture (Ports & Adapters)

Ver [ADR-0001](decisions/0001-hexagonal-architecture.md).

La arquitectura hexagonal garantiza que el **dominio de negocio** no depende de ningún framework, base de datos o mecanismo de entrega. La separación se logra mediante:

- **Puertos de entrada (inbound ports)**: interfaces que definen los casos de uso (`ICreateApplicantUseCase`, `IExecuteScoringUseCase`).
- **Puertos de salida (outbound ports)**: interfaces que el dominio necesita del exterior (`IApplicantRepository`, `IScoringEnginePort`).
- **Adapters de entrada**: controllers REST que invocan puertos de entrada.
- **Adapters de salida**: repositorios JPA, clientes HTTP externos, adaptadores de email que implementan puertos de salida.

```
            ┌──────────────────────────────────────────┐
            │             Spring Boot App               │
            │                                          │
  HTTP ──▶  │  [REST Adapter] ──▶ [Use Case] ──▶ [Port Out] ──▶ [JPA Adapter] ──▶ PostgreSQL
            │                                          │
  HTTP ──▶  │  [Security Filter]     [Domain Model]   │
            │                                          │
            └──────────────────────────────────────────┘
```

El dominio (model + ports + service) es **puro Java**, testeable con JUnit 5 sin contexto de Spring.

---

## 3. Module Decomposition

La aplicación está organizada en módulos de dominio bajo `com.udea.creditrisk.*`:

### Módulos de dominio

| Módulo | Paquete | Responsabilidad |
|--------|---------|-----------------|
| `applicant` | `com.udea.creditrisk.applicant` | Gestión de solicitantes de crédito |
| `financialdata` | `com.udea.creditrisk.financialdata` | Carga y validación de datos financieros |
| `scoring` | `com.udea.creditrisk.scoring` | Motor de scoring: variables, modelos, reglas |
| `evaluation` | `com.udea.creditrisk.evaluation` | Evaluación de riesgo y decisión de crédito |
| `reporting` | `com.udea.creditrisk.reporting` | Generación de reportes y exportación |

### Módulo transversal

| Módulo | Paquete | Responsabilidad |
|--------|---------|-----------------|
| `shared` | `com.udea.creditrisk.shared` | Security, audit, logging, exceptions comunes |

### Estructura interna de cada módulo

```
com.udea.creditrisk.{module}/
├── domain/
│   ├── model/          # Entities, Value Objects, Aggregates, Enums
│   ├── port/
│   │   ├── in/         # Use case interfaces (inbound ports)
│   │   └── out/        # Repository/service interfaces (outbound ports)
│   └── service/        # Domain Services (stateless logic)
├── application/
│   └── usecase/        # Use case implementations (orchestrate domain)
└── infrastructure/
    ├── persistence/    # JPA Entities, Repositories, Mappers
    ├── rest/           # Controllers, DTOs, Assemblers, Validators
    └── external/       # HTTP clients, email adapters (where applicable)
```

---

## 4. Persistence Layer

Ver [ADR-0003](decisions/0003-postgresql-persistence.md) y [ADR-0008](decisions/0008-flyway-migrations.md).

- **PostgreSQL 16** como única base de datos.
- **Spring Data JPA + Hibernate 6** para ORM.
- **Flyway** para migraciones versionadas (`src/main/resources/db/migration/V*.sql`).
- PKs: `UUID` generados con `gen_random_uuid()`.
- Columnas de auditoría: `created_at`, `updated_at`, `created_by` en todas las tablas.
- Connection pool: **HikariCP** con configuración en `application.yml`.

Ver el modelo E-R completo en [../database/MER.md](../database/MER.md).

---

## 5. Security

Ver [ADR-0006](decisions/0006-jwt-authentication.md) y [ADR-0007](decisions/0007-rbac-abac-authorization.md).

- **Autenticación**: JWT Bearer tokens (RS256 en producción, HS256 en dev).
- **Autorización**: RBAC + ABAC con `@PreAuthorize` y Spring Security 6.
- **Roles**: `ROLE_ADMIN`, `ROLE_ANALYST`, `ROLE_VIEWER`.
- **Endpoints públicos**: `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`.
- **Protección CSRF**: deshabilitada (API stateless).
- **CORS**: configurado para orígenes permitidos vía `application.yml`.
- **Rate limiting**: pendiente Sprint 3 (Bucket4j).

---

## 6. Error Handling

Todos los errores siguen **RFC 7807 (Problem Details for HTTP APIs)**.

```json
{
  "type": "https://creditrisk.udea.edu.co/errors/applicant-not-found",
  "title": "Applicant Not Found",
  "status": 404,
  "detail": "No applicant found with id '550e8400-e29b-41d4-a716-446655440000'",
  "instance": "/api/v1/applicants/550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-03-18T10:30:00Z",
  "traceId": "abc123def456"
}
```

El manejo centralizado se implementa con `@RestControllerAdvice` en `com.udea.creditrisk.shared.rest.GlobalExceptionHandler`.

Jerarquía de excepciones de dominio:
- `CreditRiskException` (base)
  - `EntityNotFoundException`
  - `BusinessRuleViolationException`
  - `InvalidStateTransitionException`
  - `ExternalServiceException`

---

## 7. API Design and Versioning

Ver [ADR-0004](decisions/0004-api-versioning-url-path.md) y [ADR-0005](decisions/0005-hateoas-rest.md).

- Versión en URL path: `/api/v1/`.
- HATEOAS con HAL (`spring-hateoas`).
- OpenAPI 3.1 generado automáticamente por Springdoc (`/v3/api-docs`).
- Swagger UI disponible en `/swagger-ui/index.html`.
- Ver guía completa en [../api/API-DOCS.md](../api/API-DOCS.md).

---

## 8. Testing Strategy

Ver [ADR-0009](decisions/0009-testcontainers-integration.md) y [ADR-0010](decisions/0010-sonarcloud-quality-gates.md).

| Nivel | Tecnología | Objetivo |
|-------|-----------|---------|
| Unit | JUnit 5 + Mockito | Dominio, use cases, domain services |
| Integration | Testcontainers + Spring Boot Test | JPA adapters, Flyway, REST endpoints |
| BDD / Acceptance | JUnit 5 + RestAssured | Flujos de negocio end-to-end |
| Contract | WireMock | Adapters de sistemas externos |

Cobertura mínima: **80%** (Quality Gate en SonarCloud).

---

## 9. Deployment

Ver [DEPLOYMENT.md](DEPLOYMENT.md) para diagramas detallados.

| Ambiente | Tecnología | Descripción |
|----------|-----------|-------------|
| Local dev | Docker Compose | App + PostgreSQL + Prometheus + Grafana |
| CI | GitHub Actions | Build + test + SonarCloud + Docker image |
| Production | Render | Web Service (Spring Boot) + Managed PostgreSQL |
| Sprint 3 | Kubernetes (Render K8s o Minikube) | Deployment + Service + Ingress |

---

## 10. Cross-Cutting Concerns

### Logging

- **Structured logging** (JSON) con Logback + `logstash-logback-encoder`.
- MDC con `correlation-id` (generado en el filter de entrada o tomado del header `X-Correlation-Id`).
- Niveles: `DEBUG` en dev, `INFO` en producción.

### Auditoría

- Módulo `com.udea.creditrisk.shared.audit`.
- Anotación `@Auditable` en métodos de use case.
- Persiste en tabla `audit_log`: `entity_type`, `entity_id`, `action`, `performed_by`, `performed_at`, `details` (JSONB).

### Métricas

- Spring Boot Actuator + Micrometer.
- Prometheus scrape en `/actuator/prometheus`.
- Métricas de negocio custom: `scoring.evaluations.total`, `scoring.evaluations.approved`, `scoring.score.distribution`.
- Dashboards Grafana en `docs/architecture/grafana/` (Sprint 2).

---

## 11. C4 Diagrams

Los diagramas C4 están en formato Structurizr DSL: [`c4/workspace.dsl`](c4/workspace.dsl).

Ver instrucciones de renderizado en [`c4/README.md`](c4/README.md).

| Nivel | Vista | Descripción |
|-------|-------|-------------|
| 1 | System Context | Actores y sistemas externos |
| 2 | Container | Spring Boot, PostgreSQL, Redis, Prometheus, Grafana |
| 3 | Component | Módulos internos (adapters, use cases, ports, shared) |

---

## 12. Architecture Decision Records

Todas las decisiones arquitectónicas están documentadas en `docs/architecture/decisions/`:

| ADR | Título | Status |
|-----|--------|--------|
| [0001](decisions/0001-hexagonal-architecture.md) | Hexagonal Architecture | Accepted |
| [0002](decisions/0002-spring-boot-stack.md) | Spring Boot Stack (Java 21 + SB 3.4) | Accepted |
| [0003](decisions/0003-postgresql-persistence.md) | PostgreSQL Persistence | Accepted |
| [0004](decisions/0004-api-versioning-url-path.md) | API Versioning (URL Path) | Accepted |
| [0005](decisions/0005-hateoas-rest.md) | HATEOAS REST | Accepted |
| [0006](decisions/0006-jwt-authentication.md) | JWT Authentication | Accepted |
| [0007](decisions/0007-rbac-abac-authorization.md) | RBAC + ABAC Authorization | Accepted |
| [0008](decisions/0008-flyway-migrations.md) | Flyway Migrations | Accepted |
| [0009](decisions/0009-testcontainers-integration.md) | Testcontainers Integration | Accepted |
| [0010](decisions/0010-sonarcloud-quality-gates.md) | SonarCloud Quality Gates | Accepted |
