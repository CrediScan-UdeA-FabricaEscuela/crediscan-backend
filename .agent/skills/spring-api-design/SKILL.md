---
name: spring-api-design
description: >
  Guides REST API design with Spring Boot 3.x following production-grade standards:
  HATEOAS, OpenAPI, error handling (RFC 7807), validation, versioning, and structured logging.
  Trigger: When designing REST controllers, API endpoints, error handling, or API documentation in Spring Boot.
license: Apache-2.0
metadata:
  version: "1.0"
---

## When to Use

- Designing new REST endpoints or controllers
- Adding HATEOAS links to API responses
- Setting up OpenAPI/Swagger documentation
- Implementing global error handling
- Adding request validation
- Configuring API versioning
- Setting up structured logging for APIs

---

## Critical Patterns

### 1. Never Expose Entities — Always Use DTOs

```
Controller → RequestDTO → Service → Entity → Repository
Controller ← ResponseDTO ← Service ← Entity ← Repository
```

| Layer | Object | Purpose |
|-------|--------|---------|
| API | `CreateUserRequest`, `UserResponse` | Contract with clients |
| Domain | `User` (entity) | Business logic + persistence |
| Mapping | MapStruct interface or manual mapper | Decouples layers |

**MapStruct mapper example:**

```java
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User entity);
    User toEntity(CreateUserRequest request);
}
```

**Manual mapping alternative:**

```java
@Component
public class UserMapper {
    public UserResponse toResponse(User entity) {
        return new UserResponse(entity.getId(), entity.getName(), entity.getEmail());
    }
}
```

### 2. HATEOAS — Mandatory for REST

Every response MUST include navigational links. Use Spring HATEOAS.

**Single resource — `EntityModel<T>`:**

```java
@GetMapping("/{id}")
public EntityModel<UserResponse> getUser(@PathVariable Long id) {
    UserResponse user = userService.findById(id);
    return EntityModel.of(user,
        linkTo(methodOn(UserController.class).getUser(id)).withSelfRel(),
        linkTo(methodOn(UserController.class).getAllUsers(Pageable.unpaged())).withRel("users")
    );
}
```

**Collection — `CollectionModel<T>`:**

```java
@GetMapping
public CollectionModel<EntityModel<UserResponse>> getAllUsers(Pageable pageable) {
    List<EntityModel<UserResponse>> users = userService.findAll(pageable).stream()
        .map(user -> EntityModel.of(user,
            linkTo(methodOn(UserController.class).getUser(user.id())).withSelfRel()))
        .toList();

    return CollectionModel.of(users,
        linkTo(methodOn(UserController.class).getAllUsers(pageable)).withSelfRel());
}
```

**Use `RepresentationModelAssembler` for reusable mapping:**

```java
@Component
public class UserModelAssembler implements RepresentationModelAssembler<UserResponse, EntityModel<UserResponse>> {
    @Override
    public EntityModel<UserResponse> toModel(UserResponse user) {
        return EntityModel.of(user,
            linkTo(methodOn(UserController.class).getUser(user.id())).withSelfRel(),
            linkTo(methodOn(UserController.class).getAllUsers(Pageable.unpaged())).withRel("users"));
    }
}
```

**HAL response format (default):**

```json
{
  "id": 1,
  "name": "Tony Stark",
  "_links": {
    "self": { "href": "http://localhost:8080/api/v1/users/1" },
    "users": { "href": "http://localhost:8080/api/v1/users" }
  }
}
```

### 3. OpenAPI/Swagger with springdoc-openapi

**Dependency (build.gradle.kts):**

```kotlin
implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.0")
```

**Configuration:**

```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: method
  group-configs:
    - group: v1
      paths-to-match: /api/v1/**
    - group: v2
      paths-to-match: /api/v2/**
```

**Annotate controllers:**

```java
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management operations")
public class UserController {

    @Operation(
        summary = "Get user by ID",
        description = "Returns a single user with HATEOAS links"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User found",
            content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public EntityModel<UserResponse> getUser(@PathVariable Long id) { ... }
}
```

