---
name: spring-persistence
description: >
  Guides database persistence and data modeling with Spring Boot 3.x, Spring Data JPA, PostgreSQL, and Flyway.
  Trigger: When working with JPA entities, repositories, database migrations, queries, stored procedures, or data modeling in Spring Boot.
license: Apache-2.0
metadata:
  author: gentleman-programming
  version: "1.0"
---

## When to Use

- Designing or modifying JPA entities and relationships
- Writing repository queries (JPQL, native SQL, Specifications)
- Setting up or modifying database migrations (Flyway)
- Implementing auditing on entities
- Creating stored procedures or calling them from JPA
- Adding database indexes for performance
- Configuring Spring Cache with Redis (optional layer)

## Tech Stack

| Component | Version / Tool |
|-----------|---------------|
| Java | 21+ |
| Spring Boot | 3.x |
| Spring Data JPA | Latest compatible |
| Database | PostgreSQL (recommended) |
| Migrations | Flyway |
| Cache (optional) | Redis + Spring Cache |

---

## Critical Patterns

### 1. Data Modeling — Entity Design

Every persistent entity MUST follow this structure:

```java
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_customer_id", columnList = "customer_id"),
    @Index(name = "idx_orders_status_created", columnList = "status, created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Embedded
    private Money totalAmount;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "last_modified_at")
    private Instant lastModifiedAt;

    // protected no-arg constructor for JPA
    protected Order() {}
}
```

#### Entity Rules

| Rule | Detail |
|------|--------|
| Table naming | Plural snake_case: `orders`, `order_items` |
| Column naming | snake_case via `spring.jpa.hibernate.naming.physical-strategy` or explicit `@Column(name = ...)` |
| IDs | `@GeneratedValue(strategy = GenerationType.IDENTITY)` for PostgreSQL serial/bigserial |
| No-arg constructor | `protected` — required by JPA, prevents accidental public instantiation |
| Fetch strategy | ALWAYS `FetchType.LAZY` on `@ManyToOne` and `@OneToMany`. Never rely on EAGER defaults. |
| Bidirectional sync | Owner side manages the relationship. Add helper methods `addItem()` / `removeItem()` that sync both sides. |
| `equals` / `hashCode` | Base on `@Id` or a natural business key. NEVER use all fields. |

#### Relationships Decision Table

| Relationship | Annotation | Fetch | Cascade | Notes |
|-------------|-----------|-------|---------|-------|
| Parent → Children | `@OneToMany(mappedBy, cascade=ALL, orphanRemoval=true)` | LAZY | ALL | Owner = child side |
| Child → Parent | `@ManyToOne(fetch=LAZY, optional=false)` | LAZY | NONE | `@JoinColumn` here |
| Many-to-Many | `@ManyToMany` + `@JoinTable` | LAZY | PERSIST, MERGE | Use `Set`, not `List` |
| Lookup/Enum table | `@ManyToOne(fetch=LAZY)` | LAZY | NONE | Read-only reference |

#### Embeddable Value Objects

Use `@Embeddable` for value objects that have no identity of their own:

```java
@Embeddable
public class Money {

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    protected Money() {}

    public Money(BigDecimal amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }
}
```

Use for: addresses, monetary values, date ranges, coordinates — anything that is defined by its attributes, not by an ID.

#### Normalized Model Checklist

- [ ] Every entity has a single-column surrogate primary key (`@Id`)
- [ ] No repeated groups — extract into child entities
- [ ] No transitive dependencies — every non-key column depends on the whole PK
- [ ] Reference data in separate lookup tables with FK constraints
- [ ] Use `@Embedded` for value objects, NOT separate tables

---

### 2. Auditing

#### Setup

Enable JPA auditing in configuration:

```java
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .or(() -> Optional.of("system"));
    }
}
```

#### Audit Base Entity

Extract audit fields into a mapped superclass:

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "last_modified_at")
    private Instant lastModifiedAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false, length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;
}
```

Entities extend `AuditableEntity` to get automatic audit fields.

#### Audit Trail Table for Critical Operations

For operations that require a full history (financial, compliance), create a dedicated audit trail:

```java
@Entity
@Table(name = "audit_trail", indexes = {
    @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
public class AuditTrailEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "action", nullable = false, length = 20)
    private String action; // CREATE, UPDATE, DELETE

    @Column(name = "changed_fields", columnDefinition = "jsonb")
    private String changedFields; // JSON diff of what changed

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
}
```

Populate via JPA `@EntityListeners` or Spring `@EventListener` / `ApplicationEventPublisher`.

---

### 3. Indexes

#### When to Create Indexes

| Create index when | Do NOT index when |
|-------------------|-------------------|
| Column used in `WHERE` clauses frequently | Table has < 1000 rows |
| Column used in `JOIN` conditions | Column has very low cardinality (boolean) |
| Column used in `ORDER BY` | Column is updated very frequently (write-heavy) |
| Unique business constraint needed | You already have a covering index |

#### Declaring Indexes

On the entity via `@Table`:

```java
@Table(name = "products", indexes = {
    @Index(name = "idx_products_sku", columnList = "sku", unique = true),
    @Index(name = "idx_products_category_price", columnList = "category_id, price")
})
```

Or in a Flyway migration for more control:

```sql
CREATE INDEX CONCURRENTLY idx_products_category_price
    ON products (category_id, price);
