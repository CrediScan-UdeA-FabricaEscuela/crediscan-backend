package co.udea.codefactory.creditscoring.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration for the Credit Scoring Engine API.
 *
 * <p>Allowed origins are read from the {@code cors.allowed-origins} property,
 * which maps to the {@code CORS_ALLOWED_ORIGINS} environment variable.
 * Defaults to localhost ports for local development.</p>
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:4200,http://localhost:5173}")
    private String[] allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("X-Trace-Id", "Location")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}
