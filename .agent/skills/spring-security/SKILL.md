---
name: spring-security
description: >
  Guides secure development with Spring Security 6.x for advanced backend projects.
  Trigger: When working on authentication, authorization, API security, JWT, RBAC, ABAC,
  CORS, rate limiting, MFA, or any Spring Security configuration in a Spring Boot 3.x project.
license: Apache-2.0
metadata:
  author: gentleman-programming
  version: "1.0"
---

## When to Use

- Configuring Spring Security `SecurityFilterChain`
- Implementing JWT authentication with token rotation/revocation
- Setting up RBAC per endpoint or ABAC rules
- Adding MFA for admin/sensitive access
- Configuring CORS, rate limiting, replay attack protection
- Integrating with OIDC/OAuth 2.0 identity providers
- Implementing password policies, account lockout
- Adding security event audit logging
- Reviewing code for OWASP compliance

## Critical Patterns

### Tech Stack

- Java 21+, Spring Boot 3.x, Spring Security 6.x
- `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`
- `java-jwt` or `jjwt` for JWT handling
- `spring-boot-starter-data-jpa` for audit persistence

---

## 1. Identity & Authentication

### 1.1 SecurityFilterChain Configuration

Every security config MUST use the component-based approach (no `WebSecurityConfigurerAdapter`):

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v*/auth/**").permitAll()
                .requestMatchers("/api/v*/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v*/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter, JwtAuthFilter.class)
            .addFilterBefore(replayAttackFilter, RateLimitFilter.class)
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .build();
    }
}
```

### 1.2 MFA (Multi-Factor Authentication)

MFA is REQUIRED for admin and sensitive access. Use TOTP (Time-based One-Time Password):

```java
@Entity
@Table(name = "user_mfa")
public class UserMfaConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID userId;
    private String totpSecret;       // encrypted at rest
    private boolean mfaEnabled;
    private int failedAttempts;
    private Instant lockedUntil;
}
```

| Rule | Value |
|------|-------|
| MFA required for | `ROLE_ADMIN`, sensitive state changes, password reset |
| TOTP window tolerance | 1 step (30 seconds) |
| Backup codes | 10 single-use codes, hashed with bcrypt |
| Enrollment | Mandatory on first admin login |

### 1.3 Password Policies

```java
@Component
public class StrongPasswordPolicy implements PasswordValidator {

    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH = 128;
    private static final int PASSWORD_HISTORY_SIZE = 5;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    public ValidationResult validate(String password, User user) {
        List<String> violations = new ArrayList<>();
        if (password.length() < MIN_LENGTH) violations.add("Min 12 characters");
        if (password.length() > MAX_LENGTH) violations.add("Max 128 characters");
        if (!password.matches(".*[A-Z].*")) violations.add("Requires uppercase");
        if (!password.matches(".*[a-z].*")) violations.add("Requires lowercase");
        if (!password.matches(".*\\d.*")) violations.add("Requires digit");
        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) violations.add("Requires special char");
        if (isInPasswordHistory(password, user)) violations.add("Cannot reuse last 5 passwords");
        return new ValidationResult(violations.isEmpty(), violations);
    }
}
```

| Policy | Value |
|--------|-------|
| Min length | 12 characters |
| Max length | 128 characters |
| Complexity | Upper + lower + digit + special |
| History | Last 5 passwords (bcrypt-hashed) |
| Lockout | 5 failed attempts → 15 min lockout |
| Expiry | 90 days (configurable, warn at 14 days) |

### 1.4 Token Revocation

MUST maintain a token blacklist. Use Redis or DB-backed store:

```java
@Service
public class TokenBlacklistService {

    private final StringRedisTemplate redis;
    private static final String PREFIX = "token:blacklist:";

    public void revoke(String jti, Instant expiration) {
        Duration ttl = Duration.between(Instant.now(), expiration);
        if (!ttl.isNegative()) {
            redis.opsForValue().set(PREFIX + jti, "revoked", ttl);
        }
    }

