package co.udea.codefactory.creditscoring.shared.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Central JPA configuration for the entire application.
 *
 * <p>This configuration disables Spring Boot's automatic JPA repository detection
 * and provides a single, centralized EntityManagerFactory for all repositories.</p>
 */
@Configuration
@EnableJpaRepositories(
    basePackages = {
        "co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.out.persistence",
        "co.udea.codefactory.creditscoring.shared.security.infrastructure.persistence",
        "co.udea.codefactory.creditscoring.financialdata.infrastructure.adapter.out.persistence",
        "co.udea.codefactory.creditscoring.scoring.infrastructure.adapter.out.persistence",
        "co.udea.codefactory.creditscoring.scoringmodel.infrastructure.adapter.out.persistence",
        "co.udea.codefactory.creditscoring.scoringengine.infrastructure.adapter.out.persistence",
        "co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence",
        "co.udea.codefactory.creditscoring.creditdecision.infrastructure.adapter.out.persistence"
    },
    enableDefaultTransactions = true
)
@EntityScan(basePackages = {
    "co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.out.persistence",
    "co.udea.codefactory.creditscoring.shared.security.infrastructure.persistence",
    "co.udea.codefactory.creditscoring.financialdata.infrastructure.adapter.out.persistence",
    "co.udea.codefactory.creditscoring.scoring.infrastructure.adapter.out.persistence",
    "co.udea.codefactory.creditscoring.scoringmodel.infrastructure.adapter.out.persistence",
    "co.udea.codefactory.creditscoring.scoringengine.infrastructure.adapter.out.persistence",
    "co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence",
    "co.udea.codefactory.creditscoring.creditdecision.infrastructure.adapter.out.persistence"
})
public class JpaConfig {
}
