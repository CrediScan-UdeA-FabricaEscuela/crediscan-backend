plugins {
    java
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.sonarqube") version "6.0.1.5171"
    jacoco
}

group = "co.udea.codefactory"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

val archunitVersion = "1.3.0"
val jjwtVersion = "0.12.6"
val mapstructVersion = "1.6.3"
val springdocVersion = "2.8.4"
val logstashEncoderVersion = "8.0"
val testcontainersVersion = "1.20.4"
val restAssuredVersion = "5.5.0"
val cucumberVersion = "7.20.1"

dependencies {
    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-hateoas")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // OpenAPI / Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // Mapping
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // Cache
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.0")

    // PDF generation
    implementation("com.github.librepdf:openpdf:2.0.3")

    // CSV generation
    implementation("org.apache.commons:commons-csv:1.10.0")

    // Observability & Logging
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("io.rest-assured:rest-assured:$restAssuredVersion")
    testImplementation("io.cucumber:cucumber-java:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-spring:$cucumberVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.tngtech.archunit:archunit-junit5:$archunitVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// JaCoCo configuration
jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.40".toBigDecimal()
            }
        }
    }
}

// TODO: Re-enable once business logic and tests are in place
// tasks.check {
//     dependsOn(tasks.jacocoTestCoverageVerification)
// }

// SonarQube configuration
sonarqube {
    properties {
        property("sonar.projectKey", "credit-scoring-engine")
        property("sonar.projectName", "Credit Scoring Engine")
        property("sonar.java.coveragePlugin", "jacoco")
        property("sonar.coverage.jacoco.xmlReportPaths", "${layout.buildDirectory}/reports/jacoco/test/jacocoTestReport.xml")

        // Excluir reglas Oracle/PLSQL en migraciones Flyway — el proyecto usa PostgreSQL,
        // donde VARCHAR es el tipo correcto (VARCHAR2 no existe). Ídem duplicados en seed data.
        property("sonar.issue.ignore.multicriteria", "plsql1,plsql2,seed1")
        property("sonar.issue.ignore.multicriteria.plsql1.ruleKey", "plsql:*")
        property("sonar.issue.ignore.multicriteria.plsql1.resourceKey", "**/db/migration/*.sql")
        property("sonar.issue.ignore.multicriteria.plsql2.ruleKey", "flyway:*")
        property("sonar.issue.ignore.multicriteria.plsql2.resourceKey", "**/db/migration/*.sql")
        // Literales duplicados en seed data SQL son inherentes (filas con mismos valores).
        property("sonar.issue.ignore.multicriteria.seed1.ruleKey", "*:S1192")
        property("sonar.issue.ignore.multicriteria.seed1.resourceKey", "**/db/migration/V15__seed_data.sql")
    }
}

tasks.withType<Test> {
    // Only apply WSL Docker socket on local dev (not in CI)
    if (System.getenv("CI") == null) {
        environment("DOCKER_HOST", "unix:///mnt/wsl/docker-desktop-bind-mounts/Ubuntu-24.04/docker.sock")
    }
    environment("TESTCONTAINERS_RYUK_DISABLED", "true")
    systemProperty("api.version", "1.41")
}
