---
name: spring-docker-deploy
description: >
  Guides containerization and deployment for Spring Boot projects using Docker, Docker Compose, Render, GitHub Actions CI/CD, and Kubernetes.
  Trigger: When working on Dockerfiles, docker-compose, CI/CD pipelines, deployment configs, Render blueprints, or Kubernetes manifests for Spring Boot apps.
license: Apache-2.0
metadata:
  version: "1.0"
---

## When to Use

- Creating or modifying a Dockerfile for a Spring Boot project
- Setting up Docker Compose for local development (app + database)
- Configuring Render deployment (render.yaml blueprint)
- Building GitHub Actions CI/CD pipelines (build, test, deploy, SonarCloud)
- Writing Kubernetes manifests (Deployment, Service, Ingress, HPA)
- Configuring Spring Boot application profiles for containerized environments
- Setting up `.dockerignore` or `.env` files for containers

---

## Critical Patterns

### The Golden Rule

**Secrets NEVER in source code or images.** Use environment variables, `.env` files (gitignored), or secret managers. Never bake credentials into Docker images, YAML files, or GitHub Actions logs.

### Sprint Roadmap

| Sprint | Deliverables |
|--------|-------------|
| Sprint 1 | Dockerfile (multi-stage) + Docker Compose (app + PostgreSQL) + Render deployment |
| Sprint 2 | GitHub Actions CI/CD (build, test, Docker push, deploy, SonarCloud) |
| Sprint 3 | Kubernetes manifests (Deployment, Service, ConfigMap, Secret, HPA, Ingress) |

---

## Dockerfile (Multi-Stage Build)

### Pattern: Build + Runtime Stages

```dockerfile
# ---- Build stage ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Cache dependencies first (layer optimization)
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:resolve -B

# Copy source and build
COPY src src
RUN ./mvnw package -DskipTests -B

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy artifact from build stage
COPY --from=build /app/target/*.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Gradle Variant (Replace Build Stage)

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar -x test --no-daemon
```

For Gradle, adjust the runtime COPY line:

```dockerfile
COPY --from=build /app/build/libs/*.jar app.jar
```

### .dockerignore

```
.git
.gitignore
.idea
.vscode
*.md
target/
build/
!README.md
.env
*.log
node_modules/
.gradle/
```

### Decision: Maven vs Gradle Dockerfile

| Criteria | Maven | Gradle |
|----------|-------|--------|
| Project uses `pom.xml` | Use Maven variant | - |
| Project uses `build.gradle` | - | Use Gradle variant |
| Cache step | `dependency:resolve` | `dependencies` task |
| Output path | `target/*.jar` | `build/libs/*.jar` |

---

## Docker Compose

### Mandatory: Spring Boot + PostgreSQL

```yaml
services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: spring-app
    ports:
      - "${APP_PORT:-8080}:8080"
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
    depends_on:
      db:
        condition: service_healthy
    networks:
      - app-network
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 3s
      start_period: 40s
      retries: 3

  db:
    image: postgres:16-alpine
    container_name: spring-db
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "${DB_PORT:-5432}:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - app-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Optional: Redis for caching
  # redis:
  #   image: redis:7-alpine
  #   container_name: spring-redis
  #   ports:
  #     - "${REDIS_PORT:-6379}:6379"
  #   volumes:
  #     - redis-data:/data
  #   networks:
  #     - app-network
  #   healthcheck:
  #     test: ["CMD", "redis-cli", "ping"]
  #     interval: 10s
  #     timeout: 3s
  #     retries: 3

volumes:
  postgres-data:
  # redis-data:

networks:
  app-network:
    driver: bridge
```

### .env File (gitignored)

```env
# Application
APP_PORT=8080
SPRING_PROFILES_ACTIVE=dev

# PostgreSQL
POSTGRES_DB=myapp
POSTGRES_USER=myapp_user
POSTGRES_PASSWORD=change_me_in_production

# Optional
DB_PORT=5432
REDIS_PORT=6379
```

**Rule**: Add `.env` to `.gitignore`. Provide a `.env.example` with placeholder values committed to the repo.

---

## Render Deployment

### render.yaml Blueprint

```yaml
services:
  - type: web
    name: spring-app
    runtime: docker
    repo: https://github.com/YOUR_ORG/YOUR_REPO
    branch: main
    plan: free
    dockerfilePath: ./Dockerfile
    healthCheckPath: /actuator/health
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: prod
      - key: SPRING_DATASOURCE_URL
        fromDatabase:
          name: spring-db
          property: connectionURI
      - key: SPRING_DATASOURCE_USERNAME
        fromDatabase:
          name: spring-db
          property: user
      - key: SPRING_DATASOURCE_PASSWORD
        fromDatabase:
          name: spring-db
          property: password
      - key: JAVA_OPTS
        value: "-Xmx256m -Xms128m"

databases:
  - name: spring-db
    plan: free
    databaseName: myapp
    user: myapp_user
```