```

#### Index Naming Convention

`idx_{table}_{columns}` — e.g., `idx_orders_customer_id`, `idx_products_sku`.

---

### 4. Non-trivial Queries

#### Derived Query Methods

Only for simple cases (1-2 conditions):

```java
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatusAndCustomerId(String status, Long customerId);
    Optional<Order> findByOrderNumber(String orderNumber);
}
```

#### JPQL with `@Query`

For joins, aggregations, and compound filters:

```java
@Query("""
    SELECT o FROM Order o
    JOIN FETCH o.customer c
    JOIN FETCH o.items i
    WHERE o.status = :status
      AND o.createdAt >= :since
    ORDER BY o.createdAt DESC
    """)
List<Order> findRecentOrdersByStatus(
    @Param("status") String status,
    @Param("since") Instant since
);

@Query("""
    SELECT new com.example.dto.CustomerOrderSummary(
        c.id, c.name, COUNT(o), SUM(o.totalAmount.amount)
    )
    FROM Customer c
    LEFT JOIN c.orders o
    WHERE o.createdAt BETWEEN :start AND :end
    GROUP BY c.id, c.name
    HAVING COUNT(o) > :minOrders
    """)
List<CustomerOrderSummary> getCustomerOrderSummaries(
    @Param("start") Instant start,
    @Param("end") Instant end,
    @Param("minOrders") long minOrders
);
```

#### Native SQL

When JPQL is insufficient (PostgreSQL-specific functions, CTEs, window functions):

```java
@Query(value = """
    WITH ranked AS (
        SELECT o.*, ROW_NUMBER() OVER (
            PARTITION BY o.customer_id ORDER BY o.created_at DESC
        ) AS rn
        FROM orders o
        WHERE o.status = :status
    )
    SELECT * FROM ranked WHERE rn = 1
    """, nativeQuery = true)
List<Order> findLatestOrderPerCustomerByStatus(@Param("status") String status);
```

#### Specifications Pattern for Dynamic Queries

```java
public class OrderSpecifications {

    public static Specification<Order> hasStatus(String status) {
        return (root, query, cb) ->
            status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Order> createdAfter(Instant date) {
        return (root, query, cb) ->
            date == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), date);
    }

    public static Specification<Order> customerNameContains(String name) {
        return (root, query, cb) -> {
            if (name == null) return null;
            Join<Order, Customer> customer = root.join("customer");
            return cb.like(cb.lower(customer.get("name")), "%" + name.toLowerCase() + "%");
        };
    }
}
```

Usage in service:

```java
Specification<Order> spec = Specification
    .where(OrderSpecifications.hasStatus(filter.getStatus()))
    .and(OrderSpecifications.createdAfter(filter.getSince()))
    .and(OrderSpecifications.customerNameContains(filter.getCustomerName()));

Page<Order> results = orderRepository.findAll(spec, pageable);
```

Repository must extend `JpaSpecificationExecutor<Order>`.

#### Projections and DTOs

Use interface-based projections for read-only views:

```java
public interface OrderSummaryProjection {
    Long getId();
    String getStatus();
    @Value("#{target.customer.name}")
    String getCustomerName();
    Instant getCreatedAt();
}

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<OrderSummaryProjection> findByStatus(String status);
}
```

Use constructor-based DTOs in `@Query` for aggregations (see JPQL example above with `SELECT new`).

---

### 5. Stored Procedures

#### Migration Script (Flyway)

Create `V3__add_recalculate_customer_totals_procedure.sql`:

```sql
CREATE OR REPLACE FUNCTION recalculate_customer_totals(p_customer_id BIGINT)
RETURNS TABLE(total_orders BIGINT, total_amount NUMERIC)
LANGUAGE plpgsql AS $$
BEGIN
    RETURN QUERY
    SELECT
        COUNT(o.id) AS total_orders,
        COALESCE(SUM(o.amount), 0) AS total_amount
    FROM orders o
    WHERE o.customer_id = p_customer_id
      AND o.status != 'CANCELLED';
END;
$$;
```

#### Calling from JPA — Option A: `@Procedure`

```java
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Procedure(name = "recalculate_customer_totals")
    Object[] recalculateCustomerTotals(@Param("p_customer_id") Long customerId);
}
```

With `@NamedStoredProcedureQuery` on the entity:

```java
@Entity
@NamedStoredProcedureQuery(
    name = "recalculate_customer_totals",
    procedureName = "recalculate_customer_totals",
    parameters = {
        @StoredProcedureParameter(
            name = "p_customer_id",
            type = Long.class,
            mode = ParameterMode.IN
        )
    }
)
public class Customer { ... }
```

#### Calling from JPA — Option B: `EntityManager`

For more control or complex return types:

```java
@Repository
public class CustomerRepositoryCustomImpl {

