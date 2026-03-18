package co.udea.codefactory.creditscoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Credit Risk Scoring Engine application.
 *
 * <p>JPA Auditing is enabled via {@link co.udea.codefactory.creditscoring.shared.config.JpaAuditingConfig}
 * to keep configuration responsibilities separated.</p>
 */
@SpringBootApplication
public class CreditScoringApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreditScoringApplication.class, args);
    }
}