**DTO annotations:**

```java
@Schema(description = "User creation request")
public record CreateUserRequest(
    @Schema(description = "User full name", example = "Tony Stark")
    @NotBlank @Size(max = 100)
    String name,

    @Schema(description = "Email address", example = "tony@stark.com")
    @NotBlank @Email
    String email
) {}
```

**Contract-first vs code-first:**

| Approach | When to use |
|----------|-------------|
| Code-first | Internal APIs, rapid iteration, small teams |
| Contract-first | Public APIs, multi-team, API marketplace |

For contract-first, generate from OpenAPI YAML with `openapi-generator-maven-plugin`.

### 4. Standardized Error Handling — RFC 7807 Problem Details

Spring Boot 3.x has native RFC 7807 support via `ProblemDetail`.

**Enable in application.yml:**

```yaml
spring:
  mvc:
    problemdetails:
      enabled: true
```

**Uniform error structure:**

```java
public record ApiError(
    String errorCode,
    String message,
    List<FieldError> details,
    String traceId,
    Instant timestamp
) {
    public record FieldError(String field, String message, Object rejectedValue) {}
}
```

**Global exception handler:**

```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setProperty("errorCode", "RESOURCE_NOT_FOUND");
        problem.setProperty("traceId", MDC.get("traceId"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Business Rule Violation");
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("traceId", MDC.get("traceId"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Validation Error");
        problem.setProperty("errorCode", "VALIDATION_FAILED");
        problem.setProperty("traceId", MDC.get("traceId"));
        problem.setProperty("timestamp", Instant.now());

        List<Map<String, Object>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> Map.<String, Object>of(
                "field", fe.getField(),
                "message", fe.getDefaultMessage(),
                "rejectedValue", fe.getRejectedValue() != null ? fe.getRejectedValue() : "null"))
            .toList();
        problem.setProperty("details", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        problem.setProperty("errorCode", "INTERNAL_ERROR");
        problem.setProperty("traceId", MDC.get("traceId"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
```

**HTTP status code decision table:**

| Code | When |
|------|------|
| 400 | Malformed request, validation failure |
| 401 | Missing or invalid authentication |
| 403 | Authenticated but not authorized |
| 404 | Resource does not exist |
| 409 | Conflict (duplicate, state conflict) |
| 422 | Valid syntax but business rule violation |
| 500 | Unhandled server error |

### 5. Payload Validation

**Bean Validation on DTOs:**

```java
public record CreateUserRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    String name,

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email")
    String email,

    @NotNull(message = "Role is required")
    @Pattern(regexp = "ADMIN|USER|VIEWER", message = "Must be ADMIN, USER, or VIEWER")
    String role
) {}
```

**Activate with `@Valid` on controller:**

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public EntityModel<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) { ... }
```

**Custom validator:**

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueEmailValidator.class)
public @interface UniqueEmail {
    String message() default "Email already exists";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {
    private final UserRepository userRepository;

    public UniqueEmailValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        return email != null && !userRepository.existsByEmail(email);
    }
}
```

### 6. API Versioning

**URL path versioning (recommended):**

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserControllerV1 { ... }

@RestController
@RequestMapping("/api/v2/users")
public class UserControllerV2 { ... }
```

**Header-based versioning (alternative):**

```java
@GetMapping(value = "/users", headers = "X-API-Version=1")
public CollectionModel<EntityModel<UserResponseV1>> getUsersV1() { ... }

@GetMapping(value = "/users", headers = "X-API-Version=2")
public CollectionModel<EntityModel<UserResponseV2>> getUsersV2() { ... }
```

**Deprecation strategy:**

```java
@RestController
@RequestMapping("/api/v1/users")
@Deprecated(since = "2025-01", forRemoval = true)
@Tag(name = "Users v1 (Deprecated)", description = "Use /api/v2/users instead")
public class UserControllerV1 {

