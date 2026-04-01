package co.udea.codefactory.creditscoring.shared.security.application.service;

import java.time.Instant;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import co.udea.codefactory.creditscoring.shared.security.domain.exception.InvalidCredentialsException;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AppUser;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AuthResult;
import co.udea.codefactory.creditscoring.shared.security.domain.port.in.AuthenticateUseCase;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AppUserRepositoryPort;
import co.udea.codefactory.creditscoring.shared.security.infrastructure.jwt.JwtProperties;
import co.udea.codefactory.creditscoring.shared.security.infrastructure.jwt.JwtService;

@Service
public class AuthenticateService implements AuthenticateUseCase {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AppUserRepositoryPort userRepository;

    public AuthenticateService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            JwtProperties jwtProperties,
            AppUserRepositoryPort userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.userRepository = userRepository;
    }

    @Override
    public AuthResult authenticate(String username, String password) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException();
        }

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);

        String token = jwtService.generateToken(user);
        Instant expiresAt = Instant.now().plusMillis(jwtProperties.getExpirationMs());

        return new AuthResult(token, user.role(), expiresAt);
    }
}
