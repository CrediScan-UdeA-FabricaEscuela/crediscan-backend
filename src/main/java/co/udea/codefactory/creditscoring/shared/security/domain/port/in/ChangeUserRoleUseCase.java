package co.udea.codefactory.creditscoring.shared.security.domain.port.in;

import java.util.UUID;

import co.udea.codefactory.creditscoring.shared.security.domain.model.Role;

public interface ChangeUserRoleUseCase {

    void changeRole(UUID userId, Role newRole, String actor);
}
