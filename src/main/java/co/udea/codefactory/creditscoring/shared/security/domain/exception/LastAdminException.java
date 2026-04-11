package co.udea.codefactory.creditscoring.shared.security.domain.exception;

import co.udea.codefactory.creditscoring.shared.exception.DomainException;

public class LastAdminException extends DomainException {

    public LastAdminException() {
        super("No se puede modificar el último administrador del sistema");
    }

    @Override
    public int httpStatusCode() {
        return 409;
    }

    @Override
    public String errorCode() {
        return "LAST_ADMIN";
    }
}
