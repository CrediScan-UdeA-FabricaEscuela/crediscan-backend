package co.udea.codefactory.creditscoring.shared.security.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import co.udea.codefactory.creditscoring.shared.security.domain.model.AppUser;
import co.udea.codefactory.creditscoring.shared.security.domain.model.Role;

public interface AppUserRepositoryPort {

    Optional<AppUser> findById(UUID id);

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmail(String email);

    long countByRole(Role role);

    void updateRole(UUID userId, Role newRole);

    AppUser save(AppUser user, String actor);
}