    @Operation(deprecated = true, summary = "Get users (deprecated, use v2)")
    @GetMapping
    public CollectionModel<EntityModel<UserResponseV1>> getUsers() { ... }
}
```

Add `Sunset` and `Deprecation` headers via filter or interceptor:

```java
@Component
public class DeprecationHeaderFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (request.getRequestURI().contains("/api/v1/")) {
            response.setHeader("Deprecation", "true");
            response.setHeader("Sunset", "Wed, 01 Jan 2026 00:00:00 GMT");
            response.setHeader("Link", "</api/v2/" + extractPath(request) + ">; rel=\"successor-version\"");
        }
        chain.doFilter(request, response);
    }
}
```

### 7. Structured Logging

**Dependencies (build.gradle.kts):**

```kotlin
implementation("net.logstash.logback:logstash-logback-encoder:8.0")
```

**logback-spring.xml — JSON format:**

```xml
<configuration>
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <includeMdcKeyName>requestPath</includeMdcKeyName>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON" />
    </root>

    <!-- Reduce noise from frameworks -->
    <logger name="org.springframework" level="WARN" />
    <logger name="org.hibernate" level="WARN" />
</configuration>
```

**MDC traceId propagation filter:**

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String traceId = Optional.ofNullable(request.getHeader("X-Trace-Id"))
            .orElse(UUID.randomUUID().toString());
        MDC.put("traceId", traceId);
        MDC.put("requestPath", request.getMethod() + " " + request.getRequestURI());
        response.setHeader("X-Trace-Id", traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

**Request/response logging filter:**

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        chain.doFilter(request, response);
        long duration = System.currentTimeMillis() - start;

        log.info("HTTP {} {} — status={} duration={}ms",
            request.getMethod(), request.getRequestURI(),
            response.getStatus(), duration);
    }
}
```

**Log levels strategy:**

| Level | Use for |
|-------|---------|
| ERROR | Unrecoverable failures, 5xx responses |
| WARN | Recoverable issues, deprecation usage, rate limits |
| INFO | Request/response, business events, state changes |
| DEBUG | Detailed flow, SQL queries (dev only) |
| TRACE | Full payload dumps (dev only, never production) |

---

## Decision Tables

### Controller Method → HTTP Verb + Status

| Operation | Verb | Success Status | Notes |
|-----------|------|---------------|-------|
| Get one | GET | 200 | Return `EntityModel<T>` |
| Get list | GET | 200 | Return `CollectionModel<EntityModel<T>>` |
| Create | POST | 201 | Return `EntityModel<T>` + `Location` header |
| Full update | PUT | 200 | Return `EntityModel<T>` |
| Partial update | PATCH | 200 | Return `EntityModel<T>` |
| Delete | DELETE | 204 | No body |

### Where to Put What

| Concern | Location |
|---------|----------|
| Validation annotations | Request DTO |
| OpenAPI annotations | Controller + DTOs |
| HATEOAS links | Controller or `RepresentationModelAssembler` |
| Error mapping | `@RestControllerAdvice` class |
| TraceId / MDC | Servlet filter |
| JSON log format | `logback-spring.xml` |
| API version config | `@RequestMapping` path prefix |

---

## Commands

```bash
# Add dependencies (Gradle)
./gradlew dependencies --configuration runtimeClasspath | grep -E "hateoas|springdoc|logstash"

# Run with specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'

# Access OpenAPI docs
curl http://localhost:8080/api-docs | jq .
curl http://localhost:8080/swagger-ui.html

# Test error handling
curl -s -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{}' | jq .

# Verify HATEOAS links in response
curl -s http://localhost:8080/api/v1/users/1 | jq '._links'

# Check structured logs
./gradlew bootRun 2>&1 | jq -R 'fromjson? // empty'
```

---

## Required Dependencies

**build.gradle.kts:**

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-hateoas")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.0")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    // Optional: MapStruct for DTO mapping
    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
}
```
