package co.udea.codefactory.creditscoring.shared.exception;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cubre los handlers de {@link GlobalExceptionHandler} que no están en
 * {@link GlobalExceptionHandlerTypeMismatchTest}:
 * - handleAccessDenied
 * - handleGenericException
 * - handleDomainException
 * - handleMethodArgumentNotValid
 */
class GlobalExceptionHandlerOtherHandlersTest {

    private MockMvc mockMvc;

    // --------------------------------------------------------------------------
    // Controlador de prueba
    // --------------------------------------------------------------------------

    @RestController
    static class TestController {

        @GetMapping("/test/access-denied")
        public String accessDenied() {
            throw new AccessDeniedException("No tienes permiso");
        }

        @GetMapping("/test/generic-error")
        public String genericError() throws Exception {
            throw new RuntimeException("Error inesperado");
        }

        @GetMapping("/test/domain-error")
        public String domainError() {
            throw new TestDomainException("Error de dominio de prueba");
        }

        @GetMapping("/test/domain-error-404")
        public String domainError404() {
            throw new TestDomainException404("Recurso no encontrado");
        }

        @PostMapping("/test/validation")
        public String validar(@Valid @RequestBody ValidRequest request) {
            return "ok";
        }
    }

    /** Excepción de dominio de prueba con 400 */
    static class TestDomainException extends DomainException {
        TestDomainException(String message) { super(message); }

        @Override public int httpStatusCode() { return 400; }
        @Override public String errorCode() { return "TEST_ERROR"; }
    }

    /** Excepción de dominio de prueba con 404 */
    static class TestDomainException404 extends DomainException {
        TestDomainException404(String message) { super(message); }

        @Override public int httpStatusCode() { return 404; }
        @Override public String errorCode() { return "NOT_FOUND_TEST"; }
    }

    record ValidRequest(@NotBlank String campo) {}

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // --------------------------------------------------------------------------
    // handleAccessDenied
    // --------------------------------------------------------------------------

    @Test
    void accessDenied_devuelve403ConErrorCodeAccessDenied() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.title").value("Access Denied"));
    }

    @Test
    void accessDenied_detalleContieneMessageDePermisos() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail", containsString("permisos")));
    }

    // --------------------------------------------------------------------------
    // handleGenericException
    // --------------------------------------------------------------------------

    @Test
    void genericException_devuelve500ConErrorCodeInternalError() throws Exception {
        mockMvc.perform(get("/test/generic-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.title").value("Internal Server Error"));
    }

    @Test
    void genericException_detalleMensajeUnexpectedError() throws Exception {
        mockMvc.perform(get("/test/generic-error"))
                .andExpect(jsonPath("$.detail", containsString("unexpected")));
    }

    // --------------------------------------------------------------------------
    // handleDomainException
    // --------------------------------------------------------------------------

    @Test
    void domainException_con400_devuelveBadRequestConErrorCode() throws Exception {
        mockMvc.perform(get("/test/domain-error"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TEST_ERROR"))
                .andExpect(jsonPath("$.detail", containsString("Error de dominio de prueba")));
    }

    @Test
    void domainException_con404_devuelveNotFound() throws Exception {
        mockMvc.perform(get("/test/domain-error-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND_TEST"));
    }

    @Test
    void domainException_tipoEnUrlUsaSlugConGuion() throws Exception {
        // errorCode "TEST_ERROR" → slug "test-error"
        mockMvc.perform(get("/test/domain-error"))
                .andExpect(jsonPath("$.type", containsString("test-error")));
    }

    // --------------------------------------------------------------------------
    // handleMethodArgumentNotValid
    // --------------------------------------------------------------------------

    @Test
    void methodArgumentNotValid_devuelve400ConValidationFailed() throws Exception {
        // Campo vacío → @NotBlank falla
        String json = "{\"campo\": \"\"}";
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    void methodArgumentNotValid_incluyeDetallesDelCampoInvalido() throws Exception {
        String json = "{\"campo\": \"\"}";
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details[0].field").value("campo"));
    }
}
