package co.udea.codefactory.creditscoring.creditdecision.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests unitarios para las etiquetas en español de {@link DecisionStatus}.
 * Estas etiquetas se usan en los reportes PDF generados.
 */
class DecisionStatusTest {

    @ParameterizedTest(name = "{0} -> \"{1}\"")
    @CsvSource({
        "APPROVED,       Aprobado",
        "REJECTED,       Rechazado",
        "MANUAL_REVIEW,  Revisión Manual",
        "ESCALATED,      Escalado"
    })
    void getEtiqueta_retornaLabelEnEspanol(DecisionStatus status, String expectedLabel) {
        assertThat(status.getEtiqueta()).isEqualTo(expectedLabel);
    }
}
