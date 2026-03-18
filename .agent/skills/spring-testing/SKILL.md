---
name: spring-testing
description: >
  Guides testing strategy for Spring Boot projects with unit, integration, BDD, and acceptance testing.
  Trigger: When writing tests, configuring test infrastructure, or reviewing test quality in Spring Boot projects.
license: Apache-2.0
metadata:
  author: gentleman-programming
  version: "1.0"
---

## When to Use

- Writing or reviewing unit tests for Spring Boot services/use cases
- Setting up integration tests with Testcontainers
- Creating BDD/Gherkin feature files and Cucumber step definitions
- Writing acceptance tests for REST APIs
- Configuring SonarCloud quality gates
- Organizing test structure in a Spring Boot project

---

## Critical Patterns

### Unit Testing — AAA Pattern (Mandatory)

Every unit test MUST follow Arrange-Act-Assert strictly. No mixing phases.

| Rule | Detail |
|------|--------|
| Framework | JUnit 5 + Mockito |
| Extension | `@ExtendWith(MockitoExtension.class)` on every unit test class |
| Naming | `should_expectedBehavior_when_condition()` |
| Isolation | Test ONE behavior per method. Mock ALL collaborators. |
| Coverage gate | **>= 40%** line coverage (mandatory quality gate) |
| No Spring context | Unit tests NEVER load Spring context. Use `@Mock` and `@InjectMocks`. |

```java
@ExtendWith(MockitoExtension.class)
class CreateOrderUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private CreateOrderUseCase useCase;

    @Test
    void should_createOrder_when_paymentIsValid() {
        // Arrange
        var command = new CreateOrderCommand("SKU-1", 2, "card-token");
        when(paymentGateway.charge(any())).thenReturn(PaymentResult.success());
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        var result = useCase.execute(command);

        // Assert
        assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void should_rejectOrder_when_paymentFails() {
        // Arrange
        var command = new CreateOrderCommand("SKU-1", 2, "bad-token");
        when(paymentGateway.charge(any())).thenReturn(PaymentResult.declined());

        // Act
        var result = useCase.execute(command);

        // Assert
        assertThat(result.status()).isEqualTo(OrderStatus.REJECTED);
        verify(orderRepository, never()).save(any());
    }
}
```

**Anti-patterns to reject:**
- `@SpringBootTest` in a unit test — too heavy, defeats isolation
- Multiple acts in one test — split into separate methods
- Assertions before the act phase — broken AAA structure
- Generic test names like `testCreateOrder()` — use `should_X_when_Y`

---

### Integration Testing

#### Repository Layer — `@DataJpaTest`

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class OrderRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void should_persistAndRetrieveOrder_when_savedToDatabase() {
        // Arrange
        var order = Order.create("SKU-1", 2, Money.of(29.99));

        // Act
        var saved = orderRepository.save(order);
        var found = orderRepository.findById(saved.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getSku()).isEqualTo("SKU-1");
    }
}
```

#### Controller Layer — `@WebMvcTest`

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateOrderUseCase createOrderUseCase;

    @Test
    void should_return201_when_orderCreatedSuccessfully() throws Exception {
        // Arrange
        var response = new OrderResponse("ORD-1", "CONFIRMED");
        when(createOrderUseCase.execute(any())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"sku": "SKU-1", "quantity": 2, "paymentToken": "tok_valid"}
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void should_return400_when_requestBodyInvalid() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
```

#### Full Integration — `@SpringBootTest` + Testcontainers

Use for end-to-end flows that cross multiple layers.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void should_createAndRetrieveOrder_when_fullFlowExecuted() {
        // Arrange
        var request = new CreateOrderRequest("SKU-1", 2, "tok_valid");

        // Act
        var createResponse = restTemplate.postForEntity("/api/orders", request, OrderResponse.class);

        // Assert
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody().status()).isEqualTo("CONFIRMED");
    }
}
```

**Testcontainers rules:**
- ALWAYS use `@ServiceConnection` (Spring Boot 3.1+) — eliminates manual `@DynamicPropertySource`
- Use `-alpine` images for faster CI
- Share containers across test classes with `static` + `@Container`
- NEVER use H2 as a substitute for PostgreSQL — behavior differs

---

### BDD / Gherkin (Mandatory)

#### Feature File Structure

Place feature files under `src/test/resources/features/`.

```gherkin
# src/test/resources/features/order/create-order.feature
Feature: Create Order
  As a customer
  I want to place an order
  So that I can purchase products

  Background:
    Given the product "SKU-1" exists with price $29.99

  Scenario: Successful order creation
    Given I have a valid payment token "tok_valid"
    When I create an order for 2 units of "SKU-1"
    Then the order status should be "CONFIRMED"
    And the total should be $59.98

  Scenario: Order rejected due to payment failure
    Given I have an invalid payment token "tok_declined"
    When I create an order for 1 unit of "SKU-1"
    Then the order status should be "REJECTED"
    And no order should be persisted

  Scenario Outline: Order with various quantities
    Given I have a valid payment token "tok_valid"
    When I create an order for <quantity> units of "SKU-1"
    Then the total should be <total>

    Examples:
      | quantity | total  |
      | 1        | $29.99 |
      | 3        | $89.97 |
      | 10       | $299.90 |
