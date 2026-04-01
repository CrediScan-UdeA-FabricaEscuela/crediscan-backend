package co.udea.codefactory.creditscoring.shared.security.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import co.udea.codefactory.creditscoring.shared.security.domain.exception.InvalidCredentialsException;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AppUser;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AuthResult;
import co.udea.codefactory.creditscoring.shared.security.domain.model.Role;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AppUserRepositoryPort;
import co.udea.codefactory.creditscoring.shared.security.infrastructure.jwt.JwtProperties;
import co.udea.codefactory.creditscoring.shared.security.infrastructure.jwt.JwtService;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthenticateServiceTest {

    private static final String TEST_SECRET =
            "dGVzdFNlY3JldEtleUZvckp3dDAxMjM0NTY3ODlBQkNERUY=";

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AppUserRepositoryPort userRepository;

    @Mock
    private Authentication authentication;

    private JwtService jwtService;
    private AuthenticateService service;

    private AppUser adminUser;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(TEST_SECRET);
        props.setExpirationMs(3_600_000L);

        jwtService = new JwtService(props);
        service = new AuthenticateService(authenticationManager, jwtService, props, userRepository);

        adminUser = new AppUser(
                UUID.fromString("a0000000-0000-0000-0000-000000000001"),
                "admin",
                "admin@test.local",
                "$2a$10$hash",
                Role.ADMIN,
                true,
                false);
    }

    // --- valid credentials return token with role ---

    @Test
    void authenticate_withValidCredentials_returnsTokenWithRole() {
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        AuthResult result = service.authenticate("admin", "correct-password");

        assertThat(result.token()).isNotBlank();
        assertThat(result.role()).isEqualTo(Role.ADMIN);
        assertThat(result.expiresAt()).isNotNull();
    }

    // --- bad credentials throw InvalidCredentialsException ---

    @Test
    void authenticate_withBadCredentials_throwsInvalidCredentialsException() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> service.authenticate("admin", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
