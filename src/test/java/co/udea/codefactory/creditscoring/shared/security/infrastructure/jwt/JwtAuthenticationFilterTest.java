package co.udea.codefactory.creditscoring.shared.security.infrastructure.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import co.udea.codefactory.creditscoring.shared.security.domain.model.AppUser;
import co.udea.codefactory.creditscoring.shared.security.domain.model.Role;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AppUserRepositoryPort;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.TokenBlacklistPort;
import io.jsonwebtoken.Claims;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TEST_SECRET =
            "dGVzdFNlY3JldEtleUZvckp3dDAxMjM0NTY3ODlBQkNERUY=";

    @Mock
    private AppUserRepositoryPort userRepository;

    @Mock
    private TokenBlacklistPort tokenBlacklist;

    @Mock
    private FilterChain filterChain;

    private JwtProperties jwtProperties;
    private JwtService jwtService;
    private JwtAuthenticationFilter filter;

    private AppUser adminUser;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(TEST_SECRET);
        jwtProperties.setExpirationMs(3_600_000L);

        jwtService = new JwtService(jwtProperties);
        filter = new JwtAuthenticationFilter(jwtService, userRepository, tokenBlacklist);

        adminUser = new AppUser(
                UUID.fromString("a0000000-0000-0000-0000-000000000001"),
                "admin",
                "admin@test.local",
                "$2a$10$hash",
                Role.ADMIN,
                true,
                false);

        SecurityContextHolder.clearContext();
    }

    // --- valid token sets SecurityContext ---

    @Test
    void validToken_shouldSetSecurityContext() throws Exception {
        String token = jwtService.generateToken(adminUser);
        Claims claims = jwtService.extractClaims(token);
        String jti = claims.getId();

        when(tokenBlacklist.existsByJti(jti)).thenReturn(false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("admin");
        verify(filterChain).doFilter(request, response);
    }

    // --- missing Authorization header: chain continues unauthenticated ---

    @Test
    void missingHeader_shouldContinueChainUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    // --- blacklisted JTI: 401 ---

    @Test
    void blacklistedJti_shouldReturn401() throws Exception {
        String token = jwtService.generateToken(adminUser);
        Claims claims = jwtService.extractClaims(token);
        String jti = claims.getId();

        when(tokenBlacklist.existsByJti(jti)).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(request, response);
    }
}
