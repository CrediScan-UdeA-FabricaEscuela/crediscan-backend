package co.udea.codefactory.creditscoring.shared.security.domain.exception;

public class LastAdminException extends RuntimeException {

    public LastAdminException() {
        super("No se puede modificar el último administrador del sistema");
    }
}
