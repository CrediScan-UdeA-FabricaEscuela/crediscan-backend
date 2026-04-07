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

import co.udea.codefactory.creditscoring.applicant.domain.exception.ApplicantValidationException;
import co.udea.codefactory.creditscoring.applicant.domain.exception.DuplicateApplicantException;
import co.udea.codefactory.creditscoring.applicant.domain.exception.ImmutableFieldException;
import co.udea.codefactory.creditscoring.financialdata.domain.exception.InvalidFinancialDataException;
import co.udea.codefactory.creditscoring.shared.security.domain.exception.DuplicateUserException;
import co.udea.codefactory.creditscoring.shared.security.domain.exception.InvalidCredentialsException;
import co.udea.codefactory.creditscoring.shared.security.domain.exception.LastAdminException;

/**
 * Global exception handler that produces RFC 7807 Problem Detail responses.
 *
 * <p>Every error response includes a {@code traceId} (from MDC), {@code timestamp},
 * and a machine-readable {@code errorCode} for client-side error mapping.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://api.creditscoring.udea.co/errors/validation"));
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

        if (request instanceof ServletWebRequest servletRequest) {
            problem.setProperty("path", servletRequest.getRequest().getRequestURI());
        }

        log.warn("Validation failed: {} field error(s)", fieldErrors.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(ImmutableFieldException.class)
    public ProblemDetail handleImmutableField(ImmutableFieldException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://api.creditscoring.udea.co/errors/immutable-field"));
        problem.setProperty("errorCode", "IMMUTABLE_FIELD");
        problem.setProperty("traceId", MDC.get("traceId"));
        problem.setProperty("timestamp", Instant.now().toString());
        enrichWithPath(problem, request);

        log.warn("Immutable field edit attempt: {}", ex.getFieldName());
        return problem;
    }

    @ExceptionHandler(ApplicantValidationException.class)
    public ProblemDetail handleApplicantValidation(ApplicantValidationException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://api.creditscoring.udea.co/errors/validation"));
        problem.setProperty("errorCode", "VALIDATION_FAILED");
        problem.setProperty("traceId", MDC.get("traceId"));
        problem.setProperty("timestamp", Instant.now().toString());
        enrichWithPath(problem, request);

        log.warn("Applicant validation error: {}", ex.getMessage());
        return problem;
    }

    @ExceptionHandler(InvalidFinancialDataException.class)
    public ProblemDetail handleInvalidFinancialData(InvalidFinancialDataException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://api.creditscoring.udea.co/errors/validation"));
        problem.setProperty("errorCode", "VALIDATION_FAILED");
        problem.setProperty("traceId", MDC.get("traceId"));
        problem.setProperty("timestamp", Instant.now().toString());
        enrichWithPath(problem, request);

        log.warn("Financial data validation error: {}", ex.getMessage());
        return problem;
    }

    @ExceptionHandler(DuplicateApplicantException.class)
    public ProblemDetail handleDuplicateApplicant(DuplicateApplicantException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflict");
        problem.setType(URI.create("https://api.creditscoring.udea.co/errors/conflict"));
        problem.setProperty("errorCode", "DUPLICATE_RESOURCE");
        problem.setProperty("traceId", MDC.get("traceId"));
        problem.setProperty("timestamp", Instant.now().toString());
        enrichWithPath(problem, request);

        log.warn("Duplicate applicant: {}", ex.getMessage());
        return problem;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create("https://api.creditscoring.udea.co/errors/not-found"));
        problem.setProperty("errorCode", "RESOURCE_NOT_FOUND");
        problem.setProperty("traceId", MDC.get("traceId"));
        problem.setProperty("timestamp", Instant.now().toString());
        enrichWithPath(problem, request);

        log.warn("Resource not found: {}", ex.getMessage());
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "No tiene permisos para acceder a este recurso");
        problem.setTitle("Access Denied");
        problem.setType(URI.create("https://api.creditscoring.udea.co/errors/forbidden"));
        problem.setProperty("errorCode", "ACCESS_DENIED");
        problem.setProperty("message", "No tiene permisos para acceder a este recurso");
        problem.setProperty("traceId", MDC.get("traceId"));
        problem.setProperty("timestamp", Instant.now().toString());
        enrichWithPath(problem, request);

        log.warn("Access denied: {}", ex.getMessage());
        return problem;
    }

    @ExceptionHandler(LastAdminException.class)
    public ProblemDetail handleLastAdmin(LastAdminException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflict");
        problem.setType(URI.create("https://api.creditscoring.udea.co/errors/last-admin"));
        problem.setProperty("errorCode", "LAST_ADMIN");
        problem.setProperty("message", ex.getMessage());
        problem.setProperty("traceId", MDC.get("traceId"));
        problem.setProperty("timestamp", Instant.now().toString());
        enrichWithPath(problem, request);

        log.warn("Last admin protection triggered: {}", ex.getMessage());
        return problem;
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ProblemDetail handleDuplicateUser(DuplicateUserException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflict");
        problem.setType(URI.create("https://api.creditscoring.udea.co/errors/duplicate-user"));
        problem.setProperty("errorCode", "DUPLICATE_USER");
        problem.setProperty("traceId", MDC.get("traceId"));
        problem.setProperty("timestamp", Instant.now().toString());
        enrichWithPath(problem, request);

        log.warn("Duplicate user: {}", ex.getMessage());
        return problem;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Unauthorized");
        problem.setType(URI.create("https://api.creditscoring.udea.co/errors/unauthorized"));
        problem.setProperty("errorCode", "INVALID_CREDENTIALS");
        problem.setProperty("traceId", MDC.get("traceId"));
        problem.setProperty("timestamp", Instant.now().toString());
        enrichWithPath(problem, request);

        log.warn("Invalid credentials attempt");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://api.creditscoring.udea.co/errors/internal"));
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
