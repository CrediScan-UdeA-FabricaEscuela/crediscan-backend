package co.udea.codefactory.creditscoring.shared.exception;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;

/**
 * Verifica que {@link GlobalExceptionHandler} traduce a español los errores de
 * conversión de parámetros de request (p. ej. un enum inválido en {@code ?nivel=...}).
 *
 * <p>Reproduce el escenario real reportado: un parámetro multi-valor
 * {@code @RequestParam List<RiskLevel>} con un valor que no corresponde a ninguna
 * constante del enum. Sin el handler, Spring devolvía el mensaje por defecto en inglés
 * ("Failed to convert 'nivel' with value: 'ALTO'").</p>
 */
class GlobalExceptionHandlerTypeMismatchTest {

    private MockMvc mockMvc;

    @RestController
    static class TestController {
        @GetMapping("/test/niveles")
        public String filtrar(@RequestParam("nivel") List<RiskLevel> niveles) {
            return "ok:" + niveles.size();
        }
    }

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void nivelInvalido_devuelve400ConMensajeEnEspanol() throws Exception {
        mockMvc.perform(get("/test/niveles").param("nivel", "ALTO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parámetro inválido"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.detail", containsString("nivel")))
                .andExpect(jsonPath("$.detail", containsString("valor inválido")))
                .andExpect(jsonPath("$.detail", containsString("Valores permitidos")))
                .andExpect(jsonPath("$.detail", containsString("HIGH")));
    }

    @Test
    void nivelValido_pasaSinError() throws Exception {
        mockMvc.perform(get("/test/niveles").param("nivel", "HIGH"))
                .andExpect(status().isOk());
    }
}
