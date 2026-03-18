---
name: spring-observability
description: >
  Guides observability setup for Spring Boot projects: structured logging, metrics, monitoring, health checks, and distributed tracing.
  Trigger: When setting up logging, metrics, Prometheus, Grafana, health checks, tracing, or observability in a Spring Boot project.
license: Apache-2.0
metadata:
  author: gentleman-programming
  version: "1.0"
allowed-tools: Read, Edit, Write, Glob, Grep, Bash
---

## When to Use

- Setting up structured logging with JSON output
- Adding metrics collection with Micrometer + Prometheus
- Configuring Grafana dashboards for Spring Boot services
- Implementing health checks for Kubernetes readiness/liveness
- Adding distributed tracing across microservices
- Creating a local observability stack with Docker Compose

## Critical Patterns

### Dependencies (build.gradle.kts)

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    // Distributed tracing (optional — pick ONE)
    implementation("io.micrometer:micrometer-tracing-bridge-otel") // OpenTelemetry
    implementation("io.opentelemetry:opentelemetry-exporter-zipkin")
}
```

For Maven (`pom.xml`):

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
    <dependency>
        <groupId>net.logstash.logback</groupId>
        <artifactId>logstash-logback-encoder</artifactId>
        <version>8.0</version>
    </dependency>
    <!-- Distributed tracing (optional) -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-tracing-bridge-otel</artifactId>
    </dependency>
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-exporter-zipkin</artifactId>
    </dependency>
</dependencies>
```

### application.yml — Actuator & Metrics

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true  # Kubernetes liveness/readiness
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true
  tracing:
    sampling:
      probability: 1.0  # 1.0 for dev, lower in prod
```

---

## 1. Structured Logging

### Log Level Strategy

| Level | Use For | Example |
|-------|---------|---------|
| `ERROR` | Unrecoverable failures, exceptions that need immediate attention | Payment gateway timeout, DB connection lost |
| `WARN` | Recoverable issues, degraded behavior | Cache miss fallback, retry succeeded |
| `INFO` | Business events, state transitions | Order placed, user registered, deployment started |
| `DEBUG` | Development details, method entry/exit | Query params, intermediate calculations |

### Logback Configuration (`src/main/resources/logback-spring.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProfile name="default,dev">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} [%thread] [%X{traceId:-}] [%X{userId:-}] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <springProfile name="prod,staging">
        <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <includeMdcKeyName>traceId</includeMdcKeyName>
                <includeMdcKeyName>userId</includeMdcKeyName>
                <includeMdcKeyName>requestId</includeMdcKeyName>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="JSON"/>
        </root>
    </springProfile>
</configuration>
```

### SLF4J Usage — DO and DON'T

```java
// GOOD — parameterized logging (no string concatenation)
log.info("Order created: orderId={}, userId={}, total={}", orderId, userId, total);

// GOOD — structured key-value via MDC
MDC.put("orderId", orderId);
log.info("Order payment processed");
MDC.remove("orderId");

// BAD — string concatenation (always evaluated, even if level disabled)
log.debug("Processing order " + orderId + " for user " + userId);

// BAD — logging sensitive data
log.info("User login: email={}, password={}", email, password);
```

### MDC Filter (traceId, userId, requestId)

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestId = Optional.ofNullable(request.getHeader("X-Request-Id"))
                    .orElse(UUID.randomUUID().toString());
            MDC.put("requestId", requestId);
            MDC.put("traceId", Optional.ofNullable(MDC.get("traceId")).orElse(requestId));

            // userId is set after authentication — see SecurityContextMdcFilter below
            response.setHeader("X-Request-Id", requestId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

### Security Context MDC Filter (userId after auth)

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)  // After security filter chain
public class SecurityContextMdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            MDC.put("userId", auth.getName());
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("userId");
        }
    }
}
```

### Request/Response Logging Filter with Sensitive Data Masking

```java
@Component
@Slf4j
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "cookie", "x-api-key");
    private static final Pattern SENSITIVE_BODY_PATTERN =
            Pattern.compile("(\"(?:password|secret|token|creditCard)\"\\s*:\\s*)\"[^\"]*\"");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long start = System.nanoTime();
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            logRequest(wrappedRequest);
            logResponse(wrappedResponse, durationMs);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String body = maskSensitiveData(new String(request.getContentAsByteArray(), StandardCharsets.UTF_8));
        log.info("HTTP Request: method={}, uri={}, body={}", request.getMethod(), request.getRequestURI(), body);
    }

    private void logResponse(ContentCachingResponseWrapper response, long durationMs) {
        log.info("HTTP Response: status={}, durationMs={}", response.getStatus(), durationMs);
    }

    private String maskSensitiveData(String body) {
        if (body == null || body.isBlank()) return "";
        return SENSITIVE_BODY_PATTERN.matcher(body).replaceAll("$1\"***MASKED***\"");
    }
}
```

