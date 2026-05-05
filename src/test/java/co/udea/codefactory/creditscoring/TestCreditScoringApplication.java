package co.udea.codefactory.creditscoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Entry point for running the application locally with a Testcontainers-managed PostgreSQL.
 * Run with: ./gradlew bootTestRun
 */
public class TestCreditScoringApplication {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "test");
        SpringApplication.from(CreditScoringApplication::main)
                .with(LocalDevConfig.class)
                .run(args);
    }

    @Configuration
    static class LocalDevConfig {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgresContainer() {
            return new PostgreSQLContainer<>("postgres:16-alpine");
        }
    }
}
