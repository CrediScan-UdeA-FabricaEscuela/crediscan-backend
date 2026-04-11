package co.udea.codefactory.creditscoring.shared.exception;

/**
 * Clase base para todas las excepciones de dominio del sistema.
 *
 * <p>Permite que el {@code GlobalExceptionHandler} centralice el manejo
 * de errores sin importar tipos de módulos específicos (OCP).
 * Las subclases sobreescriben {@link #httpStatusCode()} y {@link #errorCode()}
 * para personalizar la respuesta HTTP.</p>
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }

    public int httpStatusCode() {
        return 400;
    }

    public String errorCode() {
        return "DOMAIN_ERROR";
    }
}
