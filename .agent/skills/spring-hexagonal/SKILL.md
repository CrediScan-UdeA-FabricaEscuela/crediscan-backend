---
name: spring-hexagonal
description: >
  Guides Spring Boot project structure using Hexagonal/Clean Architecture.
  Trigger: When working on Spring Boot projects, creating new modules, defining package structure, or discussing architecture layers.
license: Apache-2.0
metadata:
  author: gentleman-programming
  version: "1.0"
---

## When to Use

- Creating a new Spring Boot project or module
- Defining package structure for a bounded context
- Adding new use cases, ports, or adapters
- Reviewing architecture layer boundaries
- Setting up multi-module Gradle/Maven projects
- Mapping code components to C4 diagrams

---

## Critical Patterns

### The Golden Rule

**Domain NEVER depends on infrastructure.** Dependencies flow INWARD:

```
Infrastructure → Application → Domain
     ↓                ↓
  (implements)    (defines ports)
```

- `domain/` has ZERO imports from Spring, JPA, or any framework
- `application/` defines port interfaces, domain calls domain
- `infrastructure/` implements ports and wires everything with Spring

### Layer Responsibilities

| Layer | Contains | Spring Annotations | Depends On |
|-------|----------|-------------------|------------|
| `domain` | Entities, Value Objects, Domain Services, Domain Events, Exceptions | NONE — pure Java | Nothing |
| `application` | Use Cases, Input/Output Ports, DTOs (command/query) | `@Service` on use cases | `domain` only |
| `infrastructure` | REST controllers, JPA repos, messaging, config | `@RestController`, `@Repository`, `@Component`, `@Configuration` | `application` + `domain` |
| `shared` | Cross-cutting: auth, audit, logging, error handling | `@Aspect`, `@ControllerAdvice`, filters | Any layer (sparingly) |

### Port & Adapter Pattern

**Ports** = interfaces defined in `application/port/`

| Port Type | Location | Purpose | Example |
|-----------|----------|---------|---------|
| Input (Driving) | `application/port/input/` | How the outside world calls us | `CreateOrderUseCase` |
| Output (Driven) | `application/port/output/` | How we call external systems | `OrderRepository`, `PaymentGateway` |

**Adapters** = implementations in `infrastructure/`

| Adapter Type | Location | Implements | Example |
|-------------|----------|------------|---------|
| REST API | `infrastructure/adapter/input/rest/` | Input ports (indirectly, via controller calling use case) | `OrderController` |
| Persistence | `infrastructure/adapter/output/persistence/` | Output ports | `JpaOrderRepository` |
| Messaging | `infrastructure/adapter/output/messaging/` | Output ports | `KafkaEventPublisher` |
| External API | `infrastructure/adapter/output/external/` | Output ports | `StripePaymentAdapter` |

---

## Package Structure

### Single Module (Small Project)

```
com.university.project/
├── domain/
│   ├── model/
│   │   ├── entity/              # Aggregates and entities
│   │   │   └── Order.java
│   │   └── valueobject/         # Value objects (immutable)
│   │       ├── OrderId.java
│   │       └── Money.java
│   ├── service/                 # Domain services (stateless logic involving multiple entities)
│   │   └── PricingService.java
│   ├── event/                   # Domain events
│   │   └── OrderCreatedEvent.java
│   └── exception/               # Domain-specific exceptions
│       └── InsufficientStockException.java
│
├── application/
│   ├── port/
│   │   ├── input/               # Input ports (use case interfaces)
│   │   │   └── CreateOrderUseCase.java
│   │   └── output/              # Output ports (repository/gateway interfaces)
│   │       ├── OrderRepository.java
│   │       └── PaymentGateway.java
│   ├── service/                 # Use case implementations
│   │   └── CreateOrderService.java
│   └── dto/                     # Application-level DTOs
│       ├── command/
│       │   └── CreateOrderCommand.java
│       └── query/
│           └── OrderResponse.java
│
├── infrastructure/
│   ├── adapter/
│   │   ├── input/
│   │   │   └── rest/
│   │   │       ├── OrderController.java
│   │   │       └── mapper/
│   │   │           └── OrderRestMapper.java
│   │   └── output/
│   │       ├── persistence/
│   │       │   ├── JpaOrderRepository.java
│   │       │   ├── entity/      # JPA entities (NOT domain entities)
│   │       │   │   └── OrderJpaEntity.java
│   │       │   └── mapper/
│   │       │       └── OrderPersistenceMapper.java
│   │       └── messaging/
│   │           └── KafkaEventPublisher.java
│   └── config/
│       ├── BeanConfiguration.java
│       ├── SecurityConfig.java
│       └── PersistenceConfig.java
│
└── shared/
    ├── exception/
    │   └── GlobalExceptionHandler.java
    ├── security/
    │   └── JwtAuthFilter.java
    └── audit/
        └── AuditAspect.java
```

