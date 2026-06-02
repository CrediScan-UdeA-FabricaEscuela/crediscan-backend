package co.udea.codefactory.creditscoring.shared.exception;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Global exception handler that produces RFC 7807 Problem Detail responses.
 *
 * <p>Domain exceptions are handled generically via {@link DomainException} — no imports
 * from individual modules required (OCP compliant). To integrate a new module's exceptions,
 * implement {@link DomainException} on each exception class and override
 * {@link DomainException#httpStatusCode()} and {@link DomainException#errorCode()} as needed.</p>
 *
 * <p>Every error response includes a {@code traceId} (from MDC), {@code timestamp},
 * and a machine-readable {@code errorCode} for client-side error mapping.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String BASE_ERROR_URI = "https://api.creditscoring.udea.co/errors/";
    private static final String KEY_ERROR_CODE = "errorCode";
    private static final String KEY_TRACE_ID = "traceId";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_MESSAGE = "message";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Validation Error");
        problem.setType(URI.create(BASE_ERROR_URI + "validation"));
        problem.setProperty(KEY_ERROR_CODE, "VALIDATION_FAILED");
        problem.setProperty(KEY_TRACE_ID, MDC.get(KEY_TRACE_ID));
        problem.setProperty(KEY_TIMESTAMP, Instant.now().toString());

        List<Map<String, Object>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.<String, Object>of(
                        "field", fe.getField(),
                        KEY_MESSAGE, fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        "rejectedValue", fe.getRejectedValue() != null ? fe.getRejectedValue() : "null"))
                .toList();

        if (!fieldErrors.isEmpty()) {
            Object message = fieldErrors.getFirst().get(KEY_MESSAGE);
            if (message instanceof String detailMessage) {
                problem.setDetail(detailMessage);
            }
        }
        problem.setProperty("details", fieldErrors);
        enrichWithPath(problem, request);

        log.warn("Validation failed: {} field error(s)", fieldErrors.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Generic handler for all {@link DomainException} implementations.
     * HTTP status and error code are read from the exception itself.
     * New modules only need to implement {@link DomainException} — no changes here required.
     */
    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException ex, WebRequest request) {
        HttpStatus httpStatus = HttpStatus.resolve(ex.httpStatusCode());
        if (httpStatus == null) {
            httpStatus = HttpStatus.BAD_REQUEST;
        }
        String slug = ex.errorCode().toLowerCase().replace('_', '-');
        String message = ex.getMessage();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(httpStatus, message);
        problem.setTitle(httpStatus.getReasonPhrase());
        problem.setType(URI.create(BASE_ERROR_URI + slug));
        problem.setProperty(KEY_ERROR_CODE, ex.errorCode());
        problem.setProperty(KEY_TRACE_ID, MDC.get(KEY_TRACE_ID));
        problem.setProperty(KEY_TIMESTAMP, Instant.now().toString());
        enrichWithPath(problem, request);

        log.warn("Domain exception [{}]: {}", ex.errorCode(), message);
        return problem;
    }

    /**
     * Maneja errores de conversión de parámetros de request (p. ej. un enum inválido en un
     * query param). Sin este handler, Spring devuelve un mensaje en inglés por defecto
     * ("Failed to convert 'nivel' with value: 'ALTO'"). Aquí se traduce a español y, cuando
     * el tipo esperado es un enum, se listan los valores permitidos.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String paramName = ex.getName();
        String value = ex.getValue() != null ? ex.getValue().toString() : "null";

        StringBuilder detail = new StringBuilder()
                .append("El parámetro '").append(paramName)
                .append("' tiene un valor inválido: '").append(value).append("'.");

        Class<?> enumType = resolveEnumType(ex);
        if (enumType != null) {
            String permitidos = Arrays.stream(enumType.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            detail.append(" Valores permitidos: ").append(permitidos).append('.');
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail.toString());
        problem.setTitle("Parámetro inválido");
        problem.setType(URI.create(BASE_ERROR_URI + "invalid-parameter"));
        problem.setProperty(KEY_ERROR_CODE, "INVALID_PARAMETER");
        problem.setProperty(KEY_TRACE_ID, MDC.get(KEY_TRACE_ID));
        problem.setProperty(KEY_TIMESTAMP, Instant.now().toString());
        enrichWithPath(problem, request);

        log.warn("Parámetro inválido [{}]: {}", paramName, ex.getMessage());
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "No tiene permisos para acceder a este recurso");
        problem.setTitle("Access Denied");
        problem.setType(URI.create(BASE_ERROR_URI + "forbidden"));
        problem.setProperty(KEY_ERROR_CODE, "ACCESS_DENIED");
        problem.setProperty(KEY_MESSAGE, "No tiene permisos para acceder a este recurso");
        problem.setProperty(KEY_TRACE_ID, MDC.get(KEY_TRACE_ID));
        problem.setProperty(KEY_TIMESTAMP, Instant.now().toString());
        enrichWithPath(problem, request);

        log.warn("Access denied: {}", ex.getMessage());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create(BASE_ERROR_URI + "internal"));
        problem.setProperty(KEY_ERROR_CODE, "INTERNAL_ERROR");
        problem.setProperty(KEY_TRACE_ID, MDC.get(KEY_TRACE_ID));
        problem.setProperty(KEY_TIMESTAMP, Instant.now().toString());
        enrichWithPath(problem, request);

        log.error("Unhandled exception", ex);
        return problem;
    }

    /**
     * Resuelve el tipo enum esperado por un parámetro, soportando tanto un enum directo
     * (p. ej. {@code RiskLevel}) como una colección de enums (p. ej. {@code List<RiskLevel>}),
     * que es el caso de los filtros multi-valor. Para una colección, {@code getRequiredType()}
     * devuelve {@code List}, así que el tipo del elemento se obtiene del tipo genérico del parámetro.
     * Retorna {@code null} si el parámetro no es un enum ni una colección de enums.
     */
    private Class<?> resolveEnumType(MethodArgumentTypeMismatchException ex) {
        Class<?> required = ex.getRequiredType();
        if (required != null && required.isEnum()) {
            return required;
        }
        Type generic = ex.getParameter().getGenericParameterType();
        if (generic instanceof ParameterizedType parameterized) {
            for (Type arg : parameterized.getActualTypeArguments()) {
                if (arg instanceof Class<?> clazz && clazz.isEnum()) {
                    return clazz;
                }
            }
        }
        return null;
    }

    private void enrichWithPath(ProblemDetail problem, WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            problem.setProperty("path", servletRequest.getRequest().getRequestURI());
        }
    }
}