    public boolean isRevoked(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
    }
}
```

| Rule | Detail |
|------|--------|
| Revoke on | Logout, password change, role change, security incident |
| Storage | Redis with TTL matching token expiry |
| Token ID | Every JWT MUST include `jti` claim |
| Refresh tokens | Stored in DB, deleted on revocation |

### 1.5 Session Hijacking Protection

```java
@Bean
public ResponseCookie.ResponseCookieBuilder secureCookieDefaults() {
    return ResponseCookie.from("SESSION", "")
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .path("/")
        .maxAge(Duration.ofHours(1));
}
```

| Protection | Implementation |
|------------|----------------|
| Token rotation | Issue new access token on each refresh; invalidate old refresh token |
| SameSite | `Strict` for session cookies, `Lax` for CSRF tokens |
| HttpOnly | ALL auth cookies |
| Secure | ALL cookies in production |
| Fingerprint | Bind token to client fingerprint hash (User-Agent + partial IP) |

### 1.6 OIDC / OAuth 2.0 Integration

```yaml
# application.yml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${OAUTH_GOOGLE_CLIENT_ID}
            client-secret: ${OAUTH_GOOGLE_CLIENT_SECRET}
            scope: openid, profile, email
          azure:
            client-id: ${OAUTH_AZURE_CLIENT_ID}
            client-secret: ${OAUTH_AZURE_CLIENT_SECRET}
            scope: openid, profile, email
        provider:
          azure:
            issuer-uri: https://login.microsoftonline.com/${AZURE_TENANT_ID}/v2.0
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI}
          jwk-set-uri: ${JWT_JWK_SET_URI}
```

| Rule | Detail |
|------|--------|
| Secrets | NEVER in code or config files; use env vars or vault |
| Issuer validation | ALWAYS validate `iss` claim against whitelist |
| Audience validation | ALWAYS validate `aud` claim |
| Token mapping | Map external claims to internal roles via `GrantedAuthoritiesMapper` |

---

## 2. API Security

### 2.1 Replay Attack Protection

Custom filter with nonce + timestamp validation:

```java
@Component
@Order(1)
public class ReplayAttackFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis;
    private static final Duration NONCE_TTL = Duration.ofMinutes(5);
    private static final Duration MAX_TIMESTAMP_DRIFT = Duration.ofMinutes(2);

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                     FilterChain chain) throws ServletException, IOException {
        String nonce = req.getHeader("X-Request-Nonce");
        String timestamp = req.getHeader("X-Request-Timestamp");

        if (nonce == null || timestamp == null) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing nonce/timestamp");
            return;
        }

        Instant requestTime = Instant.ofEpochMilli(Long.parseLong(timestamp));
        if (Duration.between(requestTime, Instant.now()).abs().compareTo(MAX_TIMESTAMP_DRIFT) > 0) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Request timestamp too old");
            return;
        }

        String key = "nonce:" + nonce;
        Boolean wasAbsent = redis.opsForValue().setIfAbsent(key, "1", NONCE_TTL);
        if (Boolean.FALSE.equals(wasAbsent)) {
            res.sendError(HttpServletResponse.SC_CONFLICT, "Duplicate nonce — replay rejected");
            return;
        }

        chain.doFilter(req, res);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return "GET".equalsIgnoreCase(req.getMethod());  // only protect mutating ops
    }
}
```

### 2.2 Schema Validation (OpenAPI / JSON Schema)

```java
@Component
public class RequestSchemaValidationFilter extends OncePerRequestFilter {

