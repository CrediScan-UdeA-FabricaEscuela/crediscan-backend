package co.udea.codefactory.creditscoring.shared.security.domain.port.in;

import co.udea.codefactory.creditscoring.shared.security.domain.model.AppUser;
import co.udea.codefactory.creditscoring.shared.security.domain.model.Role;

public interface CreateUserUseCase {

    AppUser create(String username, String email, String rawPassword, Role role, String actor);
}
