package co.udea.codefactory.creditscoring.shared.security.infrastructure.jwt;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import co.udea.codefactory.creditscoring.shared.security.domain.model.AppUser;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AppUserRepositoryPort;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.TokenBlacklistPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AppUserRepositoryPort userRepository;
    private final TokenBlacklistPort tokenBlacklist;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            AppUserRepositoryPort userRepository,
            TokenBlacklistPort tokenBlacklist) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        Claims claims;
        try {
            claims = jwtService.extractClaims(token);
        } catch (JwtException ex) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        String jti = claims.getId();
        if (tokenBlacklist.existsByJti(jti)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        String username = claims.getSubject();
        AppUser user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !user.enabled() || user.accountLocked()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        if (tokenBlacklist.isUserBlacklisted(user.id())) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        String role = claims.get("role", String.class);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        var authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
