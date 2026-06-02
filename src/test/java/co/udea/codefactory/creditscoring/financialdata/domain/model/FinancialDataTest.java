package co.udea.codefactory.creditscoring.financialdata.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.financialdata.domain.exception.InvalidFinancialDataException;

class FinancialDataTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID APPLICANT_ID = UUID.randomUUID();
    private static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    // =========================================================================
    // Validaciones del constructor compacto
    // =========================================================================

    @Test
    void constructor_conIdNulo_lanzaExcepcion() {
        assertThatThrownBy(() -> datosBase(null, APPLICANT_ID, 1,
                bd("36000000"), bd("2000000"), bd("5000000"), bd("20000000"), bd("15000000"), 720))
                .isInstanceOf(InvalidFinancialDataException.class)
                .hasMessageContaining("identificador");
    }

    @Test
    void constructor_conApplicantIdNulo_lanzaExcepcion() {
        assertThatThrownBy(() -> datosBase(ID, null, 1,
                bd("36000000"), bd("2000000"), bd("5000000"), bd("20000000"), bd("15000000"), 720))
                .isInstanceOf(InvalidFinancialDataException.class)
                .hasMessageContaining("solicitante");
    }

    @Test
    void constructor_conVersionNegativa_lanzaExcepcion() {
        assertThatThrownBy(() -> datosBase(ID, APPLICANT_ID, -1,
                bd("36000000"), bd("2000000"), bd("5000000"), bd("20000000"), bd("15000000"), 720))
                .isInstanceOf(InvalidFinancialDataException.class)
                .hasMessageContaining("versión");
    }

    @Test
    void constructor_conVersionCero_esValido() {
        FinancialData fd = datosBase(ID, APPLICANT_ID, 0,
                bd("36000000"), bd("2000000"), bd("5000000"), bd("20000000"), bd("15000000"), 720);
        assertThat(fd.version()).isEqualTo(0);
    }

    @Test
    void constructor_conAnnualIncomeNegativo_lanzaExcepcion() {
        assertThatThrownBy(() -> datosBase(ID, APPLICANT_ID, 1,
                bd("-1"), bd("2000000"), bd("5000000"), bd("20000000"), bd("15000000"), 720))
                .isInstanceOf(InvalidFinancialDataException.class)
                .hasMessageContaining("annualIncome");
    }

    @Test
    void constructor_conAnnualIncomeNulo_lanzaExcepcion() {
        assertThatThrownBy(() -> datosBase(ID, APPLICANT_ID, 1,
                null, bd("2000000"), bd("5000000"), bd("20000000"), bd("15000000"), 720))
                .isInstanceOf(InvalidFinancialDataException.class)
                .hasMessageContaining("annualIncome");
    }

    @Test
    void constructor_conMonthlyExpensesNegativo_lanzaExcepcion() {
        assertThatThrownBy(() -> datosBase(ID, APPLICANT_ID, 1,
                bd("36000000"), bd("-1"), bd("5000000"), bd("20000000"), bd("15000000"), 720))
                .isInstanceOf(InvalidFinancialDataException.class)
                .hasMessageContaining("monthlyExpenses");
    }

    @Test
    void constructor_conCurrentDebtsNegativo_lanzaExcepcion() {
        assertThatThrownBy(() -> datosBase(ID, APPLICANT_ID, 1,
                bd("36000000"), bd("2000000"), bd("-1"), bd("20000000"), bd("15000000"), 720))
                .isInstanceOf(InvalidFinancialDataException.class)
                .hasMessageContaining("currentDebts");
    }

    @Test
    void constructor_conAssetsValueNegativo_lanzaExcepcion() {
        assertThatThrownBy(() -> datosBase(ID, APPLICANT_ID, 1,
                bd("36000000"), bd("2000000"), bd("5000000"), bd("-1"), bd("15000000"), 720))
                .isInstanceOf(InvalidFinancialDataException.class)
                .hasMessageContaining("assetsValue");
    }

    @Test
    void constructor_conDeclaredPatrimonyNegativo_lanzaExcepcion() {
        assertThatThrownBy(() -> datosBase(ID, APPLICANT_ID, 1,
                bd("36000000"), bd("2000000"), bd("5000000"), bd("20000000"), bd("-1"), 720))
                .isInstanceOf(InvalidFinancialDataException.class)
                .hasMessageContaining("declaredPatrimony");
    }

    @Test
    void constructor_conScoreBureauNegativo_lanzaExcepcion() {
        assertThatThrownBy(() -> datosBase(ID, APPLICANT_ID, 1,
                bd("36000000"), bd("2000000"), bd("5000000"), bd("20000000"), bd("15000000"), -1))
                .isInstanceOf(InvalidFinancialDataException.class)
                .hasMessageContaining("buro");
    }

    @Test
    void constructor_conScoreBureauMayorA999_lanzaExcepcion() {
        assertThatThrownBy(() -> datosBase(ID, APPLICANT_ID, 1,
                bd("36000000"), bd("2000000"), bd("5000000"), bd("20000000"), bd("15000000"), 1000))
                .isInstanceOf(InvalidFinancialDataException.class)
                .hasMessageContaining("buro");
    }

    @Test
    void constructor_conScoreBureauExactamente999_esValido() {
        FinancialData fd = datosBase(ID, APPLICANT_ID, 1,
                bd("36000000"), bd("2000000"), bd("5000000"), bd("20000000"), bd("15000000"), 999);
        assertThat(fd.externalBureauScore()).isEqualTo(999);
    }

    @Test
    void constructor_conScoreBureauNulo_esValido() {
        FinancialData fd = datosSinScore(ID, APPLICANT_ID, 1);
        assertThat(fd.externalBureauScore()).isNull();
        assertThat(fd.hasExternalBureauScore()).isFalse();
    }

    // =========================================================================
    // debtToIncomeRatio()
    // =========================================================================

    @Test
    void debtToIncomeRatio_conIngresosCero_devuelveCero() {
        FinancialData fd = datosBase(ID, APPLICANT_ID, 1,
                BigDecimal.ZERO, bd("2000000"), bd("5000000"), bd("20000000"), bd("15000000"), 720);
        assertThat(fd.debtToIncomeRatio()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void debtToIncomeRatio_conDeudaYIngresos_calculaCorrectamente() {
        // deuda = 36000000, income = 36000000 → ratio = 100%
        FinancialData fd = datosBase(ID, APPLICANT_ID, 1,
                bd("36000000"), bd("2000000"), bd("36000000"), bd("20000000"), bd("15000000"), 720);
        assertThat(fd.debtToIncomeRatio()).isEqualByComparingTo(new BigDecimal("100.000000"));
    }

    @Test
    void debtToIncomeRatio_conDeudaBaja_noCumpleAlerta() {
        // deuda/ingreso = 5000000/36000000 ≈ 13.88% < 60%
        FinancialData fd = datosBase(ID, APPLICANT_ID, 1,
                bd("36000000"), bd("2000000"), bd("5000000"), bd("20000000"), bd("15000000"), 720);
        assertThat(fd.debtToIncomeAlert()).isFalse();
    }

    @Test
    void debtToIncomeRatio_conDeudaMuyAlta_activaAlerta() {
        // deuda = 25000000, ingreso = 36000000 → ratio ≈ 69.4% > 60%
        FinancialData fd = datosBase(ID, APPLICANT_ID, 1,
                bd("36000000"), bd("2000000"), bd("25000000"), bd("20000000"), bd("15000000"), 720);
        assertThat(fd.debtToIncomeAlert()).isTrue();
    }

    // =========================================================================
    // expenseToIncomeRatio() y expensesExceedMonthlyIncome()
    // =========================================================================

    @Test
    void expenseToIncomeRatio_conIngresosAnualesCero_devuelveCero() {
        FinancialData fd = datosBase(ID, APPLICANT_ID, 1,
                BigDecimal.ZERO, bd("2000000"), BigDecimal.ZERO, bd("20000000"), bd("15000000"), null);
        assertThat(fd.expenseToIncomeRatio()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void expensesExceedMonthlyIncome_conGastosMenoresAlIngresoMensual_retornaFalso() {
        // ingreso mensual = 36000000/12 = 3000000; gastos = 2000000 → no supera
        FinancialData fd = datosBase(ID, APPLICANT_ID, 1,
                bd("36000000"), bd("2000000"), bd("5000000"), bd("20000000"), bd("15000000"), 720);
        assertThat(fd.expensesExceedMonthlyIncome()).isFalse();
    }

    @Test
    void expensesExceedMonthlyIncome_conGastosMayoresAlIngresoMensual_retornaVerdadero() {
        // ingreso mensual = 36000000/12 = 3000000; gastos = 4000000 → supera
        FinancialData fd = datosBase(ID, APPLICANT_ID, 1,
                bd("36000000"), bd("4000000"), bd("5000000"), bd("20000000"), bd("15000000"), 720);
        assertThat(fd.expensesExceedMonthlyIncome()).isTrue();
    }

    // =========================================================================
    // liabilitiesExceedAssetsLimit()
    // =========================================================================

    @Test
    void liabilitiesExceedAssetsLimit_conActivosCero_retornaFalso() {
        // assets = 0 → condición no aplica
        FinancialData fd = datosBase(ID, APPLICANT_ID, 1,
                bd("36000000"), bd("2000000"), bd("5000000"), BigDecimal.ZERO, bd("15000000"), 720);
        assertThat(fd.liabilitiesExceedAssetsLimit()).isFalse();
    }

    @Test
    void liabilitiesExceedAssetsLimit_conDeudaMenorAlLimite_retornaFalso() {
        // assets = 20000000, límite = 100*20000000 = 2e9; deuda = 5000000 → no supera
        FinancialData fd = datosBase(ID, APPLICANT_ID, 1,
                bd("36000000"), bd("2000000"), bd("5000000"), bd("20000000"), bd("15000000"), 720);
        assertThat(fd.liabilitiesExceedAssetsLimit()).isFalse();
    }

    @Test
    void liabilitiesExceedAssetsLimit_conDeudaMuyAlta_retornaVerdadero() {
        // assets = 1, límite = 100*1 = 100; deuda = 101 → supera
        FinancialData fd = datosBase(ID, APPLICANT_ID, 1,
                bd("36000000"), bd("2000000"), bd("101"), bd("1"), bd("15000000"), 720);
        assertThat(fd.liabilitiesExceedAssetsLimit()).isTrue();
    }

    // =========================================================================
    // withVersionAndTimestamps()
    // =========================================================================

    @Test
    void withVersionAndTimestamps_actualizaVersionYTimestamps() {
        FinancialData original = datosBase(ID, APPLICANT_ID, 1,
                bd("36000000"), bd("2000000"), bd("5000000"), bd("20000000"), bd("15000000"), 720);
        OffsetDateTime nueva = AHORA.plusDays(1);

        FinancialData actualizado = original.withVersionAndTimestamps(2, nueva, nueva);

        assertThat(actualizado.version()).isEqualTo(2);
        assertThat(actualizado.createdAt()).isEqualTo(nueva);
        assertThat(actualizado.updatedAt()).isEqualTo(nueva);
        // El resto de campos no cambia
        assertThat(actualizado.annualIncome()).isEqualByComparingTo(bd("36000000"));
    }

    // =========================================================================
    // hasExternalBureauScore()
    // =========================================================================

    @Test
    void hasExternalBureauScore_conScorePresente_retornaVerdadero() {
        FinancialData fd = datosBase(ID, APPLICANT_ID, 1,
                bd("36000000"), bd("2000000"), bd("5000000"), bd("20000000"), bd("15000000"), 750);
        assertThat(fd.hasExternalBureauScore()).isTrue();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private FinancialData datosBase(UUID id, UUID applicantId, int version,
            BigDecimal annualIncome, BigDecimal monthlyExpenses, BigDecimal currentDebts,
            BigDecimal assetsValue, BigDecimal declaredPatrimony, Integer bureauScore) {
        return new FinancialData(id, applicantId, version, annualIncome, monthlyExpenses,
                currentDebts, assetsValue, declaredPatrimony, false, 12, 0, 0,
                bureauScore, 2, AHORA, AHORA);
    }

    private FinancialData datosSinScore(UUID id, UUID applicantId, int version) {
        return new FinancialData(id, applicantId, version, bd("36000000"), bd("2000000"),
                bd("5000000"), bd("20000000"), bd("15000000"), false, 12, 0, 0,
                null, 2, AHORA, AHORA);
    }

    private BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
