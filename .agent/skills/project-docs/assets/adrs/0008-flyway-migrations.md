# 0008 — Migraciones de Base de Datos con Flyway

- **Status**: Accepted
- **Date**: 2026-03-18
- **Deciders**: Equipo CodeF@ctory Advanced — UdeA

## Context

El esquema de base de datos evoluciona con el proyecto. Se necesita una herramienta que:
- Versioné las migraciones como código (en el repositorio).
- Se ejecute automáticamente al arrancar la aplicación.
- Soporte rollback o reparación en caso de error.
- Sea compatible con Testcontainers (para tests de integración sobre PostgreSQL real).

Alternativas:

| Herramienta | Pros | Contras |
|-------------|------|---------|
| **Flyway** | Simple, bien integrado con Spring Boot | Rollback pago (Flyway Teams) |
| Liquibase | XML/YAML/JSON, rollback nativo | Mayor complejidad de configuración |
| Hibernate DDL auto | Zero config | Peligroso en producción, no reproducible |
| Scripts manuales | Control total | Sin versionado, propenso a errores humanos |

## Decision

Usamos **Flyway** con scripts SQL versionados.

### Convenciones de nomenclatura

```
src/main/resources/db/migration/
├── V1__create_schema_initial.sql
├── V2__create_applicant_table.sql
├── V3__create_financial_data_table.sql
├── V4__create_scoring_tables.sql
├── V5__create_evaluation_tables.sql
├── V6__create_user_security_tables.sql
├── V7__create_audit_log_table.sql
└── V8__seed_initial_roles.sql
```

Formato: `V{version}__{descripcion_en_snake_case}.sql`

Reglas:
- **Nunca modificar** un script ya ejecutado en cualquier entorno.
- Para corregir, crear una nueva migración `V{N}__fix_...sql`.
- Scripts de datos iniciales (seeds) usan prefijo `R__` (repeatable) solo si son idempotentes.
- Para stored procedures y triggers: `V{N}__create_proc_{nombre}.sql` (ver `docs/database/DEPLOYMENT.md`).

### Configuración Spring Boot

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
    validate-on-migrate: true
    out-of-order: false
```

En tests con Testcontainers, Flyway corre automáticamente contra el contenedor PostgreSQL.

## Consequences

**Positivos:**
- El esquema está versionado junto al código → reproducible en cualquier entorno.
- CI/CD aplica migraciones automáticamente en staging y producción (Render).
- Testcontainers + Flyway garantizan que los tests de integración corren contra el esquema exacto de producción.

**Negativos:**
- Sin rollback automático en Flyway Community → ante un error en producción, se debe crear una migración de corrección.
- Las migraciones incorrectas en producción son costosas → se validan en CI antes del deploy.

**Proceso de validación:** El pipeline de CI ejecuta `mvn flyway:validate` antes del build. Si hay inconsistencia de checksum, el build falla.