    @PersistenceContext
    private EntityManager em;

    public CustomerTotals recalculateCustomerTotals(Long customerId) {
        StoredProcedureQuery query = em.createStoredProcedureQuery("recalculate_customer_totals");
        query.registerStoredProcedureParameter("p_customer_id", Long.class, ParameterMode.IN);
        query.setParameter("p_customer_id", customerId);
        query.execute();

        Object[] result = (Object[]) query.getSingleResult();
        return new CustomerTotals((Long) result[0], (BigDecimal) result[1]);
    }
}
```

---

### 6. Database Migrations (Flyway)

#### Setup

`application.yml`:

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
  jpa:
    hibernate:
      ddl-auto: validate  # NEVER use update/create in production
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

#### Migration Naming Convention

```
V{version}__{description}.sql    → Versioned (schema changes)
R__{description}.sql              → Repeatable (views, functions, procs)
```

Examples:
```
V1__create_customers_table.sql
V2__create_orders_and_items_tables.sql
V3__add_recalculate_customer_totals_procedure.sql
V4__add_audit_trail_table.sql
V5__seed_reference_data.sql
R__update_customer_summary_view.sql
```

#### Sample Migration — `V1__create_customers_table.sql`

```sql
CREATE TABLE customers (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200)  NOT NULL,
    email       VARCHAR(255)  NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    last_modified_at TIMESTAMPTZ,
    created_by  VARCHAR(100)  NOT NULL DEFAULT 'system',
    last_modified_by VARCHAR(100)
);

CREATE INDEX idx_customers_email ON customers (email);
```

#### Sample Migration — `V2__create_orders_and_items_tables.sql`

```sql
CREATE TABLE orders (
    id            BIGSERIAL PRIMARY KEY,
    order_number  VARCHAR(50)  NOT NULL UNIQUE,
    status        VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    customer_id   BIGINT       NOT NULL REFERENCES customers(id),
    amount        NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency      VARCHAR(3)    NOT NULL DEFAULT 'USD',
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    last_modified_at TIMESTAMPTZ,
    created_by    VARCHAR(100)  NOT NULL DEFAULT 'system',
    last_modified_by VARCHAR(100)
);

CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_orders_status_created ON orders (status, created_at);

CREATE TABLE order_items (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT       NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_name VARCHAR(200) NOT NULL,
    quantity    INT           NOT NULL CHECK (quantity > 0),
    unit_price  NUMERIC(19,4) NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
```

#### Seed Data — `V5__seed_reference_data.sql`

```sql
INSERT INTO customers (name, email, created_by) VALUES
    ('Acme Corp', 'acme@example.com', 'seed'),
    ('Globex Inc', 'globex@example.com', 'seed')
ON CONFLICT (email) DO NOTHING;
```

#### Flyway Rules

| Rule | Detail |
|------|--------|
| NEVER edit a versioned migration after it has been applied | Create a new migration instead |
| Use `TIMESTAMPTZ` for all timestamps | PostgreSQL best practice |
| Always add `NOT NULL` unless the column is genuinely optional | Prevents null bugs |
| Include `ON DELETE CASCADE` or `ON DELETE SET NULL` explicitly | Don't rely on defaults |
| Use `BIGSERIAL` for PKs | Future-proof for high-volume tables |

---

### 7. Spring Cache with Redis (OPTIONAL)

> This section is **optional**. Implement only when read-heavy endpoints need caching.

#### Setup

```yaml
spring:
  cache:
    type: redis
  data:
    redis:
      host: localhost
      port: 6379
```

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeValuesWith(
                SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
            );
    }
}
```

#### Usage

```java
@Cacheable(value = "customers", key = "#id")
public CustomerDTO findById(Long id) { ... }

@CacheEvict(value = "customers", key = "#id")
public void update(Long id, UpdateCustomerRequest request) { ... }

@CacheEvict(value = "customers", allEntries = true)
public void rebuildAllCustomers() { ... }
```

#### Cache Decision Table

| Use cache | Do NOT cache |
|-----------|-------------|
| Read-heavy, rarely changing data | Frequently updated data |
| Expensive queries (aggregations, joins) | Real-time consistency required |
| Reference/lookup data | User-specific sensitive data |

---

## Commands

```bash
# Generate Flyway migration timestamp
date +%Y%m%d%H%M%S

# Run Flyway migrations manually
./mvnw flyway:migrate

# Check Flyway migration status
./mvnw flyway:info

# Clean Flyway (DEVELOPMENT ONLY — destroys all objects)
./mvnw flyway:clean

# Run application with PostgreSQL
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Validate that entities match the schema
./mvnw spring-boot:run  # ddl-auto=validate will fail on mismatch
```

## Resources

- **Templates**: See [assets/](assets/) for sample migration scripts and entity templates
