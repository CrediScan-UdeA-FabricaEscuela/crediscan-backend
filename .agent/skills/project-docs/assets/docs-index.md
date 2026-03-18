# Documentation Index — Credit Risk Scoring Engine

> **Project**: Credit Risk Scoring Engine
> **Course**: CodeF@ctory Advanced — Universidad de Antioquia
> **Stack**: Java 21, Spring Boot 3.4, PostgreSQL 16, Hexagonal Architecture

Esta documentación cubre todos los artefactos requeridos por el curso y sigue las convenciones del proyecto.

---

## Arquitectura

| Artefacto | Ruta | Descripción | Sprint |
|-----------|------|-------------|--------|
| Documento de Arquitectura | [architecture/ARCHITECTURE.md](architecture/ARCHITECTURE.md) | Estilo arquitectónico, módulos, persistencia, seguridad, testing, despliegue y concerns transversales | Sprint 1 |
| Diagrama C4 — DSL | [architecture/c4/workspace.dsl](architecture/c4/workspace.dsl) | Structurizr DSL con los 3 niveles C4 (Context, Container, Component) | Sprint 1 |
| C4 — Instrucciones de render | [architecture/c4/README.md](architecture/c4/README.md) | Cómo renderizar con Docker o en línea | Sprint 1 |
| Diagramas de Deployment | [architecture/DEPLOYMENT.md](architecture/DEPLOYMENT.md) | Local (Docker Compose), Producción (Render), CI/CD, Kubernetes | Sprint 1-3 |

---

## Architecture Decision Records (ADRs)

Formato: **MADR** (Markdown Architectural Decision Records).
Ubicación: `docs/architecture/decisions/`

| ADR | Título | Status | Sprint |
|-----|--------|--------|--------|
| [0001](architecture/decisions/0001-hexagonal-architecture.md) | Hexagonal Architecture (Ports & Adapters) | Accepted | Sprint 1 |
| [0002](architecture/decisions/0002-spring-boot-stack.md) | Spring Boot Stack (Java 21 + SB 3.4) | Accepted | Sprint 1 |
| [0003](architecture/decisions/0003-postgresql-persistence.md) | PostgreSQL como Motor de Persistencia | Accepted | Sprint 1 |
| [0004](architecture/decisions/0004-api-versioning-url-path.md) | Versionado de API por URL Path | Accepted | Sprint 1 |
| [0005](architecture/decisions/0005-hateoas-rest.md) | HATEOAS en la API REST | Accepted | Sprint 1 |
| [0006](architecture/decisions/0006-jwt-authentication.md) | Autenticación con JWT (Stateless) | Accepted | Sprint 1 |
| [0007](architecture/decisions/0007-rbac-abac-authorization.md) | Autorización con RBAC + ABAC | Accepted | Sprint 1 |
| [0008](architecture/decisions/0008-flyway-migrations.md) | Migraciones de Base de Datos con Flyway | Accepted | Sprint 1 |
| [0009](architecture/decisions/0009-testcontainers-integration.md) | Testcontainers para Tests de Integración | Accepted | Sprint 1 |
| [0010](architecture/decisions/0010-sonarcloud-quality-gates.md) | SonarCloud y Quality Gates | Accepted | Sprint 1 |

---

## Base de Datos

| Artefacto | Ruta | Descripción | Sprint |
|-----------|------|-------------|--------|
| Modelo E-R (MER) | [database/MER.md](database/MER.md) | Modelo lógico y físico en Mermaid. Entidades del dominio de crédito + seguridad + auditoría | Sprint 1 |
| Guía de Deployment DB | [database/DEPLOYMENT.md](database/DEPLOYMENT.md) | Flyway conventions, triggers, roles PostgreSQL, estimación de volumen | Sprint 1-2 |

---

## API

| Artefacto | Ruta | Descripción | Sprint |
|-----------|------|-------------|--------|
| Guía de Documentación API | [api/API-DOCS.md](api/API-DOCS.md) | Springdoc, export OpenAPI spec, versionado, Swagger UI | Sprint 1 |
| OpenAPI Spec (generada) | [api/openapi.yaml](api/openapi.yaml) | Spec generada. Actualizar con `curl http://localhost:8080/v3/api-docs.yaml` | Sprint 1+ |

---

## Entregables por Sprint

### Sprint 1 — Fundamentos

- [ ] ADRs 0001–0010 completos
- [ ] Diagrama C4 niveles 1, 2 y 3
- [ ] MER lógico y físico
- [ ] ARCHITECTURE.md completo
- [ ] Migraciones Flyway V1–V8
- [ ] OpenAPI spec exportada
- [ ] Pipeline CI/CD operativo con SonarCloud

### Sprint 2 — Features de Scoring

- [ ] ADR nuevo si hay decisiones adicionales
- [ ] MER actualizado con tablas nuevas
- [ ] OpenAPI spec actualizada
- [ ] Dashboards Grafana documentados

### Sprint 3 — Producción y Observabilidad

- [ ] Diagrama Kubernetes en DEPLOYMENT.md
- [ ] Guía de deployment en Render completa
- [ ] Rate limiting documentado (ADR)
- [ ] Redis cache documentado (ADR)

---

## Herramientas

| Herramienta | Uso | Referencia |
|-------------|-----|-----------|
| Structurizr Lite | Renderizar diagramas C4 | [c4/README.md](architecture/c4/README.md) |
| Mermaid | MER + Deployment diagrams | Nativo en GitHub Markdown |
| Springdoc | Generar OpenAPI spec | [api/API-DOCS.md](api/API-DOCS.md) |
| SonarCloud | Quality gates | [ADR-0010](architecture/decisions/0010-sonarcloud-quality-gates.md) |
| Flyway | Migraciones DB | [ADR-0008](architecture/decisions/0008-flyway-migrations.md) |
| Testcontainers | Tests integración | [ADR-0009](architecture/decisions/0009-testcontainers-integration.md) |
