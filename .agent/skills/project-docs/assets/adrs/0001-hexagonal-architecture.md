# 0001 — Hexagonal Architecture (Ports & Adapters)

- **Status**: Accepted
- **Date**: 2026-03-18
- **Deciders**: Equipo CodeF@ctory Advanced — UdeA

## Context

El proyecto Credit Risk Scoring Engine requiere una arquitectura que permita:
- Independencia total del framework y la base de datos en la lógica de dominio.
- Testabilidad unitaria del dominio sin levantar contexto de Spring.
- Intercambiabilidad de adapters (p. ej. reemplazar JPA por JDBC, o agregar un adapter REST externo) sin modificar casos de uso.
- Cumplir con los requerimientos de calidad del curso CodeF@ctory Advanced (SOLID, Clean Architecture, alta cobertura de tests).

Las alternativas consideradas fueron:
1. **Arquitectura en capas tradicional (N-Tier)** — simple pero acopla el dominio a la infraestructura.
2. **Clean Architecture (Uncle Bob)** — similar en intención pero con terminología distinta (Entities, Use Cases, Interface Adapters, Frameworks).
3. **Hexagonal / Ports & Adapters (Cockburn)** — desacoplamiento explícito mediante puertos (interfaces) e implementaciones intercambiables.

## Decision

Adoptamos **Hexagonal Architecture (Ports & Adapters)** como estilo arquitectónico principal.

### Estructura de paquetes por módulo

```
com.udea.creditrisk.{module}/
├── domain/
│   ├── model/          # Entidades, Value Objects, Aggregates
│   ├── port/
│   │   ├── in/         # Puertos de entrada (use-case interfaces)
│   │   └── out/        # Puertos de salida (repository/service interfaces)
│   └── service/        # Domain Services
├── application/
│   └── usecase/        # Implementaciones de puertos de entrada
└── infrastructure/
    ├── persistence/    # Adapters JPA (implementan puertos out)
    ├── rest/           # Controllers REST (adapters in)
    └── external/       # Clientes externos (bureaux, email)
```

Módulos del dominio: `applicant`, `financialdata`, `scoring`, `evaluation`, `reporting`.
Módulo transversal: `shared` (security, audit, logging, exceptions).

## Consequences

**Positivos:**
- El dominio no depende de Spring, JPA, ni ningún framework externo → tests unitarios puros con JUnit 5 / Mockito.
- Agregar un nuevo adapter (p. ej. Kafka consumer) no modifica los casos de uso.
- Facilita mocking de puertos de salida en tests de integración.
- Alineado con los criterios de evaluación del curso.

**Negativos / trade-offs:**
- Mayor cantidad de interfaces y clases que en una arquitectura en capas.
- Curva de aprendizaje para integrantes que vienen de Spring MVC tradicional.
- Puede parecer over-engineering para funcionalidades CRUD simples.

**Mitigación:** Se documentan las convenciones en `.agent/skills/spring-hexagonal/SKILL.md` y en este archivo para que el equipo tenga referencia clara.
