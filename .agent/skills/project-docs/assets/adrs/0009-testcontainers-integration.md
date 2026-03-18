# 0009 — Testcontainers para Tests de Integración

- **Status**: Accepted
- **Date**: 2026-03-18
- **Deciders**: Equipo CodeF@ctory Advanced — UdeA

## Context

Los tests de integración necesitan una base de datos PostgreSQL real (no H2, que tiene diferencias de comportamiento). Se debe poder correr los tests:
- Localmente sin configuración manual de PostgreSQL.
- En CI (GitHub Actions) sin servicios externos.
- Contra el mismo esquema que producción (con Flyway aplicado).

Alternativas:

| Opción | Pros | Contras |
|--------|------|---------|
| **Testcontainers** | PostgreSQL real, zero config, CI-friendly | Requiere Docker en el host |
| H2 in-memory | Rápido, zero config | Dialecto diferente, JSONB no soportado |
| PostgreSQL externo (CI service) | Nativo en GitHub Actions | Requiere configuración adicional, no portable |
| `@DataJpaTest` con H2 | Simple | No valida comportamiento PostgreSQL-específico |

## Decision

Usamos **Testcontainers** con la integración nativa de Spring Boot 3.x (`@ServiceConnection`).

### Configuración base

```java
// src/test/java/com/udea/creditrisk/PostgresIntegrationTest.java
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("creditrisk_test")
            .withUsername("test")
            .withPassword("test");
}
```

Todas las clases de test de integración extienden `PostgresIntegrationTest`.

### Dependencias

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

### GitHub Actions

```yaml
# .github/workflows/ci.yml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Run tests
        run: mvn verify
        # Docker está disponible en ubuntu-latest → Testcontainers funciona sin config adicional
```

## Consequences

**Positivos:**
- Tests de integración corren contra PostgreSQL 16 real → comportamiento idéntico a producción.
- Flyway aplica las mismas migraciones → el esquema es consistente.
- `@ServiceConnection` elimina la necesidad de configurar `application-test.yml` manualmente.
- CI no requiere servicios externos: Docker disponible en `ubuntu-latest`.

**Negativos:**
- Cada suite de tests levanta un contenedor → tiempo de startup ~3-5 segundos.
- Requiere Docker en la máquina del desarrollador (documentado en `docs/database/DEPLOYMENT.md`).
- El contenedor se reutiliza entre tests de la misma JVM (estático `@Container`) para minimizar overhead.

**Optimización:** Se usa `ryuk` (reaper) de Testcontainers para limpieza automática. Se habilita `reuse` en desarrollo local para acelerar ciclos de feedback: `~/.testcontainers.properties` con `testcontainers.reuse.enable=true`.
