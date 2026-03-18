---
applyTo: "src/main/java/co/udea/codefactory/creditscoring/**/*.java"
---
# Spring Hexagonal Rules

Apply Hexagonal/Clean Architecture boundaries strictly.

## Mandatory rules
- Domain layer must not depend on Spring, JPA, web, or infrastructure classes.
- Application layer defines input/output ports and orchestrates use cases.
- Infrastructure layer implements adapters for web, persistence, and external services.
- Keep package intent explicit: domain, application, infrastructure, shared.
- Prefer constructor injection and explicit wiring.

## Code organization
- Domain: model, value objects, domain exceptions, pure business rules.
- Application: use case services, ports, command/query DTOs.
- Infrastructure: controllers, JPA adapters, external clients, Spring configuration.
- Shared: cross-cutting concerns only (logging, security filters, global exception handling).

## Anti-patterns
- No controller calling repositories directly.
- No JPA entity exposure through REST contracts.
- No domain imports from infrastructure packages.
