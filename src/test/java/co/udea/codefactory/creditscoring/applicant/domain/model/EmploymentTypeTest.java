package co.udea.codefactory.creditscoring.applicant.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import co.udea.codefactory.creditscoring.applicant.domain.exception.ApplicantValidationException;

class EmploymentTypeTest {

    // =========================================================================
    // fromApiValue() — happy paths
    // =========================================================================

    @Test
    void fromApiValue_conEmpleadoExacto_retornaEMPLEADO() {
        assertThat(EmploymentType.fromApiValue("Empleado")).isEqualTo(EmploymentType.EMPLEADO);
    }

    @Test
    void fromApiValue_conIndependienteExacto_retornaINDEPENDIENTE() {
        assertThat(EmploymentType.fromApiValue("Independiente")).isEqualTo(EmploymentType.INDEPENDIENTE);
    }

    @Test
    void fromApiValue_conPensionadoExacto_retornaPENSIONADO() {
        assertThat(EmploymentType.fromApiValue("Pensionado")).isEqualTo(EmploymentType.PENSIONADO);
    }

    @Test
    void fromApiValue_conDesempleadoExacto_retornaDESEMPLEADO() {
        assertThat(EmploymentType.fromApiValue("Desempleado")).isEqualTo(EmploymentType.DESEMPLEADO);
    }

    // =========================================================================
    // fromApiValue() — case insensitive
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {"empleado", "EMPLEADO", "Empleado", "eMpLeAdO"})
    void fromApiValue_caseInsensitive_retornaEMPLEADO(String valor) {
        assertThat(EmploymentType.fromApiValue(valor)).isEqualTo(EmploymentType.EMPLEADO);
    }

    @Test
    void fromApiValue_conEspaciosAlRededor_losIgnora() {
        assertThat(EmploymentType.fromApiValue("  Empleado  ")).isEqualTo(EmploymentType.EMPLEADO);
    }

    // =========================================================================
    // fromApiValue() — error paths
    // =========================================================================

    @Test
    void fromApiValue_conNulo_lanzaApplicantValidationException() {
        assertThatThrownBy(() -> EmploymentType.fromApiValue(null))
                .isInstanceOf(ApplicantValidationException.class)
                .hasMessageContaining("obligatorio");
    }

    @Test
    void fromApiValue_conCadenaVacia_lanzaApplicantValidationException() {
        assertThatThrownBy(() -> EmploymentType.fromApiValue(""))
                .isInstanceOf(ApplicantValidationException.class)
                .hasMessageContaining("obligatorio");
    }

    @Test
    void fromApiValue_conBlanco_lanzaApplicantValidationException() {
        assertThatThrownBy(() -> EmploymentType.fromApiValue("   "))
                .isInstanceOf(ApplicantValidationException.class)
                .hasMessageContaining("obligatorio");
    }

    @Test
    void fromApiValue_conValorInvalido_lanzaApplicantValidationException() {
        assertThatThrownBy(() -> EmploymentType.fromApiValue("FREELANCER"))
                .isInstanceOf(ApplicantValidationException.class)
                .hasMessageContaining("Valores permitidos");
    }

    // =========================================================================
    // apiValue()
    // =========================================================================

    @Test
    void apiValue_todosLosEnumsTienenApiValueNoNulo() {
        for (EmploymentType tipo : EmploymentType.values()) {
            assertThat(tipo.apiValue()).isNotNull().isNotBlank();
        }
    }

    @Test
    void apiValue_empleadoRetornaEmpleado() {
        assertThat(EmploymentType.EMPLEADO.apiValue()).isEqualTo("Empleado");
    }

    @Test
    void apiValue_independienteRetornaIndependiente() {
        assertThat(EmploymentType.INDEPENDIENTE.apiValue()).isEqualTo("Independiente");
    }

    // =========================================================================
    // allowedValues()
    // =========================================================================

    @Test
    void allowedValues_retornaCuatroValores() {
        assertThat(EmploymentType.allowedValues()).hasSize(4);
    }

    @Test
    void allowedValues_contieneEmpleadoIndependientePensionadoDesempleado() {
        assertThat(EmploymentType.allowedValues())
                .containsExactlyInAnyOrder("Empleado", "Independiente", "Pensionado", "Desempleado");
    }
}
