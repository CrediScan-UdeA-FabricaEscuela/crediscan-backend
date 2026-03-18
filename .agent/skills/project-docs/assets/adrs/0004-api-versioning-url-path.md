# 0004 — Versionado de API por URL Path

- **Status**: Accepted
- **Date**: 2026-03-18
- **Deciders**: Equipo CodeF@ctory Advanced — UdeA

## Context

La API REST debe soportar evolución sin romper clientes existentes. Se necesita una estrategia de versionado clara, simple de implementar con Spring MVC y compatible con la especificación OpenAPI generada por Springdoc.

Estrategias consideradas:

| Estrategia | Ejemplo | Pros | Contras |
|------------|---------|------|---------|
| **URL Path (elegida)** | `/api/v1/applicants` | Simple, cacheable, visible en logs | Viola purismo REST (URL debe identificar recurso, no versión) |
| Header (`Accept`) | `Accept: application/vnd.creditrisk.v1+json` | REST puro | Difícil de probar en browser/Postman |
| Query param | `/applicants?version=1` | Simple | No cacheable, contaminación de query params |
| Subdominio | `v1.api.creditrisk.com` | Separación total | Overkill, requiere DNS |

## Decision

Versión en **URL path**: todos los endpoints bajo `/api/v{N}/`.

```
/api/v1/applicants
/api/v1/applicants/{id}
/api/v1/evaluations
/api/v1/scores
```

Implementación en Spring MVC:

```java
@RequestMapping("/api/v1")
@RestController
public class ApplicantController { ... }
```

Para v2 (cuando exista), se crea un nuevo controller con `@RequestMapping("/api/v2")`.
Los controllers v1 se marcan `@Deprecated` y se mantienen hasta el fin de vida anunciado.

La versión de la API se expone también en el response header:
```
X-API-Version: 1
```

## Consequences

**Positivos:**
- Simple de implementar y entender.
- Visible en logs y herramientas de monitoreo (Grafana, Prometheus).
- Soportado nativo por Springdoc (genera specs separadas por path).
- Compatible con API Gateways y proxies (cacheable).

**Negativos:**
- Al agregar v2, se duplica código de controllers (mitigado con shared services/use cases).
- Puristas REST argumentan que viola el principio de URL estable como identificador de recurso.

**Convención de deprecación:** Al introducir v2, se anuncia v1 como deprecated con header `Sunset: <fecha>` y `Deprecation: true` según RFC 8594.
