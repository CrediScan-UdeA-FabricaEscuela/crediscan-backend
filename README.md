# Credit Scoring Engine

API REST para evaluación de solicitudes de crédito. Construida con Java 21 + Spring Boot 3.4.4, arquitectura hexagonal y PostgreSQL.

---

## Estado del proyecto

| Sprint | Descripción | Estado |
|--------|-------------|--------|
| **Sprint 1** | Autenticación, RBAC y gestión de solicitantes | ✅ Completado |
| **Sprint 2** | Variables de scoring, modelo de scoring, reglas de cálculo y evaluaciones | ✅ Completado |
| **Sprint 3** | Clasificación de riesgo, historial, decisiones y reportes analíticos | 🔄 En progreso |

---

## Stack principal

> **Tecnología:** Java 21 · Spring Boot 3.4.4 · Arquitectura Hexagonal · PostgreSQL 16

---

## 1. Descripción del proyecto

**Credit Scoring Engine** es una API REST para evaluación de solicitudes de crédito de personas naturales. Contempla el registro de solicitantes, captura de datos financieros, scoring automatizado, evaluación crediticia, decisión final y generación de reportes.

---

## 2. Requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) con WSL2 integration activada

Eso es todo. No necesitás Java ni PostgreSQL instalados localmente.

---

## 3. Levantar la app

```bash
# 1. Clonar el repositorio
git clone <url-del-repo>
cd <nombre-del-repo>

# 2. Configurar variables de entorno (los defaults ya funcionan para desarrollo)
cp .env.example .env

# 3. Levantar app + base de datos
docker compose up --build -d
```

La app queda disponible en `http://localhost:8080`.

---

## 4. Verificar que está corriendo

```bash
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```

---

## 5. Explorar la API

Swagger UI: **http://localhost:8080/swagger-ui.html**

### Autenticarse

```
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password"
}
```

La respuesta incluye un `token`. Usarlo como `Bearer <token>` en el header `Authorization` de los siguientes requests.

### Credenciales por defecto

| Usuario | Contraseña | Rol |
|---------|------------|-----|
| `admin` | `password` | ADMIN |

Para crear usuarios con otros roles (ANALYST, RISK_MANAGER, CREDIT_SUPERVISOR), usar `POST /api/v1/auth/usuarios` autenticado como admin.

---

## 6. Endpoints principales

### Públicos (sin token)

| Método | Path | Descripción |
|--------|------|-------------|
| POST | `/api/v1/auth/login` | Login → JWT |
| GET | `/actuator/health` | Health check |

### Protegidos

**Solicitantes**

| Método | Path | Roles | Descripción |
|--------|------|-------|-------------|
| POST | `/api/v1/solicitantes` | ANALYST, ADMIN | Registrar solicitante |
| GET | `/api/v1/solicitantes?q=...` | ANALYST, RISK_MANAGER, ADMIN, CREDIT_SUPERVISOR | Buscar solicitantes |
| PATCH | `/api/v1/solicitantes/{id}` | ANALYST, ADMIN | Editar solicitante |

**Datos financieros**

| Método | Path | Roles | Descripción |
|--------|------|-------|-------------|
| POST | `/api/v1/solicitantes/{id}/datos-financieros` | ANALYST, ADMIN | Crear datos financieros |
| PUT | `/api/v1/solicitantes/{id}/datos-financieros/{version}` | ANALYST, ADMIN | Actualizar versión específica |

**Variables de scoring**

| Método | Path | Roles | Descripción |
|--------|------|-------|-------------|
| POST | `/api/v1/variables-scoring` | ADMIN, RISK_MANAGER | Crear variable |
| PUT | `/api/v1/variables-scoring/{id}` | ADMIN, RISK_MANAGER | Actualizar variable |
| GET | `/api/v1/variables-scoring` | ADMIN, RISK_MANAGER, ANALYST, CREDIT_SUPERVISOR | Listar variables |

**Modelos de scoring**

| Método | Path | Roles | Descripción |
|--------|------|-------|-------------|
| POST | `/api/v1/modelos-scoring` | ADMIN, RISK_MANAGER | Crear modelo |
| GET | `/api/v1/modelos-scoring` | ADMIN, RISK_MANAGER, ANALYST, CREDIT_SUPERVISOR | Listar modelos |
| PUT | `/api/v1/modelos-scoring/{id}/activar` | ADMIN, RISK_MANAGER | Activar modelo |
| GET | `/api/v1/modelos-scoring/comparar` | ADMIN, RISK_MANAGER, ANALYST, CREDIT_SUPERVISOR | Comparar modelos |

**Motor de scoring y simulación**

| Método | Path | Roles | Descripción |
|--------|------|-------|-------------|
| POST | `/api/v1/scoring/calcular` | ADMIN, ANALYST, RISK_MANAGER | Calcular scoring |
| POST | `/api/v1/scoring/simular` | Autenticado | Simular scoring |
| POST | `/api/v1/scoring/simulaciones` | Autenticado | Crear escenario de simulación |

**Evaluaciones**

| Método | Path | Roles | Descripción |
|--------|------|-------|-------------|
| POST | `/api/v1/evaluaciones` | ANALYST, ADMIN | Ejecutar evaluación |
| GET | `/api/v1/evaluaciones/{id}` | ANALYST, RISK_MANAGER, ADMIN, CREDIT_SUPERVISOR | Ver evaluación |
| GET | `/api/v1/evaluaciones/{id}/pdf` | ANALYST, RISK_MANAGER, ADMIN, CREDIT_SUPERVISOR | Generar reporte PDF |

