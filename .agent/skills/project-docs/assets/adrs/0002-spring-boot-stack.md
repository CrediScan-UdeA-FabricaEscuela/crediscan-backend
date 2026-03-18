# 0002 — Spring Boot Stack (Java 21 + Spring Boot 3.4)

- **Status**: Accepted
- **Date**: 2026-03-18
- **Deciders**: Equipo CodeF@ctory Advanced — UdeA

## Context

El curso CodeF@ctory Advanced exige el uso del stack Java/Spring Boot como tecnología principal.
Se debió elegir la versión específica del JDK y del framework.

Alternativas evaluadas:

| Opción | Java | Spring Boot | Observación |
|--------|------|-------------|-------------|
| A | 17 (LTS) | 3.2 | Soporte hasta Nov 2026 |
| **B (elegida)** | **21 (LTS)** | **3.4** | LTS vigente, Spring Boot activo |
| C | 21 | 3.3 | Inmediatamente anterior, sin diferencias críticas |

Dependencias clave del stack:
- Spring Web MVC (REST)
- Spring Data JPA + Hibernate 6
- Spring Security 6
- Spring Boot Actuator + Micrometer
- Springdoc OpenAPI 2.x
- Spring HATEOAS
- Flyway
- Testcontainers
- MapStruct + Lombok

## Decision

Usamos **Java 21 (LTS)** con **Spring Boot 3.4.x** y la dependencia BOM `spring-boot-dependencies` para gestionar versiones.

Java 21 habilita:
- **Virtual Threads** (Project Loom) — activados con `spring.threads.virtual.enabled=true`.
- **Record patterns** y **Sealed classes** para modelar el dominio con mayor expresividad.
- **Pattern matching for switch** — útil en clasificación de riesgo.

Spring Boot 3.4 aporta:
- Native image con GraalVM (opcional, Sprint 4).
- Observabilidad OOTB con Micrometer + OTLP.
- Jakarta EE 10 (namespace `jakarta.*`).

## Consequences

**Positivos:**
- LTS con soporte hasta Sep 2029 (Java 21).
- Virtual Threads mejoran throughput bajo carga de I/O (consultas a bureaux externos).
- Ecosistema maduro con amplia documentación y soporte de la comunidad.

**Negativos:**
- Jakarta namespace (`jakarta.*`) rompe compatibilidad con librerías legacy que usen `javax.*`.
- Algunas librerías de terceros aún no tienen soporte completo para Java 21 (revisar en cada incorporación).

**Restricción:** El `pom.xml` define `<java.version>21</java.version>` y hereda de `spring-boot-starter-parent:3.4.x`. No se permite bajar la versión sin crear un nuevo ADR.
