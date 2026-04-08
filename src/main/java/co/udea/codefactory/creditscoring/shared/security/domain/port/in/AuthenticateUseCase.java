package co.udea.codefactory.creditscoring.shared.security.domain.port.in;

import co.udea.codefactory.creditscoring.shared.security.domain.model.AuthResult;

public interface AuthenticateUseCase {

    AuthResult authenticate(String username, String password, String actorIp);
}