**Decisiones crediticias**

| Método | Path | Roles | Descripción |
|--------|------|-------|-------------|
| POST | `/api/v1/evaluaciones/{id}/decision` | RISK_MANAGER, ADMIN | Registrar decisión |
| GET | `/api/v1/evaluaciones/{id}/decision` | RISK_MANAGER, ADMIN, CREDIT_SUPERVISOR | Ver decisión |

**Auditoría**

| Método | Path | Roles | Descripción |
|--------|------|-------|-------------|
| GET | `/api/v1/auditoria` | ADMIN, RISK_MANAGER, CREDIT_SUPERVISOR, ANALYST | Logs de auditoría |
| GET | `/api/v1/auditoria/export` | ADMIN, RISK_MANAGER, CREDIT_SUPERVISOR, ANALYST | Exportar logs a CSV |

**Autenticación y usuarios**

| Método | Path | Roles | Descripción |
|--------|------|-------|-------------|
| POST | `/api/v1/auth/usuarios` | ADMIN | Crear usuario |
| PATCH | `/api/v1/auth/usuarios/{id}/rol` | ADMIN | Cambiar rol |
| GET | `/api/v1/roles/permisos` | ADMIN | Matriz de permisos |

---

## 7. Tecnologías utilizadas

| Tecnología | Versión | Para qué se usa |
|------------|---------|-----------------|
| Java | 21 | Lenguaje principal |
| Spring Boot | 3.4.4 | Framework base |
| Spring Data JPA | 3.4 | Acceso a base de datos |
| PostgreSQL | 16 | Base de datos |
| Flyway | — | Migraciones de base de datos |
| JWT (jjwt) | 0.12.6 | Autenticación |
| MapStruct | 1.6.3 | Mapeo de objetos |
| Micrometer + Prometheus | — | Métricas |
| JUnit 5 | — | Tests unitarios |
| Testcontainers | — | Tests de integración |
| REST Assured | — | Tests de API |
| ArchUnit | — | Tests de arquitectura |
| SpringDoc OpenAPI | — | Swagger UI |

---

## 8. Arquitectura

El proyecto usa **Arquitectura Hexagonal** (Ports & Adapters).

```
┌─────────────────────────────────────────────────┐
│               INFRAESTRUCTURA                   │
│  (REST API, JPA, Crypto, Metrics, JWT)          │
│                                                 │
│    ┌───────────────────────────────────┐        │
│    │          APLICACIÓN               │        │
│    │  (Casos de uso, Servicios)         │        │
│    │                                   │        │
│    │    ┌─────────────────────┐        │        │
│    │    │      DOMINIO        │        │        │
│    │    │  (Modelos, Puertos) │        │        │
│    │    └─────────────────────┘        │        │
│    └───────────────────────────────────┘        │
└─────────────────────────────────────────────────┘
```

### Módulos del proyecto

| Módulo | Estado | Responsabilidad |
|--------|--------|-----------------|
| `applicant` | ✅ Implementado | Registro, búsqueda y edición de solicitantes |
| `financialdata` | 🔄 Parcial | Captura y versionado de datos financieros |
| `scoring` | ✅ Implementado | Variables de scoring: definición, tipos, rangos |
| `scoringmodel` | ✅ Implementado | Modelos de scoring: creación, activación, reglas KO |
| `scoringengine` | ✅ Implementado | Motor de cálculo y simulación |
| `evaluation` | ✅ Implementado | Evaluaciones crediticias y reportes PDF |
| `creditdecision` | ✅ Implementado | Decisiones crediticias |
| `shared/security` | ✅ Implementado | Autenticación JWT, RBAC, gestión de usuarios |
| `shared/audit` | ✅ Implementado | Trazabilidad de eventos |
| `reporting` | 📋 Stub | Reportes de distribución de riesgo |

---

## 9. Correr los tests

```bash
./gradlew test
```

Requiere Docker corriendo. Los tests de integración levantan PostgreSQL automáticamente con Testcontainers.

---

## 10. Variables de configuración

Todas las variables están en `.env` (copiar de `.env.example`):

| Variable | Valor por defecto | Descripción |
|----------|-------------------|-------------|
| `APP_PORT` | `8080` | Puerto del servidor |
| `DB_NAME` | `credit_scoring` | Nombre de la base de datos |
| `DB_USERNAME` | `postgres` | Usuario de PostgreSQL |
| `DB_PASSWORD` | `postgres` | Contraseña de PostgreSQL |
| `APP_JWT_SECRET` | *(requerido)* | Clave para firmar JWT |
| `APP_JWT_EXPIRATION_MS` | `86400000` | Expiración del JWT (24h en ms) |
| `APP_CRYPTO_ENCRYPTION_KEY_BASE64` | *(requerido)* | Key AES-256 para encriptar datos sensibles |
| `APP_CRYPTO_HASH_KEY_BASE64` | *(requerido)* | Key HMAC para hash de identificadores |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:4200,http://localhost:5173` | Orígenes permitidos |
| `APP_EVALUATION_COOLDOWN_HOURS` | `24` | Horas de cooldown entre evaluaciones |

---

## 11. Apagar los contenedores

```bash
# Solo apagar
docker compose down

# Apagar y borrar la base de datos
docker compose down -v
```

---

## 12. Documentación

- **[ARCHITECTURE.md](ARCHITECTURE.md)** — arquitectura hexagonal, estructura de packages, flujos, seguridad, cómo implementar un nuevo feature.
