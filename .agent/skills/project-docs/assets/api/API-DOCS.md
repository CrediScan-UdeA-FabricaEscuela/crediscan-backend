# API Documentation Guide — Credit Risk Scoring Engine

> Cubre: Springdoc OpenAPI, export de spec, versionado, Swagger UI.

---

## 1. Dependencia Springdoc

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.7.0</version>
</dependency>
```

---

## 2. Configuración (`application.yml`)

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui/index.html
    enabled: true
    operationsSorter: method
    tagsSorter: alpha
    try-it-out-enabled: true
  show-actuator: false
  default-produces-media-type: application/hal+json
```

### Acceso

| Recurso | URL |
|---------|-----|
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| OpenAPI YAML | `http://localhost:8080/v3/api-docs.yaml` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |

---

## 3. Anotaciones en Controllers

```java
@Tag(name = "Applicants", description = "Gestión de solicitantes de crédito")
@RestController
@RequestMapping("/api/v1/applicants")
public class ApplicantController {

    @Operation(
        summary = "Crear solicitante",
        description = "Registra un nuevo solicitante en el sistema. Requiere rol ANALYST o ADMIN."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Solicitante creado",
            content = @Content(schema = @Schema(implementation = ApplicantResponse.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Documento ya registrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<EntityModel<ApplicantResponse>> create(
            @Valid @RequestBody CreateApplicantRequest request) {
        // ...
    }
}
```

### Documentar DTOs

```java
@Schema(description = "Request para crear un nuevo solicitante")
public record CreateApplicantRequest(

    @Schema(description = "Nombre completo del solicitante", example = "Juan Pérez García")
    @NotBlank
    String nombreCompleto,

    @Schema(description = "Número de documento de identidad", example = "1234567890")
    @NotBlank
    String numeroDocumento,

    @Schema(description = "Tipo de documento", example = "cedula",
            allowableValues = {"cedula", "pasaporte", "nit", "extranjeria"})
    @NotBlank
    String tipoDocumento,

    @Schema(description = "Email de contacto", example = "jperez@email.com")
    @Email @NotBlank
    String email
) {}
```

---

## 4. OpenAPI Info — Clase de Configuración

```java
// com.udea.creditrisk.shared.config.OpenApiConfig.java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI creditRiskOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Credit Risk Scoring Engine API")
                .description("API REST para evaluación de riesgo crediticio. " +
                             "CodeF@ctory Advanced — Universidad de Antioquia.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Equipo CodeF@ctory")
                    .email("creditrisk@udea.edu.co"))
                .license(new License()
                    .name("MIT")
                    .url("https://opensource.org/licenses/MIT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Ingresá el token JWT obtenido en POST /api/v1/auth/login")));
    }
}
```

---

## 5. Exportar la Spec OpenAPI

### Durante el build (Maven)

```xml
<!-- pom.xml — plugin para generar el YAML en CI -->
<plugin>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-maven-plugin</artifactId>
    <version>1.4</version>
    <executions>
        <execution>
            <id>integration-test</id>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <apiDocsUrl>http://localhost:8080/v3/api-docs.yaml</apiDocsUrl>
        <outputFileName>openapi.yaml</outputFileName>
        <outputDir>${project.build.directory}/generated-docs</outputDir>
    </configuration>
</plugin>
```

### Manual (curl)

```bash
# Con la app corriendo localmente
curl http://localhost:8080/v3/api-docs.yaml -o docs/api/openapi.yaml
curl http://localhost:8080/v3/api-docs -o docs/api/openapi.json
```

### Versionado de la spec

El archivo `docs/api/openapi.yaml` se commitea al repositorio y se actualiza en cada sprint.

```
docs/api/
├── API-DOCS.md          # Esta guía
├── openapi.yaml         # Spec generada (actualizar cada sprint)
└── openapi.json         # Alternativa JSON (opcional)
```

---

## 6. Versionado de la API

Ver [ADR-0004](../../adrs/0004-api-versioning-url-path.md).

Cuando se introduce `v2`:
1. Crear nuevo `@RequestMapping("/api/v2/...")` controller.
2. Agregar `@Deprecated` al controller v1.
3. Actualizar `OpenApiConfig` para exponer ambas versiones:

```java
// Separar specs por versión
@Bean
public GroupedOpenApi v1Api() {
    return GroupedOpenApi.builder()
        .group("v1")
        .pathsToMatch("/api/v1/**")
        .build();
}

@Bean
public GroupedOpenApi v2Api() {
    return GroupedOpenApi.builder()
        .group("v2")
        .pathsToMatch("/api/v2/**")
        .build();
}
```

URLs con grupos:
- `/swagger-ui/index.html?urls.primaryName=v1`
- `/v3/api-docs/v1`
- `/v3/api-docs/v2`

---

## 7. Publicar Swagger UI en Render

Swagger UI está incluido en el JAR de Springdoc. En producción (Render):
- URL: `https://{app-name}.onrender.com/swagger-ui/index.html`
- La autenticación JWT se puede probar directamente desde Swagger UI con el botón **Authorize**.

Para deshabilitar en producción (si se requiere seguridad adicional):
```yaml
# application-prod.yml
springdoc:
  swagger-ui:
    enabled: false
```