### Multi-Module (Gradle)

```
project-root/
├── build.gradle                  # Root build file
├── settings.gradle
│
├── domain/                       # Module: pure domain
│   ├── build.gradle              # NO Spring dependencies
│   └── src/main/java/
│       └── com/university/project/domain/
│
├── application/                  # Module: use cases + ports
│   ├── build.gradle              # Depends on: domain
│   └── src/main/java/
│       └── com/university/project/application/
│
├── infrastructure/               # Module: adapters + config
│   ├── build.gradle              # Depends on: application, domain, spring-boot-starter-*
│   └── src/main/java/
│       └── com/university/project/infrastructure/
│
└── shared/                       # Module: cross-cutting concerns
    ├── build.gradle              # Depends on: domain (minimal)
    └── src/main/java/
        └── com/university/project/shared/
```

### Dependency Rules for Multi-Module

```groovy
// domain/build.gradle — NO framework dependencies
dependencies {
    // Only standard Java, maybe lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}

// application/build.gradle
dependencies {
    implementation project(':domain')
    // Spring context ONLY for @Service annotation
    implementation 'org.springframework:spring-context'
}

// infrastructure/build.gradle
dependencies {
    implementation project(':domain')
    implementation project(':application')
    implementation project(':shared')
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
}
```

---

## Decision Trees

### When to Use Modules vs Packages

| Criteria | Packages (single module) | Gradle/Maven modules |
|----------|------------------------|---------------------|
| Team size | 1-3 developers | 4+ developers |
| Bounded contexts | 1-2 | 3+ |
| Build time matters | No | Yes (parallel builds) |
| Enforce layer deps at compile time | No (discipline only) | Yes (Gradle enforces) |
| University project | **Recommended** — simpler | Only if evaluator requires it |

### When to Create a New Bounded Context

| Signal | Action |
|--------|--------|
| Entities share no fields | Separate bounded context |
| Teams work independently | Separate bounded context |
| Different deployment cadence | Separate module |
| Shared entity with same lifecycle | Same bounded context |

---

## Spring Boot Conventions

### Annotation Mapping

| Concept | Annotation | Location |
|---------|-----------|----------|
| Use case implementation | `@Service` | `application/service/` |
| Persistence adapter | `@Repository` | `infrastructure/adapter/output/persistence/` |
| REST adapter | `@RestController` | `infrastructure/adapter/input/rest/` |
| Configuration | `@Configuration` | `infrastructure/config/` |
| Cross-cutting | `@Aspect` / `@ControllerAdvice` | `shared/` |
| Bean wiring | `@Bean` in `@Configuration` | `infrastructure/config/BeanConfiguration.java` |

### Bean Wiring Strategy

Use explicit `@Configuration` classes in infrastructure to wire ports to adapters:

```java
@Configuration
public class BeanConfiguration {

    @Bean
    public CreateOrderUseCase createOrderUseCase(
            OrderRepository orderRepository,
            PaymentGateway paymentGateway) {
        return new CreateOrderService(orderRepository, paymentGateway);
    }
}
```

This keeps `CreateOrderService` free of `@Service` if you want PURE domain-driven wiring. For university projects, using `@Service` directly on the use case implementation is acceptable and simpler.

### Configuration Per Environment

```
src/main/resources/
├── application.yml                # Shared defaults
├── application-dev.yml            # Local development
├── application-test.yml           # Test environment
├── application-staging.yml        # Staging
└── application-prod.yml           # Production
```

Activate with: `spring.profiles.active=dev`

**Rule**: Secrets NEVER in YAML files. Use environment variables or Spring Cloud Config:

```yaml
# application-prod.yml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
```

---

## C4 Model Mapping

### How Hexagonal Maps to C4

