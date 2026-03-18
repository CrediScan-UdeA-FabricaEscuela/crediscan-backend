package co.udea.codefactory.creditscoring.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for the Credit Scoring Engine API.
 *
 * <p>Provides a grouped API definition for v1 endpoints and general API metadata
 * visible in the Swagger UI at {@code /swagger-ui.html}.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI creditScoringOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Credit Scoring Engine API")
                        .description("Motor de Scoring de Riesgo Crediticio - REST API for credit risk evaluation and scoring")
                        .version("v1")
                        .contact(new Contact()
                                .name("Code Factory - UdeA")
                                .url("https://udea.edu.co")
                                .email("codefactory@udea.edu.co"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }

    @Bean
    public GroupedOpenApi v1Api() {
        return GroupedOpenApi.builder()
                .group("v1")
                .pathsToMatch("/api/v1/**")
                .build();
    }
}
