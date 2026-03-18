# 0003 — PostgreSQL como Motor de Persistencia

- **Status**: Accepted
- **Date**: 2026-03-18
- **Deciders**: Equipo CodeF@ctory Advanced — UdeA

## Context

El sistema maneja datos estructurados con relaciones complejas (solicitantes, evaluaciones, historial de scores, auditoría). Se necesita un motor relacional confiable, con soporte para:
- Transacciones ACID.
- Integridad referencial.
- JSONB para datos financieros semiestructurados (opcional).
- Compatibilidad con Flyway para migraciones versionadas.
- Despliegue gratuito en Render (tier free).

Alternativas evaluadas:

| Motor | Pros | Contras |
|-------|------|---------|
| **PostgreSQL 16** | ACID, JSONB, extensible, free tier en Render, amplio soporte en Spring | Overhead operacional vs. H2 |
| MySQL 8 | Familiar, bien soportado | Sin JSONB nativo, licenciamiento dual |
| H2 (solo dev) | Zero config | No para producción |
| MongoDB | Flexible schema | No relacional, overkill para este dominio |

## Decision

Usamos **PostgreSQL 16** como única base de datos (dev, test con Testcontainers, staging, producción).

- En desarrollo local: PostgreSQL en Docker Compose (`docker-compose.yml`).
- En CI: Testcontainers levanta PostgreSQL automáticamente.
- En producción (Render): PostgreSQL managed database (free tier, 256 MB RAM, 1 GB storage).

Convenciones:
- Esquema: `public` (default). Naming: `snake_case` para tablas y columnas.
- Todas las tablas tienen columna `id UUID DEFAULT gen_random_uuid()` como PK.
- Columnas de auditoría: `created_at TIMESTAMPTZ`, `updated_at TIMESTAMPTZ`, `created_by VARCHAR(100)`.
- No se usan secuencias de tipo `SERIAL`; se prefiere `UUID` para portabilidad.

## Consequences

**Positivos:**
- ACID garantiza consistencia en evaluaciones de riesgo críticas.
- JSONB permite almacenar datos financieros variables sin romper el esquema.
- Testcontainers usa la misma imagen que producción → "it works on my machine" eliminado.
- Flyway + PostgreSQL tienen excelente integración con Spring Boot.

**Negativos:**
- El free tier de Render tiene limitaciones de storage y conexiones concurrentes.
- Conexión al DB en Render puede tener latencia (tier gratuito, región US East).

**Mitigación de latencia:** Se configura connection pool con HikariCP (`spring.datasource.hikari.*`) y se considera Redis como cache (ADR-0003-redis no implementado aún, ver sprint 3).