    private final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    private final Map<String, JsonSchema> schemaCache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                     FilterChain chain) throws ServletException, IOException {
        String schemaPath = resolveSchemaPath(req);
        if (schemaPath != null) {
            CachedBodyHttpServletRequest cached = new CachedBodyHttpServletRequest(req);
            JsonSchema schema = schemaCache.computeIfAbsent(schemaPath, this::loadSchema);
            Set<ValidationMessage> errors = schema.validate(
                new ObjectMapper().readTree(cached.getInputStream()));
            if (!errors.isEmpty()) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, errors.toString());
                return;
            }
            chain.doFilter(cached, res);
        } else {
            chain.doFilter(req, res);
        }
    }
}
```

| Rule | Detail |
|------|--------|
| Validate | ALL POST/PUT/PATCH request bodies |
| Schema location | `src/main/resources/schemas/{version}/{endpoint}.json` |
| Versioning | Schemas per API version |
| Fail closed | Reject requests that don't match schema |

### 2.3 Restrictive CORS Configuration

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(
        "https://app.example.com",
        "https://admin.example.com"
    ));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
    config.setAllowedHeaders(List.of(
        "Authorization", "Content-Type", "X-Request-Nonce", "X-Request-Timestamp"
    ));
    config.setExposedHeaders(List.of("X-RateLimit-Remaining", "X-RateLimit-Reset"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

| Rule | Detail |
|------|--------|
| Origins | Explicit whitelist ONLY. NEVER use `*` with credentials |
| Methods | Only methods actually used |
| Headers | Only headers actually needed |
| Max age | 1 hour (3600s) |
| Credentials | `true` only if cookies/auth headers are sent cross-origin |

### 2.4 Rate Limiting

Use Bucket4j or custom filter with Redis:

```java
@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis;

    private static final int MAX_REQUESTS = 100;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                     FilterChain chain) throws ServletException, IOException {
        String clientKey = extractClientKey(req); // IP or authenticated user ID
        String redisKey = "rate:" + clientKey;

        Long count = redis.opsForValue().increment(redisKey);
        if (count == 1) {
            redis.expire(redisKey, WINDOW);
        }

        res.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS));
        res.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, MAX_REQUESTS - count)));

        if (count > MAX_REQUESTS) {
            res.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
            res.sendError(429, "Rate limit exceeded");
            return;
        }

        chain.doFilter(req, res);
    }

    private String extractClientKey(HttpServletRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return "user:" + auth.getName();
        }
        return "ip:" + req.getRemoteAddr();
    }
}
```

| Tier | Limit | Window |
|------|-------|--------|
| Anonymous | 30 req | 1 min |
| Authenticated | 100 req | 1 min |
| Admin | 300 req | 1 min |
| Auth endpoints (`/login`, `/register`) | 5 req | 5 min |

---

## 3. Secure API Versioning & Vulnerability Management

### 3.1 Dependency Scanning (SCA)

```xml
<!-- pom.xml — OWASP Dependency-Check plugin -->
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>10.0.3</version>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>
        <suppressionFile>dependency-check-suppressions.xml</suppressionFile>
    </configuration>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

| Rule | Detail |
|------|--------|
| CI integration | Run on every PR and nightly |
| Fail threshold | CVSS >= 7 (HIGH) blocks merge |
| Suppressions | Require justification comment + expiry date |

### 3.2 Secure Code Review Practices

| Checkpoint | What to verify |
|------------|----------------|
| Auth bypass | No endpoint missing `@PreAuthorize` or `SecurityFilterChain` rule |
| SQL injection | All queries use parameterized statements or Spring Data |
| Secret exposure | No hardcoded secrets; all from env/vault |
| Input validation | `@Valid` on all `@RequestBody`; custom validators for business rules |
| Error leakage | Exception handlers return generic messages; no stack traces in production |
| Dependency | No known CVEs above threshold |
| Logging | No PII or secrets in logs |

### 3.3 Vulnerability Management SLAs

| Severity | CVSS | SLA to patch |
|----------|------|--------------|
| Critical | 9.0 – 10.0 | 24 hours |
| High | 7.0 – 8.9 | 7 days |
| Medium | 4.0 – 6.9 | 30 days |
| Low | 0.1 – 3.9 | 90 days (next release) |

### 3.4 API Versioning Security

```java
// Version-specific SecurityFilterChain
@Bean
@Order(1)
public SecurityFilterChain v2SecurityFilterChain(HttpSecurity http) throws Exception {
    return http
        .securityMatcher("/api/v2/**")
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v2/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .build();
}

@Bean
@Order(2)
public SecurityFilterChain v1SecurityFilterChain(HttpSecurity http) throws Exception {
    return http
        .securityMatcher("/api/v1/**")
        .authorizeHttpRequests(auth -> auth
            .anyRequest().authenticated()
        )
        .build();
}
```

| Rule | Detail |
|------|--------|
| Deprecated versions | Set sunset header + log usage for monitoring |
| Schema per version | Separate JSON Schema files under `schemas/v1/`, `schemas/v2/` |
| Security per version | Each version can have its own `SecurityFilterChain` with `@Order` |

---

## 4. Architecture & Resilience

### 4.1 Principle of Least Privilege

```java
// Method-level security with RBAC
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public List<UserDto> listUsers() { /* ... */ }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@PathVariable UUID id) { /* ... */ }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @ownershipChecker.isOwner(#id, authentication)")
    public UserDto updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserDto dto) {
        /* ... */
    }
}
```

| Rule | Detail |
|------|--------|
| Default | Deny all; explicitly permit |
| Service accounts | Minimal roles; never `ADMIN` |
| DB connections | App user has only SELECT/INSERT/UPDATE/DELETE — never DDL |
| File system | App never writes outside designated directories |

### 4.2 JWT Authentication with Renewal/Expiration

