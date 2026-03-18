---
applyTo: "src/main/java/co/udea/codefactory/creditscoring/**/*Controller.java,src/main/java/co/udea/codefactory/creditscoring/**/application/dto/**/*.java,src/main/java/co/udea/codefactory/creditscoring/**/infrastructure/adapter/in/rest/**/*.java"
---
# Spring API Design Rules

## Mandatory rules
- Never expose entities in API responses; use explicit request/response DTOs.
- Validate all mutable request payloads (`@Valid`, bean validation annotations).
- Keep endpoint naming resource-oriented and versioned (`/api/v1/...`).
- Return standardized errors using RFC 7807 `ProblemDetail` style.
- Add OpenAPI annotations for public endpoints and DTO schemas.

## Response design
- Include navigational links when applicable (HATEOAS style).
- Keep status code semantics strict: 400, 401, 403, 404, 409, 422, 500.

## Anti-patterns
- No business logic in controllers.
- No generic `Exception` messages leaked to clients.
- No undocumented endpoints or undocumented error contracts.
