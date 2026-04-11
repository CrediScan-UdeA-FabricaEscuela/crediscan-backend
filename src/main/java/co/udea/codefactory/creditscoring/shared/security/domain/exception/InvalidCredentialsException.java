package co.udea.codefactory.creditscoring.shared.security.domain.exception;

import co.udea.codefactory.creditscoring.shared.exception.DomainException;

public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("Credenciales inválidas");
    }

    @Override
    public int httpStatusCode() {
        return 401;
    }

    @Override
    public String errorCode() {
        return "INVALID_CREDENTIALS";
    }
}
