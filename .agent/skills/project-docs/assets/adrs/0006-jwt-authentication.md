# 0006 — Autenticación con JWT (Stateless)

- **Status**: Accepted
- **Date**: 2026-03-18
- **Deciders**: Equipo CodeF@ctory Advanced — UdeA

## Context

El sistema necesita autenticación para proteger la API REST. Los requisitos son:
- Stateless (no sesiones en servidor).
- Compatible con despliegue en Render (sin sticky sessions).
- Soporte para RBAC/ABAC (ver ADR-0007).
- Fácil de probar con Postman y pruebas de integración.

Alternativas evaluadas:

| Mecanismo | Pros | Contras |
|-----------|------|---------|
| **JWT (Bearer)** | Stateless, portable, claims custom | Revocación compleja |
| Sesión HTTP | Simple | Requiere estado en servidor, no escala |
| OAuth2 + OIDC (Keycloak) | Estándar enterprise | Overhead operacional, fuera de alcance del curso |
| API Key | Simple | Sin expiración nativa, sin claims de roles |

## Decision

Usamos **JWT (JSON Web Token)** con firma **RS256** (RSA 2048-bit) para producción y **HS256** para desarrollo local.

Flujo:
1. Cliente hace `POST /api/v1/auth/login` con `{ username, password }`.
2. El sistema valida credenciales y emite un **Access Token** (15 min) y un **Refresh Token** (7 días).
3. El cliente incluye el Access Token en `Authorization: Bearer <token>`.
4. Spring Security valida la firma y extrae claims (userId, roles, permissions).
5. Al expirar, el cliente usa `POST /api/v1/auth/refresh` con el Refresh Token.

Estructura del JWT payload:
```json
{
  "sub": "user-uuid",
  "username": "jperez",
  "roles": ["ROLE_ANALYST"],
  "permissions": ["applicant:read", "evaluation:write"],
  "iat": 1710000000,
  "exp": 1710000900
}
```

Revocación: los Refresh Tokens revocados se almacenan en una blacklist (Redis Sprint 3, o tabla `jwt_blacklist` en PostgreSQL Sprint 1-2).

Librería: `spring-security-oauth2-resource-server` con `spring-boot-starter-oauth2-resource-server` o `io.jsonwebtoken:jjwt-api`.

## Consequences

**Positivos:**
- Stateless: escala horizontalmente sin infraestructura de sesiones.
- Claims embebidos → RBAC/ABAC sin consultar DB en cada request (ver ADR-0007).
- Compatible con despliegue en Render sin configuración adicional.

**Negativos:**
- Revocación de Access Tokens requiere blacklist (complejidad adicional).
- Si la clave RS256 privada se compromete, todos los tokens activos son inválidos.
- Refresh Tokens en DB requieren limpieza periódica (job scheduled o TTL).

**Seguridad:** La clave privada RS256 se inyecta como variable de entorno (`JWT_PRIVATE_KEY`) y nunca se commitea al repositorio.