```

#### Cucumber Configuration

```java
// src/test/java/com/example/CucumberIntegrationTest.java
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:build/reports/cucumber.html")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.example.steps")
public class CucumberIntegrationTest {
}
```

#### Step Definitions Pattern

```java
// src/test/java/com/example/steps/OrderStepDefinitions.java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OrderStepDefinitions {

    @Autowired
    private TestRestTemplate restTemplate;

    private ResponseEntity<OrderResponse> lastResponse;
    private String paymentToken;

    @Given("the product {string} exists with price ${double}")
    public void theProductExistsWithPrice(String sku, double price) {
        // Seed test data
    }

    @Given("I have a valid payment token {string}")
    public void iHaveAValidPaymentToken(String token) {
        this.paymentToken = token;
    }

    @When("I create an order for {int} units of {string}")
    public void iCreateAnOrderFor(int quantity, String sku) {
        var request = new CreateOrderRequest(sku, quantity, paymentToken);
        lastResponse = restTemplate.postForEntity("/api/orders", request, OrderResponse.class);
    }

    @Then("the order status should be {string}")
    public void theOrderStatusShouldBe(String expectedStatus) {
        assertThat(lastResponse.getBody().status()).isEqualTo(expectedStatus);
    }
}
```

**Cucumber rules:**
- ONE `@CucumberContextConfiguration` class per test suite
- Step definitions grouped by domain (order steps, payment steps, etc.)
- Feature files organized by domain under `src/test/resources/features/{domain}/`
- Use `Background` for shared preconditions, not duplicate `Given` steps
- Scenario Outline for data-driven tests

#### Cucumber Dependencies (Gradle)

```groovy
testImplementation 'io.cucumber:cucumber-java:7.18+'
testImplementation 'io.cucumber:cucumber-spring:7.18+'
testImplementation 'io.cucumber:cucumber-junit-platform-engine:7.18+'
testImplementation 'org.junit.platform:junit-platform-suite'
```

---

### Acceptance Testing

Use REST-assured or MockMvc for backend-level acceptance tests.

#### REST-assured Pattern

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderAcceptanceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
    }

    @Test
    void should_completeOrderLifecycle_when_happyPath() {
        // Create order
        var orderId =
            given()
                .contentType(ContentType.JSON)
                .body("""
                    {"sku": "SKU-1", "quantity": 2, "paymentToken": "tok_valid"}
                    """)
            .when()
                .post("/orders")
            .then()
                .statusCode(201)
                .body("status", equalTo("CONFIRMED"))
                .extract().path("id");

        // Retrieve order
        given()
        .when()
            .get("/orders/{id}", orderId)
        .then()
            .statusCode(200)
            .body("id", equalTo(orderId))
            .body("status", equalTo("CONFIRMED"));
    }

    @Test
    void should_return404_when_orderDoesNotExist() {
        given()
        .when()
            .get("/orders/{id}", "non-existent-id")
        .then()
            .statusCode(404);
    }

    @Test
    void should_return400_when_quantityIsNegative() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"sku": "SKU-1", "quantity": -1, "paymentToken": "tok_valid"}
                """)
        .when()
            .post("/orders")
        .then()
            .statusCode(400);
    }
}
```

**Acceptance test rules:**
- Test complete user scenarios, not individual endpoints
- Cover happy path AND error scenarios (400, 404, 409, 422)
- Use real database via Testcontainers — no mocks at this level
- Keep tests independent — each test sets up its own data

---

### Quality Metrics — SonarCloud Quality Gates

| Metric | Threshold | How to Measure |
|--------|-----------|----------------|
| Line coverage | >= 40% | JaCoCo report → SonarCloud |
| Cyclomatic complexity | < 50 per method | SonarCloud "Complexity" tab |
| Cognitive complexity | Awareness (no hard gate) | SonarCloud "Cognitive Complexity" |
| Technical debt | <= 2 days | SonarCloud "Maintainability" |
| Critical vulnerabilities | 0 | SonarCloud "Security" tab |
| Code smells | Trend downward | SonarCloud "Maintainability" |

