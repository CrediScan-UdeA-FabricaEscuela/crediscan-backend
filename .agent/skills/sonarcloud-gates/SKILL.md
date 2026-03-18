---
name: sonarcloud-gates
description: >
  Guides SonarCloud/SonarQube setup and quality gate configuration for Spring Boot projects.
  Trigger: When configuring SonarCloud, quality gates, code quality rules, JaCoCo, or cyclomatic/cognitive complexity.
license: Apache-2.0
metadata:
  author: gentleman-programming
  version: "1.0"
---

## When to Use

- Setting up SonarCloud/SonarQube for a Spring Boot project
- Configuring quality gates and thresholds
- Adding JaCoCo coverage reporting
- Integrating SonarCloud with GitHub Actions
- Reducing cyclomatic or cognitive complexity
- Managing technical debt
- Configuring code quality rules for Java

## Critical Patterns

### Mandatory Quality Gate Thresholds

| Metric | Threshold | Rationale |
|--------|-----------|-----------|
| Unit test coverage | >= 40% | Minimum viable safety net |
| Technical debt | <= 2 days | Keeps codebase maintainable |
| Cyclomatic complexity | < 50 per method | Testability and readability |
| Issue severity | Minor or better | No Major, Critical, or Blocker issues |
| Critical vulnerabilities | 0 | Non-negotiable security baseline |

### SonarCloud Project Properties

Every project MUST have `sonar-project.properties` at the root:

```properties
sonar.projectKey=<org>_<repo>
sonar.organization=<org>
sonar.host.url=https://sonarcloud.io

# Source and test directories
sonar.sources=src/main/java
sonar.tests=src/test/java
sonar.java.binaries=build/classes/java/main
sonar.java.test.binaries=build/classes/java/test

# Coverage report path (JaCoCo)
sonar.coverage.jacoco.xmlReportPaths=build/reports/jacoco/test/jacocoTestReport.xml

# Encoding
sonar.sourceEncoding=UTF-8

# Exclusions (DTOs, config, generated code)
sonar.exclusions=**/dto/**,**/config/**,**/generated/**,**/*Application.java
sonar.coverage.exclusions=**/dto/**,**/config/**,**/entity/**,**/exception/**,**/*Application.java
```

### Gradle Plugin Setup (org.sonarqube)

```groovy
// build.gradle
plugins {
    id 'org.sonarqube' version '5.1.0.4882'
    id 'jacoco'
}

sonar {
    properties {
        property 'sonar.projectKey', '<org>_<repo>'
        property 'sonar.organization', '<org>'
        property 'sonar.host.url', 'https://sonarcloud.io'
        property 'sonar.coverage.jacoco.xmlReportPaths',
                 "${buildDir}/reports/jacoco/test/jacocoTestReport.xml"
    }
}

// Ensure JaCoCo report generates before Sonar analysis
tasks.named('sonar').configure {
    dependsOn tasks.named('jacocoTestReport')
}
```

### Maven Plugin Setup

```xml
<!-- pom.xml -->
<properties>
    <sonar.organization>your-org</sonar.organization>
    <sonar.host.url>https://sonarcloud.io</sonar.host.url>
    <sonar.projectKey>your-org_your-repo</sonar.projectKey>
    <sonar.coverage.jacoco.xmlReportPaths>
        ${project.build.directory}/site/jacoco/jacoco.xml
    </sonar.coverage.jacoco.xmlReportPaths>
</properties>

<build>
    <plugins>
        <plugin>
            <groupId>org.sonarsource.scanner.maven</groupId>
            <artifactId>sonar-maven-plugin</artifactId>
            <version>4.0.0.4121</version>
        </plugin>
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.12</version>
            <executions>
                <execution>
                    <goals><goal>prepare-agent</goal></goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals><goal>report</goal></goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

---

## JaCoCo Configuration

### Gradle Setup

```groovy
jacoco {
    toolVersion = '0.8.12'
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true   // Required for SonarCloud
        html.required = true  // For local review
        csv.required = false
    }

    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                '**/dto/**',
                '**/config/**',
                '**/entity/**',
                '**/exception/**',
                '**/model/**',
                '**/*Application*',
                '**/generated/**'
            ])
        }))
    }
}

jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.40  // 40% minimum coverage
            }
        }
    }
}

// Enforce coverage on build
check.dependsOn jacocoTestCoverageVerification
```

### Maven Setup

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <configuration>
        <excludes>
            <exclude>**/dto/**</exclude>
            <exclude>**/config/**</exclude>
            <exclude>**/entity/**</exclude>
            <exclude>**/exception/**</exclude>
            <exclude>**/model/**</exclude>
            <exclude>**/*Application*</exclude>
            <exclude>**/generated/**</exclude>
        </excludes>
    </configuration>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.40</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## Custom Quality Gate Configuration in SonarCloud

To configure a custom quality gate in SonarCloud:

1. Go to **Organization Settings > Quality Gates**
2. Click **Create** to make a new gate
3. Add these conditions:

| Metric | Operator | Value |
|--------|----------|-------|
| Coverage on New Code | is less than | 40% |
| Maintainability Rating on New Code | is worse than | A |
| Reliability Rating on New Code | is worse than | A |
| Security Rating on New Code | is worse than | A |
| Security Hotspots Reviewed on New Code | is less than | 100% |
| Duplicated Lines (%) on New Code | is greater than | 3% |

4. Set this gate as the **default** for all projects, or assign it per project under **Project Settings > Quality Gate**

---

## Code Quality Rules

### Java Rules to Enable/Customize

| Rule | Key | Action |
|------|-----|--------|
| Cognitive complexity too high | `java:S3776` | Set threshold to 15 |
| Cyclomatic complexity too high | `java:S1541` | Set threshold to 50 |
| Methods should not have too many parameters | `java:S107` | Set max to 7 |
| Classes should not be coupled to too many others | `java:S1200` | Set max to 20 |
| Unused imports | `java:S1128` | Blocker — always remove |
| Empty catch blocks | `java:S108` | Critical — always handle |

### Suppressing False Positives

| Method | When to Use | When NOT to Use |
|--------|-------------|-----------------|
| `@SuppressWarnings("java:SXXXX")` | Specific, justified suppression with a comment explaining why | Blanket suppression to hide real issues |
| `//NOSONAR` | Legacy code during migration ONLY, with a tech debt ticket | New code — NEVER acceptable |
| SonarCloud "Won't Fix" | After team review confirms false positive | Without team consensus |

