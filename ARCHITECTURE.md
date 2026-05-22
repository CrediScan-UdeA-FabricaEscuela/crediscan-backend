# Credit Scoring Engine — Guía de Arquitectura

> Documento para el equipo de desarrollo. Si vas a implementar un feature nuevo, leé esto primero.

---

## Tabla de Contenidos

1. [Visión General](#1-visión-general)
2. [Arquitectura Hexagonal + Screaming Architecture](#2-arquitectura-hexagonal--screaming-architecture)
3. [Estructura de Packages y Mapa de Bounded Contexts](#3-estructura-de-packages-y-mapa-de-bounded-contexts)
4. [Bounded Contexts — detalle](#4-bounded-contexts--detalle)
5. [Reglas de Dependencia](#5-reglas-de-dependencia)
6. [Patrones Clave](#6-patrones-clave)
7. [Manejo de Errores](#7-manejo-de-errores)
8. [Base de Datos](#8-base-de-datos)
9. [Seguridad y Autenticación](#9-seguridad-y-autenticación)
10. [Subsistema de Reportes (HU-015 / HU-016 / HU-017)](#10-subsistema-de-reportes-hu-015--hu-016--hu-017)
11. [Tests](#11-tests)
12. [CI/CD](#12-cicd)
13. [Configuración](#13-configuración)
14. [Observabilidad](#14-observabilidad)
15. [Cómo implementar un nuevo módulo](#15-cómo-implementar-un-nuevo-módulo)
16. [ADRs — Decisiones de Arquitectura](#16-adrs--decisiones-de-arquitectura)

---

## 1. Visión General

**Credit Scoring Engine** es una API REST construida con **Java 21 + Spring Boot 3.4.4**.

Evalúa solicitudes de crédito de personas naturales. Contempla el registro de solicitantes con encriptación de datos sensibles, captura y versionado de datos financieros, scoring automatizado mediante modelos configurables con reglas de knockout, evaluación crediticia completa, registro de decisiones finales y generación de reportes analíticos avanzados.

**Stack principal:**

| Capa | Tecnología |
|------|------------|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.4.4 |
| Persistencia | Spring Data JPA + Hibernate 6 |
| Base de datos | PostgreSQL 16 |
| Migraciones | Flyway (V1–V32) |
| Autenticación | JWT (jjwt 0.12.6) + BCrypt |
| Mapeo | MapStruct 1.6.3 |
| PDF | OpenPDF 2.0.3 |
| CSV | Apache Commons CSV 1.10.0 |
| Cache | Caffeine 3.2.0 |
| Logs | Logstash Logback Encoder 8.0 (JSON estructurado) |
| Métricas | Micrometer + Prometheus |
| API Docs | SpringDoc OpenAPI 2.8.4 (Swagger UI) |
| Tests | JUnit 5, Testcontainers 1.20.4, Cucumber 7.20.1, REST Assured 5.5.0, ArchUnit 1.3.0 |

---

## 2. Arquitectura Hexagonal + Screaming Architecture

### 2.1 Hexagonal (Ports & Adapters)

El proyecto usa **Arquitectura Hexagonal**. La idea central es que el **dominio de negocio no sabe nada de infraestructura** — ni de JPA, ni de HTTP, ni de criptografía, ni de PDF. Todo lo que el dominio necesita del exterior lo pide a través de **puertos** (interfaces), y la infraestructura provee **adaptadores** que implementan esos puertos.

```
┌─────────────────────────────────────────────────────────────┐
│                      INFRAESTRUCTURA                        │
│                                                             │
│  ┌─────────────┐   ┌──────────────────────────────────┐    │
│  │  REST API   │   │          ADAPTADORES OUT         │    │
│  │ (Adapter IN)│   │  JPA / Crypto / PDF / CSV / JWT  │    │
│  └──────┬──────┘   └──────────────┬───────────────────┘    │
│         │                         │                         │
└─────────┼─────────────────────────┼─────────────────────────┘
          │                         │
          ▼                         ▲
┌─────────────────────────────────────────────────────────────┐
│                       APLICACIÓN                            │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              UseCase / Service                      │   │
│  │   Orquesta el flujo. No sabe cómo se persiste.      │   │
│  └─────────────────────────────────────────────────────┘   │
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

### 2.2 Screaming Architecture

El paquete raíz `co.udea.codefactory.creditscoring` está organizado por **Bounded Context**, no por capa técnica. La estructura "grita" de qué trata el sistema:

```
creditscoring/
├── applicant/          ← el sistema registra y busca solicitantes
├── creditdecision/     ← el sistema toma decisiones crediticias
├── evaluation/         ← el sistema evalúa riesgo crediticio
├── financialdata/      ← el sistema captura datos financieros
├── reporting/          ← el sistema genera reportes analíticos
├── scoring/            ← el sistema gestiona variables de scoring
├── scoringengine/      ← el sistema calcula scores
├── scoringmodel/       ← el sistema gestiona modelos de scoring
└── shared/             ← seguridad, auditoría, config, infraestructura compartida
```

No existe un directorio `repository/` global, ni `service/` global, ni `controller/` global. Cada bounded context tiene sus propias capas.

### 2.3 Capas dentro de cada bounded context

```
<bounded-context>/
├── domain/
│   ├── model/          ← Records y entidades de dominio (Java puro, sin frameworks)
│   ├── port/
│   │   ├── in/         ← Use cases (interfaces que la infraestructura IN implementa)
│   │   └── out/        ← Repository ports, notification ports, etc.
│   └── exception/      ← Excepciones de dominio (extienden DomainException)
├── application/
│   ├── dto/            ← Commands y resultados (Java puro)
│   └── service/        ← Implementaciones de use cases (@Transactional va acá)
└── infrastructure/
    └── adapter/
        ├── in/
        │   └── rest/   ← Controllers + DTOs de request/response
        └── out/
            ├── persistence/   ← JPA entities, Spring Data repos, adapters
            ├── pdf/           ← Adaptadores de generación PDF
            ├── csv/           ← Adaptadores de exportación CSV
            ├── crypto/        ← Adaptadores de encriptación
            └── metrics/       ← Adaptadores de métricas
```

### 2.4 Ejemplo concreto: Registro de Solicitante

```
HTTP POST /api/v1/solicitantes
        │
        ▼
ApplicantController          ← Adapter IN (infrastructure)
        │  convierte Request → RegisterApplicantCommand
        ▼
RegisterApplicantUseCase     ← Puerto IN (interface en domain)
        │  implementado por
        ▼
RegisterApplicantService     ← Servicio de aplicación (@Transactional)
        │  usa 3 puertos OUT:
        ├─ ApplicantRepositoryPort ──→ ApplicantRepositoryAdapter → JPA → PostgreSQL
        ├─ IdentificationCryptoPort ─→ IdentificationCryptoAdapter → AES-GCM / HMAC
        └─ ApplicantRegistrationMetricsPort → MetricsAdapter → Micrometer
```

---

## 3. Estructura de Packages y Mapa de Bounded Contexts

### Mapa de módulos

| Módulo | Responsabilidad |
|--------|-----------------|
| `applicant` | Registro, búsqueda con filtros, edición y exportación CSV de solicitantes |
| `financialdata` | Captura y versionado de datos financieros del solicitante, comparación entre versiones |
| `scoring` | Variables de scoring: definición, tipos (NUMERIC/CATEGORICAL), rangos, categorías, pesos |
| `scoringmodel` | Modelos de scoring: ciclo de vida (DRAFT→ACTIVE→INACTIVE), comparación, reglas de knockout |
| `scoringengine` | Motor de cálculo de scoring: puntuación ponderada + KO, simulación y escenarios guardados |
| `evaluation` | Ejecución de evaluaciones, búsqueda con filtros, exportación PDF/CSV, estadísticas |
| `creditdecision` | Decisiones crediticias finales (inmutables); escalado con deadline automático |
| `reporting` | Reportes analíticos: distribución de riesgo, efectividad del modelo, actividad de analistas |
| `shared/security` | Autenticación JWT, RBAC, gestión de usuarios, blacklist de tokens |
| `shared/audit` | Registro inmutable de eventos críticos del sistema con exportación CSV |

### Estructura de packages completa

```
co.udea.codefactory.creditscoring/
│
├── applicant/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Applicant.java          ← Record inmutable
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
│   ├── application/
│   │   ├── dto/
│   │   └── service/
│   │       ├── RegisterApplicantService.java
│   │       ├── SearchApplicantService.java
│   │       └── UpdateApplicantService.java
│   └── infrastructure/adapter/
│       ├── in/rest/
│       └── out/
│           ├── persistence/
│           ├── crypto/
│           └── metrics/
│
├── financialdata/
│   ├── domain/model/ → FinancialData.java (record con versionado)
│   ├── application/service/
│   └── infrastructure/adapter/in/rest/ + out/persistence/
│
├── scoring/
│   ├── domain/model/
│   │   ├── ScoringVariable.java        ← Aggregate root
│   │   ├── VariableType.java           ← NUMERIC | CATEGORICAL
│   │   ├── VariableRange.java
│   │   └── VariableCategory.java
│   ├── application/service/
│   │   ├── ScoringVariableCommandService.java
│   │   └── ScoringVariableQueryService.java
│   └── infrastructure/adapter/in/rest/ + out/persistence/
│
├── scoringmodel/
│   ├── domain/model/
│   │   ├── ScoringModel.java           ← Aggregate root (DRAFT→ACTIVE→INACTIVE)
│   │   ├── ModelVariable.java
│   │   ├── KnockoutRule.java
│   │   └── KnockoutOperator.java       ← GT | LT | GTE | LTE | EQ | NEQ
│   ├── application/service/
│   │   ├── ScoringModelCommandService.java
│   │   ├── ScoringModelQueryService.java
│   │   └── KnockoutRuleService.java
│   └── infrastructure/adapter/
│       └── in/rest/ → ScoringModelController.java + KnockoutRuleController.java
│
├── scoringengine/
│   ├── domain/model/
│   │   ├── ScoringResult.java
│   │   ├── VariableScoreDetail.java
│   │   ├── KnockoutEvaluationDetail.java
│   │   └── SimulationScenario.java
│   ├── application/service/
│   │   ├── ScoringEngineService.java
│   │   ├── ScoringSimulationService.java
│   │   ├── ScoringCalculator.java      ← Cálculo ponderado + evaluación KO
│   │   └── FinancialDataValueExtractor.java
│   └── infrastructure/adapter/
│       └── in/rest/ → ScoringEngineController.java + ScoringSimulationController.java
│
├── evaluation/
│   ├── domain/model/
│   │   ├── Evaluation.java             ← Aggregate root
│   │   ├── EvaluationDetail.java       ← Detalle por variable
│   │   ├── EvaluationKnockout.java     ← Reglas KO evaluadas
│   │   ├── RiskLevel.java              ← VERY_LOW | LOW | MEDIUM | HIGH | VERY_HIGH | REJECTED
│   │   └── search/                     ← Modelos de búsqueda/filtro
│   ├── application/service/
│   │   ├── ExecuteEvaluationService.java
│   │   ├── GetEvaluationService.java
│   │   ├── GetEvaluationReportService.java
│   │   └── search/                     ← Servicios de búsqueda y estadísticas
│   └── infrastructure/adapter/out/
│       ├── persistence/ + projection/
│       ├── pdf/
│       └── csv/
│
├── creditdecision/
│   ├── domain/model/
│   │   ├── CreditDecision.java         ← Aggregate root (inmutable una vez registrada)
│   │   └── DecisionStatus.java         ← APPROVED | REJECTED | MANUAL_REVIEW | ESCALATED
│   ├── application/service/
│   │   └── RegisterCreditDecisionService.java
│   └── infrastructure/adapter/out/
│       ├── persistence/
│       └── notification/               ← LoggingEscalationNotificationAdapter.java
│
├── reporting/
│   ├── domain/model/
│   │   ├── (distribución de riesgo)
│   │   ├── efectividad/                ← Matriz de confusión, tasas de concordancia y override
│   │   └── analistas/                  ← Métricas por analista, horas hábiles, outliers
│   ├── domain/port/in/
│   │   ├── efectividad/
│   │   └── analistas/
│   ├── application/service/
│   │   ├── efectividad/
│   │   ├── analistas/
│   │   └── util/                       ← BusinessHoursCalculator, OutlierDetector
│   └── infrastructure/adapter/out/
│       ├── persistence/efectividad/ + analistas/   ← Queries JPA nativas/JPQL agregadas
│       ├── pdf/efectividad/ + analistas/
│       └── csv/                                    ← CSV streaming para HU-017
│
└── shared/
    ├── PagedResult.java                ← Record de paginación sin dependencias Spring
    ├── PageRequest.java
    ├── audit/
    │   └── AuditableEntity.java        ← @MappedSuperclass para auditoría JPA
    ├── config/
    │   ├── CorsConfig.java
    │   ├── JpaAuditingConfig.java
    │   └── OpenApiConfig.java
    ├── exception/
    │   ├── DomainException.java        ← Clase abstracta base (OCP)
    │   ├── GlobalExceptionHandler.java ← @RestControllerAdvice (RFC 7807)
    │   └── ResourceNotFoundException.java
    ├── logging/
    │   └── MdcFilter.java              ← Inyecta traceId en cada request
    └── security/
        ├── domain/model/ → AppUser.java, Role.java, AuthResult.java, RolePermission.java
        ├── domain/port/in/ → AuthenticateUseCase, ChangeUserRoleUseCase, GetPermissionMatrixUseCase
        ├── domain/port/out/ → AppUserRepositoryPort, TokenBlacklistPort, RolePermissionPort, AuditLogPort
        ├── application/service/ → AuthenticateService, ChangeUserRoleService, GetPermissionMatrixService
        └── infrastructure/
            ├── jwt/ → JwtService, JwtProperties, JwtAuthenticationFilter
            ├── persistence/ → JPA entities + adapters (user, audit, token blacklist, role permissions)
            └── rest/ → AuthController, RoleController
```

---

## 4. Bounded Contexts — detalle

### APPLICANT

Registro, búsqueda con filtros avanzados, edición y exportación CSV de solicitantes de crédito.

**Modelos clave:**

```java
public record Applicant(UUID id, String name, String identification,
                        LocalDate birthDate, EmploymentType employmentType,
                        BigDecimal monthlyIncome, Integer workExperienceMonths,
                        String phone)
// Reglas de dominio en registerNew() y rehydrate():
// - nombre obligatorio
// - identificación obligatoria y única (chequeada via HMAC-SHA256 hash en BD)
// - edad >= 18 años
// - ingresos mensuales > 0
// - experiencia laboral >= 0
// - teléfono opcional, máx 20 caracteres
```

**Flujo de registro:**
1. Controller recibe `RegisterApplicantRequest` → `RegisterApplicantCommand`
2. Service crea `Applicant` con validaciones de dominio
3. Genera HMAC-SHA256 del número de identificación → chequea duplicados sin exponer el dato
4. Encripta la identificación con AES-256-GCM
5. Persiste con ID encriptado + hash
6. Registra métrica de éxito via `ApplicantRegistrationMetricsPort`

**Filtros de búsqueda:** nombre (LIKE), identificación exacta (por hash), rango de ingresos, tipo de empleo, rango de experiencia, rango de fechas de registro. Exportación a CSV vía endpoint `/export`.

**Campos inmutables:** `identificacion` y `fecha_nacimiento` no pueden modificarse una vez registrados (`ImmutableFieldException` si se intenta).

---

### FINANCIAL DATA

Captura y versionado de datos financieros del solicitante. Cada actualización crea una nueva versión; se pueden comparar versiones entre sí.

```java
public record FinancialData(UUID id, UUID applicantId, int version,
                            BigDecimal annualIncome, BigDecimal monthlyExpenses,
                            BigDecimal currentDebts, BigDecimal assetsValue,
                            BigDecimal declaredPatrimony, boolean hasOutstandingDefaults,
                            int creditHistoryMonths, int defaultsLast12m, int defaultsLast24m,
                            Integer externalBureauScore, int activeCreditProducts,
                            OffsetDateTime createdAt, OffsetDateTime updatedAt)
// externalBureauScore: 0–999 si presente; montos >= 0
```

---

### SCORING

Gestión de variables de scoring: definición, pesos, rangos numéricos y categorías.

```java
public record ScoringVariable(UUID id, String nombre, String descripcion,
                               VariableType tipo, BigDecimal peso, boolean activa,
                               List<VariableRange> rangos, List<VariableCategory> categorias)
// tipo: NUMERIC o CATEGORICAL
// peso: 0.01–1.00
// rangos NUMERIC: contiguos, sin solapamientos, desde 0
// La suma de pesos del modelo completo debe ser 1.00 al activar
```

---

### SCORING MODEL

Ciclo de vida de modelos y reglas de knockout. Solo puede haber un modelo `ACTIVE` al mismo tiempo.

```java
public record ScoringModel(UUID id, String nombre, String descripcion, int version,
                            ModelStatus estado, List<ModelVariable> variables,
                            OffsetDateTime fechaCreacion, OffsetDateTime fechaActivacion)
// DRAFT (editable) → ACTIVE (en uso) → INACTIVE (histórico)
// Restricciones de activación: pesos suman 1.00 (RN1), mínimo 3 variables activas (RN2)

public record KnockoutRule(UUID id, UUID modeloId, String campo, KnockoutOperator operador,
                            BigDecimal umbral, String mensaje, int prioridad, boolean activa)
// Operadores: GT | LT | GTE | LTE | EQ | NEQ
// Se evalúan en orden ascendente por prioridad; la primera que dispara rechaza
```

---

### SCORING ENGINE

Motor de cálculo y modo de simulación.

```java
public record ScoringResult(UUID modeloId, UUID aplicanteId, BigDecimal puntajeFinal,
                             List<VariableScoreDetail> desglose,
                             List<KnockoutEvaluationDetail> reglasKoEvaluadas,
                             boolean rechazadoPorKo, String mensajeKo)
// puntajeFinal = Σ(puntaje_rango_variable_i × peso_i)
// Si alguna KO rule dispara → puntajeFinal = 0, rechazadoPorKo = true
```

**Flujo de cálculo:**
1. Obtiene el modelo `ACTIVE` con sus variables y reglas KO
2. `FinancialDataValueExtractor` extrae los valores del `FinancialData` del solicitante
3. `ScoringCalculator` evalúa KO rules por prioridad; si alguna dispara → score = 0
4. Si no hay KO: calcula `puntajeFinal = Σ(puntaje_rango × peso)`

**Simulación:** permite calcular el score con valores hipotéticos sin persistencia. Los escenarios guardados (`SimulationScenario`) persisten los valores en un campo JSONB.

---

### EVALUATION

Ejecución de evaluaciones crediticias completas y generación de reportes.

```java
public record Evaluation(UUID id, UUID applicantId, UUID modelId, UUID financialDataId,
                          BigDecimal totalScore, RiskLevel riskLevel, boolean knockedOut,
                          String knockoutReasons, OffsetDateTime evaluatedAt, String evaluatedBy,
                          List<EvaluationDetail> details, List<EvaluationKnockout> knockouts)
// RiskLevel: VERY_LOW | LOW | MEDIUM | HIGH | VERY_HIGH | REJECTED
// knockedOut = true cuando una KO rule dispara → riskLevel = REJECTED, totalScore = 0
```

**Flujo de evaluación:**
1. Valida que el solicitante tenga `FinancialData` registrado (→ 422 si no)
2. Verifica cooldown configurable (→ 409 si está en período de espera)
3. Delega el cálculo al motor de scoring vía puerto interno
4. Persiste la evaluación con todos sus detalles y registros KO

**Búsqueda:** filtros por solicitante, período, nivel de riesgo, analista evaluador, con paginación. Exportación CSV con streaming (sin límite), exportación PDF (cap 1000 filas).

---

### CREDIT DECISION

Registro de la decisión final sobre una evaluación. Inmutable una vez registrada.

```java
public record CreditDecision(UUID id, UUID evaluationId, DecisionStatus decision,
                              String observations, String analystId,
                              OffsetDateTime decidedAt, String supervisorId,
                              OffsetDateTime resolutionDeadlineAt)
// DecisionStatus: APPROVED | REJECTED | MANUAL_REVIEW | ESCALATED
// observations: mínimo 20 caracteres
// ESCALATED: deadline automático de 48h + supervisorId obligatorio
// Si la evaluación fue knockedOut → solo REJECTED es válido
```

---

### REPORTING

Reportes analíticos para gestión de riesgo. Ver sección 10 para detalle completo.

| Reporte | HU | Formatos |
|---------|----|---------|
| Distribución de riesgo | HU-015 | JSON, PDF |
| Efectividad del modelo | HU-016 | JSON, PDF |
| Actividad de analistas | HU-017 | JSON, PDF, CSV |

---

### SHARED/SECURITY

Autenticación JWT y control de acceso basado en roles (RBAC). Ver sección 9.

---

### SHARED/AUDIT

Captura inmutable de eventos críticos del sistema.

```java
public record AuditLogRecord(UUID id, OffsetDateTime timestamp,
                             String userId, String username,
                             String action, String resource, String resourceId,
                             String ipAddress, String result,
                             String beforeState, String afterState, String metadataJson)
```

Consulta con filtros opcionales (from/to, userId, action, resource) y paginación. Exportación a CSV. El log es inmutable: no hay operaciones de actualización ni eliminación.

---

## 5. Reglas de Dependencia

### Regla fundamental

```
infrastructure  →  application  →  domain
                                   ↑
                              (no depende de nada)
```

El dominio **nunca** importa clases de Spring, JPA, ni ninguna librería de infraestructura.

### Tabla de dependencias permitidas

| Desde | Hacia | Permitido |
|-------|-------|-----------|
| `infrastructure` | `application` | Sí |
| `infrastructure` | `domain` | Sí (solo modelos y puertos) |
| `application` | `domain` | Sí |
| `application` | `infrastructure` | **No** — usa puertos (interfaces) |
| `domain` | `application` | **No** |
| `domain` | `infrastructure` | **No** |
| `domain` | `org.springframework.*` | **No** (excepto `shared.exception`) |
| `domain` | `jakarta.persistence.*` | **No** |

### Enforcement con ArchUnit

Las reglas anteriores están **verificadas en build time** por `ArchitectureRulesTest`:

| Test | Regla |
|------|-------|
| `domain_should_not_import_infrastructure` | Ninguna clase en `..domain..` depende de `..infrastructure..` |
| `domain_should_not_import_spring_framework` | Ninguna clase en `..domain..` depende de `org.springframework..` |
| `domain_should_not_import_jpa_annotations` | Ninguna clase en `..domain..` depende de `jakarta.persistence..` |
| `application_should_not_import_infrastructure_adapters` | Ninguna clase en `..application..` depende de `..infrastructure..` |

Un import incorrecto rompe el build de CI.

> Nota: `shared.exception` está excluido de la regla de Spring porque `GlobalExceptionHandler` (infraestructura de Spring) vive ahí, pero las excepciones de dominio en sí son Java puro. `DomainException` extiende `RuntimeException`, sin dependencias de framework.

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

**Servicio de aplicación** (en `application/service/`): implementa el puerto IN, inyecta puertos OUT por constructor, lleva `@Transactional`.

---

### 6.2 DomainException y GlobalExceptionHandler (OCP)

Todas las excepciones de dominio extienden `DomainException`:

```java
public abstract class DomainException extends RuntimeException {
    public int httpStatusCode() { return 400; }
    public String errorCode() { return "DOMAIN_ERROR"; }
}
```

El `GlobalExceptionHandler` tiene un único `@ExceptionHandler(DomainException.class)` que delega en el polimorfismo. Agregar un nuevo módulo con nuevas excepciones no requiere tocar el handler.

---

### 6.3 PagedResult / PageRequest — Paginación sin Spring

Los puertos de dominio usan tipos Java puros:

```java
public record PageRequest(int page, int size) {}

public record PagedResult<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int pageNumber,
        int pageSize) {}
```

Los adaptadores convierten entre estos tipos y los de Spring Data (`Pageable` / `Page`). El dominio nunca importa `org.springframework.data.domain.*`.

---

### 6.4 @Transactional — Solo en servicios de aplicación

`@Transactional` va **únicamente** en los servicios de aplicación. Los adaptadores de infraestructura no gestionan transacciones propias. La transacción abarca la unidad de trabajo completa del caso de uso.

Razón: si el adaptador manejara su propia transacción, podría confirmar parcialmente y romper la consistencia.

---

### 6.5 CORS externalizado

Los orígenes permitidos se leen de `CORS_ALLOWED_ORIGINS`. Default en dev: `http://localhost:3000,http://localhost:4200,http://localhost:5173`.

---

## 7. Manejo de Errores

Todos los errores siguen **RFC 7807 (Problem Details)**:

```json
{
  "type": "https://api.creditscoring.udea.co/errors/validation",
  "title": "Validation Error",
  "status": 400,
  "detail": "Monthly income must be greater than zero",
  "errorCode": "VALIDATION_FAILED",
  "timestamp": "2026-05-22T10:30:45.123Z",
  "traceId": "a1b2c3d4e5f6",
  "path": "/api/v1/solicitantes"
}
```

### Catálogo de excepciones de dominio

| Excepción | Módulo | HTTP | `errorCode` | Cuándo ocurre |
|-----------|--------|------|-------------|---------------|
| `ApplicantValidationException` | applicant | 400 | `VALIDATION_FAILED` | Datos inválidos del solicitante |
| `ImmutableFieldException` | applicant | 400 | `IMMUTABLE_FIELD` | Intento de editar `identificacion` o `fecha_nacimiento` |
| `ScoringVariableValidationException` | scoring | 400 | `VALIDATION_FAILED` | Rangos inválidos, peso fuera de rango |
| `ScoringModelValidationException` | scoringmodel | 400 | `VALIDATION_FAILED` | Peso total ≠ 1.00, menos de 3 variables |
| `CreditDecisionValidationException` | creditdecision | 400 | `CREDIT_DECISION_VALIDATION_FAILED` | Observaciones < 20 caracteres |
| `CreditDecisionKnockoutException` | creditdecision | 400 | `CREDIT_DECISION_KNOCKOUT_CONFLICT` | KO evaluation → solo REJECTED válido |
| `MethodArgumentNotValidException` | shared | 400 | `VALIDATION_FAILED` | Falla en `@Valid` del request |
| `ApplicantNoFinancialDataException` | evaluation | 422 | `NO_FINANCIAL_DATA` | Solicitante sin datos financieros |
| `InvalidCredentialsException` | security | 401 | `INVALID_CREDENTIALS` | Login fallido |
| `AccessDeniedException` | shared | 403 | `ACCESS_DENIED` | Rol insuficiente |
| `ResourceNotFoundException` | shared | 404 | `RESOURCE_NOT_FOUND` | Entidad no encontrada |
| `DuplicateApplicantException` | applicant | 409 | `DUPLICATE_RESOURCE` | Identificación ya registrada |
| `DuplicateUserException` | security | 409 | `DUPLICATE_USER` | Username/email ya registrado |
| `LastAdminException` | security | 409 | `LAST_ADMIN` | Intento de quitar el único admin |
| `EvaluationCooldownException` | evaluation | 409 | `EVALUATION_COOLDOWN` | Re-evaluación antes del cooldown |
| `CreditDecisionAlreadyExistsException` | creditdecision | 409 | `CREDIT_DECISION_ALREADY_EXISTS` | Ya existe decisión para esa evaluación |
| `Exception` (genérico) | shared | 500 | `INTERNAL_ERROR` | Error no esperado |

---

## 8. Base de Datos

El esquema se gestiona con **Flyway**. Migraciones en `src/main/resources/db/migration/`.

**Regla:** nunca modificar una migración ya aplicada. Siempre crear una nueva.

### Evolución del esquema (V1–V32)

| Migración | Cambios |
|-----------|---------|
| V1 | Esquema base e inicialización |
| V2 | Tabla `applicant` |
| V3 | Extensiones PostgreSQL (pgcrypto, uuid-ossp) |
| V4 | Tablas `app_user`, `role_permission` |
| V5 | Tablas `authentication_log`, `token_blacklist` |
| V6 | Tabla `financial_data` |
| V7 | Tablas `scoring_model`, `scoring_variable` base |
| V8 | Tablas `model_variable_mapping`, rangos y categorías |
| V9 | Tabla `knockout_rule` |
| V10 | Tablas `evaluation`, `evaluation_detail`, `evaluation_knockout` |
| V11 | Tabla `credit_decision` |
| V12 | Tabla `applicant_edit_audit` |
| V13 | Tabla `audit_log` |
| V14 | Stored procedures y vistas |
| V15 | Seed: usuario admin + matriz de permisos |
| V16 | Reemplaza rol AUDITOR → CREDIT_SUPERVISOR |
| V17 | Simplifica PK de `audit_log`; agrega `created_by`/`updated_by` a `token_blacklist` |
| V18 | Columna `phone` en `applicant`; permiso `ANALYST APPLICANT UPDATE` |
| V19 | Columnas `defaults_last_12m`, `defaults_last_24m`, `external_bureau_score`, `active_credit_products` en `financial_data`; `email`, `address` en `applicant` |
| V20 | Columna `result` en `audit_log` |
| V21 | Hace `entity_id` nullable en `audit_log` (para fallos de login) |
| V22 | Columna `peso` en `scoring_variable` (BigDecimal 0.01–1.00) |
| V23 | Actualiza constraint de `scoring_model.status` a DRAFT/ACTIVE/INACTIVE |
| V24 | Columna `model_id` FK en `knockout_rule` (reglas scoped por modelo) |
| V25 | Tabla `simulation_scenario` con campo JSONB `valores_variables` |
| V26 | Agrega `REJECTED` al constraint de `evaluation.risk_level` |
| V27 | Agrega `ESCALATED` al constraint de `credit_decision.decision` |
| V28 | Columnas `supervisor_id` y `resolution_deadline_at` en `credit_decision` |
| V29 | Corrige usuarios de demo (seed actualizado) |
| V30 | Índice de clasificación de riesgo en `evaluation` |
| V31 | Índices de búsqueda en `evaluation` (filtros de HU-010) |
| V32 | Índices de analytics para reportes HU-015/016/017 |

### Convenciones de la BD

- Todas las PKs son **UUID** (no auto-increment)
- Toda tabla tiene `created_at`, `created_by` (no actualizables)
- Tablas mutables tienen además `updated_at`, `updated_by`
- Los datos sensibles nunca se almacenan en plano:
  - Identificaciones: `identification_encrypted` (AES-256-GCM + IV, base64)
  - Detección de duplicados via `identification_hash` (HMAC-SHA256, UNIQUE)
  - Contraseñas: BCrypt en `password_hash`

### Tabla `applicant`

```sql
id                      UUID PRIMARY KEY
name                    VARCHAR(150) NOT NULL
identification_encrypted VARCHAR(700) NOT NULL   -- AES-GCM + IV, base64
identification_hash     VARCHAR(128) NOT NULL UNIQUE  -- HMAC-SHA256, base64
birth_date              DATE NOT NULL
employment_type         VARCHAR(30) NOT NULL
monthly_income          NUMERIC(19,2) NOT NULL
work_experience_months  INTEGER NOT NULL
phone                   VARCHAR(20) NULL
email                   VARCHAR(255) NULL
address                 VARCHAR(500) NULL
created_at              TIMESTAMP WITH TIME ZONE NOT NULL
created_by              VARCHAR(100) NOT NULL
updated_at              TIMESTAMP WITH TIME ZONE
updated_by              VARCHAR(100)
```

### Tabla `app_user`

```sql
id                      UUID PRIMARY KEY
username                VARCHAR(50) NOT NULL UNIQUE
email                   VARCHAR(255) NOT NULL UNIQUE
password_hash           VARCHAR(255) NOT NULL    -- BCrypt
role                    VARCHAR(30) NOT NULL     -- ADMIN | ANALYST | RISK_MANAGER | CREDIT_SUPERVISOR
enabled                 BOOLEAN NOT NULL DEFAULT true
account_locked          BOOLEAN NOT NULL DEFAULT false
failed_login_attempts   INTEGER NOT NULL DEFAULT 0
password_changed_at     TIMESTAMP WITH TIME ZONE NOT NULL
created_at              TIMESTAMP WITH TIME ZONE NOT NULL
created_by              VARCHAR(100) NOT NULL
```

### Tabla `token_blacklist`

```sql
id          UUID PRIMARY KEY
jti         VARCHAR(36) NOT NULL UNIQUE  -- JWT ID revocado
user_id     UUID NOT NULL
reason      VARCHAR(50) NOT NULL
revoked_at  TIMESTAMP WITH TIME ZONE NOT NULL
expires_at  TIMESTAMP WITH TIME ZONE NOT NULL
```

---

## 9. Seguridad y Autenticación

### Flujo de autenticación

```
1. POST /api/v1/auth/login  { "username": "ana", "password": "..." }
        ↓
2. AuthController → AuthenticateService
        ↓
3. Spring Security valida credenciales (DaoAuthenticationProvider + BCrypt)
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
| `ADMIN` | Acceso total. Único que puede crear usuarios y cambiar roles. |
| `ANALYST` | Registra solicitantes, captura datos financieros, crea evaluaciones. |
| `RISK_MANAGER` | Gestiona modelos de scoring, registra decisiones, accede a reportes de distribución y efectividad. |
| `CREDIT_SUPERVISOR` | Supervisa evaluaciones, accede al reporte de actividad de analistas. |

### Protecciones especiales

- **Último admin:** si solo hay un `ADMIN`, el sistema rechaza cambiarle el rol (`LastAdminException`).
- **Token blacklist:** al revocar acceso, todos los tokens del usuario quedan inválidos inmediatamente.
- **Cuenta bloqueada:** si `account_locked = true`, el JWT es rechazado aunque sea válido.
- **Encriptación de PII:** identificaciones almacenadas con AES-256-GCM; búsqueda por hash HMAC sin exponer el dato.

### Variables de entorno requeridas en producción

```bash
APP_JWT_SECRET=<base64 de al menos 32 bytes aleatorios>
APP_CRYPTO_ENCRYPTION_KEY_BASE64=<AES-256 key en base64, exactamente 32 bytes>
APP_CRYPTO_HASH_KEY_BASE64=<HMAC key en base64, exactamente 32 bytes>
CORS_ALLOWED_ORIGINS=https://app.creditscoring.example.com
```

---

## 10. Subsistema de Reportes (HU-015 / HU-016 / HU-017)

Los tres reportes analíticos están implementados en el bounded context `reporting` con arquitectura hexagonal completa (dominio → application → infrastructure).

### Diseño común

- **Persistencia:** los datos de cada reporte se obtienen con queries JPQL o JPA nativas de agregación sobre las tablas `evaluation`, `credit_decision` y `app_user`. No hay tablas de reporte propias.
- **Formatos:** cada reporte puede solicitarse en JSON (respuesta directa), PDF (OpenPDF, adjunto descargable) o CSV donde aplica (streaming via `StreamingResponseBody`).
- **Filtros:** todos los reportes aceptan filtros de rango de fechas (`desde`, `hasta`); algunos aceptan filtros adicionales (tipo de empleo, ID de analista).

### HU-015 — Distribución de Riesgo

Distribución del total de evaluaciones por nivel de riesgo (`VERY_LOW`, `LOW`, `MEDIUM`, `HIGH`, `VERY_HIGH`, `REJECTED`) para un período y tipo de empleo dados.

- **Query:** agregación `GROUP BY risk_level` sobre `evaluation`.
- **PDF:** tabla con nivel de riesgo, cantidad y porcentaje; gráfico de barras textual.
- **Rol:** ADMIN, RISK_MANAGER.

### HU-016 — Efectividad del Modelo

Analiza la concordancia entre la clasificación automática del modelo y la decisión final del analista.

- **Matriz de confusión 5×4:** filas = nivel de riesgo (excluyendo REJECTED), columnas = decisión (APPROVED / REJECTED / MANUAL_REVIEW / ESCALATED).
- **Tasa de concordancia:** porcentaje de casos donde la decisión fue congruente con el nivel de riesgo.
- **Tasa de override:** porcentaje de casos donde se aprobó una evaluación de alto riesgo o se rechazó una de bajo riesgo.
- **Query:** join entre `evaluation` y `credit_decision`, agregaciones por combinación riesgo × decisión.
- **Rol:** ADMIN, RISK_MANAGER.

### HU-017 — Actividad de Analistas

Métricas de productividad por analista para un período dado.

- **Métricas por analista:** total de evaluaciones, tiempo promedio de resolución, distribución de decisiones tomadas.
- **Cálculo de horas hábiles:** `BusinessHoursCalculator` — considera solo días hábiles (lunes a viernes), horario 08:00–18:00 zona horaria `America/Bogota` (configurable via `APP_REPORTING_BUSINESS_TIMEZONE`).
- **Detección de outliers:** `OutlierDetector` — marca analistas cuyo tiempo promedio de resolución se aleja más de ±2 desviaciones estándar de la media del grupo.
- **Exportación CSV:** streaming sin límite de filas.
- **Rol:** ADMIN, RISK_MANAGER, CREDIT_SUPERVISOR.

### Estructura del bounded context `reporting`

```
reporting/
├── domain/
│   ├── model/efectividad/ → ConfusionMatrix, ModelEffectivenessReport, ...
│   ├── model/analistas/   → AnalystMetrics, AnalystActivityReport, OutlierInfo, ...
│   └── port/in/efectividad/ + analistas/
├── application/
│   ├── service/efectividad/  → ModelEffectivenessService
│   ├── service/analistas/    → AnalystActivityService
│   └── util/                 → BusinessHoursCalculator, OutlierDetector
└── infrastructure/adapter/out/
    ├── persistence/efectividad/  ← queries JPA de agregación
    ├── persistence/analistas/    ← queries JPA de agregación
    ├── pdf/efectividad/          ← OpenPDF: tabla + métricas de concordancia
    ├── pdf/analistas/            ← OpenPDF: tabla por analista + outliers marcados
    └── csv/                      ← commons-csv streaming (HU-017)
```

---

## 11. Tests

### Estructura de tests

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
│   ├── domain/model/ApplicantTest.java
│   └── migration/V18MigrationTest.java
├── creditdecision/
│   ├── CreditDecisionIntegrationTest.java
│   ├── application/service/RegisterCreditDecisionServiceTest.java
│   └── domain/model/CreditDecisionTest.java
├── evaluation/
│   ├── integration/EvaluationIntegrationTest.java
│   ├── application/ExecuteEvaluationServiceTest.java
│   └── domain/ → EvaluationDetailTest, EvaluationKnockoutTest
├── financialdata/
│   └── application/service/FinancialDataCommandServiceTest.java
├── scoring/
│   └── ScoringVariableIntegrationTest.java
├── scoringengine/
│   └── ScoringSimulationIntegrationTest.java
├── scoringmodel/
│   └── ScoringModelIntegrationTest.java
└── shared/security/
    ├── acceptance/ → AuthLoginAT, PermissionMatrixAT
    ├── application/service/ → AuthenticateServiceTest, ChangeUserRoleServiceTest, GetAuditLogsServiceTest
    ├── infrastructure/jwt/ → JwtServiceTest, JwtAuthenticationFilterTest
    ├── infrastructure/rest/ → AuditLogControllerTest
    ├── integration/ → CreateUserIT, LastAdminProtectionIT, SecurityFilterChainIT, TokenBlacklistFlowIT
    └── migration/V16MigrationTest.java
```

### Tipos de test

| Tipo | Sufijo | Qué usa | Velocidad |
|------|--------|---------|-----------|
| Unit | `*Test` | Java puro, mocks | Muy rápido |
| Integration | `*IT` / `*IntegrationTest` | Spring context completo + Testcontainers | Lento (PostgreSQL real) |
| Acceptance | `*AT` | REST Assured + servidor Spring levantado | Lento |
| Architecture | — | ArchUnit en build time | Instantáneo |

### Perfil de test

Los tests corren con `@ActiveProfiles("test")`:
- Testcontainers JDBC driver levanta PostgreSQL automáticamente
- Flyway ejecuta todas las migraciones en cada context de test
- JWT secret y crypto keys hardcodeadas (solo para tests, no son secretos reales)

---

## 12. CI/CD

Pipeline en `.github/workflows/build-test.yml`, se ejecuta en cada push o PR a `main`.

### Pasos

| Paso | Detalle |
|------|---------|
| Checkout | `actions/checkout@v4` |
| Java 21 | Temurin JDK via `actions/setup-java@v4` |
| Cache Gradle | Hash de `*.gradle.kts` + `gradle-wrapper.properties` |
| Tests | `./gradlew clean test` — unit + integration + acceptance + ArchUnit |
| Cobertura | `./gradlew jacocoTestReport` (corre siempre, incluso si tests fallan) |
| SonarCloud | Análisis de calidad + cobertura (requiere `SONAR_TOKEN` en GitHub Secrets) |
| Artefactos | JaCoCo XML + resultados de tests, retención 7 días |

### Entorno de CI

Las siguientes variables están hardcodeadas en el workflow para el entorno de CI (no son claves de producción):

| Variable | Propósito |
|----------|-----------|
| `APP_JWT_SECRET` | Secret de desarrollo para firmar JWT en tests |
| `APP_CRYPTO_ENCRYPTION_KEY_BASE64` | Clave AES-256 de desarrollo |
| `APP_CRYPTO_HASH_KEY_BASE64` | Clave HMAC de desarrollo |
| `TESTCONTAINERS_RYUK_DISABLED=true` | Deshabilita limpieza de Testcontainers en CI |

### SonarCloud

Proyecto: `crediscan-backend` (organización `crediscan-udea-fabricaescuela`). Configurado para excluir reglas Oracle/PLSQL en migraciones Flyway (el proyecto usa PostgreSQL).

---

## 13. Configuración

### Perfiles

| Perfil | Cuándo se usa | Configurado en |
|--------|---------------|----------------|
| `dev` | Desarrollo local | `application-dev.yml` |
| `test` | Tests con Testcontainers | `application-test.yml` |
| `prod` | Producción (Render.com) | Variables de entorno |

### `application.yml` — propiedades clave

```yaml
spring:
  data:
    web:
      pageable:
        serialization-mode: via-dto   # Formato Spring Data 3.3+: metadata bajo clave "page"
  mvc:
    problemdetails:
      enabled: true                   # RFC 7807 Problem Details

app:
  evaluation:
    cooldown-hours: ${APP_EVALUATION_COOLDOWN_HOURS:24}
  reporting:
    business-timezone: ${APP_REPORTING_BUSINESS_TIMEZONE:America/Bogota}
  security:
    jwt:
      secret: ${APP_JWT_SECRET}
      expiration-ms: ${APP_JWT_EXPIRATION_MS:86400000}
    crypto:
      encryption-key-base64: ${APP_CRYPTO_ENCRYPTION_KEY_BASE64}
      hash-key-base64: ${APP_CRYPTO_HASH_KEY_BASE64}
```

### `application-dev.yml`

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/credit_scoring_dev}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:#{null}}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
```

### Beans de configuración

| Clase | Propósito |
|-------|-----------|
| `SecurityConfig` | Spring Security: filtro JWT, endpoints públicos, CSRF deshabilitado |
| `JwtProperties` | Bind de `app.security.jwt.*` |
| `CryptoProperties` | Bind de `app.security.crypto.*` |
| `JpaAuditingConfig` | Habilita `@CreatedDate`, `@CreatedBy`, `@LastModifiedDate`, `@LastModifiedBy` |
| `CorsConfig` | CORS leído de `CORS_ALLOWED_ORIGINS` |
| `OpenApiConfig` | Swagger con autenticación Bearer JWT |

---

## 14. Observabilidad

### Logs

Todos los logs salen en JSON (Logstash format). Cada request incluye un `traceId` en el MDC (inyectado por `MdcFilter`):

```json
{
  "@timestamp": "2026-05-22T10:30:45.123Z",
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

Endpoints de actuator expuestos: `health`, `info`, `prometheus`, `metrics`.

### Auditoría

- **JPA Auditing:** `created_at`, `created_by`, `updated_at`, `updated_by` en cada entidad via `AuditableEntity` (`@MappedSuperclass`).
- **audit_log:** tabla para auditoría de operaciones críticas (cambios de rol, login, etc.). Consultable vía `GET /api/v1/auditoria`.
- **authentication_log:** cada intento de login registrado.

---

## 15. Cómo implementar un nuevo módulo

Seguir este orden. No saltear pasos.

### Paso 1 — Modelo de dominio

En `<bounded-context>/domain/model/`, crear el modelo como `record` si es inmutable:

```java
public record NewEntity(UUID id, UUID applicantId, String someField) {
    public static NewEntity registerNew(UUID applicantId, String someField) {
        if (someField == null || someField.isBlank()) {
            throw new NewEntityValidationException("someField is required");
        }
        return new NewEntity(UUID.randomUUID(), applicantId, someField);
    }
}
```

### Paso 2 — Excepciones de dominio

En `<bounded-context>/domain/exception/`, extender `DomainException`:

```java
public class NewEntityValidationException extends DomainException {
    public NewEntityValidationException(String message) { super(message); }
    @Override public int httpStatusCode() { return 400; }
    @Override public String errorCode() { return "NEW_ENTITY_VALIDATION_FAILED"; }
}
```

No hay que tocar `GlobalExceptionHandler`.

### Paso 3 — Puertos

**Puerto IN** (`domain/port/in/`):
```java
public interface RegisterNewEntityUseCase {
    NewEntity register(RegisterNewEntityCommand command);
}
```

**Puertos OUT** (`domain/port/out/`):
```java
public interface NewEntityRepositoryPort {
    NewEntity save(NewEntity entity);
    Optional<NewEntity> findById(UUID id);
    PagedResult<NewEntity> findAll(PageRequest pageRequest);
}
```

### Paso 4 — Servicio de aplicación

```java
@Service
@Transactional  // ← acá, no en el adaptador
public class RegisterNewEntityService implements RegisterNewEntityUseCase {
    private final NewEntityRepositoryPort repository;
    // inyección por constructor
}
```

### Paso 5 — Adaptadores de infraestructura

**Adaptador OUT (persistencia):** `@Component`, sin `@Transactional`. Convierte entre el modelo de dominio y la JPA entity. Convierte `PageRequest`/`PagedResult` ↔ `Pageable`/`Page`.

**Adaptador IN (REST):** `@RestController`, `@RequestMapping("/api/v1/new-entities")`, con `@PreAuthorize` en cada operación.

### Paso 6 — Tests

Mínimo esperado:
- Unit test del servicio (mockear puertos OUT con Mockito)
- Integration test con Testcontainers: `@SpringBootTest`, `@ActiveProfiles("test")`, `@Testcontainers` con `PostgreSQLContainer`

### Paso 7 — Migración Flyway

Si se necesita tabla nueva: `V{siguiente_número}__descripcion_breve.sql` en `src/main/resources/db/migration/`.

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
- [ ] Excepciones de dominio extienden `DomainException` con `httpStatusCode()` y `errorCode()`
- [ ] Puertos IN y OUT como interfaces en `domain/port/`
- [ ] Paginación usando `PagedResult` / `PageRequest` (no Spring Data)
- [ ] `@Transactional` en el servicio de aplicación, no en los adaptadores
- [ ] Adaptadores en `infrastructure/adapter/in` y `out`
- [ ] Controller con `@PreAuthorize` con los roles correctos
- [ ] Unit test del servicio
- [ ] Integration test con Testcontainers
- [ ] Migración Flyway si hay cambios de esquema
- [ ] `./gradlew test` pasa (incluyendo ArchUnit)

---

## 16. ADRs — Decisiones de Arquitectura

### ADR-001: Arquitectura Hexagonal con módulos por Bounded Context (Screaming Architecture)

**Contexto:** El sistema tiene múltiples dominios de negocio independientes (solicitantes, scoring, evaluación, reportes, etc.) con reglas de negocio propias.

**Decisión:** Se adopta Arquitectura Hexagonal (Ports & Adapters) organizada por Bounded Context. Cada contexto tiene sus propias capas `domain`, `application` e `infrastructure`. No hay capas horizontales compartidas (no existe un único `repository/` o `service/` global).

**Consecuencias:**
- El dominio es independiente de frameworks y puede testearse sin Spring.
- Agregar un nuevo bounded context no requiere tocar los existentes.
- Los adaptadores pueden reemplazarse sin modificar el dominio ni la aplicación.
- Costo: más archivos y estructura que una arquitectura en capas tradicional.

---

### ADR-002: DomainException como clase abstracta para manejo de errores (OCP)

**Contexto:** El `GlobalExceptionHandler` necesita manejar excepciones de todos los módulos sin importarlos directamente.

**Decisión:** `DomainException` abstracta con métodos `httpStatusCode()` y `errorCode()`. El handler tiene un único `@ExceptionHandler(DomainException.class)` que delega en polimorfismo.

**Consecuencias:**
- Agregar un módulo nuevo con nuevas excepciones no requiere modificar el handler.
- El contrato de error vive en la excepción misma, cerca de donde se lanza.
- Las excepciones de dominio son Java puro, sin dependencias de Spring.

---

### ADR-003: PagedResult / PageRequest para desacoplar el dominio de Spring Data

**Contexto:** Los puertos de dominio originalmente usaban `org.springframework.data.domain.Page` y `Pageable`, violando la independencia del dominio.

**Decisión:** Records Java puros `PagedResult<T>` y `PageRequest` en `shared`. Los adaptadores hacen la conversión.

**Consecuencias:**
- El dominio no importa ninguna clase de Spring Data.
- Cambiar el mecanismo de acceso a datos no requiere modificar los puertos.
- Costo mínimo: conversión en cada adaptador (pocas líneas).

---

### ADR-004: @Transactional solo en servicios de aplicación

**Contexto:** Adaptadores con `@Transactional` generaban transacciones anidadas y dificultaban razonar sobre los límites transaccionales.

**Decisión:** `@Transactional` exclusivamente en servicios de aplicación. Los adaptadores son stateless respecto a transacciones.

**Consecuencias:**
- Un único límite transaccional por caso de uso.
- El rollback ocurre a nivel del caso de uso completo, no por operación individual.
- Los adaptadores no pueden confirmar parcialmente.

---

### ADR-005: Queries JPA nativas/JPQL de agregación para reportes analíticos

**Contexto:** Los reportes HU-015, HU-016 y HU-017 requieren agregaciones complejas sobre múltiples tablas con filtros dinámicos.

**Decisión:** Se usan queries JPQL o JPA nativas directamente en los repositorios de `reporting/infrastructure/adapter/out/persistence/`. No hay tablas de reporte propias ni materializadas. Los índices V30–V32 garantizan performance aceptable en el volumen esperado.

**Consecuencias:**
- Sin overhead de sincronización de datos.
- Reportes siempre sobre datos actuales (no lag).
- Costo: las queries son más complejas y requieren índices explícitos.

---

### ADR-006: BusinessHoursCalculator y OutlierDetector como componentes de dominio de aplicación

**Contexto:** HU-017 requiere calcular horas hábiles (L-V 08:00–18:00 zona horaria configurable) y detectar analistas outliers (±2σ).

**Decisión:** `BusinessHoursCalculator` y `OutlierDetector` viven en `reporting/application/util/`. Son Java puro, sin dependencias de infraestructura, con la zona horaria leída desde configuración (`APP_REPORTING_BUSINESS_TIMEZONE`, default `America/Bogota`).

**Consecuencias:**
- Testables de forma unitaria sin Spring context.
- La zona horaria es configurable por entorno sin recompilar.
- La detección de outliers ±2σ es determinista y documentada.

---

*Última actualización: Mayo 2026*
