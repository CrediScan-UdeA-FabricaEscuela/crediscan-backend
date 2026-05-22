# Credit Scoring Engine

API REST para evaluación de solicitudes de crédito. Construida con Java 21 + Spring Boot 3.4.4, arquitectura hexagonal (Screaming Architecture) y PostgreSQL 16.

Proyecto académico — UdeA Fábrica Escuela / CrediScan.

---

## Estado del proyecto

Todos los sprints completados. Las 17 historias de usuario están implementadas.

| Sprint | Descripción | Estado |
|--------|-------------|--------|
| **Sprint 1** | Autenticación, RBAC y gestión de solicitantes | ✅ Completado |
| **Sprint 2** | Variables de scoring, modelo de scoring, reglas KO, motor de cálculo y evaluaciones | ✅ Completado |
| **Sprint 3** | Clasificación de riesgo, decisiones crediticias, simulación y reportes analíticos | ✅ Completado |

---

## Stack principal

| Tecnología | Versión | Para qué se usa |
|------------|---------|-----------------|
| Java | 21 | Lenguaje principal |
| Spring Boot | 3.4.4 | Framework base |
| Spring Data JPA / Hibernate 6 | — | Acceso a base de datos |
| PostgreSQL | 16 | Base de datos |
| Flyway | — | Migraciones (V1–V32) |
| JWT (jjwt) | 0.12.6 | Autenticación stateless |
| MapStruct | 1.6.3 | Mapeo de objetos |
| Lombok | — | Reducción de boilerplate |
| OpenPDF | 2.0.3 | Generación de reportes PDF |
| Apache Commons CSV | 1.10.0 | Exportación CSV streaming |
| Caffeine Cache | 3.2.0 | Cache en memoria |
| Micrometer + Prometheus | — | Métricas |
| Logstash Logback Encoder | 8.0 | Logs estructurados JSON |
| SpringDoc OpenAPI | 2.8.4 | Swagger UI |
| JUnit 5 | — | Tests unitarios |
| Testcontainers | 1.20.4 | Tests de integración |
| Cucumber | 7.20.1 | Tests de aceptación |
| REST Assured | 5.5.0 | Tests de API |
| ArchUnit | 1.3.0 | Tests de arquitectura |
| SonarCloud | — | Análisis de calidad |
| JaCoCo | 0.8.12 | Cobertura de código |

---

## 1. Descripción del proyecto

**Credit Scoring Engine** es una API REST para evaluación de solicitudes de crédito de personas naturales. Contempla el registro de solicitantes, captura de datos financieros con versionado, scoring automatizado mediante modelos configurables, evaluación crediticia completa, registro de decisiones finales y generación de reportes analíticos (distribución de riesgo, efectividad del modelo y actividad por analista).

El proyecto usa **Arquitectura Hexagonal** (Ports & Adapters) organizada por **Bounded Context** (Screaming Architecture). Cada contexto contiene sus propias capas `domain`, `application` e `infrastructure`. El dominio de negocio no depende de ningún framework — las reglas de dependencia se verifican en build time con ArchUnit.

---

## 2. Requisitos previos

- **Docker Desktop** con WSL2 integration activada (para la base de datos local)
- **JDK 21** (Temurin recomendado) para correr la app directamente con Gradle
- Variables de entorno configuradas (ver sección 4)

---

## 3. Levantar la base de datos (Docker)

```bash
# Levantar PostgreSQL 16 en segundo plano
docker run -d \
  --name credit-scoring-db \
  -e POSTGRES_DB=credit_scoring \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16-alpine
```

O si el proyecto tiene `docker-compose.yml`:

```bash
docker compose up -d
```

---

## 4. Variables de entorno

Estas variables son necesarias para levantar la app en modo `dev`:

| Variable | Valor de desarrollo | Descripción |
|----------|---------------------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/credit_scoring` | URL de conexión a PostgreSQL |
| `DB_USERNAME` | `postgres` | Usuario de la base de datos |
| `DB_PASSWORD` | `postgres` | Contraseña de la base de datos |
| `APP_JWT_SECRET` | *(ver abajo)* | Clave base64 para firmar JWT (≥32 bytes) |
| `APP_CRYPTO_ENCRYPTION_KEY_BASE64` | *(ver abajo)* | Clave AES-256 para encriptar datos sensibles |
| `APP_CRYPTO_HASH_KEY_BASE64` | *(ver abajo)* | Clave HMAC-SHA256 para hash de identificadores |
| `APP_JWT_EXPIRATION_MS` | `86400000` | Expiración del JWT en ms (24h por defecto) |
| `APP_EVALUATION_COOLDOWN_HOURS` | `24` | Horas de cooldown entre evaluaciones del mismo solicitante |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,...` | Orígenes permitidos por CORS |

