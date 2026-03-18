# 0005 — HATEOAS en la API REST

- **Status**: Accepted
- **Date**: 2026-03-18
- **Deciders**: Equipo CodeF@ctory Advanced — UdeA

## Context

El curso CodeF@ctory Advanced exige implementar una API REST nivel 3 del Modelo de Madurez de Richardson (RMM), lo que implica incluir **HATEOAS** (Hypermedia As The Engine Of Application State).

HATEOAS hace que las respuestas incluyan links a las acciones posibles sobre el recurso, permitiendo a los clientes navegar la API sin conocer las URLs de antemano.

Alternativas:
1. **No HATEOAS (RMM nivel 2)** — solo recursos + verbos HTTP. Más simple pero no cumple el requisito.
2. **HAL (Hypertext Application Language)** — estándar de facto con Spring HATEOAS. `_links` en JSON.
3. **JSON:API** — más estricto y completo, pero requiere librerías adicionales y mayor complejidad.
4. **Siren** — permite acciones, pero menor adopción.

## Decision

Implementamos **HATEOAS con HAL** usando la librería `spring-hateoas`.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-hateoas</artifactId>
</dependency>
```

Todos los DTOs de respuesta extienden `RepresentationModel<T>` o se envuelven en `EntityModel<T>` / `CollectionModel<T>`.

Ejemplo de respuesta:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "fullName": "Juan Pérez",
  "riskScore": 720,
  "_links": {
    "self":       { "href": "/api/v1/applicants/550e8400-e29b-41d4-a716-446655440000" },
    "evaluation": { "href": "/api/v1/evaluations?applicantId=550e8400-e29b-41d4-a716-446655440000" },
    "applicants": { "href": "/api/v1/applicants" }
  }
}
```

Los links se construyen con `WebMvcLinkBuilder.linkTo(methodOn(...))`.

## Consequences

**Positivos:**
- Cumple requisito del curso (RMM nivel 3).
- Los clientes pueden descubrir acciones disponibles sin hardcodear URLs.
- Los links reflejan el estado del recurso (p. ej., link de `approve` solo aparece si la evaluación está pendiente).

**Negativos:**
- Los DTOs de respuesta son más verbosos.
- Los tests deben verificar la presencia de los links (`assertThat(response).hasLink("self")`).
- Mayor complejidad en el assembler/mapper layer.

**Convención:** Se crea un `*ModelAssembler` por recurso (p. ej. `ApplicantModelAssembler`) que centraliza la construcción de links.
