package co.udea.codefactory.creditscoring.shared.security.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.shared.security.domain.model.AppUser;
import co.udea.codefactory.creditscoring.shared.security.domain.model.Role;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AppUserRepositoryPort;

@Component
public class AppUserRepositoryAdapter implements AppUserRepositoryPort {

    private final JpaAppUserRepository jpaRepository;

    public AppUserRepositoryAdapter(JpaAppUserRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<AppUser> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<AppUser> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public Optional<AppUser> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public AppUser save(AppUser user, String actor) {
        JpaAppUserEntity entity = toEntity(user, actor);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public long countByRole(Role role) {
        return jpaRepository.countByRole(role);
    }

    @Override
    public void updateRole(UUID userId, Role newRole) {
        jpaRepository.updateRole(userId, newRole);
    }

    private AppUser toDomain(JpaAppUserEntity entity) {
        return new AppUser(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.isEnabled(),
                entity.isAccountLocked());
    }

    private JpaAppUserEntity toEntity(AppUser user, String actor) {
        JpaAppUserEntity entity = new JpaAppUserEntity();
        entity.setId(user.id());
        entity.setUsername(user.username());
        entity.setEmail(user.email());
        entity.setPasswordHash(user.passwordHash());
        entity.setRole(user.role());
        entity.setEnabled(user.enabled());
        entity.setAccountLocked(user.accountLocked());
        entity.setFailedLoginAttempts(0);
        entity.setPasswordChangedAt(Instant.now());
        entity.setCreatedAt(Instant.now());
        entity.setCreatedBy(actor);
        return entity;
    }
}