```java
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}") private String secret;
    @Value("${jwt.access-expiration:PT15M}") private Duration accessExpiration;
    @Value("${jwt.refresh-expiration:P7D}") private Duration refreshExpiration;

    public TokenPair generateTokenPair(UserDetails user, Map<String, Object> claims) {
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();

        String accessToken = Jwts.builder()
            .setId(jti)
            .setSubject(user.getUsername())
            .addClaims(claims)
            .claim("roles", user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plus(accessExpiration)))
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact();

        String refreshToken = Jwts.builder()
            .setId(UUID.randomUUID().toString())
            .setSubject(user.getUsername())
            .claim("type", "refresh")
            .claim("access_jti", jti)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plus(refreshExpiration)))
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact();

        return new TokenPair(accessToken, refreshToken, accessExpiration, refreshExpiration);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
```

| Parameter | Value |
|-----------|-------|
| Access token TTL | 15 minutes |
| Refresh token TTL | 7 days |
| Algorithm | HS512 (symmetric) or RS256 (asymmetric for multi-service) |
| Rotation | New refresh token on each use; old one invalidated |
| Claims | `sub`, `jti`, `roles`, `iat`, `exp` — minimum set |

### 4.3 JWT Authentication Filter

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklistService blacklistService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                     FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }

        String token = header.substring(7);
        try {
            Claims claims = tokenProvider.parseToken(token);
            if (blacklistService.isRevoked(claims.getId())) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token revoked");
                return;
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException e) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        chain.doFilter(req, res);
    }
}
```

### 4.4 RBAC per Endpoint + Simple ABAC

```java
// Custom ABAC ownership checker
@Component("ownershipChecker")
public class OwnershipChecker {

    private final ResourceRepository resourceRepository;

    /**
     * ABAC rule: "only owner or ADMIN can modify"
     */
    public boolean isOwner(UUID resourceId, Authentication auth) {
        String username = auth.getName();
        return resourceRepository.findById(resourceId)
            .map(r -> r.getOwnerUsername().equals(username))
            .orElse(false);
    }

    /**
     * ABAC rule: "only same department or ADMIN"
     */
    public boolean isSameDepartment(UUID resourceId, Authentication auth) {
        UserDetails user = (UserDetails) auth.getPrincipal();
        return resourceRepository.findById(resourceId)
            .map(r -> r.getDepartment().equals(extractDepartment(user)))
            .orElse(false);
    }
}
```

Use in controllers:

```java
@PutMapping("/{id}")
@PreAuthorize("hasRole('ADMIN') or @ownershipChecker.isOwner(#id, authentication)")
public ResponseEntity<ResourceDto> update(@PathVariable UUID id, @Valid @RequestBody UpdateDto dto) {
    return ResponseEntity.ok(service.update(id, dto));
}
```

| Access Model | When to use |
|--------------|-------------|
| RBAC (`hasRole`) | Role-based access to endpoints |
| ABAC (`@ownershipChecker`) | Resource-level ownership or attribute checks |
| Combined | `hasRole('ADMIN') or @ownershipChecker.isOwner(...)` |

### 4.5 OWASP Controls

| Control | Implementation |
|---------|----------------|
| Input validation | `@Valid` + `@Pattern` + `@Size` on all DTOs; sanitize HTML with OWASP Java Encoder |
| SQL injection | Spring Data JPA parameterized queries ONLY; no string concatenation |
| XSS | Response `Content-Type` headers; no raw HTML rendering from user input |
| Secret management | `spring-cloud-vault` or env vars; NEVER in `application.yml` |
| CSRF | Enabled for browser clients; STATELESS APIs use token-based auth |
| Security headers | `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Strict-Transport-Security` |

```java
@Bean
public SecurityFilterChain securityHeaders(HttpSecurity http) throws Exception {
    return http
        .headers(headers -> headers
            .frameOptions(frame -> frame.deny())
            .contentTypeOptions(Customizer.withDefaults())
            .httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .maxAgeInSeconds(31536000))
            .xssProtection(Customizer.withDefaults())
        )
        .build();
}
```

### 4.6 Security Event Audit Logging

```java
@Entity
@Table(name = "security_audit_log")
public class SecurityAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SecurityEventType eventType;

    @Column(length = 255)
    private String principal;        // username or "anonymous"

    @Column(length = 45)
    private String sourceIp;

    @Column(length = 500)
    private String details;          // JSON: affected resource, old/new state

    @Column(length = 50)
    private String outcome;          // SUCCESS, FAILURE, BLOCKED

    @Column(length = 100)
    private String requestUri;
}