### Render Environment Variables

| Variable | Source | Notes |
|----------|--------|-------|
| `SPRING_PROFILES_ACTIVE` | Static value | Always `prod` on Render |
| `SPRING_DATASOURCE_URL` | `fromDatabase` | Auto-populated by Render |
| `SPRING_DATASOURCE_USERNAME` | `fromDatabase` | Auto-populated by Render |
| `SPRING_DATASOURCE_PASSWORD` | `fromDatabase` | Auto-populated by Render |
| `JAVA_OPTS` | Static value | Tune for Render free tier memory limits |

### Auto-Deploy

Render auto-deploys on push to the configured branch (`main`). No extra CI step needed for basic deployment, but GitHub Actions adds testing and quality gates before deploy.

---

## GitHub Actions CI/CD (Sprint 2)

### .github/workflows/ci.yml

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: testdb
          POSTGRES_USER: testuser
          POSTGRES_PASSWORD: testpass
        ports:
          - 5432:5432
        options: >-
          --health-cmd "pg_isready -U testuser -d testdb"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0  # Full history for SonarCloud

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'  # Change to 'gradle' for Gradle projects

      - name: Cache SonarCloud packages
        uses: actions/cache@v4
        with:
          path: ~/.sonar/cache
          key: ${{ runner.os }}-sonar
          restore-keys: ${{ runner.os }}-sonar

      - name: Build and Test
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/testdb
          SPRING_DATASOURCE_USERNAME: testuser
          SPRING_DATASOURCE_PASSWORD: testpass
        run: ./mvnw verify -B  # Use ./gradlew build for Gradle

      - name: SonarCloud Analysis
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: >
          ./mvnw sonar:sonar
          -Dsonar.projectKey=${{ vars.SONAR_PROJECT_KEY }}
          -Dsonar.organization=${{ vars.SONAR_ORG }}
          -Dsonar.host.url=https://sonarcloud.io

  docker-build-push:
    needs: build-and-test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'

    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}

      - name: Build and Push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: |
            ${{ secrets.DOCKERHUB_USERNAME }}/${{ vars.IMAGE_NAME }}:latest
            ${{ secrets.DOCKERHUB_USERNAME }}/${{ vars.IMAGE_NAME }}:${{ github.sha }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  deploy-render:
    needs: docker-build-push
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'

    steps:
      - name: Deploy to Render
        env:
          RENDER_DEPLOY_HOOK_URL: ${{ secrets.RENDER_DEPLOY_HOOK_URL }}
        run: curl -X POST "$RENDER_DEPLOY_HOOK_URL"
```

### Required GitHub Secrets

| Secret | Purpose |
|--------|---------|
| `SONAR_TOKEN` | SonarCloud authentication |
| `DOCKERHUB_USERNAME` | Docker Hub login |
| `DOCKERHUB_TOKEN` | Docker Hub access token |
| `RENDER_DEPLOY_HOOK_URL` | Render deploy hook (Settings > Deploy Hook) |

### Required GitHub Variables

| Variable | Purpose |
|----------|---------|
| `SONAR_PROJECT_KEY` | SonarCloud project identifier |
| `SONAR_ORG` | SonarCloud organization |
| `IMAGE_NAME` | Docker image name |

### Branch Protection Rules (Recommended)

Configure on `main` branch in GitHub Settings > Branches:

- Require pull request before merging
- Require status checks to pass: `build-and-test`
- Require branches to be up to date before merging
- Require conversation resolution before merging

---

## Kubernetes (Sprint 3)

### Deployment Manifest

```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: spring-app
  labels:
    app: spring-app
spec:
  replicas: 2
  selector:
    matchLabels:
      app: spring-app
  template:
    metadata:
      labels:
        app: spring-app
    spec:
      containers:
        - name: spring-app
          image: your-registry/spring-app:latest
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: spring-app-config
            - secretRef:
                name: spring-app-secret
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 15
          resources:
            requests:
              cpu: 250m
              memory: 512Mi
            limits:
              cpu: 500m
              memory: 1Gi
```

### Service Manifest

```yaml
# k8s/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: spring-app-service
spec:
  type: ClusterIP
  selector:
    app: spring-app
  ports:
    - port: 80
      targetPort: 8080
      protocol: TCP
```

### ConfigMap

```yaml
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: spring-app-config
data:
  SPRING_PROFILES_ACTIVE: "prod"
  SPRING_DATASOURCE_URL: "jdbc:postgresql://postgres-service:5432/myapp"
  SERVER_PORT: "8080"
```

### Secret

```yaml
# k8s/secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: spring-app-secret
type: Opaque
stringData:
  SPRING_DATASOURCE_USERNAME: "myapp_user"
  SPRING_DATASOURCE_PASSWORD: "change_me"
```

**Rule**: Never commit real secrets. Use `kubectl create secret` or a secrets manager (Sealed Secrets, External Secrets Operator). The YAML above is a template only.

### Horizontal Pod Autoscaler

```yaml
# k8s/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: spring-app-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: spring-app
  minReplicas: 2
  maxReplicas: 5
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

### Ingress

```yaml
# k8s/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: spring-app-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  ingressClassName: nginx
  rules:
    - host: api.yourdomain.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: spring-app-service
                port:
                  number: 80
```

### Local Development with Minikube

For local K8s development, use Minikube. For free cloud K8s, consider Civo (free trial) or Oracle Cloud free tier.

---

## Spring Boot Configuration

### application.yml (Shared Defaults)

```yaml
server:
  port: 8080

spring:
  application:
    name: my-spring-app
  jpa:
    open-in-view: false
    properties:
      hibernate:
        format_sql: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when_authorized
      probes:
        enabled: true
```

### application-dev.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/myapp
    username: myapp_user
    password: local_password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

logging:
  level:
    root: INFO
    com.university.project: DEBUG
    org.hibernate.SQL: DEBUG
```

### application-test.yml

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
```

### application-prod.yml

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

logging:
  level:
    root: WARN
    com.university.project: INFO
```

### Profile Activation

| Environment | How to Activate |
|------------|----------------|
| Local dev | `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` |
| Docker Compose | `SPRING_PROFILES_ACTIVE=dev` in `.env` |
| Render | `SPRING_PROFILES_ACTIVE=prod` in `render.yaml` |
| Kubernetes | `SPRING_PROFILES_ACTIVE=prod` in ConfigMap |
| Tests | `@ActiveProfiles("test")` or `application-test.yml` auto-detected |

---

## Anti-Patterns to AVOID

| Anti-Pattern | Why It's Wrong | Correct Approach |
|-------------|---------------|-----------------|
| Single-stage Dockerfile | Fat image with JDK + build tools + source code | Multi-stage: build with JDK, run with JRE |
| Running as root in container | Security vulnerability, violates principle of least privilege | `adduser` + `USER appuser` |
| Hardcoded DB credentials in Compose | Secrets in version control | `.env` file (gitignored) + `${VAR}` references |
| No health checks | Orchestrator cannot detect unhealthy containers | `HEALTHCHECK` in Dockerfile + health checks in Compose/K8s |
| `depends_on` without condition | App starts before DB is ready | `depends_on: db: condition: service_healthy` |
| `ddl-auto: update` in production | Schema changes without control, data loss risk | `ddl-auto: validate` + Flyway/Liquibase migrations |
| Secrets in ConfigMap | ConfigMaps are not encrypted | Use K8s Secrets (or Sealed Secrets for GitOps) |
| No resource limits in K8s | Pod can consume all node resources | Set `requests` and `limits` on every container |
| Skipping SonarCloud in CI | Quality gates not enforced | Add sonar step after build in GitHub Actions |

---

## Commands

```bash
# Build Docker image locally
docker build -t spring-app:latest .

# Run with Docker Compose
docker compose up -d

# Stop and remove containers
docker compose down

# Stop, remove containers AND volumes (destroys DB data)
docker compose down -v

# View logs
docker compose logs -f app

# Rebuild after code changes
docker compose up -d --build

# Render: create blueprint from render.yaml
# (push render.yaml to repo root, Render detects it automatically)

# Kubernetes: apply all manifests
kubectl apply -f k8s/

# Kubernetes: check pod status
kubectl get pods -l app=spring-app

# Kubernetes: view logs
kubectl logs -l app=spring-app --tail=100

# Kubernetes: port-forward for local testing
kubectl port-forward svc/spring-app-service 8080:80

# Minikube: start local cluster
minikube start --driver=docker

# Minikube: build image inside Minikube's Docker
eval $(minikube docker-env) && docker build -t spring-app:latest .
```

---

## Resources

- **Templates**: See [assets/](assets/) for Dockerfile, docker-compose.yml, render.yaml, and K8s manifest templates
