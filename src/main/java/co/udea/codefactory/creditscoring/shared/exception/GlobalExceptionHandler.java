package co.udea.codefactory.creditscoring.shared.exception;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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
        problem.setProperty("errorCode", "VALIDATION_FAILED");
        problem.setProperty("traceId", MDC.get("traceId"));
        problem.setProperty("timestamp", Instant.now().toString());

        List<Map<String, Object>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.<String, Object>of(
                        "field", fe.getField(),
                        "message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        "rejectedValue", fe.getRejectedValue() != null ? fe.getRejectedValue() : "null"))
                .toList();

        if (!fieldErrors.isEmpty()) {
            Object message = fieldErrors.getFirst().get("message");
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
        String message = ((Exception) ex).getMessage();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(httpStatus, message);
        problem.setTitle(httpStatus.getReasonPhrase());
        problem.setType(URI.create(BASE_ERROR_URI + slug));
        problem.setProperty("errorCode", ex.errorCode());
        problem.setProperty("traceId", MDC.get("traceId"));
        problem.setProperty("timestamp", Instant.now().toString());
        enrichWithPath(problem, request);

        log.warn("Domain exception [{}]: {}", ex.errorCode(), message);
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "No tiene permisos para acceder a este recurso");
        problem.setTitle("Access Denied");
        problem.setType(URI.create(BASE_ERROR_URI + "forbidden"));
        problem.setProperty("errorCode", "ACCESS_DENIED");
        problem.setProperty("message", "No tiene permisos para acceder a este recurso");
        problem.setProperty("traceId", MDC.get("traceId"));
        problem.setProperty("timestamp", Instant.now().toString());
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
        problem.setProperty("errorCode", "INTERNAL_ERROR");
        problem.setProperty("traceId", MDC.get("traceId"));
        problem.setProperty("timestamp", Instant.now().toString());
        enrichWithPath(problem, request);

        log.error("Unhandled exception", ex);
        return problem;
    }

    private void enrichWithPath(ProblemDetail problem, WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            problem.setProperty("path", servletRequest.getRequest().getRequestURI());
        }
    }
}