Valores de ejemplo para desarrollo local (no usar en producción):

```bash
export DB_URL=jdbc:postgresql://localhost:5432/credit_scoring
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export APP_JWT_SECRET=dGVzdFNlY3JldEtleUZvckp3dDAxMjM0NTY3ODlBQkNERUY=
export APP_CRYPTO_ENCRYPTION_KEY_BASE64=MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=
export APP_CRYPTO_HASH_KEY_BASE64=RkVEQ0JBOTg3NjU0MzIxMEZFRENCQTk4NzY1NDMyMTA=
```

---

## 5. Correr la app en modo desarrollo

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

La app queda disponible en `http://localhost:8080`.

Flyway ejecuta las migraciones automáticamente al iniciar. Si la base de datos está vacía, las crea todas desde V1 hasta V32, incluyendo el seed con los usuarios iniciales.

---

## 6. Verificar que está corriendo

```bash
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```

---

## 7. Explorar la API

Swagger UI: **http://localhost:8080/swagger-ui.html**

OpenAPI spec (JSON): **http://localhost:8080/api-docs**

### Autenticarse

```
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

La respuesta incluye un `token`. Usarlo como `Bearer <token>` en el header `Authorization` de los requests siguientes.

### Credenciales por defecto (seed V15 + V29)

| Usuario | Contraseña | Rol |
|---------|------------|-----|
| `admin` | `admin123` | ADMIN |
| `analista1` | `pass1234` | ANALYST |
| `riskmanager1` | `pass1234` | RISK_MANAGER |

Para crear usuarios adicionales, usar `POST /api/v1/auth/usuarios` autenticado como ADMIN.

---

## 8. Endpoints principales

### Públicos (sin token)

| Método | Path | Descripción |
|--------|------|-------------|
| POST | `/api/v1/auth/login` | Login → JWT |
| GET | `/actuator/health` | Health check |
| GET | `/swagger-ui.html` | Swagger UI |
| GET | `/api-docs` | OpenAPI spec |

### Solicitantes

| Método | Path | Roles | Descripción |
|--------|------|-------|-------------|
| POST | `/api/v1/solicitantes` | ANALYST, ADMIN | Registrar solicitante |
| GET | `/api/v1/solicitantes` | Todos | Listar / buscar con filtros + paginación |
| PATCH | `/api/v1/solicitantes/{id}` | ANALYST, ADMIN | Editar solicitante |
| GET | `/api/v1/solicitantes/export` | Todos | Exportar CSV |

### Datos financieros

| Método | Path | Roles | Descripción |
|--------|------|-------|-------------|
| POST | `/api/v1/solicitantes/{id}/datos-financieros` | ANALYST, ADMIN | Crear versión de datos financieros |
| PUT | `/api/v1/solicitantes/{id}/datos-financieros/{version}` | ANALYST, ADMIN | Actualizar versión específica |
| GET | `/api/v1/solicitantes/{id}/datos-financieros` | Todos | Listar versiones / comparar |

### Variables de scoring

| Método | Path | Roles | Descripción |
|--------|------|-------|-------------|
| POST | `/api/v1/variables-scoring` | ADMIN, RISK_MANAGER | Crear variable |
| PUT | `/api/v1/variables-scoring/{id}` | ADMIN, RISK_MANAGER | Actualizar variable |
| GET | `/api/v1/variables-scoring` | Todos | Listar variables |

### Modelos de scoring

| Método | Path | Roles | Descripción |
|--------|------|-------|-------------|
| POST | `/api/v1/modelos-scoring` | ADMIN, RISK_MANAGER | Crear modelo |
| GET | `/api/v1/modelos-scoring` | Todos | Listar modelos |
| GET | `/api/v1/modelos-scoring/{id}` | Todos | Ver modelo |
| PUT | `/api/v1/modelos-scoring/{id}/activar` | ADMIN, RISK_MANAGER | Activar modelo |
| GET | `/api/v1/modelos-scoring/comparar` | Todos | Comparar modelos |
| POST/PUT/DELETE | `/api/v1/modelos-scoring/{id}/reglas-knockout` | ADMIN, RISK_MANAGER | CRUD de reglas KO |

### Motor de scoring y simulación

| Método | Path | Roles | Descripción |
|--------|------|-------|-------------|
| POST | `/api/v1/scoring/calcular` | ADMIN, ANALYST, RISK_MANAGER | Calcular scoring |
| POST | `/api/v1/scoring/simular` | Autenticado | Simulación sin persistencia |
| POST | `/api/v1/scoring/simulaciones` | Autenticado | Crear escenario guardado |
| GET | `/api/v1/scoring/simulaciones` | Autenticado | Listar escenarios |
| POST | `/api/v1/scoring/simulaciones/{id}/ejecutar` | Autenticado | Ejecutar escenario guardado |

### Evaluaciones

| Método | Path | Roles | Descripción |
|--------|------|-------|-------------|
| POST | `/api/v1/evaluaciones` | ANALYST, ADMIN | Ejecutar evaluación crediticia |
| GET | `/api/v1/evaluaciones` | Todos | Buscar evaluaciones con filtros + paginación |
| GET | `/api/v1/evaluaciones/{id}` | Todos | Ver detalle de evaluación |
| GET | `/api/v1/evaluaciones/{id}/pdf` | Todos | Exportar evaluación a PDF |
| GET | `/api/v1/evaluaciones/export` | Todos | Exportar CSV streaming (cap PDF 1000 filas) |
| GET | `/api/v1/evaluaciones/estadisticas` | Todos | Estadísticas de evaluaciones |

### Decisiones crediticias

| Método | Path | Roles | Descripción |
|--------|------|-------|-------------|
| POST | `/api/v1/evaluaciones/{id}/decision` | RISK_MANAGER, ADMIN | Registrar decisión |
| GET | `/api/v1/evaluaciones/{id}/decision` | Todos | Ver decisión |

### Reportes analíticos

| Método | Path | Roles | Descripción |
|--------|------|-------|-------------|
| GET | `/api/v1/reportes/distribucion-riesgo` | ADMIN, RISK_MANAGER | HU-015: distribución de riesgo (JSON / PDF) |
| GET | `/api/v1/reportes/efectividad-modelo` | ADMIN, RISK_MANAGER | HU-016: efectividad del modelo — matriz de confusión (JSON / PDF) |
| GET | `/api/v1/reportes/actividad-analistas` | ADMIN, RISK_MANAGER, CREDIT_SUPERVISOR | HU-017: actividad por analista (JSON / PDF / CSV) |

### Auditoría y usuarios

| Método | Path | Roles | Descripción |
|--------|------|-------|-------------|
| GET | `/api/v1/auditoria` | ADMIN, RISK_MANAGER, CREDIT_SUPERVISOR | Logs de auditoría |
| GET | `/api/v1/auditoria/export` | ADMIN, RISK_MANAGER, CREDIT_SUPERVISOR | Exportar logs a CSV |
| POST | `/api/v1/auth/usuarios` | ADMIN | Crear usuario |
| PATCH | `/api/v1/auth/usuarios/{id}/rol` | ADMIN | Cambiar rol |
| GET | `/api/v1/roles/permisos` | ADMIN | Matriz de permisos |

---

## 9. Roles y permisos

| Rol | Descripción |
|-----|-------------|
| `ADMIN` | Acceso total. Único que puede gestionar usuarios y cambiar roles. |
| `ANALYST` | Registra solicitantes, captura datos financieros, ejecuta evaluaciones. |
| `RISK_MANAGER` | Gestiona variables y modelos de scoring, registra decisiones crediticias, accede a reportes de distribución y efectividad. |
| `CREDIT_SUPERVISOR` | Supervisa evaluaciones y ve el reporte de actividad de analistas. |

---

## 10. Funcionalidades implementadas (resumen HU)

| HU | Descripción | Módulo |
|----|-------------|--------|
| HU-001 | Registro de solicitantes con validaciones de dominio y encriptación de identificación | `applicant` |
| HU-002 | Listado, filtros (nombre, ID, ingresos, tipo empleo, experiencia, fecha) y exportación CSV | `applicant` |
| HU-003 | Datos financieros con versionado, actualización y comparación entre versiones | `financialdata` |
| HU-004 | CRUD de variables de scoring (NUMERIC / CATEGORICAL), validación de pesos | `scoring` |
| HU-005 | Ciclo de vida de modelos (DRAFT → ACTIVE → INACTIVE), activación requiere ≥3 variables | `scoringmodel` |
| HU-006 | Reglas de knockout por modelo (6 operadores, evaluación por prioridad) | `scoringmodel` |
| HU-007 | Motor de cálculo de scoring: puntuación ponderada + evaluación KO | `scoringengine` |
| HU-008 | Comparación de modelos (activo vs. históricos) | `scoringmodel` |
| HU-009 | Registro de decisiones crediticias por RISK_MANAGER (observaciones mínimo 20 char) | `creditdecision` |
| HU-010 | Búsqueda de evaluaciones con filtros, paginación, exportación CSV streaming y PDF (cap 1000 filas) | `evaluation` |
| HU-011 | Detalle de evaluación + exportación PDF individual | `evaluation` |
| HU-012 | Estadísticas de evaluaciones por período | `evaluation` |
| HU-013 | Clasificación automática de riesgo (VERY_LOW / LOW / MEDIUM / HIGH / VERY_HIGH / REJECTED) | `evaluation` |
| HU-014 | Simulación de scoring y escenarios guardados | `scoringengine` |
| HU-015 | Reporte de distribución de riesgo por tipo de empleo y rango de fechas (JSON + PDF) | `reporting` |
| HU-016 | Reporte de efectividad del modelo: matriz de confusión 5×4, tasa de concordancia, tasas de override (JSON + PDF) | `reporting` |
| HU-017 | Reporte de actividad de analistas: métricas por analista, horas hábiles (L-V 08:00-18:00 Bogotá), detección de outliers ±2σ (JSON + PDF + CSV) | `reporting` |

---

## 11. Correr los tests

```bash
# Todos los tests (unit + integration + acceptance + architecture)
./gradlew test

