---
applyTo: "src/main/resources/logback-spring.xml,src/main/resources/application*.yml,src/main/java/co/udea/codefactory/creditscoring/**/shared/logging/**/*.java,src/main/java/co/udea/codefactory/creditscoring/**/shared/audit/**/*.java,src/main/java/co/udea/codefactory/creditscoring/**/*Metrics*.java,src/main/java/co/udea/codefactory/creditscoring/**/*Health*.java"
---
# Spring Observability Rules

## Mandatory rules
- Use structured logging and include correlation identifiers (trace/request ids).
- Keep log levels intentional: ERROR, WARN, INFO, DEBUG.
- Enable Actuator health and metrics endpoints required by operations.
- Use Micrometer metrics for business-critical flows.
- Add health indicators for critical dependencies.

## Security and privacy
- Mask sensitive values in logs.
- Never log secrets, tokens, passwords, or full personal data.

## Anti-patterns
- No noisy debug logging in production profiles.
- No observability setup without explicit endpoint exposure policy.
