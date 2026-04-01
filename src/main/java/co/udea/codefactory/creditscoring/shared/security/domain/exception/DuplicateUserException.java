package co.udea.codefactory.creditscoring.shared.security.domain.exception;

public class DuplicateUserException extends RuntimeException {

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
}
