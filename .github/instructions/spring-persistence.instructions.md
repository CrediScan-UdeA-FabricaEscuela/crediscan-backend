---
applyTo: "src/main/java/co/udea/codefactory/creditscoring/**/*Repository*.java,src/main/java/co/udea/codefactory/creditscoring/**/infrastructure/adapter/output/persistence/**/*.java,src/main/resources/db/migration/*.sql"
---
# Spring Persistence Rules

## Mandatory rules
- Use explicit entity mappings (`@Table`, `@Column`, `@Index`) for non-trivial models.
- Use Flyway migrations for all schema changes.
- Default to `FetchType.LAZY` for associations unless justified.
- Keep repository interfaces focused; move complex filters to specifications or explicit queries.
- Apply audit fields consistently for critical entities.

## Query rules
- Use derived queries only for simple lookups.
- Use JPQL for joins/aggregations.
- Use native SQL only when JPQL cannot express the query efficiently.

## Anti-patterns
- Avoid `CascadeType.ALL` by default in sensitive relationships.
- Avoid N+1 query patterns; fetch or project intentionally.
- Do not put schema-changing SQL outside Flyway migrations.