**Reducing complexity:**
- Extract complex conditionals into named methods
- Replace nested if/else with early returns (guard clauses)
- Use strategy pattern instead of switch/case on type
- Break methods longer than 20 lines into smaller units

**JaCoCo configuration (Gradle):**

```groovy
plugins {
    id 'jacoco'
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true  // Required for SonarCloud
        html.required = true
    }
}

jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.40
            }
        }
    }
}

test {
    finalizedBy jacocoTestReport
}

check {
    dependsOn jacocoTestCoverageVerification
}
```

**SonarCloud Gradle integration:**

```groovy
plugins {
    id 'org.sonarqube' version '5.0+'
}

sonar {
    properties {
        property 'sonar.projectKey', 'your-project-key'
        property 'sonar.organization', 'your-org'
        property 'sonar.host.url', 'https://sonarcloud.io'
        property 'sonar.coverage.jacoco.xmlReportPaths', 'build/reports/jacoco/test/jacocoTestReport.xml'
    }
}
```

---

### Test Organization

#### Directory Structure

```
src/
├── main/java/com/example/
│   ├── order/
│   │   ├── domain/
│   │   ├── application/
│   │   └── infrastructure/
│   └── ...
└── test/
    ├── java/com/example/
    │   ├── order/
    │   │   ├── domain/                    # Unit tests
    │   │   │   └── OrderTest.java
    │   │   ├── application/               # Unit tests (use cases)
    │   │   │   └── CreateOrderUseCaseTest.java
    │   │   └── infrastructure/
    │   │       ├── OrderRepositoryIntegrationTest.java
    │   │       └── OrderControllerTest.java
    │   ├── acceptance/                    # Acceptance tests
    │   │   └── OrderAcceptanceTest.java
    │   └── steps/                         # Cucumber step definitions
    │       └── OrderStepDefinitions.java
    └── resources/
        ├── application-test.yml           # Test profile
        └── features/                      # Gherkin feature files
            └── order/
                └── create-order.feature
```

#### Test Profile — `application-test.yml`

```yaml
spring:
  datasource:
    # Testcontainers handles this via @ServiceConnection
    # Only add overrides here if needed
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

#### Test Fixtures — Builder Pattern

```java
public class OrderFixture {

    public static Order.OrderBuilder aConfirmedOrder() {
        return Order.builder()
                .id(UUID.randomUUID())
                .sku("SKU-1")
                .quantity(2)
                .total(Money.of(59.98))
                .status(OrderStatus.CONFIRMED)
                .createdAt(Instant.now());
    }

    public static Order.OrderBuilder aPendingOrder() {
        return aConfirmedOrder()
                .status(OrderStatus.PENDING)
                .total(Money.ZERO);
    }
}

// Usage in tests:
var order = OrderFixture.aConfirmedOrder().sku("SKU-99").build();
```

#### `@TestConfiguration` for Test-Specific Beans

```java
@TestConfiguration
public class TestClockConfig {

    @Bean
    @Primary
    public Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneId.of("UTC"));
    }
}

// Import in test:
@SpringBootTest
@Import(TestClockConfig.class)
class TimeBasedFeatureTest { ... }
```

---

## Test Type Decision Tree

| Question | Yes | No |
|----------|-----|----|
| Testing a single class in isolation? | Unit test (`@ExtendWith(MockitoExtension.class)`) | Continue |
| Testing DB queries/mappings? | `@DataJpaTest` + Testcontainers | Continue |
| Testing controller routing/validation? | `@WebMvcTest` | Continue |
| Testing cross-layer flow? | `@SpringBootTest` + Testcontainers | Continue |
| Testing user scenario end-to-end? | Acceptance test (REST-assured) | Continue |
| Validating business rules in domain language? | BDD (Cucumber + Gherkin) | Unit test |

---

## Commands

```bash
# Run all tests
./gradlew test

# Run only unit tests (by convention: no Spring context)
./gradlew test --tests "*Test" --exclude-task integrationTest

# Run integration tests
./gradlew test --tests "*IntegrationTest"

# Run Cucumber BDD tests
./gradlew test --tests "CucumberIntegrationTest"

# Run acceptance tests
./gradlew test --tests "*AcceptanceTest"

# Generate coverage report
./gradlew jacocoTestReport

# Check coverage gate
./gradlew jacocoTestCoverageVerification

# Run SonarCloud analysis
./gradlew sonar -Dsonar.token=$SONAR_TOKEN

# Run tests with verbose output
./gradlew test --info
```

---

## Resources

- **Templates**: See test class examples above for each test type
- **Test profile**: `src/test/resources/application-test.yml`
- **Feature files**: `src/test/resources/features/{domain}/`
