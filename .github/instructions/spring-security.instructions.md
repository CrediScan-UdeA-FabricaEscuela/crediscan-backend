---
applyTo: "src/main/java/co/udea/codefactory/creditscoring/**/shared/security/**/*.java,src/main/java/co/udea/codefactory/creditscoring/**/*Security*.java,src/main/java/co/udea/codefactory/creditscoring/**/*Auth*.java,src/main/java/co/udea/codefactory/creditscoring/**/*Jwt*.java,src/main/resources/application*.yml"
---
# Spring Security Rules

## Mandatory rules
- Use SecurityFilterChain-based config (Spring Security 6 style).
- Enforce deny-by-default endpoint authorization.
- Keep JWT/OAuth/OIDC validation strict (issuer, audience, expiry, signature).
- Use stateless sessions for token-based APIs.
- Keep secrets in environment variables or secret stores, never in source.
- Implement rate limiting for auth-sensitive endpoints.

## Secure defaults
- Restrictive CORS allowlist, never wildcard with credentials.
- Consistent auth/audit event logging without exposing secrets.
- Use role checks at endpoint and/or method level.

## Anti-patterns
- No hardcoded credentials or keys.
- No permissive global `permitAll()` for business endpoints.
- No logging of passwords, tokens, or raw secret payloads.
