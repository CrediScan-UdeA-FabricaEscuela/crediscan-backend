package co.udea.codefactory.creditscoring.shared.security.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    public long countByRole(Role role) {
        return jpaRepository.countByRole(role);
    }

    @Override
    @Transactional
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
}
