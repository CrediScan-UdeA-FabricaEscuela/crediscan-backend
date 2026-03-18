# 0010 — SonarCloud y Quality Gates

- **Status**: Accepted
- **Date**: 2026-03-18
- **Deciders**: Equipo CodeF@ctory Advanced — UdeA

## Context

El curso CodeF@ctory Advanced exige métricas de calidad verificables: cobertura de código, deuda técnica, bugs y vulnerabilidades. Se necesita integrar análisis estático continuo en el pipeline de CI.

Alternativas:

| Herramienta | Pros | Contras |
|-------------|------|---------|
| **SonarCloud** | Gratuito para repos públicos, integración GitHub nativa, Quality Gates | Requiere repo público o plan de pago |
| SonarQube self-hosted | Control total | Infraestructura adicional |
| Checkstyle + SpotBugs | Ligero, configurable | Sin dashboard centralizado, sin Quality Gate |
| PMD | Análisis estático Java | Sin cobertura integrada |

## Decision

Usamos **SonarCloud** con **JaCoCo** para cobertura, integrado en el pipeline de GitHub Actions.

### Quality Gate definido

| Métrica | Umbral mínimo |
|---------|---------------|
| Cobertura de código | ≥ 80% |
| Duplicación de código | ≤ 3% |
| Maintainability rating | A |
| Reliability rating | A |
| Security rating | A |
| Security Hotspots revisados | 100% |
| Cognitive complexity | ≤ 15 por método |
| Cyclomatic complexity | ≤ 10 por método |

### Configuración Maven (`pom.xml`)

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
```

### GitHub Actions

```yaml
- name: SonarCloud Scan
  uses: SonarSource/sonarcloud-github-action@master
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
  with:
    args: >
      -Dsonar.projectKey=udea_credit-risk-scoring-engine
      -Dsonar.organization=udea
      -Dsonar.java.coveragePlugin=jacoco
      -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
```

### Exclusiones de cobertura

Se excluyen del análisis:
- `**/domain/model/**` (DTOs, records, value objects sin lógica)
- `**/infrastructure/config/**` (clases de configuración Spring)
- `**/*Application.java`
- `**/generated/**`

Configurado en `sonar-project.properties`:
```properties
sonar.exclusions=**/domain/model/**,**/infrastructure/config/**,**/*Application.java
sonar.coverage.exclusions=**/domain/model/**,**/*Dto.java,**/*Request.java,**/*Response.java
```

## Consequences

**Positivos:**
- Quality Gate bloquea merges a `main` si no se cumplen los umbrales.
- Dashboard en SonarCloud visible para el evaluador del curso.
- JaCoCo + Testcontainers garantizan cobertura de código real (no mocks triviales).

**Negativos:**
- El repo debe ser público en GitHub para el tier gratuito de SonarCloud.
- El análisis agrega ~2-3 minutos al pipeline de CI.

**Convención:** Los PR a `main` requieren que el Quality Gate pase. Branch protection configurado en GitHub (`Settings → Branches → Require status checks`).
