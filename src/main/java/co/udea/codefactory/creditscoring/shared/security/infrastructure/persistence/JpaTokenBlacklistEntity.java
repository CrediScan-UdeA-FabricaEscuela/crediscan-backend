package co.udea.codefactory.creditscoring.shared.security.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "token_blacklist")
public class JpaTokenBlacklistEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String jti;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "blacklisted_at", nullable = false)
    private Instant blacklistedAt;

    @Column(length = 100)
    private String reason;

    protected JpaTokenBlacklistEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getBlacklistedAt() { return blacklistedAt; }
    public void setBlacklistedAt(Instant blacklistedAt) { this.blacklistedAt = blacklistedAt; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