public enum SecurityEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    TOKEN_ISSUED,
    TOKEN_REVOKED,
    TOKEN_REFRESH,
    MFA_CHALLENGE,
    MFA_SUCCESS,
    MFA_FAILURE,
    PASSWORD_CHANGE,
    ACCOUNT_LOCKED,
    ACCOUNT_UNLOCKED,
    ROLE_CHANGED,
    PERMISSION_DENIED,
    RATE_LIMIT_EXCEEDED,
    REPLAY_ATTACK_DETECTED,
    RESOURCE_CREATED,
    RESOURCE_UPDATED,
    RESOURCE_DELETED,
    CRITICAL_CONFIG_CHANGE
}
```

```java
@Component
public class SecurityAuditLogger {

    private final SecurityAuditEventRepository repository;
    private static final Logger log = LoggerFactory.getLogger(SecurityAuditLogger.class);

    public void log(SecurityEventType type, String principal, String sourceIp,
                    String outcome, String details) {
        SecurityAuditEvent event = new SecurityAuditEvent();
        event.setTimestamp(Instant.now());
        event.setEventType(type);
        event.setPrincipal(principal);
        event.setSourceIp(sourceIp);
        event.setOutcome(outcome);
        event.setDetails(details);
        repository.save(event);

        if (type == SecurityEventType.LOGIN_FAILURE || type == SecurityEventType.REPLAY_ATTACK_DETECTED) {
            log.warn("SECURITY: {} by {} from {} — {}", type, principal, sourceIp, outcome);
        }
    }
}
```

| Event | When to log |
|-------|-------------|
| `LOGIN_FAILURE` | Every failed auth attempt |
| `ACCOUNT_LOCKED` | After lockout threshold |
| `TOKEN_REVOKED` | On logout, password change, security incident |
| `PERMISSION_DENIED` | 403 responses |
| `RATE_LIMIT_EXCEEDED` | 429 responses |
| `REPLAY_ATTACK_DETECTED` | Duplicate nonce |
| `CRITICAL_CONFIG_CHANGE` | Role changes, security config updates |
| `RESOURCE_*` | Create/update/delete of sensitive entities |

**Retention**: 90 days hot (DB), 1 year cold (archived). NEVER log passwords, tokens, or PII in `details`.

---

## 5. Filter Chain Order

Filters MUST be registered in this order:

| Order | Filter | Purpose |
|-------|--------|---------|
| 1 | `ReplayAttackFilter` | Reject replayed requests early |
| 2 | `RateLimitFilter` | Throttle before expensive auth |
| 3 | `JwtAuthFilter` | Authenticate and set security context |
| 4 | `RequestSchemaValidationFilter` | Validate body against JSON Schema |
| 5 | Spring Security filters | Authorization (`@PreAuthorize`, etc.) |

---

## 6. Project Structure

```
src/main/java/com/example/project/
├── security/
│   ├── config/
│   │   ├── SecurityConfig.java          # SecurityFilterChain beans
│   │   └── CorsConfig.java              # CORS configuration source
│   ├── filter/
│   │   ├── JwtAuthFilter.java           # JWT validation filter
│   │   ├── RateLimitFilter.java         # Rate limiting filter
│   │   ├── ReplayAttackFilter.java      # Nonce/timestamp filter
│   │   └── SchemaValidationFilter.java  # JSON Schema validation
│   ├── jwt/
│   │   ├── JwtTokenProvider.java        # Token generation/parsing
│   │   └── TokenBlacklistService.java   # Token revocation
│   ├── mfa/
│   │   ├── TotpService.java             # TOTP generation/verification
│   │   └── MfaController.java           # MFA enrollment/verification
│   ├── audit/
│   │   ├── SecurityAuditEvent.java      # JPA entity
│   │   ├── SecurityEventType.java       # Event type enum
│   │   ├── SecurityAuditLogger.java     # Logging service
│   │   └── SecurityAuditEventRepository.java
│   ├── password/
│   │   ├── StrongPasswordPolicy.java    # Validation rules
│   │   └── PasswordHistoryService.java  # History tracking
│   ├── abac/
│   │   └── OwnershipChecker.java        # ABAC ownership rules
│   └── oauth/
│       └── OidcUserMapper.java          # Map OIDC claims to roles
```

---

## Commands

```bash
# Run OWASP dependency check
./mvnw dependency-check:check

# Generate security audit report
./mvnw dependency-check:aggregate -DoutputDirectory=target/security-report

# Run with security profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=secure

# Check for outdated dependencies
./mvnw versions:display-dependency-updates

# Run tests with security context
./mvnw test -Dtest="*Security*"
```