| C4 Level | Hexagonal Concept | Example |
|----------|------------------|---------|
| **System Context** (L1) | Entire application + external systems | "Order System" talks to "Payment Provider" |
| **Container** (L2) | Deployable unit (Spring Boot app, DB, message broker) | Spring Boot API, PostgreSQL, Kafka |
| **Component** (L3) | Bounded context / module | Order module, Inventory module |
| **Code** (L4) | Ports, adapters, domain entities | `CreateOrderUseCase`, `JpaOrderRepository` |

### Component Diagram Guidelines

Each bounded context becomes a C4 Component containing:
- **Input adapters** → shown as API endpoints on the component boundary
- **Use cases** → internal component behavior
- **Output adapters** → connections to external containers (DB, queues)
- **Domain** → NOT shown individually (internal implementation detail)

---

## Code Examples

### Domain Entity (Pure Java, No Framework)

```java
// domain/model/entity/Order.java
public class Order {
    private final OrderId id;
    private final List<OrderLine> lines;
    private OrderStatus status;

    public Order(OrderId id, List<OrderLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one line");
        }
        this.id = id;
        this.lines = List.copyOf(lines);
        this.status = OrderStatus.CREATED;
    }

    public Money totalAmount() {
        return lines.stream()
            .map(OrderLine::subtotal)
            .reduce(Money.ZERO, Money::add);
    }

    public void confirm() {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException("Can only confirm CREATED orders");
        }
        this.status = OrderStatus.CONFIRMED;
    }
}
```

### Input Port (Interface)

```java
// application/port/input/CreateOrderUseCase.java
public interface CreateOrderUseCase {
    OrderResponse execute(CreateOrderCommand command);
}
```

### Output Port (Interface)

```java
// application/port/output/OrderRepository.java
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
}
```

### Use Case Implementation

```java
// application/service/CreateOrderService.java
@Service
@RequiredArgsConstructor
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;

    @Override
    @Transactional
    public OrderResponse execute(CreateOrderCommand command) {
        Order order = OrderMapper.toDomain(command);
        paymentGateway.authorize(order.totalAmount());
        Order saved = orderRepository.save(order);
        return OrderMapper.toResponse(saved);
    }
}
```

### Persistence Adapter

```java
// infrastructure/adapter/output/persistence/JpaOrderRepository.java
@Repository
@RequiredArgsConstructor
public class JpaOrderRepository implements OrderRepository {

    private final SpringDataOrderRepository springRepo;
    private final OrderPersistenceMapper mapper;

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = mapper.toJpa(order);
        OrderJpaEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return springRepo.findById(id.value())
            .map(mapper::toDomain);
    }
}
```

### REST Adapter

```java
// infrastructure/adapter/input/rest/OrderController.java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        CreateOrderCommand command = OrderRestMapper.toCommand(request);
        return createOrderUseCase.execute(command);
    }
}
```

---

## Anti-Patterns to AVOID

| Anti-Pattern | Why It's Wrong | Correct Approach |
|-------------|---------------|-----------------|
| JPA annotations in domain entities | Domain coupled to persistence framework | Separate JPA entities in `infrastructure/adapter/output/persistence/entity/` |
| `@Autowired` in domain | Framework dependency in domain | Constructor injection via `@Configuration` bean wiring |
| Use case returns JPA entity | Leaks infrastructure to API layer | Return application DTOs |
| Controller calls repository directly | Skips application layer, no use case | Controller → Use Case → Repository |
| Single `model/` package for everything | No layer separation | Split into `domain/model/`, `application/dto/`, `infrastructure/.../entity/` |
| Business logic in controller | Controller is an adapter, not business | Move logic to use case or domain service |

---

## Commands

```bash
# Generate Spring Boot project with Gradle
curl https://start.spring.io/starter.zip \
  -d type=gradle-project \
  -d language=java \
  -d javaVersion=21 \
  -d bootVersion=3.3.0 \
  -d groupId=com.university.project \
  -d artifactId=project-name \
  -d dependencies=web,data-jpa,validation,lombok,h2 \
  -o project.zip && unzip project.zip

# Run with specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'

# Run tests
./gradlew test

# Build without tests (for fast iteration)
./gradlew build -x test

# Check dependency tree (verify no domain→infrastructure leaks in multi-module)
./gradlew :domain:dependencies
```
