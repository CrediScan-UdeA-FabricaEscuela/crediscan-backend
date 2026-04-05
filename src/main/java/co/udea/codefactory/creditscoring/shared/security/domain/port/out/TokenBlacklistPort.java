package co.udea.codefactory.creditscoring.shared.security.domain.port.out;

import java.time.Instant;
import java.util.UUID;

public interface TokenBlacklistPort {

    boolean existsByJti(String jti);

    boolean isUserBlacklisted(UUID userId);

    void blacklistByJti(String jti, UUID userId, Instant expiresAt, String reason);

    void blacklistAllByUserId(UUID userId);
}
