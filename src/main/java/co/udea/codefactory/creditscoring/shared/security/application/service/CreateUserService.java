package co.udea.codefactory.creditscoring.shared.security.application.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.shared.security.domain.exception.DuplicateUserException;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AppUser;
import co.udea.codefactory.creditscoring.shared.security.domain.model.Role;
import co.udea.codefactory.creditscoring.shared.security.domain.port.in.CreateUserUseCase;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AppUserRepositoryPort;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AuditLogPort;

@Service
public class CreateUserService implements CreateUserUseCase {

    private final AppUserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogPort auditLog;

    public CreateUserService(
            AppUserRepositoryPort userRepository,
            PasswordEncoder passwordEncoder,
            AuditLogPort auditLog) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLog = auditLog;
    }

    @Override
    @Transactional
    public AppUser create(String username, String email, String rawPassword, Role role, String actor) {
        userRepository.findByUsername(username).ifPresent(existing -> {
            throw DuplicateUserException.byUsername(username);
        });

        userRepository.findByEmail(email).ifPresent(existing -> {
            throw DuplicateUserException.byEmail(email);
        });

        String passwordHash = passwordEncoder.encode(rawPassword);

        AppUser user = new AppUser(
                UUID.randomUUID(),
                username,
                email,
                passwordHash,
                role,
                true,
                false);

        AppUser saved = userRepository.save(user, actor);

        auditLog.record("USER", saved.id(), "CREATE", actor, null,
                Map.of("username", saved.username(), "email", saved.email(), "role", saved.role().name()));

        return saved;
    }
}
