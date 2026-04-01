package co.udea.codefactory.creditscoring.shared.security.infrastructure.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.shared.security.domain.model.AppUser;
import co.udea.codefactory.creditscoring.shared.security.domain.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    // 256-bit base64-encoded secret for tests (at least 32 bytes)
    private static final String TEST_SECRET =
            "dGVzdFNlY3JldEtleUZvckp3dDAxMjM0NTY3ODlBQkNERUY=";

    private JwtProperties jwtProperties;
    private JwtService jwtService;
    private AppUser testUser;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(TEST_SECRET);
        jwtProperties.setExpirationMs(3_600_000L); // 1 hour

        jwtService = new JwtService(jwtProperties);

        testUser = new AppUser(
                UUID.fromString("a0000000-0000-0000-0000-000000000001"),
                "admin",
                "admin@test.local",
                "$2a$10$hash",
                Role.ADMIN,
                true,
                false);
    }

    // --- Task 7.1: generate token contains role + JTI claims ---

    @Test
    void generateToken_shouldContainRoleClaim() {
        String token = jwtService.generateToken(testUser);

        Claims claims = jwtService.extractClaims(token);

        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void generateToken_shouldContainJtiClaim() {
        String token = jwtService.generateToken(testUser);

        Claims claims = jwtService.extractClaims(token);

        assertThat(claims.getId()).isNotNull().isNotBlank();
    }

    @Test
    void generateToken_shouldContainSubjectEqualToUsername() {
        String token = jwtService.generateToken(testUser);

        Claims claims = jwtService.extractClaims(token);

        assertThat(claims.getSubject()).isEqualTo("admin");
    }

    // --- expired token rejected ---

    @Test
    void validateToken_shouldRejectExpiredToken() {
        JwtProperties shortLived = new JwtProperties();
        shortLived.setSecret(TEST_SECRET);
        shortLived.setExpirationMs(-1L); // already expired
        JwtService expiredService = new JwtService(shortLived);

        String token = expiredService.generateToken(testUser);

        assertThatThrownBy(() -> expiredService.extractClaims(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    // --- tampered token rejected ---

    @Test
    void validateToken_shouldRejectTamperedToken() {
        String token = jwtService.generateToken(testUser);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtService.extractClaims(tampered))
                .isInstanceOf(JwtException.class);
    }

    // --- two tokens for same user get different JTIs ---

    @Test
    void generateToken_shouldProduceUniqueJtiPerCall() {
        String token1 = jwtService.generateToken(testUser);
        String token2 = jwtService.generateToken(testUser);

        String jti1 = jwtService.extractClaims(token1).getId();
        String jti2 = jwtService.extractClaims(token2).getId();

        assertThat(jti1).isNotEqualTo(jti2);
    }
}
