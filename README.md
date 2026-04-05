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