### Security Event Logging

```java
@Component
@Slf4j
public class SecurityEventLogger {

    public void logFailedLogin(String username, String reason) {
        log.warn("SECURITY: Failed login attempt: username={}, reason={}", username, reason);
    }

    public void logSuccessfulLogin(String username) {
        log.info("SECURITY: Successful login: username={}", username);
    }

    public void logCriticalChange(String userId, String resource, String action) {
        log.info("SECURITY: Critical change: userId={}, resource={}, action={}", userId, resource, action);
    }

    public void logStateTransition(String entityType, String entityId, String from, String to) {
        log.info("STATE: Transition: entityType={}, entityId={}, from={}, to={}", entityType, entityId, from, to);
    }

    public void logAccessDenied(String userId, String resource) {
        log.warn("SECURITY: Access denied: userId={}, resource={}", userId, resource);
    }
}
```

---

## 2. Metrics with Micrometer + Prometheus

### Metric Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Counter | `{domain}.{action}.total` | `orders.created.total` |
| Timer | `{domain}.{operation}.duration` | `payment.processing.duration` |
| Gauge | `{domain}.{resource}.current` | `queue.messages.current` |
| Distribution | `{domain}.{metric}.distribution` | `response.size.distribution` |

**Rules:**
- Use dots as separators (Micrometer normalizes per registry)
- Lowercase only
- Tag for dimensions: `status`, `type`, `endpoint` — NOT in the name

### Custom Business Metrics

```java
@Component
public class BusinessMetrics {

    private final Counter ordersCreated;
    private final Counter ordersFailed;
    private final Counter authFailures;
    private final Timer orderProcessingTime;
    private final AtomicInteger activeUsers;

    public BusinessMetrics(MeterRegistry registry) {
        this.ordersCreated = Counter.builder("orders.created.total")
                .description("Total orders created")
                .tag("source", "api")
                .register(registry);

        this.ordersFailed = Counter.builder("orders.failed.total")
                .description("Total failed order attempts")
                .register(registry);

        this.authFailures = Counter.builder("auth.failures.total")
                .description("Authentication failures")
                .tag("type", "login")
                .register(registry);

        this.orderProcessingTime = Timer.builder("orders.processing.duration")
                .description("Time to process an order")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        this.activeUsers = new AtomicInteger(0);
        Gauge.builder("users.active.current", activeUsers, AtomicInteger::get)
                .description("Currently active users")
                .register(registry);
    }

    public void recordOrderCreated() { ordersCreated.increment(); }
    public void recordOrderFailed() { ordersFailed.increment(); }
    public void recordAuthFailure() { authFailures.increment(); }
    public Timer.Sample startOrderTimer() { return Timer.start(); }
    public void stopOrderTimer(Timer.Sample sample) { sample.stop(orderProcessingTime); }
    public void userConnected() { activeUsers.incrementAndGet(); }
    public void userDisconnected() { activeUsers.decrementAndGet(); }
}
```

### Using Metrics in Services

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final BusinessMetrics metrics;

    public Order createOrder(CreateOrderCommand command) {
        Timer.Sample sample = metrics.startOrderTimer();
        try {
            Order order = processOrder(command);
            metrics.recordOrderCreated();
            return order;
        } catch (Exception e) {
            metrics.recordOrderFailed();
            throw e;
        } finally {
            metrics.stopOrderTimer(sample);
        }
    }
}
```

### MeterBinder for Reusable Metric Groups

```java
@Component
public class CacheMetrics implements MeterBinder {

    private final CacheManager cacheManager;

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("cache.size", cacheManager, cm -> cm.getCacheNames().size())
                .description("Number of active caches")
                .register(registry);
    }
}
```

---

## 3. Health Checks

### Custom Health Indicator

```java
@Component
public class ExternalApiHealthIndicator implements HealthIndicator {

    private final RestClient restClient;

    public ExternalApiHealthIndicator(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://api.external.com").build();
    }

