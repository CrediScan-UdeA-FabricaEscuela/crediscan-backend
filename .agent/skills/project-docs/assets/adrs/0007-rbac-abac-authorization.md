# 0007 — Autorización con RBAC + ABAC

- **Status**: Accepted
- **Date**: 2026-03-18
- **Deciders**: Equipo CodeF@ctory Advanced — UdeA

## Context

El sistema tiene múltiples actores con distintos niveles de acceso:
- **Analista de Crédito**: puede crear evaluaciones, consultar solicitantes y sus propias decisiones.
- **Administrador**: gestiona usuarios, roles, reglas de scoring y versiones del modelo.
- En el futuro: auditor (solo lectura), gerente de crédito (aprobación final).

Se necesita un modelo de autorización que soporte:
1. Control por roles (**RBAC** — Role-Based Access Control).
2. Control por atributos del recurso o del usuario (**ABAC** — Attribute-Based Access Control), p. ej. "un analista solo puede ver sus propias evaluaciones".

## Decision

Combinamos **RBAC + ABAC** implementado con Spring Security 6 y anotaciones `@PreAuthorize`.

### Roles definidos

| Rol | Descripción |
|-----|-------------|
| `ROLE_ADMIN` | Acceso total al sistema |
| `ROLE_ANALYST` | Crear/ver solicitantes, crear evaluaciones propias |
| `ROLE_VIEWER` | Solo lectura de reportes (Sprint 3) |

### Permisos granulares (ABAC)

Los permisos se expresan como strings en el JWT: `resource:action`.

```
applicant:read, applicant:write
evaluation:read, evaluation:write
scoring:read, scoring:admin
report:read
user:admin
```

### Implementación

```java
// RBAC puro
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> deleteUser(...) { ... }

// ABAC: el analista solo puede ver sus propias evaluaciones
@PreAuthorize("hasRole('ANALYST') and @evaluationSecurity.isOwner(#id, authentication)")
public ResponseEntity<?> getEvaluation(@PathVariable UUID id) { ... }
```

El bean `evaluationSecurity` consulta el repositorio para verificar si el `createdBy` del recurso coincide con el `username` del token.

Los roles y permisos se persisten en las tablas `usuario`, `rol_usuario` y `permiso` (ver MER).

## Consequences

**Positivos:**
- RBAC simple para controles gruesos (roles).
- ABAC para reglas finas sin duplicar lógica en cada servicio.
- `@PreAuthorize` mantiene la autorización cerca del método, visible en code review.
- No requiere infraestructura adicional (no OPA, no Casbin).

**Negativos:**
- El bean de seguridad hace consultas a DB en cada request con ABAC → se cachea con `@Cacheable` (Spring Cache / Caffeine).
- Las reglas ABAC dispersas en anotaciones pueden ser difíciles de auditar → se documentan en `docs/architecture/ARCHITECTURE.md`.

**Auditoría:** Cada acceso a recursos protegidos queda registrado en `AuditLog` (ver ADR-0001 cross-cutting concerns).