# Un test específico
./gradlew test --tests "co.udea.codefactory.creditscoring.applicant.ApplicantRegistrationIntegrationTest"

# Con logs visibles
./gradlew test --info

# Generar reporte de cobertura
./gradlew jacocoTestReport
# Reporte en: build/reports/jacoco/test/jacocoTestReport.xml
```

**Requisito:** Docker debe estar corriendo. Los tests de integración usan Testcontainers para levantar PostgreSQL automáticamente.

Tipos de tests:
- **Unit** (`*Test.java`): prueban una clase en aislamiento, sin Spring ni BD.
- **Integration** (`*IT.java` / `*IntegrationTest.java`): Spring context completo con Testcontainers (PostgreSQL real).
- **Acceptance** (`*AT.java`): REST Assured contra un servidor Spring levantado.
- **Architecture** (ArchUnit): verifican reglas de dependencia en build time.

---

## 12. CI/CD

Pipeline en `.github/workflows/build-test.yml`, se ejecuta en cada push o PR a `main`.

| Paso | Qué hace |
|------|----------|
| Checkout | `actions/checkout@v4` |
| Java 21 | Temurin JDK 21 via `actions/setup-java@v4` |
| Cache Gradle | Cachea `~/.gradle/caches` y `~/.gradle/wrapper` |
| Tests | `./gradlew clean test` — incluye ArchUnit, unit, integration, acceptance |
| Cobertura | `./gradlew jacocoTestReport` (corre siempre) |
| SonarCloud | Análisis de calidad y cobertura (requiere `SONAR_TOKEN` en GitHub Secrets) |
| Artefactos | Sube reporte JaCoCo y resultados de tests (retención 7 días) |

Un PR que rompa cualquier test (incluyendo ArchUnit) no puede mergearse si el repositorio tiene protección de ramas activa.

---

## 13. Variables de configuración completas

| Variable | Default | Descripción |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/credit_scoring_dev` | URL de conexión (perfil dev) |
| `DB_USERNAME` | `postgres` | Usuario de PostgreSQL |
| `DB_PASSWORD` | — | Contraseña de PostgreSQL |
| `APP_JWT_SECRET` | *(requerido)* | Clave base64 ≥32 bytes para firmar JWT |
| `APP_JWT_EXPIRATION_MS` | `86400000` | Expiración del JWT en ms (24h) |
| `APP_CRYPTO_ENCRYPTION_KEY_BASE64` | *(requerido)* | Clave AES-256 base64 para encriptar datos sensibles |
| `APP_CRYPTO_HASH_KEY_BASE64` | *(requerido)* | Clave HMAC-SHA256 base64 para hash de identificadores |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:4200,http://localhost:5173` | Orígenes CORS permitidos |
| `APP_EVALUATION_COOLDOWN_HOURS` | `24` | Horas de cooldown entre evaluaciones del mismo solicitante |
| `APP_REPORTING_BUSINESS_TIMEZONE` | `America/Bogota` | Zona horaria para cálculo de horas hábiles en reportes |

---

## 14. Despliegue (Render.com)

La app está desplegada en [Render.com](https://render.com) usando Docker con el perfil `prod`.

Variables requeridas en producción:

```bash
SPRING_PROFILES_ACTIVE=prod
PORT=<asignado por Render>
DB_URL=<URL PostgreSQL de Render>
DB_USERNAME=<usuario>
DB_PASSWORD=<contraseña>
APP_JWT_SECRET=<clave base64 segura>
APP_CRYPTO_ENCRYPTION_KEY_BASE64=<AES-256 key, 32 bytes en base64>
APP_CRYPTO_HASH_KEY_BASE64=<HMAC key, 32 bytes en base64>
CORS_ALLOWED_ORIGINS=https://<dominio-del-frontend>
```

---

## 15. Documentación técnica

- **[ARCHITECTURE.md](ARCHITECTURE.md)** — arquitectura hexagonal, mapa de bounded contexts, reglas de dependencia, base de datos, seguridad, subsistema de reportes y ADRs.
