# Credit Scoring Engine — Guía de Arquitectura

> Documento para el equipo de desarrollo. Si vas a implementar un feature nuevo, leé esto primero.

---

## Tabla de Contenidos

1. [Visión General](#1-visión-general)
2. [Arquitectura Hexagonal](#2-arquitectura-hexagonal)
3. [Estructura de Packages](#3-estructura-de-packages)
4. [Bounded Contexts](#4-bounded-contexts)
5. [Base de Datos](#5-base-de-datos)
6. [Seguridad y Autenticación](#6-seguridad-y-autenticación)
7. [Endpoints y Roles](#7-endpoints-y-roles)
8. [Manejo de Errores](#8-manejo-de-errores)
9. [Tests](#9-tests)
10. [Configuración](#10-configuración)
11. [Cómo implementar un nuevo feature](#11-cómo-implementar-un-nuevo-feature)

---

## 1. Visión General

**Credit Scoring Engine** es una API REST construida con **Java 21 + Spring Boot 3.4.4**.

Evalúa solicitudes de crédito de personas naturales. Contempla el registro de solicitantes, captura de datos financieros, scoring automatizado, evaluación crediticia, decisión final y generación de reportes.

**Stack principal:**

| Capa | Tecnología |
|------|------------|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.4.4 |
| Persistencia | Spring Data JPA + Hibernate 6 |
| Base de datos | PostgreSQL 16 |
| Migraciones | Flyway |
| Autenticación | JWT (jjwt 0.12.6) |
| Mapeo | MapStruct 1.6.3 |
| Logs | Logstash Logback Encoder (JSON) |
| Métricas | Micrometer + Prometheus |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Tests | JUnit 5, Testcontainers, Cucumber, REST Assured |

---

## 2. Arquitectura Hexagonal

El proyecto usa **Arquitectura Hexagonal** (también llamada Ports & Adapters o Clean Architecture).

La idea central es que el **dominio de negocio no sabe nada de infraestructura** (ni de JPA, ni de HTTP, ni de cómo se encripta algo). Todo lo que el dominio necesita del exterior lo pide a través de **puertos** (interfaces), y la infraestructura provee **adaptadores** que implementan esos puertos.

```
┌─────────────────────────────────────────────────────────────┐
│                      INFRAESTRUCTURA                        │
│                                                             │
│  ┌─────────────┐   ┌──────────────────────────────────┐    │
│  │  REST API   │   │          ADAPTADORES OUT           │    │
│  │ (Adapter IN)│   │  JPA / Crypto / Metrics / JWT     │    │
│  └──────┬──────┘   └──────────────┬───────────────────┘    │
│         │                         │                         │
└─────────┼─────────────────────────┼─────────────────────────┘
          │                         │
          ▼                         ▲
┌─────────────────────────────────────────────────────────────┐
│                       APLICACIÓN                            │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              UseCase / Service                       │    │
│  │   Orquesta el flujo. No sabe cómo se persiste.      │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
          │                         │
          ▼                         ▼
┌─────────────────────────────────────────────────────────────┐
│                        DOMINIO                              │
│                                                             │
│   Modelos · Puertos IN · Puertos OUT · Excepciones         │
│   (No depende de nada externo. Es el corazón.)             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Ejemplo concreto: Registro de Solicitante

```
HTTP POST /api/v1/solicitantes
        │
        ▼
ApplicantController          ← Adapter IN (capa infrastructure)
        │  convierte Request → RegisterApplicantCommand
        ▼
RegisterApplicantUseCase     ← Puerto IN (interface en domain)
        │  implementado por
        ▼
RegisterApplicantService     ← Servicio de aplicación
        │  usa 3 puertos OUT:
        ├─ ApplicantRepositoryPort ──→ ApplicantRepositoryAdapter → JPA → PostgreSQL
        ├─ IdentificationCryptoPort ─→ IdentificationCryptoAdapter → AES-GCM / HMAC
        └─ ApplicantRegistrationMetricsPort → MetricsAdapter → Micrometer
```

### Regla fundamental de dependencias

```
infrastructure  →  application  →  domain
                                   ↑
                              (no depende de nada)
```

El dominio NUNCA importa clases de Spring, JPA, ni ninguna librería de infraestructura.

---

## 3. Estructura de Packages

```
co.udea.codefactory.creditscoring/
│
├── applicant/                          ← Bounded context: Solicitantes
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Applicant.java          ← Record inmutable (value object); incluye phone
│   │   │   └── EmploymentType.java     ← Enum con factory method fromApiValue()
│   │   ├── port/
│   │   │   ├── in/
│   │   │   │   ├── RegisterApplicantUseCase.java
│   │   │   │   ├── SearchApplicantUseCase.java
│   │   │   │   └── UpdateApplicantUseCase.java
│   │   │   └── out/
│   │   │       ├── ApplicantRepositoryPort.java       ← save, findById, search, findAll, update
│   │   │       ├── IdentificationCryptoPort.java      ← hash, encrypt, decrypt
│   │   │       ├── ApplicantEditAuditPort.java
│   │   │       └── ApplicantRegistrationMetricsPort.java
│   │   └── exception/
│   │       ├── ApplicantValidationException.java
│   │       ├── DuplicateApplicantException.java
│   │       └── ImmutableFieldException.java          ← Cuando se intenta editar id o fecha_nacimiento
│   ├── application/
│   │   ├── dto/
│   │   │   ├── RegisterApplicantCommand.java
│   │   │   ├── ApplicantSummary.java               ← Resultado de búsqueda (identificación en plano)
│   │   │   ├── UpdateApplicantCommand.java          ← Campos nullable; null = sin cambio
│   │   │   └── UpdateApplicantResult.java           ← applicant + changedFields
│   │   └── service/
│   │       ├── RegisterApplicantService.java
│   │       ├── SearchApplicantService.java
│   │       └── UpdateApplicantService.java
│   └── infrastructure/
│       └── adapter/
│           ├── in/rest/
│           │   ├── ApplicantController.java
│           │   ├── ApplicantRestMapper.java
│           │   └── dto/
│           │       ├── RegisterApplicantRequest.java
│           │       ├── RegisterApplicantResponse.java
│           │       ├── ApplicantResponse.java         ← Incluye telefono
│           │       ├── ApplicantSearchResponse.java   ← Respuesta de GET /solicitantes
│           │       ├── UpdateApplicantRequest.java    ← Campos nullable
│           │       └── UpdateApplicantResponse.java   ← Incluye campos_auditados
│           └── out/
│               ├── persistence/
│               │   ├── ApplicantJpaEntity.java                ← Incluye phone
│               │   ├── JpaApplicantRepository.java            ← Query searchByHashOrName
│               │   ├── ApplicantRepositoryAdapter.java        ← Incluye decrypt en read paths
│               │   ├── ApplicantEditAuditJpaEntity.java
│               │   ├── JpaApplicantEditAuditRepository.java
│               │   └── ApplicantEditAuditAdapter.java
│               ├── crypto/
│               │   ├── CryptoProperties.java
│               │   └── IdentificationCryptoAdapter.java       ← Incluye decrypt()
│               └── metrics/
│                   └── ApplicantRegistrationMetricsAdapter.java
│
├── evaluation/                         ← STUB — tablas creadas, lógica pendiente
│   └── infrastructure/adapter/in/rest/
│       └── EvaluationController.java
│
├── reporting/                          ← STUB
│   └── infrastructure/adapter/in/rest/
│       └── ReportController.java
│
├── scoring/                            ← STUB
│   └── infrastructure/adapter/in/rest/
│       └── ScoringVariableController.java
│
└── shared/
    ├── audit/
    │   └── AuditableEntity.java        ← @MappedSuperclass para auditoría JPA
    ├── config/
    │   ├── CorsConfig.java
    │   ├── JpaAuditingConfig.java      ← @EnableJpaAuditing + AuditorAware
    │   └── OpenApiConfig.java
    ├── exception/
    │   ├── GlobalExceptionHandler.java ← @RestControllerAdvice (RFC 7807)
    │   └── ResourceNotFoundException.java
    ├── logging/
    │   └── MdcFilter.java              ← Inyecta traceId en cada request
    └── security/
        ├── SecurityConfig.java         ← Configuración de Spring Security
        ├── domain/
        │   ├── model/
        │   │   ├── AppUser.java         ← Record
        │   │   ├── Role.java            ← Enum: ADMIN, ANALYST, RISK_MANAGER, CREDIT_SUPERVISOR
        │   │   ├── AuthResult.java      ← Record: token + role + expiresAt
        │   │   └── RolePermission.java
        │   ├── port/
        │   │   ├── in/
        │   │   │   ├── AuthenticateUseCase.java
        │   │   │   ├── ChangeUserRoleUseCase.java
        │   │   │   └── GetPermissionMatrixUseCase.java
        │   │   └── out/
        │   │       ├── AppUserRepositoryPort.java
        │   │       ├── TokenBlacklistPort.java
        │   │       ├── RolePermissionPort.java
        │   │       └── AuditLogPort.java
        │   └── exception/
        │       ├── InvalidCredentialsException.java
        │       └── LastAdminException.java
        ├── application/service/
        │   ├── AuthenticateService.java
        │   ├── ChangeUserRoleService.java
        │   └── GetPermissionMatrixService.java
        └── infrastructure/
            ├── jwt/
            │   ├── JwtService.java              ← Genera y valida JWT
            │   ├── JwtProperties.java           ← @ConfigurationProperties
            │   └── JwtAuthenticationFilter.java ← OncePerRequestFilter
            ├── persistence/
            │   ├── JpaAppUserEntity.java
            │   ├── JpaAppUserRepository.java
            │   ├── JpaAuditLogEntity.java
            │   ├── JpaAuditLogRepository.java
            │   ├── JpaRolePermissionEntity.java
            │   ├── JpaRolePermissionRepository.java
            │   ├── JpaTokenBlacklistEntity.java
            │   ├── JpaTokenBlacklistRepository.java
            │   ├── JpaUserDetailsService.java   ← Implementa UserDetailsService de Spring
            │   ├── AppUserRepositoryAdapter.java
            │   ├── AuditLogAdapter.java
            │   ├── RolePermissionAdapter.java
            │   ├── TokenBlacklistAdapter.java
            │   └── AuditLogId.java              ← Composite key (id + created_at)
            └── rest/
                ├── AuthController.java
                ├── RoleController.java
                └── dto/
                    ├── LoginRequest.java
                    ├── LoginResponse.java
                    ├── ChangeRoleRequest.java
                    └── PermissionMatrixResponse.java
```

---

## 4. Bounded Contexts

El sistema está dividido en contextos de negocio independientes. Cada uno tiene su propia carpeta y sus propias capas.

### ✅ APPLICANT — Completamente implementado

Registro, búsqueda y edición de solicitantes de crédito.

**Modelos clave:**

```java
// Applicant es un record inmutable con validaciones en los factory methods
public record Applicant(UUID id, String name, String identification,
                        LocalDate birthDate, EmploymentType employmentType,
                        BigDecimal monthlyIncome, Integer workExperienceMonths,
                        String phone)

// Reglas de negocio aplicadas en registerNew() y rehydrate():
// - nombre obligatorio
// - identificación obligatoria y única (chequeada via hash en BD)
// - edad >= 18 años
// - ingresos mensuales > 0
// - experiencia laboral >= 0
// - teléfono opcional, máx 20 caracteres
```

**Flujo de registro:**
1. Controller recibe `RegisterApplicantRequest` → mapea a `RegisterApplicantCommand`
2. Service crea el modelo `Applicant` (con validaciones de dominio)
3. Genera HMAC-SHA256 del número de identificación
4. Chequea si ya existe ese hash en BD (detección de duplicados sin exponer el dato)
5. Encripta el número de identificación con AES-256-GCM
6. Persiste el aplicante con el ID encriptado y el hash
7. Registra métrica de éxito

**Flujo de búsqueda (GET /api/v1/solicitantes):**
1. Controller recibe `q` (opcional) y parámetros de paginación
2. `SearchApplicantService`: si `q` es null/blank → `findAll(pageable)`; si tiene valor → hashea `q` con HMAC y construye patrón `%q%` para nombre
3. Repositorio ejecuta JPQL: `WHERE identification_hash = :hash OR LOWER(name) LIKE LOWER(:nameCriteria)`
4. `ApplicantRepositoryAdapter.toSummary()` desencripta `identification_encrypted` → `ApplicantSummary` con identificación en texto plano
5. Retorna `Page<ApplicantSearchResponse>` con paginación estándar Spring (`page`, `size`, `totalElements`)

**Flujo de edición (PATCH /api/v1/solicitantes/{id}):**
1. Controller recibe `UpdateApplicantRequest` (todos los campos nullable) y `Authentication` para obtener el actor
2. `UpdateApplicantService` valida que `identificacion` y `fecha_nacimiento` sean null (inmutables → `ImmutableFieldException` si no)
3. Carga el aplicante existente via `findById` (desencripta identificación para rehydrate)
4. Por cada campo presente en el command: compara contra el valor actual; si cambió → llama `ApplicantEditAuditPort.saveEditAudit()`
5. Construye nuevo `Applicant` via `rehydrate()` con valores merged (dominio valida ingresos negativos, etc.)
6. Persiste via `update()` y retorna `UpdateApplicantResult(applicant, changedFields)`
7. Response incluye `campos_auditados` (lista de nombres de campo que cambiaron)

**Seguridades:**
- Registro: `@PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")`
- Búsqueda: `@PreAuthorize("hasRole('ANALYST') or hasRole('RISK_MANAGER') or hasRole('ADMIN') or hasRole('CREDIT_SUPERVISOR')")`
- Edición: `@PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")`

---

### ✅ SHARED/SECURITY — Completamente implementado

Autenticación JWT y control de acceso basado en roles (RBAC).

Ver sección [6. Seguridad y Autenticación](#6-seguridad-y-autenticación) para el detalle completo.

---

### ⏳ EVALUATION — Estructura creada, lógica pendiente

Evaluación de crédito: corre el scoring model contra los datos del solicitante.

- **Tablas creadas:** `evaluation`, `evaluation_detail`, `evaluation_knockout`
- **Controller stub:** `GET /api/v1/evaluaciones`, `POST /api/v1/evaluaciones`
- **Pendiente:** lógica de negocio, puertos, adaptadores

---

### ⏳ FINANCIAL DATA — Estructura creada, lógica pendiente

Datos financieros del solicitante (ingresos, gastos, deudas, activos).

- **Tabla creada:** `financial_data` (con versionado — cada update es una nueva version)
- **Pendiente:** controller, puertos, adaptadores

---

### ⏳ SCORING — Estructura creada, lógica pendiente

Modelos de scoring y variables con rangos para puntuación.

- **Tablas creadas:** `scoring_model`, `scoring_variable`, `model_variable_mapping`, `knockout_rule`
- **Controller stub:** `GET /api/v1/scoring-variables`
- **Pendiente:** lógica de gestión de modelos, activación/desactivación

---

### ⏳ REPORTING — Estructura creada, lógica pendiente

Reportes de distribución de riesgo y estadísticas.

- **Controller stub:** `GET /api/v1/reportes/distribución`
- **Pendiente:** queries agregadas, exportación

---

## 5. Base de Datos

El esquema se gestiona con **Flyway**. Las migraciones están en `src/main/resources/db/migration/`.

**Regla:** nunca modifiques una migración ya aplicada. Siempre creá una nueva.

### Tablas por migración

| Migración | Cambios |
|-----------|---------|
| V2 | `applicant` |
| V3 | Extensiones PostgreSQL (pgcrypto, uuid-ossp) |
| V4 | `app_user`, `role_permission` |
| V5 | `authentication_log`, `token_blacklist` |
| V6 | `financial_data` |
| V7 | `scoring_model` |
| V8 | `scoring_variable`, `model_variable_mapping` |
| V9 | `knockout_rule` |
| V10 | `evaluation`, `evaluation_detail`, `evaluation_knockout` |
| V11 | `credit_decision` |
| V12 | `applicant_edit_audit` |
| V13 | `audit_log` |
| V14 | Stored procedures y vistas |
| V15 | Seed data (usuario admin + matriz de permisos) |
| V16 | Reemplaza rol AUDITOR → CREDIT_SUPERVISOR |
| V17 | Simplifica PK de `audit_log`; agrega `created_by`/`updated_by` a `token_blacklist` |
| V18 | Columna `phone` en `applicant`; permiso `ANALYST APPLICANT UPDATE` en `role_permission` |

### Convenciones de la BD

- Todas las PKs son **UUID** (no auto-increment)
- Toda tabla tiene `created_at`, `created_by` (no updatable)
- Tablas mutables tienen además `updated_at`, `updated_by`
- Los datos sensibles nunca se almacenan en plano (identificaciones → AES-GCM encrypted)
- Detección de duplicados via hash (HMAC-SHA256) sin exponer el dato original

### Tabla `applicant`

```sql
id UUID PRIMARY KEY
name VARCHAR(150) NOT NULL
identification_encrypted VARCHAR(700) NOT NULL   -- AES-GCM + IV, base64
identification_hash VARCHAR(128) NOT NULL         -- HMAC-SHA256, base64, UNIQUE
birth_date DATE NOT NULL
employment_type VARCHAR(30) NOT NULL
monthly_income NUMERIC(19,2) NOT NULL
work_experience_months INTEGER NOT NULL
phone VARCHAR(20) NULL                            -- agregado en V18
created_at TIMESTAMP WITH TIME ZONE NOT NULL
created_by VARCHAR(100) NOT NULL
updated_at TIMESTAMP WITH TIME ZONE
updated_by VARCHAR(100)
```

### Tabla `app_user`

```sql
id UUID PRIMARY KEY
username VARCHAR(50) NOT NULL UNIQUE
email VARCHAR(255) NOT NULL UNIQUE
password_hash VARCHAR(255) NOT NULL    -- BCrypt
role VARCHAR(30) NOT NULL              -- ADMIN | ANALYST | RISK_MANAGER | CREDIT_SUPERVISOR
enabled BOOLEAN NOT NULL DEFAULT true
account_locked BOOLEAN NOT NULL DEFAULT false
failed_login_attempts INTEGER NOT NULL DEFAULT 0
password_changed_at TIMESTAMP WITH TIME ZONE NOT NULL
created_at TIMESTAMP WITH TIME ZONE NOT NULL
created_by VARCHAR(100) NOT NULL
```

### Tabla `token_blacklist`

```sql
id UUID PRIMARY KEY
jti VARCHAR(36) NOT NULL UNIQUE      -- JWT ID revocado
user_id UUID NOT NULL
reason VARCHAR(50) NOT NULL
revoked_at TIMESTAMP WITH TIME ZONE NOT NULL
expires_at TIMESTAMP WITH TIME ZONE NOT NULL
```

---

## 6. Seguridad y Autenticación

### Flujo de autenticación

```
1. POST /api/v1/auth/login
   { "username": "ana", "password": "..." }
        ↓
2. AuthController → AuthenticateService
        ↓
3. Spring Security valida credenciales
   (DaoAuthenticationProvider + BCrypt)
        ↓
4. Si OK → JwtService.generateToken(user)
   Claims del JWT: sub=username, jti=UUID, role=ANALYST, exp=...
        ↓
5. Response: { "token": "eyJ...", "role": "ANALYST", "expiresAt": "..." }
```

### Flujo por request autenticado

```
Request con: Authorization: Bearer eyJ...
        ↓
JwtAuthenticationFilter (OncePerRequestFilter)
   1. Extrae y valida firma del JWT
   2. Extrae claims (jti, username, role)
   3. Verifica que jti NO esté en token_blacklist
   4. Carga usuario desde BD, verifica enabled y !accountLocked
   5. Verifica que usuario NO esté bloqueado (isUserBlacklisted)
   6. Coloca SecurityContext: ROLE_ANALYST (o el rol correspondiente)
        ↓
Controller con @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
   OK → 200
   Sin JWT → 401
   JWT válido pero rol incorrecto → 403
```

### Roles y permisos

| Rol | Descripción |
|-----|-------------|
| `ADMIN` | Acceso total. Único que puede cambiar roles. |
| `ANALYST` | Registra solicitantes, captura datos financieros, crea evaluaciones. |
| `RISK_MANAGER` | Gestiona modelos de scoring, decide créditos, ve reportes. |
| `CREDIT_SUPERVISOR` | Supervisa evaluaciones y ve reportes. |

### Protecciones especiales

- **Último admin:** si solo hay un `ADMIN`, el sistema rechaza cambiarle el rol.
- **Blacklist de tokens:** al revocar acceso de un usuario, todos sus tokens quedan inválidos.
- **Cuenta bloqueada:** si `account_locked = true`, el JWT es rechazado aunque sea válido.

### Variables de entorno requeridas en producción

```bash
APP_JWT_SECRET=<base64 de al menos 32 bytes>
APP_CRYPTO_ENCRYPTION_KEY_BASE64=<AES-256 key en base64, 32 bytes>
APP_CRYPTO_HASH_KEY_BASE64=<HMAC key en base64, 32 bytes>
```

---

## 7. Endpoints y Roles

### Públicos (sin token)

| Método | Path | Descripción |
|--------|------|-------------|
| POST | `/api/v1/auth/login` | Login → JWT |
| GET | `/actuator/health` | Health check |
| GET | `/api-docs/**` | OpenAPI spec |
| GET | `/swagger-ui/**` | Swagger UI |

### Protegidos

| Método | Path | Roles permitidos | Estado |
|--------|------|-----------------|--------|
| POST | `/api/v1/solicitantes` | ANALYST, ADMIN | ✅ |
| GET | `/api/v1/solicitantes` | ANALYST, RISK_MANAGER, ADMIN, CREDIT_SUPERVISOR | ✅ |
| PATCH | `/api/v1/solicitantes/{id}` | ANALYST, ADMIN | ✅ |
| POST | `/api/v1/auth/usuarios` | ADMIN | ✅ |
| PATCH | `/api/v1/auth/usuarios/{id}/rol` | ADMIN | ✅ |
| POST | `/api/v1/auth/login` | público | ✅ |
| GET | `/api/v1/evaluaciones` | ADMIN, ANALYST, CREDIT_SUPERVISOR, RISK_MANAGER | ⏳ stub |
| POST | `/api/v1/evaluaciones` | ADMIN, ANALYST | ⏳ stub |
| GET | `/api/v1/reportes/distribución` | ADMIN, RISK_MANAGER | ⏳ stub |
| GET | `/api/v1/scoring-variables` | (autenticado) | ⏳ stub |

---

## 8. Manejo de Errores

Todos los errores siguen **RFC 7807 (Problem Details)**. La respuesta siempre tiene esta estructura:

```json
{
  "type": "https://api.creditscoring.udea.co/errors/validation",
  "title": "Validation Error",
  "status": 400,
  "detail": "Los ingresos mensuales deben ser un valor numérico mayor a cero",
  "errorCode": "VALIDATION_FAILED",
  "timestamp": "2026-03-31T10:30:45.123Z",
  "traceId": "a1b2c3d4e5f6",
  "path": "/api/v1/solicitantes"
}
```

### Mapa de excepciones → HTTP

| Excepción | HTTP | `errorCode` | Cuándo ocurre |
|-----------|------|-------------|---------------|
| `ApplicantValidationException` | 400 | `VALIDATION_FAILED` | Datos inválidos del solicitante |
| `ImmutableFieldException` | 400 | `IMMUTABLE_FIELD` | Intento de editar `identificacion` o `fecha_nacimiento` |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_FAILED` | Falla en `@Valid` de la request |
| `InvalidCredentialsException` | 401 | `INVALID_CREDENTIALS` | Login fallido |
| `AccessDeniedException` | 403 | `ACCESS_DENIED` | Rol insuficiente (`@PreAuthorize`) |
| `ResourceNotFoundException` | 404 | `RESOURCE_NOT_FOUND` | Entidad no encontrada por ID |
| `DuplicateApplicantException` | 409 | `DUPLICATE_RESOURCE` | Identificación ya registrada |
| `DuplicateUserException` | 409 | `DUPLICATE_USER` | Username/email ya registrado |
| `LastAdminException` | 409 | `LAST_ADMIN` | Intento de quitar el único admin |
| `Exception` (genérico) | 500 | `INTERNAL_ERROR` | Error no esperado |

El handler global está en `GlobalExceptionHandler.java`.

---

## 9. Tests

Los tests están en `src/test/java/` organizados en tres niveles:

```
test/
├── applicant/
│   ├── ApplicantRegistrationIntegrationTest.java    ← IT con BD real (registro)
│   ├── SearchApplicantIntegrationTest.java          ← IT: AC-01 a AC-05 + roles (GET)
│   ├── UpdateApplicantIntegrationTest.java          ← IT: AC-06 a AC-09 + 404 (PATCH)
│   ├── application/service/
│   │   ├── SearchApplicantServiceTest.java          ← Unit: hash, LIKE, paginación
│   │   └── UpdateApplicantServiceTest.java          ← Unit: inmutables, auditoría, happy path
│   ├── domain/model/
│   │   └── ApplicantTest.java                      ← Unit: phone field, registerNew, rehydrate
│   └── migration/
│       └── V18MigrationTest.java                   ← Verifica columna phone + permiso ANALYST
└── shared/security/
    ├── acceptance/
    │   ├── AuthLoginAT.java                          ← Cucumber / REST Assured
    │   └── PermissionMatrixAT.java
    ├── application/service/
    │   ├── AuthenticateServiceTest.java              ← Unit test
    │   └── ChangeUserRoleServiceTest.java
    ├── infrastructure/jwt/
    │   ├── JwtServiceTest.java
    │   └── JwtAuthenticationFilterTest.java
    ├── integration/
    │   ├── CreateUserIntegrationTest.java
    │   ├── LastAdminProtectionIT.java
    │   ├── SecurityFilterChainIT.java
    │   └── TokenBlacklistFlowIT.java
    └── migration/
        └── V16MigrationTest.java
```

### Tipos de test

**Unit tests** (`*Test.java`): prueban una clase en aislamiento, sin Spring context ni BD.

**Integration tests** (`*IT.java`): levantan Spring context completo con Testcontainers (PostgreSQL real). Tardan más pero validan el flujo end-to-end real.

**Acceptance tests** (`*AT.java`): usan REST Assured para hacer requests HTTP reales contra un servidor Spring levantado. Son los más cercanos a lo que haría un cliente real.

### Cómo correr los tests

```bash
# Todos
./gradlew test

# Un test específico
./gradlew test --tests "co.udea.codefactory.creditscoring.applicant.ApplicantRegistrationIntegrationTest"

# Con logs visibles
./gradlew test --info
```

**Requisito:** Docker debe estar corriendo. Los tests de integración usan Testcontainers para levantar PostgreSQL automáticamente.

### Perfil de test (`application-test.yml`)

Los tests corren con `@ActiveProfiles("test")` que activa:
- Testcontainers JDBC driver (levanta PostgreSQL en Docker automáticamente)
- Flyway ejecuta todas las migraciones en cada test context
- JWT secret y crypto keys hardcodeadas (sólo para tests)

---

## 10. Configuración

### Variables de entorno (producción)

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `APP_JWT_SECRET` | Secret para firmar JWT (base64) | `dGVzdFNlY3JldA==` |
| `APP_JWT_EXPIRATION_MS` | Duración del token en ms | `86400000` (1 día) |
| `APP_CRYPTO_ENCRYPTION_KEY_BASE64` | Clave AES-256 para encriptar (32 bytes, base64) | |
| `APP_CRYPTO_HASH_KEY_BASE64` | Clave HMAC-SHA256 (32 bytes, base64) | |

### `application.yml` — propiedades clave

```yaml
app:
  security:
    jwt:
      secret: ${APP_JWT_SECRET}
      expiration-ms: ${APP_JWT_EXPIRATION_MS:86400000}
    crypto:
      encryption-key-base64: ${APP_CRYPTO_ENCRYPTION_KEY_BASE64}
      hash-key-base64: ${APP_CRYPTO_HASH_KEY_BASE64}
```

### Beans de configuración

| Clase | Propósito |
|-------|-----------|
| `SecurityConfig` | Spring Security: filtro JWT, autenticación, endpoints públicos |
| `JwtProperties` | Bind de `app.security.jwt.*` a objeto Java |
| `CryptoProperties` | Bind de `app.security.crypto.*` a objeto Java |
| `JpaAuditingConfig` | Habilita auditoría automática JPA (`@CreatedDate`, `@CreatedBy`, etc.) |
| `CorsConfig` | Configuración CORS para el frontend |
| `OpenApiConfig` | Configuración de Swagger/SpringDoc |

---

## 11. Cómo implementar un nuevo feature

Seguí este orden. No saltees pasos.

### Paso 1 — Definí el modelo de dominio

En `<bounded-context>/domain/model/`, creá el modelo como `record` si es inmutable, o como clase si necesita mutación controlada.

```java
// Ejemplo: si implementás FINANCIAL DATA
public record FinancialData(
    UUID id,
    UUID applicantId,
    int version,
    BigDecimal annualIncome,
    // ...
) {
    public static FinancialData registerNew(...) {
        // validaciones de dominio acá
        return new FinancialData(...);
    }
}
```

### Paso 2 — Definí los puertos

**Puerto IN** (`domain/port/in/`): la interface del use case.

```java
public interface RegisterFinancialDataUseCase {
    FinancialData register(RegisterFinancialDataCommand command);
}
```

**Puertos OUT** (`domain/port/out/`): lo que el dominio necesita del exterior.

```java
public interface FinancialDataRepositoryPort {
    FinancialData save(FinancialData data);
    Optional<FinancialData> findLatestByApplicantId(UUID applicantId);
}
```

### Paso 3 — Implementá el servicio de aplicación

En `application/service/`, implementá el use case.

```java
@Service
public class RegisterFinancialDataService implements RegisterFinancialDataUseCase {
    private final FinancialDataRepositoryPort repository;
    // constructor injection

    @Override
    public FinancialData register(RegisterFinancialDataCommand command) {
        // orquestá acá: validar → crear modelo → persistir
    }
}
```

### Paso 4 — Implementá los adaptadores de infraestructura

**Adaptador OUT (persistencia):** crealo en `infrastructure/adapter/out/persistence/`.

```java
@Component
public class FinancialDataRepositoryAdapter implements FinancialDataRepositoryPort {
    private final JpaFinancialDataRepository jpaRepository;
    // ...
}
```

**Adaptador IN (REST):** crealo en `infrastructure/adapter/in/rest/`.

```java
@RestController
@RequestMapping("/api/v1/datos-financieros")
public class FinancialDataController {
    private final RegisterFinancialDataUseCase registerUseCase;

    @PostMapping
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    public ResponseEntity<FinancialDataResponse> register(@Valid @RequestBody RegisterFinancialDataRequest request) {
        // ...
    }
}
```

### Paso 5 — Escribí tests antes de implementar (TDD)

El proyecto tiene **Strict TDD Mode** activo. Los tests deben existir antes o en paralelo a la implementación.

Mínimo esperado:
- **Unit test** para el servicio de aplicación
- **Integration test** con Testcontainers para el flujo completo

Plantilla de integration test:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class FinancialDataRegistrationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ANALYST")
    void shouldRegisterFinancialData() throws Exception {
        mockMvc.perform(post("/api/v1/datos-financieros")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ ... }"))
            .andExpect(status().isCreated());
    }
}
```

### Paso 6 — Agregá la migración de Flyway

Si necesitás una tabla nueva o modificar una existente, creá un archivo nuevo en `src/main/resources/db/migration/`.

**Nombre:** `V{siguiente_número}__descripción_breve.sql`

```sql
-- V17__create_financial_data_table.sql
CREATE TABLE financial_data (
    id UUID PRIMARY KEY,
    applicant_id UUID NOT NULL REFERENCES applicant(id),
    version INTEGER NOT NULL,
    annual_income NUMERIC(19,2) NOT NULL,
    -- ...
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL
);
```

### Checklist antes de hacer PR

- [ ] Dominio sin imports de Spring/JPA/infraestructura
- [ ] Puertos IN y OUT definidos como interfaces en domain
- [ ] Use case implementado en application/service
- [ ] Adaptadores en infrastructure/adapter/in y out
- [ ] Controller con `@PreAuthorize` con los roles correctos
- [ ] Errores de negocio como excepciones de dominio, mapeadas en `GlobalExceptionHandler`
- [ ] Unit test para el servicio
- [ ] Integration test con Testcontainers
- [ ] Migración Flyway si hay cambios de esquema
- [ ] Tests pasando (`./gradlew test`)

---

## Observabilidad

### Logs

Todos los logs salen en JSON (Logstash format). Cada request incluye un `traceId` en el MDC.

```json
{
  "@timestamp": "2026-03-31T10:30:45.123Z",
  "level": "INFO",
  "logger_name": "co.udea...RegisterApplicantService",
  "message": "Applicant registered successfully",
  "traceId": "a1b2c3d4e5f6"
}
```

### Métricas

Disponibles en `/actuator/prometheus`. Métricas custom:

- `applicant.registration.success` — contador de registros exitosos
- `applicant.registration.failure[reason]` — contador de fallos por tipo

### Auditoría

- **JPA Auditing**: `created_at`, `created_by`, `updated_at`, `updated_by` en cada entidad automáticamente
- **audit_log**: tabla para auditoría de operaciones críticas (cambios de rol, etc.)
- **authentication_log**: cada intento de login registrado

---

*Última actualización: Abril 1 2026*
