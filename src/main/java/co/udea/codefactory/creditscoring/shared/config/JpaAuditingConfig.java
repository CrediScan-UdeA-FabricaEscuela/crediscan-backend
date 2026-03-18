package co.udea.codefactory.creditscoring.shared.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Enables JPA Auditing and provides the current auditor for {@code @CreatedBy}
 * and {@code @LastModifiedBy} fields.
 *
 * <p>Falls back to "system" when no authenticated user is available (e.g., during
 * migrations or background jobs). Will be enhanced once Spring Security is fully
 * configured with real authentication.</p>
 */
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
