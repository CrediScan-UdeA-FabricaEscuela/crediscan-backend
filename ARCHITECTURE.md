# Credit Scoring Engine — Guía de Arquitectura

> Documento para el equipo de desarrollo. Si vas a implementar un feature nuevo, leé esto primero.

---

## Tabla de Contenidos

1. [Visión General](#1-visión-general)
2. [Arquitectura Hexagonal](#2-arquitectura-hexagonal)
3. [Estructura de Packages y Mapa de Módulos](#3-estructura-de-packages-y-mapa-de-módulos)
4. [Bounded Contexts](#4-bounded-contexts)
5. [Reglas de Dependencia](#5-reglas-de-dependencia)
6. [Patrones Clave](#6-patrones-clave)
7. [Manejo de Errores](#7-manejo-de-errores)
8. [Base de Datos](#8-base-de-datos)
9. [Seguridad y Autenticación](#9-seguridad-y-autenticación)
10. [Endpoints y Roles](#10-endpoints-y-roles)
11. [Tests](#11-tests)
12. [CI/CD](#12-cicd)
13. [Configuración](#13-configuración)
14. [Cómo implementar un nuevo módulo](#14-cómo-implementar-un-nuevo-módulo)
15. [Observabilidad](#15-observabilidad)
16. [ADRs — Decisiones de Arquitectura](#16-adrs--decisiones-de-arquitectura)

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
| Tests | JUnit 5, Testcontainers, Cucumber, REST Assured, ArchUnit |

---

## 2. Arquitectura Hexagonal

El proyecto usa **Arquitectura Hexagonal** (también llamada Ports & Adapters o Clean Architecture).

La idea central es que el **dominio de negocio no sabe nada de infraestructura** (ni de JPA, ni de HTTP, ni de cómo se encripta algo). Todo lo que el dominio necesita del exterior lo pide a través de **puertos** (interfaces), y la infraestructura provee **adaptadores** que implementan esos puertos.

```
┌─────────────────────────────────────────────────────────────┐
│                      INFRAESTRUCTURA                        │
│                                                             │
│  ┌─────────────┐   ┌──────────────────────────────────┐     │
│  │  REST API   │   │          ADAPTADORES OUT         │     │
│  │ (Adapter IN)│   │  JPA / Crypto / Metrics / JWT    │     │
│  └──────┬──────┘   └──────────────┬───────────────────┘     │
│         │                         │                         │
└─────────┼─────────────────────────┼─────────────────────────┘
          │                         │
          ▼                         ▲
┌─────────────────────────────────────────────────────────────┐
│                       APLICACIÓN                            │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              UseCase / Service                      │    │
│  │   Orquesta el flujo. No sabe cómo se persiste.      │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
          │                         │
          ▼                         ▼
┌─────────────────────────────────────────────────────────────┐
│                        DOMINIO                              │
│                                                             │
│   Modelos · Puertos IN · Puertos OUT · Excepciones          │
│   (No depende de nada externo. Es el corazón.)              │
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

---

## 3. Estructura de Packages y Mapa de Módulos

### Mapa de módulos

| Módulo | Estado | Responsabilidad |
|--------|--------|-----------------|
| `applicant` | Implementado | Registro, búsqueda y edición de solicitantes de crédito |
| `financialdata` | Parcialmente implementado | Captura y versionado de datos financieros del solicitante |
| `shared/security` | Implementado | Autenticación JWT, RBAC, gestión de usuarios |
| `shared/audit` | Implementado | Trazabilidad de eventos críticos del sistema |
| `evaluation` | Stub | Evaluación crediticia: corre el scoring model |
| `scoring` | Stub | Gestión de modelos y variables de scoring |
| `reporting` | Stub | Reportes de distribución de riesgo y estadísticas |

### Estructura de packages

```
co.udea.codefactory.creditscoring/
│
├── applicant/                          ← Bounded context: Solicitantes
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Applicant.java          ← Record inmutable; incluye phone
│   │   │   └── EmploymentType.java     ← Enum con factory method fromApiValue()
│   │   ├── port/
│   │   │   ├── in/
│   │   │   │   ├── RegisterApplicantUseCase.java
│   │   │   │   ├── SearchApplicantUseCase.java
│   │   │   │   └── UpdateApplicantUseCase.java
│   │   │   └── out/
│   │   │       ├── ApplicantRepositoryPort.java
│   │   │       ├── IdentificationCryptoPort.java
│   │   │       ├── ApplicantEditAuditPort.java
│   │   │       └── ApplicantRegistrationMetricsPort.java
│   │   └── exception/
│   │       ├── ApplicantValidationException.java
│   │       ├── DuplicateApplicantException.java
│   │       └── ImmutableFieldException.java
│   ├── application/
│   │   ├── dto/
│   │   │   ├── RegisterApplicantCommand.java
│   │   │   ├── ApplicantSummary.java
│   │   │   ├── UpdateApplicantCommand.java
│   │   │   └── UpdateApplicantResult.java
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
│           │       ├── ApplicantResponse.java
│           │       ├── ApplicantSearchResponse.java
│           │       ├── UpdateApplicantRequest.java
│           │       └── UpdateApplicantResponse.java
│           └── out/
│               ├── persistence/
│               │   ├── ApplicantJpaEntity.java
│               │   ├── JpaApplicantRepository.java
│               │   ├── ApplicantRepositoryAdapter.java
│               │   ├── ApplicantEditAuditJpaEntity.java
│               │   ├── JpaApplicantEditAuditRepository.java
│               │   └── ApplicantEditAuditAdapter.java
│               ├── crypto/
│               │   ├── CryptoProperties.java
│               │   └── IdentificationCryptoAdapter.java
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
    ├── PagedResult.java                ← Record de paginación sin dependencias Spring
    ├── PageRequest.java                ← Record de request de paginación sin dependencias Spring
    ├── audit/
    │   └── AuditableEntity.java        ← @MappedSuperclass para auditoría JPA
    ├── config/
    │   ├── CorsConfig.java             ← CORS externalizado via CORS_ALLOWED_ORIGINS
    │   ├── JpaAuditingConfig.java
    │   └── OpenApiConfig.java
    ├── exception/
    │   ├── DomainException.java        ← Clase abstracta base (OCP)
    │   ├── GlobalExceptionHandler.java ← @RestControllerAdvice (RFC 7807)
    │   └── ResourceNotFoundException.java
    ├── logging/
    │   └── MdcFilter.java              ← Inyecta traceId en cada request
    └── security/
        ├── SecurityConfig.java
        ├── domain/
        │   ├── model/
        │   │   ├── AppUser.java
        │   │   ├── Role.java           ← ADMIN, ANALYST, RISK_MANAGER, CREDIT_SUPERVISOR
        │   │   ├── AuthResult.java
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
            │   ├── JwtService.java
            │   ├── JwtProperties.java
            │   └── JwtAuthenticationFilter.java
            ├── persistence/
            │   ├── JpaAppUserEntity.java
            │   ├── JpaAppUserRepository.java
            │   ├── JpaAuditLogEntity.java
            │   ├── JpaAuditLogRepository.java
            │   ├── JpaRolePermissionEntity.java
            │   ├── JpaRolePermissionRepository.java
            │   ├── JpaTokenBlacklistEntity.java
            │   ├── JpaTokenBlacklistRepository.java
            │   ├── JpaUserDetailsService.java
            │   ├── AppUserRepositoryAdapter.java
            │   ├── AuditLogAdapter.java
            │   ├── RolePermissionAdapter.java
            │   ├── TokenBlacklistAdapter.java
            │   └── AuditLogId.java
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

### APPLICANT — Implementado

Registro, búsqueda y edición de solicitantes de crédito.

**Modelos clave:**

```java
public record Applicant(UUID id, String name, String identification,
                        LocalDate birthDate, EmploymentType employmentType,
                        BigDecimal monthlyIncome, Integer workExperienceMonths,
                        String phone)
// Reglas aplicadas en registerNew() y rehydrate():
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
5. Retorna `PagedResult<ApplicantSummary>` (tipo de dominio, sin dependencia Spring)

**Flujo de edición (PATCH /api/v1/solicitantes/{id}):**
1. Controller recibe `UpdateApplicantRequest` (todos los campos nullable) y `Authentication` para obtener el actor
2. `UpdateApplicantService` valida que `identificacion` y `fecha_nacimiento` sean null (inmutables → `ImmutableFieldException` si no)
3. Carga el aplicante existente via `findById` (desencripta identificación para rehydrate)
4. Por cada campo presente en el command: compara contra el valor actual; si cambió → llama `ApplicantEditAuditPort.saveEditAudit()`
5. Construye nuevo `Applicant` via `rehydrate()` con valores merged
6. Persiste via `update()` y retorna `UpdateApplicantResult(applicant, changedFields)`

**Seguridades:**
- Registro: `@PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")`
- Búsqueda: `@PreAuthorize("hasRole('ANALYST') or hasRole('RISK_MANAGER') or hasRole('ADMIN') or hasRole('CREDIT_SUPERVISOR')")`
- Edición: `@PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")`

---

### SHARED/SECURITY — Implementado

Autenticación JWT y control de acceso basado en roles (RBAC).

Ver sección [9. Seguridad y Autenticación](#9-seguridad-y-autenticación) para el detalle completo.

---

### SHARED/AUDIT — Implementado

Captura y consulta de eventos relevantes del sistema, con filtrado, paginación y exportación.

**Modelos clave:**

```java
public record AuditLogRecord(UUID id,
                             OffsetDateTime timestamp,
                             String userId,
                             String username,
                             String action,
                             String resource,
                             String resourceId,
                             String ipAddress,
                             String result,
                             String beforeState,
                             String afterState,
                             String metadataJson) {}
```

**Flujo de escritura:** cada evento relevante del sistema invoca `AuditLogPort.record(...)`. El adaptador JPA persiste el registro en `audit_log`. El log queda inmutable.

**Flujo de consulta (GET /api/v1/auditoria):** el controller recibe filtros opcionales (`from`, `to`, `userId`, `action`, `resource`), el servicio construye la consulta dinámica y devuelve registros paginados.

**Flujo de exportación (GET /api/v1/auditoria/export):** mismos filtros, responde con `text/csv`.

**Notas:** el log de auditoría es inmutable; no se ofrecen operaciones de actualización o eliminación.

---

### FINANCIAL DATA — Parcialmente implementado

Captura y versionado de datos financieros de los solicitantes: ingresos, gastos, deudas y activos.

```java
public record FinancialData(UUID id, UUID applicantId, int version,
                            BigDecimal annualIncome, BigDecimal monthlyExpenses,
                            BigDecimal currentDebts, BigDecimal assetsValue,
                            BigDecimal declaredPatrimony, boolean hasOutstandingDefaults,
                            int creditHistoryMonths, int defaultsLast12m, int defaultsLast24m,
                            Integer externalBureauScore, int activeCreditProducts,
                            OffsetDateTime createdAt, OffsetDateTime updatedAt)
// Reglas: applicantId obligatorio; version >= 0;
// montos >= 0; externalBureauScore entre 0 y 999 si presente
```

**Pendiente:** consulta de historial (`GET /api/v1/solicitantes/{id}/datos-financieros?incluir_historial=true`)

---

### EVALUATION — Stub

- **Tablas creadas:** `evaluation`, `evaluation_detail`, `evaluation_knockout`
- **Controller stub:** `GET /api/v1/evaluaciones`, `POST /api/v1/evaluaciones`
- **Pendiente:** lógica de negocio, puertos, adaptadores

---

### SCORING — Stub

- **Tablas creadas:** `scoring_model`, `scoring_variable`, `model_variable_mapping`, `knockout_rule`
- **Controller stub:** `GET /api/v1/scoring-variables`
- **Pendiente:** lógica de gestión de modelos, activación/desactivación

---

### REPORTING — Stub

- **Controller stub:** `GET /api/v1/reportes/distribución`
- **Pendiente:** queries agregadas, exportación

---

## 5. Reglas de Dependencia

### Regla fundamental

```
infrastructure  →  application  →  domain
                                   ↑
                              (no depende de nada)
```

El dominio **nunca** importa clases de Spring, JPA, ni ninguna librería de infraestructura.

### Reglas de dependencia — detalle

| Desde | Hacia | Permitido |
|-------|-------|-----------|
| `infrastructure` | `application` | Sí |
| `infrastructure` | `domain` | Sí (solo para usar modelos y puertos) |
| `application` | `domain` | Sí |
| `application` | `infrastructure` | **No** — usa puertos (interfaces) |
| `domain` | `application` | **No** |
| `domain` | `infrastructure` | **No** |
| `domain` | `org.springframework.*` | **No** (excepto `shared.exception`) |
| `domain` | `jakarta.persistence.*` | **No** |

### Enforcement con ArchUnit

Las reglas anteriores están **verificadas en build time** por `ArchitectureRulesTest`. Cualquier violación rompe el pipeline de CI.

```
src/test/java/.../architecture/ArchitectureRulesTest.java
```

Las 4 reglas actualmente verificadas:

| Test | Regla |
|------|-------|
| `domain_should_not_import_infrastructure` | Ninguna clase en `..domain..` depende de `..infrastructure..` |
| `domain_should_not_import_spring_framework` | Ninguna clase en `..domain..` (fuera de `shared.exception`) depende de `org.springframework..` |
| `domain_should_not_import_jpa_annotations` | Ninguna clase en `..domain..` depende de `jakarta.persistence..` |
| `application_should_not_import_infrastructure_adapters` | Ninguna clase en `..application..` depende de `..infrastructure..` |

> Nota: `shared.exception` está excluido de la regla de Spring porque `GlobalExceptionHandler` (que es infraestructura de Spring) necesita ser encontrado, pero las excepciones de dominio en sí son Java puro. `DomainException` extends `RuntimeException`, sin dependencias de framework.

---

## 6. Patrones Clave

### 6.1 Hexagonal Ports & Adapters

**Puerto OUT** (interface en `domain/port/out/`):

```java
public interface ApplicantRepositoryPort {
    Applicant save(Applicant applicant);
    Optional<Applicant> findById(UUID id);
    PagedResult<ApplicantSummary> search(String hashCriteria, String nameCriteria, PageRequest pageRequest);
    PagedResult<ApplicantSummary> findAll(PageRequest pageRequest);
    void update(Applicant applicant);
}
```

**Adaptador OUT** (en `infrastructure/adapter/out/persistence/`): implementa el puerto usando JPA. El dominio no sabe que existe JPA.

**Puerto IN** (interface en `domain/port/in/`):

```java
public interface RegisterApplicantUseCase {
    Applicant register(RegisterApplicantCommand command);
}
```

**Servicio de aplicación** (en `application/service/`): implementa el puerto IN, inyecta puertos OUT por constructor.

---

### 6.2 DomainException y GlobalExceptionHandler (OCP)

Todas las excepciones de dominio extienden `DomainException`:

```java
// shared/exception/DomainException.java
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) { super(message); }
    protected DomainException(String message, Throwable cause) { super(message, cause); }

    public int httpStatusCode() { return 400; }          // override en subclase
    public String errorCode() { return "DOMAIN_ERROR"; } // override en subclase
}
```

Cada excepción de dominio sobreescribe los métodos para definir su propio código HTTP y `errorCode`:

```java
// Ejemplo: applicant/domain/exception/DuplicateApplicantException.java
public class DuplicateApplicantException extends DomainException {
    public DuplicateApplicantException(String id) {
        super("Applicant with identification already exists: " + id);
    }
    @Override public int httpStatusCode() { return 409; }
    @Override public String errorCode() { return "DUPLICATE_RESOURCE"; }
}
```

El `GlobalExceptionHandler` tiene **un único handler genérico** para todas las excepciones de dominio. No importa ningún módulo específico:

```java
// GlobalExceptionHandler.java
@ExceptionHandler(DomainException.class)
public ProblemDetail handleDomainException(DomainException ex, WebRequest request) {
    HttpStatus httpStatus = HttpStatus.resolve(ex.httpStatusCode());
    // ...construye ProblemDetail con ex.httpStatusCode() y ex.errorCode()
}
```

**Consecuencia práctica (OCP):** cuando se agrega un nuevo módulo con nuevas excepciones, basta con que extiendan `DomainException` — no hay que tocar `GlobalExceptionHandler`.

---

### 6.3 PagedResult / PageRequest — Paginación sin Spring

Los puertos de dominio usan tipos Java puros para paginación:

```java
// shared/PageRequest.java
public record PageRequest(int page, int size) {}

// shared/PagedResult.java
public record PagedResult<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int pageNumber,
        int pageSize) {}
```

Los adaptadores de infraestructura convierten entre `PageRequest`/`PagedResult` y los tipos de Spring Data (`Pageable`/`Page`). El dominio nunca importa `org.springframework.data.domain.*`.

**Ejemplo de conversión en el adaptador:**

```java
// ApplicantRepositoryAdapter.java
@Override
public PagedResult<ApplicantSummary> findAll(PageRequest pageRequest) {
    Pageable pageable = org.springframework.data.domain.PageRequest.of(
            pageRequest.page(), pageRequest.size());
    Page<ApplicantJpaEntity> page = jpaRepository.findAll(pageable);
    List<ApplicantSummary> content = page.getContent().stream()
            .map(this::toSummary).toList();
    return new PagedResult<>(content, page.getTotalElements(),
            page.getTotalPages(), page.getNumber(), page.getSize());
}
```

---

### 6.4 @Transactional — Solo en servicios de aplicación

`@Transactional` pertenece **únicamente** en los servicios de aplicación (`application/service/`). Los adaptadores de infraestructura **no llevan** `@Transactional`.

**Correcto:**
```java
// application/service/RegisterApplicantService.java
@Service
@Transactional
public class RegisterApplicantService implements RegisterApplicantUseCase {
    // ...
}
```

**Incorrecto (no hacer esto):**
```java
// infrastructure/adapter/out/persistence/ApplicantRepositoryAdapter.java
@Component
// NO: @Transactional  ← no corresponde acá
public class ApplicantRepositoryAdapter implements ApplicantRepositoryPort {
    // ...
}
```

La razón: la transacción abarca un caso de uso completo (unidad de trabajo de negocio). Si el adaptador manejara su propia transacción, podría confirmar parcialmente y romper la consistencia. Los adaptadores `TokenBlacklistAdapter`, `AuditLogAdapter` y `AppUserRepositoryAdapter` no tienen `@Transactional` por este motivo.

---

### 6.5 CORS Externalizado

Los orígenes permitidos se leen desde la variable de entorno `CORS_ALLOWED_ORIGINS`:

```java
// shared/config/CorsConfig.java
@Value("${cors.allowed-origins:http://localhost:3000,http://localhost:4200,http://localhost:5173}")
private String[] allowedOrigins;
```

En desarrollo, el default cubre los puertos típicos de React, Angular y Vite. En producción, definir `CORS_ALLOWED_ORIGINS` con los dominios del frontend.

---

## 7. Manejo de Errores

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

### Catálogo de excepciones de dominio

| Excepción | Módulo | HTTP | `errorCode` | Cuándo ocurre |
|-----------|--------|------|-------------|---------------|
| `ApplicantValidationException` | applicant | 400 | `VALIDATION_FAILED` | Datos inválidos del solicitante |
| `ImmutableFieldException` | applicant | 400 | `IMMUTABLE_FIELD` | Intento de editar `identificacion` o `fecha_nacimiento` |
| `MethodArgumentNotValidException` | shared | 400 | `VALIDATION_FAILED` | Falla en `@Valid` de la request |
| `InvalidCredentialsException` | security | 401 | `INVALID_CREDENTIALS` | Login fallido |
| `AccessDeniedException` | shared | 403 | `ACCESS_DENIED` | Rol insuficiente (`@PreAuthorize`) |
| `ResourceNotFoundException` | shared | 404 | `RESOURCE_NOT_FOUND` | Entidad no encontrada por ID |
| `DuplicateApplicantException` | applicant | 409 | `DUPLICATE_RESOURCE` | Identificación ya registrada |
| `DuplicateUserException` | security | 409 | `DUPLICATE_USER` | Username/email ya registrado |
| `LastAdminException` | security | 409 | `LAST_ADMIN` | Intento de quitar el único admin |
| `Exception` (genérico) | shared | 500 | `INTERNAL_ERROR` | Error no esperado |

El handler global está en `shared/exception/GlobalExceptionHandler.java`.

---

## 8. Base de Datos

El esquema se gestiona con **Flyway**. Las migraciones están en `src/main/resources/db/migration/`.

**Regla:** nunca modifiques una migración ya aplicada. Siempre creá una nueva.

### Tablas por migración

| Migración | Cambios |
|-----------|---------|
| V1 | Esquema base e inicialización del proyecto |
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
| V19 | Columnas `defaults_last_12m`, `defaults_last_24m`, `external_bureau_score`, `active_credit_products` en `financial_data` y `email`, `address` en `applicant` |
| V20 | Columna `result` en `audit_log` |
| V21 | Hace `entity_id` nullable en `audit_log` para fallos de login |

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
phone VARCHAR(20) NULL
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

## 9. Seguridad y Autenticación

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
   5. Coloca SecurityContext: ROLE_ANALYST (o el rol correspondiente)
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
CORS_ALLOWED_ORIGINS=https://app.creditscoring.example.com
```

---

## 10. Endpoints y Roles

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
| POST | `/api/v1/solicitantes` | ANALYST, ADMIN | Implementado |
| GET | `/api/v1/solicitantes` | ANALYST, RISK_MANAGER, ADMIN, CREDIT_SUPERVISOR | Implementado |
| PATCH | `/api/v1/solicitantes/{id}` | ANALYST, ADMIN | Implementado |
| POST | `/api/v1/auth/usuarios` | ADMIN | Implementado |
| PATCH | `/api/v1/auth/usuarios/{id}/rol` | ADMIN | Implementado |
| GET | `/api/v1/auditoria` | ADMIN, RISK_MANAGER, CREDIT_SUPERVISOR | Implementado |
| GET | `/api/v1/auditoria/export` | ADMIN, RISK_MANAGER, CREDIT_SUPERVISOR | Implementado |
| GET | `/api/v1/evaluaciones` | ADMIN, ANALYST, CREDIT_SUPERVISOR, RISK_MANAGER | Stub |
| POST | `/api/v1/evaluaciones` | ADMIN, ANALYST | Stub |
| GET | `/api/v1/reportes/distribución` | ADMIN, RISK_MANAGER | Stub |
| GET | `/api/v1/scoring-variables` | (autenticado) | Stub |

---

## 11. Tests

Los tests están en `src/test/java/` organizados en tres niveles:

```
test/
├── architecture/
│   └── ArchitectureRulesTest.java           ← ArchUnit: 4 reglas arquitectónicas
├── applicant/
│   ├── ApplicantRegistrationIntegrationTest.java
│   ├── SearchApplicantIntegrationTest.java
│   ├── UpdateApplicantIntegrationTest.java
│   ├── application/service/
│   │   ├── SearchApplicantServiceTest.java
│   │   └── UpdateApplicantServiceTest.java
│   ├── domain/model/
│   │   └── ApplicantTest.java
│   └── migration/
│       └── V18MigrationTest.java
└── shared/security/
    ├── acceptance/
    │   ├── AuthLoginAT.java
    │   └── PermissionMatrixAT.java
    ├── application/service/
    │   ├── AuthenticateServiceTest.java
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

**Architecture tests**: corren con ArchUnit y verifican las reglas de dependencia en tiempo de compilación. Un import incorrecto rompe el build.

### Cómo correr los tests

```bash
# Todos
./gradlew test

# Un test específico
./gradlew test --tests "co.udea.codefactory.creditscoring.applicant.ApplicantRegistrationIntegrationTest"

# Con logs visibles
./gradlew test --info

# Generar reporte de cobertura
./gradlew jacocoTestReport
# Reporte disponible en: build/reports/jacoco/test/jacocoTestReport.xml
```

**Requisito:** Docker debe estar corriendo. Los tests de integración usan Testcontainers para levantar PostgreSQL automáticamente.

### Perfil de test (`application-test.yml`)

Los tests corren con `@ActiveProfiles("test")` que activa:
- Testcontainers JDBC driver (levanta PostgreSQL en Docker automáticamente)
- Flyway ejecuta todas las migraciones en cada test context
- JWT secret y crypto keys hardcodeadas (solo para tests)

---

## 12. CI/CD

El pipeline está en `.github/workflows/build-test.yml` y se ejecuta en cada push o pull request a `main`.

### Pasos del pipeline

| Paso | Qué hace |
|------|----------|
| Checkout | Clona el repositorio con `actions/checkout@v4` |
| Set up Java 21 | Instala Temurin JDK 21 con `actions/setup-java@v4` |
| Cache Gradle | Cachea `~/.gradle/caches` y `~/.gradle/wrapper` con hash de `*.gradle.kts` |
| Grant execute | `chmod +x gradlew` |
| Run tests | `./gradlew clean test` (incluye ArchUnit, unit, integration, acceptance) |
| Generate coverage | `./gradlew jacocoTestReport` (corre siempre, incluso si hay fallos) |
| Upload coverage | Sube `build/reports/jacoco/test/jacocoTestReport.xml` como artefacto (7 días) |
| Upload test results | Sube `build/reports/tests/test/` como artefacto (7 días) |

### Variables de entorno del pipeline

Las siguientes variables están hardcodeadas en el workflow solo para el entorno de CI:

| Variable | Propósito |
|----------|-----------|
| `APP_JWT_SECRET` | Secret de desarrollo para firmar JWT en tests |
| `APP_CRYPTO_ENCRYPTION_KEY_BASE64` | Clave AES-256 de desarrollo |
| `APP_CRYPTO_HASH_KEY_BASE64` | Clave HMAC de desarrollo |
| `TESTCONTAINERS_RYUK_DISABLED` | Deshabilita el proceso de limpieza de Testcontainers en CI |

### Implicancias

- Un PR a `main` que rompa cualquier test (incluyendo los de ArchUnit) **no puede mergearse** si el repositorio tiene protección de ramas activa.
- El reporte de cobertura Jacoco queda disponible como artefacto del run en GitHub Actions.

---

## 13. Configuración

### Ejecución con Docker (recomendado para desarrollo)

```bash
# 1. Copiar el archivo de variables de entorno
cp .env.example .env

# 2. Levantar la app + PostgreSQL
docker compose up --build -d

# 3. Verificar que todo esté corriendo
curl http://localhost:8080/actuator/health
# → {"status":"UP"}

# 4. Swagger UI disponible en:
# http://localhost:8080/swagger-ui.html
```

```bash
# Bajar los contenedores
docker compose down

# Bajar y borrar la base de datos (volumen)
docker compose down -v
```

### Variables de entorno

| Variable | Descripción | Default en `.env.example` |
|----------|-------------|---------------------------|
| `APP_JWT_SECRET` | Secret para firmar JWT (base64, ≥32 bytes) | valor de desarrollo |
| `APP_JWT_EXPIRATION_MS` | Duración del token en ms | `86400000` (1 día) |
| `APP_CRYPTO_ENCRYPTION_KEY_BASE64` | Clave AES-256 para encriptar (32 bytes, base64) | valor de desarrollo |
| `APP_CRYPTO_HASH_KEY_BASE64` | Clave HMAC-SHA256 (32 bytes, base64) | valor de desarrollo |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos por CORS (separados por coma) | localhost 3000/4200/5173 |
| `DB_NAME` | Nombre de la base de datos | `credit_scoring` |
| `DB_USERNAME` | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `postgres` |
| `APP_PORT` | Puerto expuesto de la app | `8080` |

### `application.yml` — propiedades clave

```yaml
spring:
  data:
    web:
      pageable:
        serialization-mode: via-dto   # Formato Spring Data 3.3+: page metadata bajo clave "page"

app:
  security:
    jwt:
      secret: ${APP_JWT_SECRET}
      expiration-ms: ${APP_JWT_EXPIRATION_MS:86400000}
    crypto:
      encryption-key-base64: ${APP_CRYPTO_ENCRYPTION_KEY_BASE64}
      hash-key-base64: ${APP_CRYPTO_HASH_KEY_BASE64}

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:4200,http://localhost:5173}
```

### Beans de configuración

| Clase | Propósito |
|-------|-----------|
| `SecurityConfig` | Spring Security: filtro JWT, autenticación, endpoints públicos |
| `JwtProperties` | Bind de `app.security.jwt.*` a objeto Java |
| `CryptoProperties` | Bind de `app.security.crypto.*` a objeto Java |
| `JpaAuditingConfig` | Habilita auditoría automática JPA (`@CreatedDate`, `@CreatedBy`, etc.) |
| `CorsConfig` | CORS: orígenes leídos de `CORS_ALLOWED_ORIGINS` |
| `OpenApiConfig` | Configuración de Swagger/SpringDoc |

---

## 14. Cómo implementar un nuevo módulo

Seguí este orden. No saltees pasos.

### Paso 1 — Definí el modelo de dominio

En `<bounded-context>/domain/model/`, creá el modelo como `record` si es inmutable.

```java
public record NewEntity(UUID id, UUID applicantId, String someField) {
    public static NewEntity registerNew(UUID applicantId, String someField) {
        // validaciones de dominio acá — lanzar DomainException si falla
        if (someField == null || someField.isBlank()) {
            throw new NewEntityValidationException("someField is required");
        }
        return new NewEntity(UUID.randomUUID(), applicantId, someField);
    }
}
```

### Paso 2 — Definí las excepciones de dominio

En `<bounded-context>/domain/exception/`, extendé `DomainException`:

```java
public class NewEntityValidationException extends DomainException {
    public NewEntityValidationException(String message) { super(message); }
    @Override public int httpStatusCode() { return 400; }
    @Override public String errorCode() { return "NEW_ENTITY_VALIDATION_FAILED"; }
}
```

No hay que tocar `GlobalExceptionHandler`. El handler ya lo maneja via polimorfismo.

### Paso 3 — Definí los puertos

**Puerto IN** (`domain/port/in/`):

```java
public interface RegisterNewEntityUseCase {
    NewEntity register(RegisterNewEntityCommand command);
}
```

**Puertos OUT** (`domain/port/out/`) — usá `PagedResult`/`PageRequest` para paginación:

```java
public interface NewEntityRepositoryPort {
    NewEntity save(NewEntity entity);
    Optional<NewEntity> findById(UUID id);
    PagedResult<NewEntity> findAll(PageRequest pageRequest);
}
```

### Paso 4 — Implementá el servicio de aplicación

En `application/service/`. El `@Transactional` va acá, no en el adaptador.

```java
@Service
@Transactional
public class RegisterNewEntityService implements RegisterNewEntityUseCase {
    private final NewEntityRepositoryPort repository;

    public RegisterNewEntityService(NewEntityRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public NewEntity register(RegisterNewEntityCommand command) {
        NewEntity entity = NewEntity.registerNew(command.applicantId(), command.someField());
        return repository.save(entity);
    }
}
```

### Paso 5 — Implementá los adaptadores de infraestructura

**Adaptador OUT (persistencia):**

```java
@Component
public class NewEntityRepositoryAdapter implements NewEntityRepositoryPort {
    private final JpaNewEntityRepository jpaRepository;
    // SIN @Transactional acá

    @Override
    public PagedResult<NewEntity> findAll(PageRequest pageRequest) {
        var pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.page(), pageRequest.size());
        var page = jpaRepository.findAll(pageable);
        return new PagedResult<>(page.getContent().stream().map(this::toDomain).toList(),
                page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize());
    }
}
```

**Adaptador IN (REST):**

```java
@RestController
@RequestMapping("/api/v1/new-entities")
public class NewEntityController {
    private final RegisterNewEntityUseCase registerUseCase;

    @PostMapping
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    public ResponseEntity<NewEntityResponse> register(
            @Valid @RequestBody RegisterNewEntityRequest request) {
        // ...
    }
}
```

### Paso 6 — Escribí tests

Mínimo esperado:
- **Unit test** para el servicio de aplicación (mockear puertos OUT)
- **Integration test** con Testcontainers para el flujo completo

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class NewEntityIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    @WithMockUser(roles = "ANALYST")
    void shouldRegisterNewEntity() throws Exception {
        // ...
    }
}
```

### Paso 7 — Agregá la migración de Flyway

Si necesitás una tabla nueva, creá `V{siguiente_número}__descripcion_breve.sql` en `src/main/resources/db/migration/`.

```sql
CREATE TABLE new_entity (
    id UUID PRIMARY KEY,
    applicant_id UUID NOT NULL REFERENCES applicant(id),
    some_field VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL
);
```

### Checklist antes de hacer PR

- [ ] Dominio sin imports de Spring / JPA / infraestructura
- [ ] Excepciones de dominio extienden `DomainException` con `httpStatusCode()` y `errorCode()` correctos
- [ ] Puertos IN y OUT definidos como interfaces en `domain/port/`
- [ ] Puertos OUT de paginación usan `PagedResult` / `PageRequest` (no Spring Data)
- [ ] `@Transactional` en el servicio de aplicación, no en los adaptadores
- [ ] Adaptadores en `infrastructure/adapter/in` y `out`
- [ ] Controller con `@PreAuthorize` con los roles correctos
- [ ] Unit test para el servicio
- [ ] Integration test con Testcontainers
- [ ] Migración Flyway si hay cambios de esquema
- [ ] `./gradlew test` pasa (incluyendo ArchUnit)

---

## 15. Observabilidad

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
- **audit_log**: tabla para auditoría de operaciones críticas (cambios de rol, login, etc.)
- **authentication_log**: cada intento de login registrado

---

## 16. ADRs — Decisiones de Arquitectura

### ADR-001: Arquitectura Hexagonal con módulos por Bounded Context

**Contexto:** El sistema tiene múltiples dominios de negocio independientes (solicitantes, scoring, evaluación, etc.) con reglas de negocio propias.

**Decisión:** Se adopta Arquitectura Hexagonal (Ports & Adapters) organizada por Bounded Context. Cada contexto tiene sus propias capas `domain`, `application` e `infrastructure`. No hay capas horizontales compartidas (no hay un único `repository/` global).

**Consecuencias:**
- El dominio es independiente de frameworks y puede testearse sin Spring.
- Agregar un nuevo bounded context no requiere tocar los existentes.
- Los adaptadores pueden reemplazarse (ej: cambiar JPA por otro ORM) sin modificar el dominio ni la aplicación.
- Costo: más archivos y estructuras que una arquitectura en capas tradicional.

---

### ADR-002: DomainException como clase abstracta para manejo de errores (OCP)

**Contexto:** El `GlobalExceptionHandler` necesita manejar excepciones de todos los módulos sin importarlos directamente. Si por cada módulo nuevo se agregaba un `@ExceptionHandler` específico, el handler violaba el Principio Abierto/Cerrado.

**Decisión:** Se introduce `DomainException` como clase abstracta con métodos `httpStatusCode()` y `errorCode()`. Todas las excepciones de dominio la extienden y sobreescriben esos métodos. El handler tiene un único `@ExceptionHandler(DomainException.class)` que delega en el polimorfismo.

**Consecuencias:**
- Agregar un nuevo módulo con nuevas excepciones no requiere modificar `GlobalExceptionHandler`.
- El contrato de error (código HTTP + errorCode) vive en la excepción misma, cerca de donde se lanza.
- Las excepciones de dominio siguen siendo Java puro (no dependen de Spring ni HTTP).

---

### ADR-003: PagedResult / PageRequest para desacoplar el dominio de Spring Data

**Contexto:** Los puertos de dominio originalmente usaban `org.springframework.data.domain.Page` y `Pageable`. Esto violaba la regla de que el dominio no depende de frameworks.

**Decisión:** Se crean `PagedResult<T>` y `PageRequest` como records Java puros en el paquete `shared`. Los puertos de dominio usan estos tipos. Los adaptadores de infraestructura convierten entre ellos y los tipos de Spring Data.

**Consecuencias:**
- El dominio no importa ninguna clase de Spring Data.
- Si se cambia Spring Data por otro mecanismo de acceso a datos, los puertos no cambian.
- Costo mínimo: cada adaptador hace la conversión (pocas líneas).

---

### ADR-004: @Transactional solo en servicios de aplicación

**Contexto:** En versiones anteriores, algunos adaptadores de infraestructura (`TokenBlacklistAdapter`, `AuditLogAdapter`, `AppUserRepositoryAdapter`) tenían `@Transactional`. Esto generaba transacciones anidadas y dificultaba razonar sobre los límites transaccionales.

**Decisión:** `@Transactional` se coloca exclusivamente en los servicios de aplicación. Los adaptadores de infraestructura no gestionan transacciones propias. La transacción abarca la unidad de trabajo completa del caso de uso.

**Consecuencias:**
- Hay un único límite transaccional por caso de uso, fácil de razonar.
- El rollback ocurre a nivel del caso de uso completo, no por operación individual.
- Los adaptadores son stateless respecto a transacciones: no pueden confirmar parcialmente.

---

*Última actualización: Abril 2026*