**Rule**: Every suppression MUST have a comment explaining the justification. No exceptions.

### Security Hotspot Review Process

1. SonarCloud flags potential security issues as "hotspots"
2. A developer reviews each hotspot and marks it as **Safe**, **Fixed**, or **To Fix**
3. Hotspots MUST be reviewed before merging — the quality gate requires 100% review on new code
4. Document the reasoning when marking as "Safe"

---

## Cyclomatic Complexity

**What it is**: Number of linearly independent paths through a method. Each `if`, `for`, `while`, `case`, `&&`, `||` adds +1.

**How SonarCloud measures it**: Counts decision points per method. Rule `java:S1541`.

### Strategies to Reduce (Target: < 50 per method)

| Strategy | Before | After |
|----------|--------|-------|
| Extract method | Giant method with 10 ifs | Small methods with 2-3 ifs each |
| Use polymorphism | `switch` on type | Strategy/Command pattern |
| Simplify conditionals | Nested `if/else` chains | Guard clauses with early returns |
| Use Map lookups | `switch` for mapping values | `Map.of(key, value)` |
| Replace conditionals with Optional | Null checks everywhere | `Optional.map().orElse()` |

---

## Cognitive Complexity

**Difference from cyclomatic**: Cyclomatic counts paths. Cognitive counts how hard it is for a HUMAN to understand. Nesting adds extra penalty — a nested `if` inside a `for` inside a `try` scores much higher.

### Strategies to Reduce

| Strategy | Example |
|----------|---------|
| Flatten nested ifs | Replace `if (a) { if (b) { ... } }` with `if (!a) return; if (!b) return; ...` |
| Use early returns | Guard clauses at method start instead of wrapping in `else` |
| Extract complex conditions | `if (isEligibleForDiscount(customer))` instead of inline boolean chain |
| Break into smaller methods | Each method does ONE thing at ONE nesting level |
| Use Stream API | Replace nested loops with `stream().filter().map().collect()` |

---

## Technical Debt

**How SonarCloud calculates it**: Estimates remediation time for all issues. Each rule violation has an assigned time cost (e.g., "fix this unused variable = 5 min").

### Strategies to Keep Under 2 Days

1. **Fix issues as they appear** — never let debt accumulate across sprints
2. **Prioritize by severity** — Critical > Major > Minor
3. **Focus on new code** — apply "Clean as You Code" principle
4. **Exclude generated code** — DTOs, mappers, config should not count
5. **Allocate 10-15% of sprint capacity** to debt reduction
6. **Track trend** — if debt is increasing sprint-over-sprint, stop and address

### Prioritization Matrix

| Severity | Action | Timeline |
|----------|--------|----------|
| Blocker | Fix immediately | Same day |
| Critical | Fix before merge | Same PR |
| Major | Fix in current sprint | This sprint |
| Minor | Schedule for next sprint | Backlog |
| Info | Evaluate, may ignore | Optional |

---

## CI Integration

### GitHub Actions Workflow

```yaml
# .github/workflows/sonarcloud.yml
name: SonarCloud Analysis

on:
  push:
    branches: [main, develop]
  pull_request:
    types: [opened, synchronize, reopened]

jobs:
  sonarcloud:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0  # Full history for accurate blame

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Run tests with coverage
        run: ./gradlew test jacocoTestReport

      - name: SonarCloud Scan
        uses: SonarSource/sonarqube-scan-action@v5
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}

      - name: SonarQube Quality Gate check
        uses: SonarSource/sonarqube-quality-gate-action@v1
        timeout-minutes: 5
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

### PR Decoration

SonarCloud automatically decorates PRs when:
1. The GitHub App is installed on the repository
2. `SONAR_TOKEN` secret is configured
3. PR analysis is triggered via the workflow above

The decoration shows: quality gate status, new issues, coverage delta, and duplications.

### Branch Analysis Configuration

```properties
# sonar-project.properties
# Long-lived branches (analyzed independently)
sonar.branch.longLivedBranches.regex=main|develop|release/.*

# Short-lived branches get compared against their target
# This is automatic for PRs via GitHub integration
```

## Commands

```bash
# Gradle - run Sonar analysis locally
./gradlew test jacocoTestReport sonar \
  -Dsonar.token=$SONAR_TOKEN

# Maven - run Sonar analysis locally
mvn clean verify sonar:sonar \
  -Dsonar.token=$SONAR_TOKEN

# Gradle - check coverage locally
./gradlew test jacocoTestReport jacocoTestCoverageVerification

# Maven - check coverage locally
mvn clean verify

# View JaCoCo HTML report (Gradle)
open build/reports/jacoco/test/html/index.html

# View JaCoCo HTML report (Maven)
open target/site/jacoco/index.html
```
