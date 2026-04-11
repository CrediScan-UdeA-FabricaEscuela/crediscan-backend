package co.udea.codefactory.creditscoring.shared.security.domain.exception;

import co.udea.codefactory.creditscoring.shared.exception.DomainException;

public class DuplicateUserException extends DomainException {

    private DuplicateUserException(String message) {
        super(message);
    }

    public static DuplicateUserException byUsername(String username) {
        return new DuplicateUserException(
                "Ya existe un usuario con el nombre de usuario: " + username);
    }

    public static DuplicateUserException byEmail(String email) {
        return new DuplicateUserException(
                "Ya existe un usuario con el correo electrónico: " + email);
    }

    @Override
    public int httpStatusCode() {
        return 409;
    }

    @Override
    public String errorCode() {
        return "DUPLICATE_USER";
    }
}