    @Override
    public Health health() {
        try {
            restClient.get().uri("/health").retrieve().toBodilessEntity();
            return Health.up()
                    .withDetail("service", "external-api")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("service", "external-api")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
```

### Database Health Check

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Health.up().withDetail("database", "reachable").build();
        } catch (Exception e) {
            return Health.down().withDetail("database", "unreachable")
                    .withException(e).build();
        }
    }
}
```

### Kubernetes Probes Configuration

In `application.yml` (already included above with `probes.enabled: true`), Spring Boot auto-registers:
- `/actuator/health/liveness` — app is running
- `/actuator/health/readiness` — app can accept traffic

Kubernetes deployment snippet:

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
```

### Custom Readiness Contributor

```java
@Component
public class WarmupReadinessIndicator implements HealthIndicator, ApplicationListener<ApplicationReadyEvent> {

    private volatile boolean ready = false;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        this.ready = true;
    }

    @Override
    public Health health() {
        return ready ? Health.up().build()
                     : Health.down().withDetail("reason", "warmup incomplete").build();
    }
}
```

---

## 4. Distributed Tracing

### Configuration (application.yml)

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 0.1 for 10% sampling in prod
    propagation:
      type: w3c  # or b3 for Zipkin native
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

### Trace Propagation Across Services

Spring Boot 3 auto-propagates `traceparent` headers via `RestClient`, `WebClient`, and `RestTemplate` when tracing is on the classpath.

For custom propagation:

```java
@Bean
public RestClient restClient(RestClient.Builder builder) {
    // Tracing auto-configured — instrumented automatically
    return builder.baseUrl("http://other-service:8080").build();
}
```

### Correlation IDs in Logs

When Micrometer Tracing is on the classpath, `traceId` and `spanId` are auto-injected into MDC. Logback pattern:

```xml
<pattern>%d{HH:mm:ss.SSS} [%thread] [%X{traceId:-}] [%X{spanId:-}] %-5level %logger{36} - %msg%n</pattern>
```

---

## 5. Grafana Dashboards

### Dashboard Provisioning Structure

```
docker/grafana/
├── provisioning/
│   ├── dashboards/
│   │   └── dashboards.yml
│   └── datasources/
│       └── datasources.yml
└── dashboards/
    ├── spring-boot-overview.json
    └── business-metrics.json
```

### Datasource Provisioning (`datasources.yml`)

```yaml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
```

### Dashboard Provisioning (`dashboards.yml`)

```yaml
apiVersion: 1
providers:
  - name: default
    folder: Spring Boot
    type: file
    options:
      path: /var/lib/grafana/dashboards
```

### Key Panels (PromQL Queries)

| Panel | Query |
|-------|-------|
| Request Rate | `rate(http_server_requests_seconds_count{application="$app"}[5m])` |
| Error Rate (%) | `100 * rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m])` |
| P95 Latency | `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{application="$app"}[5m]))` |
| P99 Latency | `histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{application="$app"}[5m]))` |
| JVM Heap Used | `jvm_memory_used_bytes{area="heap", application="$app"}` |
| JVM Threads | `jvm_threads_live_threads{application="$app"}` |
| Orders Created/min | `rate(orders_created_total{application="$app"}[5m]) * 60` |
| Auth Failures/min | `rate(auth_failures_total{application="$app"}[5m]) * 60` |
| Active Users | `users_active_current{application="$app"}` |

### Alert Rules

```yaml
# In Grafana alert rule or Prometheus alerting rules
groups:
  - name: spring-boot-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate (>5%) on {{ $labels.application }}"

      - alert: HighLatency
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "P95 latency >2s on {{ $labels.application }}"

      - alert: JvmHeapHigh
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "JVM heap usage >85% on {{ $labels.application }}"
```

---

## 6. Docker Compose — Observability Stack

See [assets/docker-compose.observability.yml](assets/docker-compose.observability.yml) for the full compose file.

See [assets/prometheus.yml](assets/prometheus.yml) for Prometheus scrape configuration.

---

## Commands

```bash
# Start observability stack
docker compose -f docker-compose.observability.yml up -d

# Access endpoints
# Prometheus:  http://localhost:9090
# Grafana:     http://localhost:3000 (admin/admin)
# Zipkin:      http://localhost:9411
# App metrics: http://localhost:8080/actuator/prometheus
# App health:  http://localhost:8080/actuator/health

# Test metrics endpoint
curl -s http://localhost:8080/actuator/prometheus | grep orders_created

# Test health checks
curl -s http://localhost:8080/actuator/health | jq .
curl -s http://localhost:8080/actuator/health/liveness | jq .
curl -s http://localhost:8080/actuator/health/readiness | jq .
```

## Resources

- **Templates**: See [assets/](assets/) for Docker Compose, Prometheus config, and Grafana dashboard JSON
