package co.udea.codefactory.creditscoring.shared.security.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaTokenBlacklistRepository extends JpaRepository<JpaTokenBlacklistEntity, UUID> {

    boolean existsByJti(String jti);

    List<JpaTokenBlacklistEntity> findAllByUserId(UUID userId);

    @Query("SELECT COUNT(t) > 0 FROM JpaTokenBlacklistEntity t WHERE t.userId = :userId AND t.expiresAt > :now")
    boolean existsByUserIdAndExpiresAtAfter(@Param("userId") UUID userId, @Param("now") Instant now);
}
