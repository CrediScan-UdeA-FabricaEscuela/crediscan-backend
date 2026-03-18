---
applyTo: "src/test/**/*.java,src/test/**/*.feature,src/main/java/co/udea/codefactory/creditscoring/**/*Test*.java"
---
# Spring Testing Rules

## Mandatory rules
- Unit tests use JUnit 5 + Mockito with strict Arrange-Act-Assert.
- Name tests as `should_expectedBehavior_when_condition`.
- Unit tests must not start Spring context unless explicitly needed.
- Use slice tests (`@WebMvcTest`, `@DataJpaTest`) for focused integration coverage.
- Use full `@SpringBootTest` only for real cross-layer flows.

## Quality
- Target minimum 40% line coverage.
- Cover both happy path and key failure scenarios.
- Prefer deterministic tests and isolated test data.

## Anti-patterns
- No assertion-light tests.
- No mixed AAA phases.
- No fragile tests coupled to execution order.
