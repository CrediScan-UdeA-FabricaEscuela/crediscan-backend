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
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AuditLogPort;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.TokenPort;

// Servicio de aplicación para autenticación. Usa TokenPort para desacoplarse de la infra JWT
@Service
public class AuthenticateService implements AuthenticateUseCase {

    private final AuthenticationManager authenticationManager;
    private final TokenPort tokenPort;
    private final AppUserRepositoryPort userRepository;
    private final AuditLogPort auditLog;

    public AuthenticateService(
            AuthenticationManager authenticationManager,
            TokenPort tokenPort,
            AppUserRepositoryPort userRepository,
            AuditLogPort auditLog) {
        this.authenticationManager = authenticationManager;
        this.tokenPort = tokenPort;
        this.userRepository = userRepository;
        this.auditLog = auditLog;
    }

    @Override
    public AuthResult authenticate(String username, String password, String actorIp) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (AuthenticationException ex) {
            auditLog.record("USER", null, "LOGIN", username, actorIp, "FAILURE", null, null);
            throw new InvalidCredentialsException();
        }

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);

        // Delega la generación del token y la expiración al puerto correspondiente
        String token = tokenPort.generateToken(user);
        Instant expiresAt = Instant.now().plusMillis(tokenPort.getExpirationMs());

        auditLog.record("USER", null, "LOGIN", username, actorIp, "SUCCESS", null, null);
        return new AuthResult(token, user.role(), expiresAt);
    }
}
