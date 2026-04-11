# tests/

Esta carpeta contiene **artefactos de QA manual**, no tests de código automatizados.

Los tests automatizados (unitarios, integración, acceptance) se encuentran en `src/test/`.

## Contenido

| Archivo | Descripción |
|---------|-------------|
| `QA_Plan_Caso9.md` | Plan de pruebas manual para el Caso de Scoring 9 |

## Convención de tests automatizados

| Directorio | Tipo | Comando |
|---|---|---|
| `src/test/java/.../*/domain/` | Unit tests (domain model) | `./gradlew test` |
| `src/test/java/.../*/application/service/` | Unit tests (application services) | `./gradlew test` |
| `src/test/java/.../*IntegrationTest.java` | Integration tests (Testcontainers + PostgreSQL) | `./gradlew test` |
| `src/test/java/.../*/acceptance/*AT.java` | Acceptance tests (REST Assured) | `./gradlew test` |
