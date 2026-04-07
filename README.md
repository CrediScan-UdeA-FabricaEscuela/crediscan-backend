# Credit Scoring Engine

API REST para evaluación de solicitudes de crédito. Construida con Java 21 + Spring Boot 3.4.4, arquitectura hexagonal y PostgreSQL.

---

## Requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) con WSL2 integration activada

Eso es todo. No necesitás Java ni PostgreSQL instalados localmente.

---

## Levantar la app

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

## Verificar que está corriendo

```bash
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```

---

## Explorar la API

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

## Endpoints principales

| Método | Path | Roles | Descripción |
|--------|------|-------|-------------|
| POST | `/api/v1/auth/login` | público | Obtener JWT |
| POST | `/api/v1/auth/usuarios` | ADMIN | Crear usuario |
| PATCH | `/api/v1/auth/usuarios/{id}/rol` | ADMIN | Cambiar rol |
| POST | `/api/v1/solicitantes` | ANALYST, ADMIN | Registrar solicitante |
| GET | `/api/v1/solicitantes?q=...` | ANALYST, RISK_MANAGER, ADMIN, CREDIT_SUPERVISOR | Buscar solicitantes |
| PATCH | `/api/v1/solicitantes/{id}` | ANALYST, ADMIN | Editar solicitante |
| POST | `/api/v1/solicitantes/{id}/datos-financieros` | ANALYST, ADMIN | Crear datos financieros del solicitante |
| PUT | `/api/v1/solicitantes/{id}/datos-financieros/{version}` | ANALYST, ADMIN | Actualizar versión específica de datos financieros |

---

## Datos financieros

Los nuevos endpoints permiten registrar y actualizar datos financieros asociados a un solicitante.

### Crear datos financieros

`POST /api/v1/solicitantes/{id}/datos-financieros`

Datos esperados:
- `income` (decimal, mayor o igual a 0)
- `expenses` (decimal, mayor o igual a 0)
- `debtAmount` (decimal, mayor o igual a 0)
- `creditScore` (integer, entre 0 y 1000)
- `numCreditCards` (integer, mayor o igual a 0)
- `numLoans` (integer, mayor o igual a 0)
- `assets` (decimal, mayor o igual a 0)
- `hasMortgages` (boolean)

El endpoint devuelve los datos creados junto con ratios calculados como `debtToIncomeRatio` y `debtToExpensesRatio`, y banderas de alerta como `highDebtToIncome`.

### Actualizar datos financieros

`PUT /api/v1/solicitantes/{id}/datos-financieros/{version}`

Este endpoint actualiza una versión específica de los datos financieros de un solicitante. El campo `version` permite conservar un historial de cambios y aplicar validaciones de versión.

---

## Correr los tests

```bash
./gradlew test
```

Requiere Docker corriendo. Los tests de integración levantan PostgreSQL automáticamente con Testcontainers.

---

## Apagar los contenedores

```bash
# Solo apagar
docker compose down

# Apagar y borrar la base de datos
docker compose down -v
```

---

## Documentación

- **[ARCHITECTURE.md](ARCHITECTURE.md)** — arquitectura hexagonal, estructura de packages, flujos, seguridad, cómo implementar un nuevo feature.
